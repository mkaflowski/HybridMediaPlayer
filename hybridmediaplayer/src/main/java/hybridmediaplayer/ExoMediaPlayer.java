package hybridmediaplayer;

import android.content.Context;
import android.net.Uri;

import com.google.android.exoplayer.ExoPlaybackException;
import com.google.android.exoplayer.ExoPlayer;
import com.google.android.exoplayer.MediaCodecAudioTrackRenderer;
import com.google.android.exoplayer.MediaCodecSelector;
import com.google.android.exoplayer.extractor.ExtractorSampleSource;
import com.google.android.exoplayer.upstream.DataSource;
import com.google.android.exoplayer.upstream.DefaultAllocator;
import com.google.android.exoplayer.upstream.DefaultUriDataSource;
import com.google.android.exoplayer.util.PlayerControl;

public class ExoMediaPlayer extends HybridMediaPLayer {

    private ExoPlayer player;
    private MediaCodecAudioTrackRenderer audioRenderer;
    private static final String TAG = "SimpleExoMp3";
    private PlayerControl playerControl;
    private Context context;


    public ExoMediaPlayer(Context context) {
        this.context = context;
        player = ExoPlayer.Factory.newInstance(1);
        playerControl = new PlayerControl(player);
    }

    @Override
    public void setDataSource(String path) {
        DataSource dataSource = new DefaultUriDataSource(context, TAG);
        Uri uri = Uri.parse(path);
        ExtractorSampleSource extractorSampleSource = new ExtractorSampleSource(uri, dataSource, new DefaultAllocator(64 * 1024), 64 * 1024 * 256);
        audioRenderer = new MediaCodecAudioTrackRenderer(extractorSampleSource, MediaCodecSelector.DEFAULT);
    }

    @Override

    public void prepare() {

        player.prepare(audioRenderer);
        player.addListener(new ExoPlayer.Listener() {
            @Override
            public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
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
            }

            @Override
            public void onPlayWhenReadyCommitted() {

            }

            @Override
            public void onPlayerError(ExoPlaybackException error) {
                if (onErrorListener != null)
                    onErrorListener.onError(ExoMediaPlayer.this);
            }
        });
    }

    @Override
    public void release() {
        player.release();
    }

    @Override
    public void play() {
        playerControl.start();
    }

    @Override
    public void pause() {
        playerControl.pause();
    }

    @Override
    public void seekTo(int msec) {
        playerControl.seekTo(msec);
    }

    @Override
    public int getDuration() {
        return playerControl.getDuration();
    }

    @Override
    public int getCurrentPosition() {
        return playerControl.getCurrentPosition();
    }

    @Override
    public boolean isPlaying() {
        return playerControl.isPlaying();
    }

}
