package hybridmediaplayer;

import android.content.Context;
import android.net.Uri;
import android.os.Handler;

import com.google.android.exoplayer2.DefaultLoadControl;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.LoadControl;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory;
import com.google.android.exoplayer2.extractor.ExtractorsFactory;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.trackselection.AdaptiveVideoTrackSelection;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelection;
import com.google.android.exoplayer2.trackselection.TrackSelector;
import com.google.android.exoplayer2.upstream.BandwidthMeter;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.util.Util;
import com.socks.library.KLog;


public class ExoMediaPlayer extends HybridMediaPlayer {

    private SimpleExoPlayer player;
    private Context context;
    Handler mainHandler = new Handler();
    private MediaSource mediaSource;
    private int currentState;


    public ExoMediaPlayer(Context context) {
        this.context = context;

        Handler mainHandler = new Handler();
        BandwidthMeter bandwidthMeter = new DefaultBandwidthMeter();
        TrackSelection.Factory videoTrackSelectionFactory =
                new AdaptiveVideoTrackSelection.Factory(bandwidthMeter);
        TrackSelector trackSelector =
                new DefaultTrackSelector(mainHandler, videoTrackSelectionFactory);

        LoadControl loadControl = new DefaultLoadControl();

        player = ExoPlayerFactory.newSimpleInstance(context, trackSelector, loadControl);
    }

    @Override
    public void setDataSource(String path) {
        // Produces DataSource instances through which media data is loaded.
        DataSource.Factory dataSourceFactory = new DefaultDataSourceFactory(context, Util.getUserAgent(context, "yourApplicationName"));
        // Produces Extractor instances for parsing the media data.
        ExtractorsFactory extractorsFactory = new DefaultExtractorsFactory();
        // This is the MediaSource representing the media to be played.
        mediaSource = new ExtractorMediaSource(Uri.parse(path),
                dataSourceFactory, extractorsFactory, null, null);
    }

    @Override

    public void prepare() {

        player.prepare(mediaSource);
        player.addListener(new ExoPlayer.EventListener() {
            @Override
            public void onLoadingChanged(boolean isLoading) {

            }

            @Override
            public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
                KLog.e(playbackState);
                if (currentState != playbackState)
                    switch (playbackState) {
                        case ExoPlayer.STATE_ENDED:
                            if (onCompletionListener != null)
                                onCompletionListener.onCompletion(ExoMediaPlayer.this);
                            break;

                        case ExoPlayer.STATE_READY:
                            if (onPreparedListener != null)
                                onPreparedListener.onPrepared(ExoMediaPlayer.this);
                            break;
                    }
                currentState = playbackState;
            }

            @Override
            public void onTimelineChanged(Timeline timeline, Object manifest) {

            }

            @Override
            public void onPlayerError(ExoPlaybackException error) {
                if (onErrorListener != null)
                    onErrorListener.onError(ExoMediaPlayer.this);
            }

            @Override
            public void onPositionDiscontinuity() {

            }
        });
    }

    @Override
    public void release() {
        player.release();
    }

    @Override
    public void play() {
        KLog.i("play");
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

    @Override
    public int getDuration() {
        return (int) player.getDuration();
    }

    @Override
    public int getCurrentPosition() {
        return (int) player.getCurrentPosition();
    }

    @Override
    public boolean isPlaying() {
        return player.getPlayWhenReady();
    }

}
