package hybridmediaplayer;

import android.content.Context;
import android.content.Intent;
import android.media.audiofx.AudioEffect;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.view.SurfaceView;

import androidx.media3.common.C;
import androidx.media3.common.MediaItem;
import androidx.media3.common.MediaMetadata;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.PlaybackParameters;
import androidx.media3.common.Player;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.common.util.Util;
import androidx.media3.datasource.DataSource;
import androidx.media3.datasource.DefaultDataSource;
import androidx.media3.datasource.DefaultHttpDataSource;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.LoadControl;
import androidx.media3.exoplayer.analytics.AnalyticsListener;
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector;
import androidx.media3.exoplayer.upstream.LoadErrorHandlingPolicy;
import androidx.media3.cast.CastPlayer;
import androidx.media3.cast.SessionAvailabilityListener;

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
 * Main player class - Media3 version
 */
@UnstableApi
public class ExoMediaPlayer extends HybridMediaPlayer implements SessionAvailabilityListener {

    private Player currentPlayer;
    private ExoPlayer exoPlayer;
    private CastPlayer castPlayer;
    private int currentWindow = -1;

    private Context context;
    private List<MediaItem> localMediaItems;
    private List<MediaItem> castMediaItems;
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
    private String userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36";
    private CastContext castContext;
    private DataSource.Factory dataSourceFactory;

    public ExoMediaPlayer(Context context, CastContext castContext) {
        this(context, castContext, 20000);
    }

    public ExoMediaPlayer(Context context) {
        this(context, null);
    }

    public ExoMediaPlayer(Context context, CastContext castContext, int backBufferMs) {
        this.context = context;

        DefaultTrackSelector trackSelector = new DefaultTrackSelector(context);
        LoadControl loadControl = new MyLoadControl(backBufferMs);

        exoPlayer = new ExoPlayer.Builder(context)
                .setTrackSelector(trackSelector)
                .setLoadControl(loadControl)
                .build();

        exoPlayer.addListener(new MyPlayerEventListener(exoPlayer));
        currentPlayer = exoPlayer;

        createCastPlayer(castContext);
        initialWindowNum = 0;
    }

    private void createCastPlayer(CastContext castContext) {
        if (castContext != null) {
            castPlayer = new CastPlayer(castContext, new HlsMediaItemConverter());
            castPlayer.setSessionAvailabilityListener(this);
            castPlayer.addListener(new MyPlayerEventListener(castPlayer));
        }
    }

    @Override
    public void setDataSource(String path) {
        MediaSourceInfo source = new MediaSourceInfo.Builder()
                .setUrl(path)
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
        setDataSource(mediaSourceInfoList, mediaSourceInfoList, 0);
    }

    /*
    Must be set before setDataSource()
     */
    public void setAppUserAgent(String appName) {
        this.userAgent = Util.getUserAgent(context, appName);
    }

    /*
    Must be set before setDataSource()
     */
    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public void setDataSourceWithMediaItems(List<MediaItem> normalSources, List<MediaItem> castSources, long defaultCastPosition) {
        if (exoPlayer != null)
            exoPlayer.stop();

        this.defaultCastPosition = defaultCastPosition;

        // Set up HTTP data source
        DefaultHttpDataSource.Factory httpDataSourceFactory = new DefaultHttpDataSource.Factory()
                .setUserAgent(userAgent)
                .setConnectTimeoutMs(DefaultHttpDataSource.DEFAULT_CONNECT_TIMEOUT_MILLIS)
                .setReadTimeoutMs(DefaultHttpDataSource.DEFAULT_READ_TIMEOUT_MILLIS)
                .setAllowCrossProtocolRedirects(true);

        // Set default cookie manager
        CookieManager cookieManager = new CookieManager();
        cookieManager.setCookiePolicy(CookiePolicy.ACCEPT_ORIGINAL_SERVER);
        CookieHandler.setDefault(cookieManager);

        // Create data source factory
        dataSourceFactory = new DefaultDataSource.Factory(context, httpDataSourceFactory);

        // Build MediaItems for local playback - użyj tej samej logiki co dla Cast
        localMediaItems = new ArrayList<>();
        for (int i = 0; i < normalSources.size(); i++) {
            localMediaItems.add(normalSources.get(i));
        }

        // Prepare cast items
        this.mediaSourceInfoList = new ArrayList<>();

        // Media items for CastPlayer
        castMediaItems = new ArrayList<>();
        for (int i = 0; i < castSources.size(); i++) {
            MediaItem source = castSources.get(i);
            castMediaItems.add(source);

            MediaSourceInfo.Builder builder = new MediaSourceInfo.Builder();

            // URL
            if (source.localConfiguration != null && source.localConfiguration.uri != null) {
                builder.setUrl(source.localConfiguration.uri.toString());
            }

            // Title
            if (source.mediaMetadata.title != null) {
                builder.setTitle(source.mediaMetadata.title.toString());
            }

            // Artist/Author
            if (source.mediaMetadata.artist != null) {
                builder.setAuthor(source.mediaMetadata.artist.toString());
            }

            // Album
            if (source.mediaMetadata.albumTitle != null) {
                builder.setAlbumTitle(source.mediaMetadata.albumTitle.toString());
            }

            // Image URL
            if (source.mediaMetadata.artworkUri != null) {
                builder.setImageUrl(source.mediaMetadata.artworkUri.toString());
            }

            // Media type
            if (source.mediaMetadata.mediaType != null) {
                builder.setMediaType(source.mediaMetadata.mediaType);
            }

            mediaSourceInfoList.add(builder.build());
        }

        if (isCasting)
            setCastItems();
    }

    public void setDataSource(List<MediaSourceInfo> normalSources, List<MediaSourceInfo> castSources, long defaultCastPosition) {
        if (exoPlayer != null)
            exoPlayer.stop();

        this.defaultCastPosition = defaultCastPosition;

        // Set up HTTP data source
        DefaultHttpDataSource.Factory httpDataSourceFactory = new DefaultHttpDataSource.Factory()
                .setUserAgent(userAgent)
                .setConnectTimeoutMs(DefaultHttpDataSource.DEFAULT_CONNECT_TIMEOUT_MILLIS)
                .setReadTimeoutMs(DefaultHttpDataSource.DEFAULT_READ_TIMEOUT_MILLIS)
                .setAllowCrossProtocolRedirects(true);

        // Set default cookie manager
        CookieManager cookieManager = new CookieManager();
        cookieManager.setCookiePolicy(CookiePolicy.ACCEPT_ORIGINAL_SERVER);
        CookieHandler.setDefault(cookieManager);

        // Create data source factory
        dataSourceFactory = new DefaultDataSource.Factory(context, httpDataSourceFactory);

        // Build MediaItems for local playback - użyj tej samej logiki co dla Cast
        localMediaItems = new ArrayList<>();
        for (int i = 0; i < normalSources.size(); i++) {
            MediaSourceInfo source = normalSources.get(i);

            MediaMetadata.Builder metadataBuilder = new MediaMetadata.Builder()
                    .setTitle(source.getTitle())
                    .setArtist(source.getAuthor())
                    .setAlbumArtist(source.getAlbumTitle())
                    .setMediaType(source.getMediaType());

            // Dodaj artwork URI
            String imageUrl = source.getImageUrl();
            if (imageUrl != null && !imageUrl.isEmpty()) {
                metadataBuilder.setArtworkUri(Uri.parse(imageUrl));
            }

            MediaItem.Builder builder = new MediaItem.Builder()
                    .setUri(Uri.parse(source.getUrl()))
                    .setMediaMetadata(metadataBuilder.build());

            // Set mime type if it's HLS
            if (source.getUrl().contains(".m3u8")) {
                builder.setMimeType(androidx.media3.common.MimeTypes.APPLICATION_M3U8);
            }

            localMediaItems.add(builder.build());
        }

        // Prepare cast items
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

        // Media items for CastPlayer
        castMediaItems = new ArrayList<>();
        for (int i = 0; i < mediaSourceInfoList.size(); i++) {
            MediaItem mediaItem = buildMediaQueueItem(mediaSourceInfoList.get(i), i + 1);
            castMediaItems.add(mediaItem);
        }
    }

    private MediaItem buildMediaQueueItem(MediaSourceInfo mediaSourceInfo, int position) {
        if (mediaSourceInfo == null)
            mediaSourceInfo = MediaSourceInfo.PLACEHOLDER;

        MediaMetadata.Builder metadataBuilder = new MediaMetadata.Builder()
                .setTitle(mediaSourceInfo.getTitle())
                .setArtist(mediaSourceInfo.getAuthor())
                .setArtworkUri(Uri.parse(mediaSourceInfo.getImageUrl()))
                .setAlbumArtist(mediaSourceInfo.getAlbumTitle())
                .setMediaType(mediaSourceInfo.getMediaType())
                .setTrackNumber(position);

        MediaItem.Builder builder = new MediaItem.Builder()
                .setUri(mediaSourceInfo.getUrl())
                .setMediaMetadata(metadataBuilder.build());

        // Set mime type
        if (mediaSourceInfo.isVideo()) {
            builder.setMimeType(androidx.media3.common.MimeTypes.VIDEO_UNKNOWN);
        } else {
            builder.setMimeType(androidx.media3.common.MimeTypes.AUDIO_UNKNOWN);
        }

        if (mediaSourceInfo.getUrl().contains(".m3u8")) {
            builder.setMimeType(androidx.media3.common.MimeTypes.APPLICATION_M3U8);
        }

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

        // Set media items and prepare
        exoPlayer.setMediaItems(localMediaItems, initialWindowNum, 0);
        exoPlayer.prepare();

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
        if (castMediaItems == null || castMediaItems.isEmpty())
            return;

        Timber.d("Setting cast items: " + castMediaItems.size());
        castPlayer.setMediaItems(castMediaItems, currentWindow, defaultCastPosition);
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
                return;
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
                return;
            }
        }

        try {
            if (getCurrentWindow() != windowIndex) {
                isChangingWindowByUser = true;
                shouldBeWindow = windowIndex;
            }
            currentPlayer.seekTo(windowIndex, msec);
        } catch (Exception e) {
            Timber.e(e, "Error seeking to window");
        }
    }

    public void stop() {
        currentPlayer.stop();
    }

    @Override
    public int getDuration() {
        try {
            long duration = currentPlayer.getDuration();
            if (duration == C.TIME_UNSET || duration < 0)
                return -1;
            return (int) duration;
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
        return currentPlayer.getPlayWhenReady() &&
                currentPlayer.getPlaybackState() == Player.STATE_READY;
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

    public ExoPlayer getExoPlayer() {
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
        Timber.d("Cast session available");
        if (currentPlayer != castPlayer) {
            defaultCastPosition = exoPlayer.getCurrentPosition();
            setCurrentPlayer(castPlayer);
        }
        if (onCastAvailabilityChangeListener != null)
            onCastAvailabilityChangeListener.onCastAvailabilityChange(true);
    }

    @Override
    public void onCastSessionUnavailable() {
        Timber.e("Cast session unavailable");
        Timber.e("Cast window: " + castPlayer.getCurrentMediaItemIndex());
        Timber.e("ExoPlayer window: " + exoPlayer.getCurrentMediaItemIndex());
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
        int window = currentPlayer.getCurrentMediaItemIndex();

        Timber.d("Switching player - window: " + window + ", time: " + time);

        currentPlayer = player;
        isPreparing = true;

        if (currentPlayer == castPlayer) {
            Timber.d("Switching to Cast player");
            isCasting = true;
            if (castMediaItems != null && !castMediaItems.isEmpty())
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
        return currentPlayer.getCurrentMediaItemIndex();
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

    public void setCastContext(CastContext castContext) {
        this.castContext = castContext;
        createCastPlayer(castContext);
    }

    // Interfaces
    public interface OnTrackChangedListener {
        void onTrackChanged(boolean isFinished);
    }

    public interface OnAudioSessionIdSetListener {
        void onAudioSessionIdset(int audioSessionId);
    }

    public interface OnPositionDiscontinuityListener {
        void onPositionDiscontinuity(int reason, int currentWindowIndex);
    }

    public interface OnCastAvailabilityChangeListener {
        void onCastAvailabilityChange(boolean isAvailable);
    }

    public interface OnLoadingChanged {
        void onLoadingChanged(boolean isLoading);
    }

    public interface OnPlayerStateChanged {
        void onPlayerStateChanged(boolean playWhenReady, int playbackState);
    }

    // Player Event Listener
    class MyPlayerEventListener implements Player.Listener {
        private final Player player;

        public MyPlayerEventListener(Player player) {
            this.player = player;
        }

        @Override
        public void onPlaybackStateChanged(int playbackState) {
            if (currentPlayer != player)
                return;

            handlePlayerStateChange(player.getPlayWhenReady(), playbackState);
        }

        @Override
        public void onPlayWhenReadyChanged(boolean playWhenReady, int reason) {
            if (currentPlayer != player)
                return;

            handlePlayerStateChange(playWhenReady, player.getPlaybackState());
        }

        private void handlePlayerStateChange(boolean playWhenReady, int playbackState) {
            if (onPlayerStateChanged != null)
                onPlayerStateChanged.onPlayerStateChanged(playWhenReady, playbackState);

            if (currentState != playbackState || isPreparing) {
                if (playbackState == Player.STATE_READY)
                    checkWindowChanged();

                switch (playbackState) {
                    case Player.STATE_ENDED:
                        if (onCompletionListener != null)
                            onCompletionListener.onCompletion(ExoMediaPlayer.this);
                        break;

                    case Player.STATE_READY:
                        if (isPreparing && onPreparedListener != null && shouldBeWindow == getCurrentWindow()) {
                            if ((currentPlayer.getDuration() == C.TIME_UNSET && currentPlayer.isCurrentMediaItemSeekable())
                                    || currentPlayer.getCurrentMediaItemIndex() >= getWindowCount())
                                return;
                            isPreparing = false;
                            onPreparedListener.onPrepared(ExoMediaPlayer.this);
                        }
                        break;

                    case Player.STATE_IDLE:
                        if (isCasting && getCurrentWindow() == getWindowCount() - 1) {
                            handleCastCompletion();
                        }
                        break;
                }
            }
            currentState = playbackState;
        }

        private void handleCastCompletion() {
            SessionManager sessionManager = castContext.getSessionManager();
            CastSession castSession = sessionManager.getCurrentCastSession();
            if (castSession != null) {
                RemoteMediaClient remoteMediaClient = castSession.getRemoteMediaClient();
                if (remoteMediaClient != null) {
                    Timber.i("Cast idle reason: " + remoteMediaClient.getIdleReason());
                    if (MediaStatus.IDLE_REASON_FINISHED == remoteMediaClient.getIdleReason()) {
                        if (onCompletionListener != null) {
                            currentPlayer.setPlayWhenReady(false);
                            onCompletionListener.onCompletion(ExoMediaPlayer.this);
                        }
                    }
                }
            }
        }

        @Override
        public void onPositionDiscontinuity(Player.PositionInfo oldPosition,
                                            Player.PositionInfo newPosition,
                                            int reason) {
            if (currentPlayer != player)
                return;

            checkWindowChanged();

            if (onPositionDiscontinuityListener != null)
                onPositionDiscontinuityListener.onPositionDiscontinuity(
                        reason,
                        currentPlayer.getCurrentMediaItemIndex()
                );
        }

        private void checkWindowChanged() {
            int newIndex = currentPlayer.getCurrentMediaItemIndex();
            if (newIndex < 0)
                return;

            if (newIndex != currentWindow && currentPlayer.getPlaybackState() != Player.STATE_IDLE) {
                shouldBeWindow = newIndex;
                currentWindow = newIndex;

                if (player.getDuration() == C.TIME_UNSET)
                    isPreparing = true;

                if (onTrackChangedListener != null)
                    onTrackChangedListener.onTrackChanged(!isChangingWindowByUser);

                isChangingWindowByUser = false;

                // Workaround for Cast notification image loading
                if (isCasting) {
                    Handler handler = new Handler(Looper.getMainLooper());
                    Runnable reloadRunnable = () -> {
                        if (player != null) {
                            player.setPlayWhenReady(player.getPlayWhenReady());
                        }
                    };
                    handler.postDelayed(reloadRunnable, 3000);
                    handler.postDelayed(reloadRunnable, 6000);
                }
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
        public void onIsLoadingChanged(boolean isLoading) {
            if (onLoadingChanged != null)
                onLoadingChanged.onLoadingChanged(isLoading);
        }
    }
}