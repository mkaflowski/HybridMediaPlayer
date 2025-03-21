package hybridmediaplayer;

import android.content.Context;
import android.content.Intent;
import android.media.audiofx.AudioEffect;
import android.net.Uri;
import android.os.Handler;
import android.view.SurfaceView;

import com.google.android.exoplayer2.LoadControl;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.MediaMetadata;
import com.google.android.exoplayer2.PlaybackException;
import com.google.android.exoplayer2.PlaybackParameters;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.analytics.AnalyticsListener;
import com.google.android.exoplayer2.ext.cast.CastPlayer;
import com.google.android.exoplayer2.ext.cast.SessionAvailabilityListener;
import com.google.android.exoplayer2.source.ConcatenatingMediaSource;
import com.google.android.exoplayer2.source.DefaultMediaSourceFactory;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.MediaSourceFactory;
import com.google.android.exoplayer2.source.ProgressiveMediaSource;
import com.google.android.exoplayer2.source.hls.HlsMediaSource;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultDataSource;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSource;
import com.google.android.exoplayer2.upstream.LoadErrorHandlingPolicy;
import com.google.android.exoplayer2.util.MimeTypes;
import com.google.android.gms.cast.MediaStatus;
import com.google.android.gms.cast.framework.CastContext;
import com.google.android.gms.cast.framework.CastSession;
import com.google.android.gms.cast.framework.SessionManager;
import com.google.android.gms.cast.framework.media.RemoteMediaClient;

import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.util.ArrayList;
import java.util.List;

import timber.log.Timber;


/**
 * Main player class.
 */
public class ExoMediaPlayer extends HybridMediaPlayer implements SessionAvailabilityListener {

    private Player currentPlayer;
    private SimpleExoPlayer exoPlayer;
    private CastPlayer castPlayer;
    private int currentWindow = -1;

    private Context context;
    private MediaSource exoMediaSource;
    private List<MediaItem> mediaItems;
    private int currentState;
    private boolean isPreparing = false;
    private OnTrackChangedListener onTrackChangedListener;
    private OnLoadingChanged onLoadingChanged;
    private OnPositionDiscontinuityListener onPositionDiscontinuityListener;
    private OnPlayerStateChanged onPlayerStateChanged;
    private boolean isSupportingSystemEqualizer;
    private int shouldBeWindow;

    private List<MediaSourceInfo> mediaSourceInfoList = new ArrayList<>();
    private boolean isCasting;
    private OnCastAvailabilityChangeListener onCastAvailabilityChangeListener;
    private OnAudioSessionIdSetListener onAudioSessionIdSetListener;
    private boolean isChangingWindowByUser;

    private int initialWindowNum;

    private LoadErrorHandlingPolicy loadErrorHandlingPolicy;
    private long defaultCastPosition;

    public ExoMediaPlayer(Context context, CastContext castContext) {
        this(context, castContext, 20000);
    }

    public ExoMediaPlayer(Context context) {
        this(context, null);
    }

    public ExoMediaPlayer(Context context, CastContext castContext, long backBufferMs) {
        this.context = context;

//        bandwidthMeter = new DefaultBandwidthMeter();
//        TrackSelection.Factory videoTrackSelectionFactory =
//                new AdaptiveTrackSelection.Factory(bandwidthMeter);
//        final TrackSelector trackSelector =
//                new DefaultTrackSelector(videoTrackSelectionFactory);
//
        DefaultTrackSelector trackSelector = new DefaultTrackSelector(context);

        LoadControl loadControl = new MyLoadControl(backBufferMs);

        exoPlayer = new SimpleExoPlayer.Builder(context).setTrackSelector(trackSelector)
                .setLoadControl(loadControl).build();
//        exoPlayer = ExoPlayerFactory.newSimpleInstance(context, renderersFactory, trackSelector, loadControl);
        exoPlayer.addListener(new MyPlayerEventListener(exoPlayer));
        currentPlayer = exoPlayer;

        if (castContext != null) {
            castPlayer = new CastPlayer(castContext, new HlsMediaItemConverter());
            castPlayer.setSessionAvailabilityListener(this);
            castPlayer.addListener(new MyPlayerEventListener(castPlayer));
        }

        initialWindowNum = 0;
    }

    @Override
    public void setDataSource(String path) {
        MediaSourceInfo source = new MediaSourceInfo.Builder().setUrl(path)
                .setTitle("Title")
                .build();
        setDataSource(source);
    }

    public void setDataSource(MediaSourceInfo mediaSourceInfo) {
        List<MediaSourceInfo> list = new ArrayList<>();
        list.add(mediaSourceInfo);
        setDataSource(list);
    }


    public void setDataSource(List<MediaSourceInfo> mediaSourceInfoList) {
        setDataSource(mediaSourceInfoList, mediaSourceInfoList,0);
    }

    public void setDataSource(List<MediaSourceInfo> normalSources, List<MediaSourceInfo> castSources, long defaultCastPosition) {
        if (exoPlayer != null)
            exoPlayer.stop();

        this.defaultCastPosition = defaultCastPosition;

        // Set user agent
        String userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/108.0.0.0 Safari/537.36";
        DefaultHttpDataSource.Factory httpDataSourceFactory = new DefaultHttpDataSource.Factory();
        httpDataSourceFactory.setUserAgent(userAgent);

        httpDataSourceFactory.setConnectTimeoutMs(DefaultHttpDataSource.DEFAULT_CONNECT_TIMEOUT_MILLIS);
        httpDataSourceFactory.setReadTimeoutMs(DefaultHttpDataSource.DEFAULT_READ_TIMEOUT_MILLIS);

        // Set AllowCrossProtocolRedirects
        httpDataSourceFactory.setAllowCrossProtocolRedirects(true);

        // Set default cookie manager
        CookieManager cookieManager = new CookieManager();
        cookieManager.setCookiePolicy(CookiePolicy.ACCEPT_ORIGINAL_SERVER);
        CookieHandler.setDefault(cookieManager);

        // Produces DataSource instances through which media data is loaded.
        DataSource.Factory dataSourceFactory = new DefaultDataSource.Factory(context, httpDataSourceFactory);

        MediaSource[] sources = new MediaSource[normalSources.size()];
        for (int i = 0; i < normalSources.size(); i++) {
            // This is the MediaSource representing the media to be played.
            MediaSourceFactory factory = new DefaultMediaSourceFactory(dataSourceFactory);

            if (normalSources.get(i).getUrl().contains(".m3u8")) {
                factory = new HlsMediaSource.Factory(dataSourceFactory);
            } else
                factory = new ProgressiveMediaSource.Factory(dataSourceFactory);


            if (loadErrorHandlingPolicy != null)
                factory.setLoadErrorHandlingPolicy(loadErrorHandlingPolicy);


            sources[i] = factory
                    .createMediaSource(MediaItem.fromUri(Uri.parse(normalSources.get(i).getUrl())));
        }

        exoMediaSource = new ConcatenatingMediaSource(sources);

        prepareCastMediaSourceInfoList(castSources);
        if (isCasting)
            setCastItems();
    }

    private void init() {
        if (castPlayer != null)
            setCurrentPlayer(castPlayer.isCastSessionAvailable() ? castPlayer : exoPlayer);
    }


    private void prepareCastMediaSourceInfoList(List<MediaSourceInfo> mediaSourceInfoList) {
        this.mediaSourceInfoList = mediaSourceInfoList;
        //media sources for CastPlayer
        mediaItems = new ArrayList<>();
        for (int i = 0; i < mediaSourceInfoList.size(); i++) {
            MediaItem mediaItem = buildMediaQueueItem(mediaSourceInfoList.get(i), i + 1);
            mediaItems.add(mediaItem);
        }
    }

    private MediaItem buildMediaQueueItem(MediaSourceInfo mediaSourceInfo, int position) {
        if (mediaSourceInfo == null)
            mediaSourceInfo = MediaSourceInfo.PLACEHOLDER;

        MediaMetadata.Builder movieMetadata = new MediaMetadata.Builder();
        movieMetadata.setTitle(mediaSourceInfo.getTitle());
        movieMetadata.setArtist(mediaSourceInfo.getAuthor());
        movieMetadata.setArtworkUri(Uri.parse(mediaSourceInfo.getImageUrl()));
        movieMetadata.setTrackNumber(position);


        MediaItem.Builder builder = new MediaItem.Builder();
        builder.setMimeType(mediaSourceInfo.isVideo() ? MimeTypes.VIDEO_UNKNOWN : MimeTypes.AUDIO_UNKNOWN);
        if (mediaSourceInfo.getUrl().contains(".m3u8"))
            builder.setMimeType(MimeTypes.APPLICATION_M3U8);
        builder.setUri(mediaSourceInfo.getUrl());
        builder.setMediaMetadata(movieMetadata.build());


        return builder.build();
    }


    @Override
    public void prepare() {
        exoPlayer.addAnalyticsListener(new AnalyticsListener() {
            @Override
            public void onAudioSessionIdChanged(EventTime eventTime, int audioSessionId) {
                setEqualizer();
                if (onAudioSessionIdSetListener != null)
                    onAudioSessionIdSetListener.onAudioSessionIdset(audioSessionId);
            }
        });

        isPreparing = true;
        exoPlayer.prepare(exoMediaSource);

        shouldBeWindow = initialWindowNum;

        currentWindow = initialWindowNum;

        if (initialWindowNum != 0)
            exoPlayer.seekTo(initialWindowNum, 0);

        if (!isCasting)
            init();

        if (castPlayer != null && isCasting()) {
            castPlayer.setPlayWhenReady(true);
        }

        if (onTrackChangedListener != null)
            onTrackChangedListener.onTrackChanged(false);
    }

    private void setCastItems() {
        ArrayList<MediaItem> mediaItemsCast = new ArrayList<>();
        int i = 0;
        for (MediaSourceInfo mediaItem : mediaSourceInfoList) {
            mediaItemsCast.add(buildMediaQueueItem(mediaItem, i));
            i++;
        }
        Timber.d(String.valueOf(mediaItemsCast.size()));
        castPlayer.setMediaItems(mediaItemsCast, currentWindow, defaultCastPosition);
        castPlayer.play();
    }

    private void setEqualizer() {
        if (!isSupportingSystemEqualizer)
            return;
        final Intent intent = new Intent(AudioEffect.ACTION_OPEN_AUDIO_EFFECT_CONTROL_SESSION);
        intent.putExtra(AudioEffect.EXTRA_AUDIO_SESSION, exoPlayer.getAudioSessionId());
        intent.putExtra(AudioEffect.EXTRA_PACKAGE_NAME, context.getPackageName());
        context.sendBroadcast(intent);
    }

    private void releaseEqualizer() {
        if (!isSupportingSystemEqualizer)
            return;
        final Intent intent = new Intent(AudioEffect.ACTION_CLOSE_AUDIO_EFFECT_CONTROL_SESSION);
        intent.putExtra(AudioEffect.EXTRA_AUDIO_SESSION, exoPlayer.getAudioSessionId());
        intent.putExtra(AudioEffect.EXTRA_PACKAGE_NAME, context.getPackageName());
        context.sendBroadcast(intent);
    }

    public void setSupportingSystemEqualizer(boolean supportingSystemEqualizer) {
        isSupportingSystemEqualizer = supportingSystemEqualizer;
        if (supportingSystemEqualizer)
            setEqualizer();
        else
            releaseEqualizer();
    }

    public boolean isSupportingSystemEqualizer() {
        return isSupportingSystemEqualizer;
    }

    public CastPlayer getCastPlayer() {
        return castPlayer;
    }

    @Override
    public void release() {
        releaseEqualizer();
        exoPlayer.release();
        if (castPlayer != null)
            castPlayer.release();
    }

    @Override
    public void play() {
        currentPlayer.setPlayWhenReady(true);
    }

    @Override
    public void pause() {
        currentPlayer.setPlayWhenReady(false);
    }

    @Override
    public void seekTo(int msec) {
        if (currentPlayer == castPlayer) {
            if (castPlayer.getPlaybackState() == Player.STATE_IDLE) {
                defaultCastPosition = msec;
                setCastItems();
            }
        }

        defaultCastPosition = msec;
        currentPlayer.seekTo(msec);
    }

    public void seekTo(int windowIndex, int msec) {
        if (currentPlayer == castPlayer) {
            if (castPlayer.getPlaybackState() == Player.STATE_IDLE) {
                defaultCastPosition = msec;
                setCastItems();
            }
        }
        try {
            if (getCurrentWindow() != windowIndex) {
                isChangingWindowByUser = true;
                shouldBeWindow = windowIndex;
            }
            try {
                currentPlayer.seekTo(windowIndex, msec);
            } catch (ArrayIndexOutOfBoundsException e) {
                // TODO: 30.03.2018 https://github.com/google/ExoPlayer/issues/4063
            }
        } catch (Exception ignored) {

        }
    }

    public void stop() {
        currentPlayer.stop();
    }

    @Override
    public int getDuration() {
        try {
            if (currentPlayer.getDuration() < 0)
                return -1;
            return (int) currentPlayer.getDuration();
        } catch (Exception e) {
            return -1;
        }
    }

    @Override
    public int getCurrentPosition() {
        return (int) currentPlayer.getCurrentPosition();
    }

    @Override
    public float getVolume() {
        return exoPlayer.getVolume();
    }

    @Override
    public void setVolume(float level) {
        exoPlayer.setVolume(level);
    }

    public void setDefaultCastPosition(long defaultCastPosition) {
        this.defaultCastPosition = defaultCastPosition;
    }

    @Override
    public void setPlaybackParams(float speed, float pitch) {
        PlaybackParameters params = new PlaybackParameters(speed, pitch);
        currentPlayer.setPlaybackParameters(params);
    }

    @Override
    public boolean isPlaying() {
        return currentPlayer.getPlayWhenReady();
    }

    @Override
    public void setPlayerView(Context context, final SurfaceView surfaceView) {
        exoPlayer.setVideoSurfaceView(surfaceView);
    }

    @Override
    public boolean hasVideo() {
        return exoPlayer.getVideoFormat() != null;
    }

    public Player getCurrentPlayer() {
        return currentPlayer;
    }

    public SimpleExoPlayer getExoPlayer() {
        return exoPlayer;
    }

    public void setOnTrackChangedListener(OnTrackChangedListener onTrackChangedListener) {
        this.onTrackChangedListener = onTrackChangedListener;
    }

    public void setOnPlayerStateChanged(OnPlayerStateChanged onPlayerStateChanged) {
        this.onPlayerStateChanged = onPlayerStateChanged;
    }

    public void setOnPositionDiscontinuityListener(OnPositionDiscontinuityListener onPositionDiscontinuityListener) {
        this.onPositionDiscontinuityListener = onPositionDiscontinuityListener;
    }

    public void setLoadErrorHandlingPolicy(LoadErrorHandlingPolicy loadErrorHandlingPolicy) {
        this.loadErrorHandlingPolicy = loadErrorHandlingPolicy;
    }

    @Override
    public void onCastSessionAvailable() {
        Timber.d("");
        if (currentPlayer != castPlayer) {
            defaultCastPosition = exoPlayer.getCurrentPosition();
            setCurrentPlayer(castPlayer);
        }
        if (onCastAvailabilityChangeListener != null)
            onCastAvailabilityChangeListener.onCastAvailabilityChange(true);
    }

    @Override
    public void onCastSessionUnavailable() {
        Timber.e(String.valueOf(castPlayer.getCurrentWindowIndex()));
        Timber.e(String.valueOf(exoPlayer.getCurrentWindowIndex()));
        setCurrentPlayer(exoPlayer);
        if (onCastAvailabilityChangeListener != null)
            onCastAvailabilityChangeListener.onCastAvailabilityChange(false);
    }

    public void setOnLoadingChanged(OnLoadingChanged onLoadingChanged) {
        this.onLoadingChanged = onLoadingChanged;
    }

    private void setCurrentPlayer(Player player) {
        if (currentPlayer == player)
            return;

        boolean shouldPlay = isPlaying() && currentPlayer.getPlaybackState() != Player.STATE_IDLE;

        pause();

        long time = currentPlayer.getCurrentPosition();
        int window = currentPlayer.getCurrentWindowIndex();

        Timber.d(String.valueOf(window));
        Timber.i(String.valueOf(time));

        currentPlayer = player;
        isPreparing = true;

        if (currentPlayer == castPlayer) {
            Timber.d("");
            isCasting = true;
            if (mediaItems != null && mediaItems.size() != 0)
                setCastItems();
        }

        if (currentPlayer == exoPlayer) {
            seekTo(currentWindow, (int) time);
            if (shouldPlay)
                play();
            else
                pause();
            isCasting = false;
        }
    }

    public boolean isCasting() {
        return isCasting;
    }

    public void setInitialWindowNum(int initialWindowNum) {
        this.initialWindowNum = initialWindowNum;
    }

    public int getCurrentWindow() {
        return currentPlayer.getCurrentWindowIndex();
    }

    public int getWindowCount() {

        return mediaSourceInfoList.size();
    }

    public void setOnCastAvailabilityChangeListener(OnCastAvailabilityChangeListener onCastAvailabilityChangeListener) {
        this.onCastAvailabilityChangeListener = onCastAvailabilityChangeListener;
    }

    public void setOnAudioSessionIdSetListener(OnAudioSessionIdSetListener onAudioSessionIdSetListener) {
        this.onAudioSessionIdSetListener = onAudioSessionIdSetListener;
    }

    public interface OnTrackChangedListener {
        /**
         * @param isFinished is track finished, if false it was changed by user
         */
        void onTrackChanged(boolean isFinished);
    }

    public interface OnAudioSessionIdSetListener {

        void onAudioSessionIdset(int audioSessionId);
    }

    public interface OnPositionDiscontinuityListener {
        /**
         * @param reason             reason
         * @param currentWindowIndex currentWindowIndex
         */
        void onPositionDiscontinuity(int reason, int currentWindowIndex);
    }

    public interface OnCastAvailabilityChangeListener {
        /**
         * @param isAvailable is casting availabe
         */
        void onCastAvailabilityChange(boolean isAvailable);
    }

    public interface OnLoadingChanged {
        /**
         * @param isLoading is player Loading
         */
        void onLoadingChanged(boolean isLoading);
    }

    public interface OnPlayerStateChanged {
        /**
         * @param playWhenReady playWhenReady
         * @param playbackState playbackState states from Player class
         */
        void onPlayerStateChanged(boolean playWhenReady, int playbackState);
    }


    class MyPlayerEventListener implements Player.Listener {
        private Player player;

        public MyPlayerEventListener(Player player) {
            this.player = player;
        }

        @Override
        public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
//            super.onPlayerStateChanged(playWhenReady, playbackState);

//            Timber.d("playback state = " + playbackState);

            if (currentPlayer != player)
                return;

            if (onPlayerStateChanged != null)
                onPlayerStateChanged.onPlayerStateChanged(playWhenReady, playbackState);

            if (currentState != playbackState || isPreparing) {

                // handling: https://github.com/google/ExoPlayer/issues/4049
                if (playbackState == Player.STATE_READY)
                    checkWindowChanged();


                switch (playbackState) {
                    case Player.STATE_ENDED:

                        if (onCompletionListener != null)
                            onCompletionListener.onCompletion(ExoMediaPlayer.this);
                        break;

                    case Player.STATE_READY:
                        if (isPreparing && onPreparedListener != null && shouldBeWindow == getCurrentWindow()) {
//                            Timber.d("ret " + currentPlayer.getDuration());
                            if ((currentPlayer.getDuration() < 0 && currentPlayer.isCurrentWindowSeekable())
                                    || currentPlayer.getCurrentWindowIndex() >= getWindowCount())
                                return;
                            isPreparing = false;
                            onPreparedListener.onPrepared(ExoMediaPlayer.this);
                        }
                        break;

                    case Player.STATE_IDLE:
                        if (isCasting && getCurrentWindow() == getWindowCount() - 1) {
                            SessionManager sessionManager = CastContext.getSharedInstance(context).getSessionManager();
                            CastSession castSession = sessionManager.getCurrentCastSession();
                            if (castSession != null) {
                                RemoteMediaClient remoteMediaClient = castSession.getRemoteMediaClient();
                                if (remoteMediaClient != null) {
                                    Timber.i("playback state session= " + remoteMediaClient.getIdleReason());

                                    if (MediaStatus.IDLE_REASON_FINISHED == remoteMediaClient.getIdleReason()) {
                                        if (onCompletionListener != null) {
                                            currentPlayer.setPlayWhenReady(false);
                                            onCompletionListener.onCompletion(ExoMediaPlayer.this);
                                        }
                                    }
                                }
                            }
                            break;
                        }
                }
            }
            currentState = playbackState;

        }

        @Override
        public void onPositionDiscontinuity(int reason) {
            if (currentPlayer != player)
                return;

            checkWindowChanged();

            if (onPositionDiscontinuityListener != null)
                onPositionDiscontinuityListener.onPositionDiscontinuity(reason, currentPlayer.getCurrentWindowIndex());
        }

        private void checkWindowChanged() {
            int newIndex = currentPlayer.getCurrentWindowIndex();
            if (newIndex < 0)
                return;

//            Timber.d("onTrackChanged currentWindow " + currentWindow);
//            Timber.d("onTrackChanged newIndex " + newIndex);
//            Timber.d("onTrackChanged shouldBeWindow " + shouldBeWindow);
//            Timber.d("onTrackChanged player.getDuration() " + player.getDuration());

            if (newIndex != currentWindow && currentPlayer.getPlaybackState() != Player.STATE_IDLE) {
                // The index has changed; update the UI to show info for source at newIndex
                shouldBeWindow = newIndex;
                currentWindow = newIndex;

                if (player.getDuration() < 0)
                    isPreparing = true;

                if (onTrackChangedListener != null)
                    onTrackChangedListener.onTrackChanged(!isChangingWindowByUser);

                isChangingWindowByUser = false;


                // TODO: 02/03/2020 delete this with new library:
                //workaround for bug in cast library 18.1 - covers sometimes doesn't load after track changed (play/pause/seek helps)
                if (isCasting) {
                    Runnable reloadNotificationImageRunnable = () -> {
                        if (player == null)
                            return;
                        if (player.getPlayWhenReady())
                            player.setPlayWhenReady(true);
                        else
                            player.setPlayWhenReady(false);
                    };
                    Handler handler = new Handler();
                    handler.postDelayed(reloadNotificationImageRunnable, 3000);
                    handler.postDelayed(reloadNotificationImageRunnable, 6000);
                }
                /// TODO: 02/03/2020 end of the workaround
            }
        }

        @Override
        public void onPlayerError(PlaybackException error) {
            if (currentPlayer != player)
                return;

            if (onErrorListener != null)
                onErrorListener.onError(error, ExoMediaPlayer.this);
        }


        @Override
        public void onLoadingChanged(boolean isLoading) {
            if (onLoadingChanged != null)
                onLoadingChanged.onLoadingChanged(isLoading);
        }
    }
}