package hybridmediaplayer;

import android.content.Context;
import android.content.Intent;
import android.media.audiofx.AudioEffect;
import android.net.Uri;
import android.view.SurfaceView;

import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.Format;
import com.google.android.exoplayer2.PlaybackParameters;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.audio.AudioRendererEventListener;
import com.google.android.exoplayer2.decoder.DecoderCounters;
import com.google.android.exoplayer2.extractor.ExtractorsFactory;
import com.google.android.exoplayer2.source.ConcatenatingMediaSource;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
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
import com.google.android.exoplayer2.util.MimeTypes;
import com.google.android.exoplayer2.util.Util;
import com.google.android.gms.cast.MediaInfo;
import com.google.android.gms.cast.MediaMetadata;
import com.google.android.gms.cast.MediaQueueItem;
import com.google.android.gms.cast.framework.CastContext;
import com.google.android.gms.common.images.WebImage;
import com.socks.library.KLog;

import java.util.ArrayList;
import java.util.List;


public class ExoMediaPlayer extends HybridMediaPlayer implements CastPlayer.SessionAvailabilityListener {

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
    private boolean isSupportingSystemEqualizer;
    private int shouldBeWindow;

    private List<MediaSourceInfo> mediaSourceInfoList;
    private boolean isCasting;
    private OnCastAvailabilityChangeListener onCastAvailabilityChangeListener;
    private boolean isChangingWindowByUser;

    public ExoMediaPlayer(Context context, CastContext castContext) {
        this.context = context;

        BandwidthMeter bandwidthMeter = new DefaultBandwidthMeter();
        TrackSelection.Factory videoTrackSelectionFactory =
                new AdaptiveTrackSelection.Factory(bandwidthMeter);
        final TrackSelector trackSelector =
                new DefaultTrackSelector(videoTrackSelectionFactory);

        exoPlayer = ExoPlayerFactory.newSimpleInstance(context, trackSelector);
        currentPlayer = exoPlayer;

        if (castContext != null) {
            castPlayer = new CastPlayer(castContext);
            castPlayer.setSessionAvailabilityListener(this);
            castPlayer.addListener(new MyPlayerEventListener(castPlayer));
        }
    }

    @Override
    public void setDataSource(String path) {
        MediaSourceInfo source = new MediaSourceInfo.Builder().setUrl(path)
                .setTitle("Title")
                .setImageUrl("http://www.pvhc.net/img29/amkulkkbogfvmihgspru.png")
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
        if(exoPlayer!=null)
            exoPlayer.stop();

        KLog.w("abc init ");

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
            sources[i] = new ExtractorMediaSource(Uri.parse(normalSources.get(i).getUrl()),
                    dataSourceFactory, extractorsFactory, null, null);
        }

        exoMediaSource = new ConcatenatingMediaSource(sources);

        prepareCastMediaSourceInfoList(castSources);

        prepare();

        init();

        if(castPlayer!=null && isCasting()) {
            castPlayer.stop();
            castPlayer.loadItems(mediaItems, 0, 0, Player.REPEAT_MODE_OFF);
        }
    }

    private void init() {
        KLog.d("abc init");
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
        movieMetadata.addImage(new WebImage(Uri.parse(mediaSourceInfo.getImageUrl())));
        MediaInfo mediaInfo = new MediaInfo.Builder(url)
                .setStreamType(MediaInfo.STREAM_TYPE_BUFFERED)
                .setContentType(mediaSourceInfo.isVideo() ? MimeTypes.VIDEO_UNKNOWN : MimeTypes.AUDIO_UNKNOWN)
                .setMetadata(movieMetadata).build();

        return new MediaQueueItem.Builder(mediaInfo).build();
    }

    private void prepare() {
        exoPlayer.setAudioDebugListener(new AudioRendererEventListener() {
            @Override
            public void onAudioEnabled(DecoderCounters counters) {

            }

            @Override
            public void onAudioSessionId(int audioSessionId) {
                setEqualizer();
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
        exoPlayer.addListener(new MyPlayerEventListener(exoPlayer));
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
    }

    @Override
    public int getDuration() {
        KLog.d(currentPlayer.getDuration() + " " + currentPlayer);
        if (currentPlayer.getDuration() < 0)
            return -1;
        return (int) currentPlayer.getDuration();
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

    public void setOnPositionDiscontinuityListener(OnPositionDiscontinuityListener onPositionDiscontinuityListener) {
        this.onPositionDiscontinuityListener = onPositionDiscontinuityListener;
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

        isPreparing = true;

        boolean shouldPlay = isPlaying();

        pause();

        long time = currentPlayer.getCurrentPosition();
        int window = currentPlayer.getCurrentWindowIndex();

        currentPlayer = player;

        if (currentPlayer == castPlayer) {
            isCasting = true;
            castPlayer.loadItems(mediaItems, window, time, Player.REPEAT_MODE_OFF);
        }

        if (currentPlayer == exoPlayer) {
            currentPlayer.seekTo(window, time);
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

    public int getCurrentWindow() {
        return currentPlayer.getCurrentWindowIndex();
    }

    public int getWindowCount() {
        return mediaSourceInfoList.size();
    }

    public void setOnCastAvailabilityChangeListener(OnCastAvailabilityChangeListener onCastAvailabilityChangeListener) {
        this.onCastAvailabilityChangeListener = onCastAvailabilityChangeListener;
    }


    public interface OnTrackChangedListener {
        void onTrackChanged(boolean isFinished);
    }

    public interface OnPositionDiscontinuityListener {
        void onPositionDiscontinuity(int reason, int currentWindowIndex);
    }

    public interface OnCastAvailabilityChangeListener {
        void onCastAvailabilityChange(boolean available);
    }

    public interface OnLoadingChanged {
        void onLoadingChanged(boolean isLoading);
    }


    class MyPlayerEventListener extends Player.DefaultEventListener {
        private Player player;

        public MyPlayerEventListener(Player player) {
            this.player = player;
        }

        @Override
        public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
            super.onPlayerStateChanged(playWhenReady, playbackState);

            if (currentPlayer != player)
                return;

            if (currentState != playbackState) {

                switch (playbackState) {
                    case Player.STATE_ENDED:

                        if (onCompletionListener != null)
                            onCompletionListener.onCompletion(ExoMediaPlayer.this);
                        break;

                    case Player.STATE_READY:
                        if (isPreparing && onPreparedListener != null && shouldBeWindow == getCurrentWindow()) {
                            if (currentPlayer.getDuration() < 0)
                                return;
                            isPreparing = false;
                            onPreparedListener.onPrepared(ExoMediaPlayer.this);
                        }

                        break;
                }
            }
            currentState = playbackState;

        }

        @Override
        public void onPositionDiscontinuity(int reason) {
            super.onPositionDiscontinuity(reason);
            if (currentPlayer != player)
                return;

            if (currentWindow == -1)
                isChangingWindowByUser = true;

            KLog.e("abc " + currentWindow + " " + currentPlayer.getCurrentWindowIndex() + " " + currentPlayer.getPlaybackState() + " " + reason);
            int newIndex = currentPlayer.getCurrentWindowIndex();
            if (newIndex != currentWindow && currentPlayer.getPlaybackState() != Player.STATE_IDLE) {
                // The index has changed; update the UI to show info for source at newIndex
                shouldBeWindow = newIndex;
                currentWindow = newIndex;

                if (player.getDuration() < 0)
                    isPreparing = true;

                if (onTrackChangedListener != null)
                    onTrackChangedListener.onTrackChanged(!isChangingWindowByUser);

                isChangingWindowByUser = false;
            }

            if (onPositionDiscontinuityListener != null)
                onPositionDiscontinuityListener.onPositionDiscontinuity(reason, currentPlayer.getCurrentWindowIndex());
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