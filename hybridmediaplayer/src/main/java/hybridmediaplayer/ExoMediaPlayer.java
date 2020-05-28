package hybridmediaplayer;

import android.content.Context;
import android.content.Intent;
import android.media.audiofx.AudioEffect;
import android.net.Uri;
import android.os.Handler;
import android.view.SurfaceView;

import com.google.android.exoplayer2.DefaultRenderersFactory;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.Format;
import com.google.android.exoplayer2.LoadControl;
import com.google.android.exoplayer2.PlaybackParameters;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.RenderersFactory;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.audio.AudioRendererEventListener;
import com.google.android.exoplayer2.decoder.DecoderCounters;
import com.google.android.exoplayer2.ext.cast.SessionAvailabilityListener;
import com.google.android.exoplayer2.extractor.ExtractorsFactory;
import com.google.android.exoplayer2.source.ConcatenatingMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.ProgressiveMediaSource;
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelection;
import com.google.android.exoplayer2.trackselection.TrackSelector;
import com.google.android.exoplayer2.upstream.BandwidthMeter;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSource;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory;
import com.google.android.exoplayer2.upstream.LoadErrorHandlingPolicy;
import com.google.android.exoplayer2.util.MimeTypes;
import com.google.android.exoplayer2.util.Util;
import com.google.android.gms.cast.MediaInfo;
import com.google.android.gms.cast.MediaMetadata;
import com.google.android.gms.cast.MediaQueueItem;
import com.google.android.gms.cast.MediaStatus;
import com.google.android.gms.cast.framework.CastContext;
import com.google.android.gms.cast.framework.CastSession;
import com.google.android.gms.cast.framework.SessionManager;
import com.google.android.gms.cast.framework.media.RemoteMediaClient;
import com.google.android.gms.common.images.WebImage;
import com.socks.library.KLog;

import java.util.ArrayList;
import java.util.List;


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
    private MediaQueueItem[] mediaItems;
    private int currentState;
    private boolean isPreparing = false;
    private OnTrackChangedListener onTrackChangedListener;
    private OnLoadingChanged onLoadingChanged;
    private OnPositionDiscontinuityListener onPositionDiscontinuityListener;
    private OnPlayerStateChanged onPlayerStateChanged;
    private boolean isSupportingSystemEqualizer;
    private int shouldBeWindow;

    private List<MediaSourceInfo> mediaSourceInfoList;
    private boolean isCasting;
    private OnCastAvailabilityChangeListener onCastAvailabilityChangeListener;
    private OnAudioSessionIdSetListener onAudioSessionIdSetListener;
    private boolean isChangingWindowByUser;

    private int initialWindowNum;
    private BandwidthMeter bandwidthMeter;

    private LoadErrorHandlingPolicy loadErrorHandlingPolicy;

    public ExoMediaPlayer(Context context, CastContext castContext) {
        this(context, castContext, 20000);
    }

    public ExoMediaPlayer(Context context) {
        this(context, null);
    }

    public ExoMediaPlayer(Context context, CastContext castContext, long backBufferMs) {
        this.context = context;

        bandwidthMeter = new DefaultBandwidthMeter();
        TrackSelection.Factory videoTrackSelectionFactory =
                new AdaptiveTrackSelection.Factory(bandwidthMeter);
        final TrackSelector trackSelector =
                new DefaultTrackSelector(videoTrackSelectionFactory);

        RenderersFactory renderersFactory = new DefaultRenderersFactory(context);
        LoadControl loadControl = new MyLoadControl(backBufferMs);

        exoPlayer = ExoPlayerFactory.newSimpleInstance(context, renderersFactory, trackSelector, loadControl);
        exoPlayer.addListener(new MyPlayerEventListener(exoPlayer));
        currentPlayer = exoPlayer;

        if (castContext != null) {
            castPlayer = new CastPlayer(castContext);
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
        setDataSource(mediaSourceInfoList, mediaSourceInfoList);
    }

    public void setDataSource(List<MediaSourceInfo> normalSources, List<MediaSourceInfo> castSources) {
        if (exoPlayer != null)
            exoPlayer.stop();

        String userAgent = Util.getUserAgent(context, "yourApplicationName");
        DefaultHttpDataSourceFactory httpDataSourceFactory = new DefaultHttpDataSourceFactory(
                userAgent,
                null /* listener */,
                DefaultHttpDataSource.DEFAULT_CONNECT_TIMEOUT_MILLIS,
                DefaultHttpDataSource.DEFAULT_READ_TIMEOUT_MILLIS,
                true /* allowCrossProtocolRedirects */
        );

        // Produces DataSource instances through which media data is loaded.
        DataSource.Factory dataSourceFactory = new DefaultDataSourceFactory(context, null, httpDataSourceFactory);
        // Produces Extractor instances for parsing the media data.
        ExtractorsFactory extractorsFactory = new SeekableExtractorsFactory();


        MediaSource[] sources = new MediaSource[normalSources.size()];
        for (int i = 0; i < normalSources.size(); i++) {
            // This is the MediaSource representing the media to be played.
            ProgressiveMediaSource.Factory factory = new ProgressiveMediaSource.Factory(dataSourceFactory);
            if (loadErrorHandlingPolicy != null)
                factory.setLoadErrorHandlingPolicy(loadErrorHandlingPolicy);
            sources[i] = factory
                    .createMediaSource(Uri.parse(normalSources.get(i).getUrl()));
        }

        exoMediaSource = new ConcatenatingMediaSource(sources);

        prepareCastMediaSourceInfoList(castSources);
    }

    private void init() {
        if (castPlayer != null)
            setCurrentPlayer(castPlayer.isCastSessionAvailable() ? castPlayer : exoPlayer);
    }


    private void prepareCastMediaSourceInfoList(List<MediaSourceInfo> mediaSourceInfoList) {
        this.mediaSourceInfoList = mediaSourceInfoList;
        //media sources for CastPlayer
        mediaItems = new MediaQueueItem[mediaSourceInfoList.size()];
        for (int i = 0; i < mediaSourceInfoList.size(); i++) {
            mediaItems[i] = buildMediaQueueItem(mediaSourceInfoList.get(i).getUrl(), mediaSourceInfoList.get(i), i + 1);
        }
    }

    private MediaQueueItem buildMediaQueueItem(String url, MediaSourceInfo mediaSourceInfo, int position) {
        if (mediaSourceInfo == null)
            mediaSourceInfo = MediaSourceInfo.PLACEHOLDER;

        MediaMetadata movieMetadata = new MediaMetadata(mediaSourceInfo.isVideo() ? MediaMetadata.MEDIA_TYPE_MOVIE : MediaMetadata.MEDIA_TYPE_MUSIC_TRACK);
        movieMetadata.putString(MediaMetadata.KEY_TITLE, mediaSourceInfo.getTitle());
        movieMetadata.putString(MediaMetadata.KEY_ALBUM_ARTIST, mediaSourceInfo.getAuthor());
        movieMetadata.putInt(MediaMetadata.KEY_TRACK_NUMBER, position);
        String imageUrl = mediaSourceInfo.getImageUrl();
        if (imageUrl != null) {
            Uri parse = Uri.parse(imageUrl);
            movieMetadata.addImage(new WebImage(parse));
        }
        MediaInfo mediaInfo = new MediaInfo.Builder(url)
                .setStreamType(MediaInfo.STREAM_TYPE_BUFFERED)
                .setContentType(mediaSourceInfo.isVideo() ? MimeTypes.VIDEO_UNKNOWN : MimeTypes.AUDIO_UNKNOWN)
                .setMetadata(movieMetadata).build();

        return new MediaQueueItem.Builder(mediaInfo).build();
    }


    @Override
    public void prepare() {
        exoPlayer.setAudioDebugListener(new AudioRendererEventListener() {
            @Override
            public void onAudioEnabled(DecoderCounters counters) {

            }

            @Override
            public void onAudioSessionId(int audioSessionId) {
                setEqualizer();
                if (onAudioSessionIdSetListener != null)
                    onAudioSessionIdSetListener.onAudioSessionIdset(audioSessionId);
            }

            @Override
            public void onAudioDecoderInitialized(String decoderName, long initializedTimestampMs, long initializationDurationMs) {

            }

            @Override
            public void onAudioInputFormatChanged(Format format) {

            }

            @Override
            public void onAudioSinkUnderrun(int bufferSize, long bufferSizeMs, long elapsedSinceLastFeedMs) {

            }


            @Override
            public void onAudioDisabled(DecoderCounters counters) {

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
            castPlayer.loadItems(mediaItems, initialWindowNum, 0, Player.REPEAT_MODE_OFF);
//            castPlayer.setPlayWhenReady(true);
        }

        if (onTrackChangedListener != null)
            onTrackChangedListener.onTrackChanged(false);
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
        currentPlayer.seekTo(msec);
    }

    public void stop() {
        currentPlayer.stop();
    }

    public void seekTo(int windowIndex, int msec) {
        try {
            if (getCurrentWindow() != windowIndex) {
                isChangingWindowByUser = true;
                shouldBeWindow = windowIndex;
            }
            try {
                if (currentPlayer == castPlayer)
                    castPlayer.loadItems(mediaItems, windowIndex, msec, Player.REPEAT_MODE_OFF);
                else
                    currentPlayer.seekTo(windowIndex, msec);
            } catch (ArrayIndexOutOfBoundsException e) {
                // TODO: 30.03.2018 https://github.com/google/ExoPlayer/issues/4063
            }
        } catch (Exception ignored) {

        }
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
        setCurrentPlayer(castPlayer);
        if (onCastAvailabilityChangeListener != null)
            onCastAvailabilityChangeListener.onCastAvailabilityChange(true);
    }

    @Override
    public void onCastSessionUnavailable() {
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

        currentPlayer = player;
        isPreparing = true;


        if (currentPlayer == castPlayer) {
            isCasting = true;
            castPlayer.loadItems(mediaItems, window, time, Player.REPEAT_MODE_OFF);
        }

        if (currentPlayer == exoPlayer) {
            seekTo(window, (int) time);
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


    class MyPlayerEventListener extends Player.DefaultEventListener {
        private Player player;

        public MyPlayerEventListener(Player player) {
            this.player = player;
        }

        @Override
        public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
            super.onPlayerStateChanged(playWhenReady, playbackState);

//            KLog.d("playback state = " + playbackState);

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
//                            KLog.d("ret " + currentPlayer.getDuration());
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
                                    KLog.i("playback state session= " + remoteMediaClient.getIdleReason());

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
            super.onPositionDiscontinuity(reason);
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

//            KLog.d("onTrackChanged currentWindow " + currentWindow);
//            KLog.d("onTrackChanged newIndex " + newIndex);
//            KLog.d("onTrackChanged shouldBeWindow " + shouldBeWindow);
//            KLog.d("onTrackChanged player.getDuration() " + player.getDuration());

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
        public void onPlayerError(ExoPlaybackException error) {
            if (currentPlayer != player)
                return;

            if (onErrorListener != null)
                onErrorListener.onError(error, ExoMediaPlayer.this);
        }

        @Override
        public void onLoadingChanged(boolean isLoading) {
            super.onLoadingChanged(isLoading);
            if (onLoadingChanged != null)
                onLoadingChanged.onLoadingChanged(isLoading);
        }
    }
}