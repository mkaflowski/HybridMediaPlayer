package hybridmediaplayer;

import android.content.Context;
import android.content.Intent;
import android.media.audiofx.AudioEffect;
import android.net.Uri;
import android.view.SurfaceView;

import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.Format;
import com.google.android.exoplayer2.PlaybackParameters;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.audio.AudioRendererEventListener;
import com.google.android.exoplayer2.audio.MediaCodecAudioRenderer;
import com.google.android.exoplayer2.decoder.DecoderCounters;
import com.google.android.exoplayer2.extractor.ExtractorsFactory;
import com.google.android.exoplayer2.source.ConcatenatingMediaSource;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelection;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;
import com.google.android.exoplayer2.trackselection.TrackSelector;
import com.google.android.exoplayer2.ui.SimpleExoPlayerView;
import com.google.android.exoplayer2.upstream.BandwidthMeter;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSource;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory;
import com.google.android.exoplayer2.util.Util;


public class ExoMediaPlayer extends HybridMediaPlayer {

    private SimpleExoPlayer player;
    private Context context;
    private MediaSource mediaSource;
    private int currentState;
    private boolean isPreparing = false;
    private OnTracksChangedListener onTracksChangedListener;
    private OnPositionDiscontinuityListener onPositionDiscontinuityListener;


    public ExoMediaPlayer(Context context) {
        this.context = context;

        BandwidthMeter bandwidthMeter = new DefaultBandwidthMeter();
        TrackSelection.Factory videoTrackSelectionFactory =
                new AdaptiveTrackSelection.Factory(bandwidthMeter);
        TrackSelector trackSelector =
                new DefaultTrackSelector(videoTrackSelectionFactory);

        player = ExoPlayerFactory.newSimpleInstance(context, trackSelector);
    }

    @Override
    public void setDataSource(String path) {
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
        // This is the MediaSource representing the media to be played.
        mediaSource = new ExtractorMediaSource(Uri.parse(path),
                dataSourceFactory, extractorsFactory, null, null);
    }

    public void setDataSource(String... paths) {
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


        MediaSource[] sources = new MediaSource[paths.length];
        for (int i = 0; i < paths.length; i++) {
            // This is the MediaSource representing the media to be played.
            sources[i] = new ExtractorMediaSource(Uri.parse(paths[i]),
                    dataSourceFactory, extractorsFactory, null, null);
        }

        mediaSource = new ConcatenatingMediaSource(sources);
    }

    @Override
    public void prepare() {
        player.setAudioDebugListener(new AudioRendererEventListener() {
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
            public void onAudioTrackUnderrun(int bufferSize, long bufferSizeMs, long elapsedSinceLastFeedMs) {

            }

            @Override
            public void onAudioDisabled(DecoderCounters counters) {

            }
        });

        isPreparing = true;
        player.prepare(mediaSource);
        player.addListener(new ExoPlayer.EventListener() {
            @Override
            public void onLoadingChanged(boolean isLoading) {
            }

            @Override
            public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
                if (currentState != playbackState)
                    switch (playbackState) {
                        case ExoPlayer.STATE_ENDED:
                            if (onCompletionListener != null)
                                onCompletionListener.onCompletion(ExoMediaPlayer.this);
                            break;

                        case ExoPlayer.STATE_READY:
                            if (isPreparing && onPreparedListener != null) {
                                isPreparing = false;
                                onPreparedListener.onPrepared(ExoMediaPlayer.this);
                            }
                            break;
                    }
                currentState = playbackState;
            }

            @Override
            public void onTimelineChanged(Timeline timeline, Object manifest) {
            }

            @Override
            public void onTracksChanged(TrackGroupArray trackGroups, TrackSelectionArray trackSelections) {
                if (onTracksChangedListener != null)
                    onTracksChangedListener.onTracksChanged();
            }

            @Override
            public void onPlayerError(ExoPlaybackException error) {
                if (onErrorListener != null)
                    onErrorListener.onError(error, ExoMediaPlayer.this);
            }

            @Override
            public void onPositionDiscontinuity() {
                if (onPositionDiscontinuityListener != null)
                    onPositionDiscontinuityListener.onPositionDiscontinuity(player.getCurrentWindowIndex());

            }

            @Override
            public void onPlaybackParametersChanged(PlaybackParameters playbackParameters) {

            }
        });

    }

    private void setEqualizer() {
        final Intent intent = new Intent(AudioEffect.ACTION_OPEN_AUDIO_EFFECT_CONTROL_SESSION);
        intent.putExtra(AudioEffect.EXTRA_AUDIO_SESSION, player.getAudioSessionId());
        intent.putExtra(AudioEffect.EXTRA_PACKAGE_NAME, context.getPackageName());
        context.sendBroadcast(intent);
    }

    private void releaseEqualizer() {
        final Intent intent = new Intent(AudioEffect.ACTION_CLOSE_AUDIO_EFFECT_CONTROL_SESSION);
        intent.putExtra(AudioEffect.EXTRA_AUDIO_SESSION, player.getAudioSessionId());
        intent.putExtra(AudioEffect.EXTRA_PACKAGE_NAME, context.getPackageName());
        context.sendBroadcast(intent);
    }

    @Override
    public void release() {
        releaseEqualizer();
        player.release();
    }

    @Override
    public void play() {
        player.setPlayWhenReady(true);
    }

    @Override
    public void pause() {
        player.setPlayWhenReady(false);
    }

    @Override
    public void seekTo(int msec) {
        player.seekTo(msec);
    }

    public void seekTo(int windowIndex, int msec) {
        player.seekTo(windowIndex, msec);
    }

    @Override
    public int getDuration() {
        return (int) player.getDuration();
    }

    @Override
    public int getCurrentPosition() {
        return (int) player.getCurrentPosition();
    }

    @Override
    public float getVolume() {
        return player.getVolume();
    }

    @Override
    public void setVolume(float level) {
        player.setVolume(level);
    }

    @Override
    public void setPlaybackParams(float speed, float pitch) {
        PlaybackParameters params = new PlaybackParameters(speed, pitch);
        player.setPlaybackParameters(params);
    }

    @Override
    public boolean isPlaying() {
        return player.getPlayWhenReady();
    }

    @Override
    public void setPlayerView(Context context, final SurfaceView surfaceView) {
        player.setVideoSurfaceView(surfaceView);
    }

    @Override
    public boolean hasVideo() {
        return player.getVideoFormat() != null;
    }

    public SimpleExoPlayer getPlayer() {
        return player;
    }

    public void setOnTracksChangedListener(OnTracksChangedListener onTracksChangedListener) {
        this.onTracksChangedListener = onTracksChangedListener;
    }

    public void setOnPositionDiscontinuityListener(OnPositionDiscontinuityListener onPositionDiscontinuityListener) {
        this.onPositionDiscontinuityListener = onPositionDiscontinuityListener;
    }

    public interface OnTracksChangedListener {
        public void onTracksChanged();
    }

    public interface OnPositionDiscontinuityListener {
        public void onPositionDiscontinuity(int currentWindowIndex);
    }
}
