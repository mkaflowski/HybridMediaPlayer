package hybridmediaplayer;

import android.content.Context;
import android.media.MediaPlayer;
import android.media.PlaybackParams;
import android.view.SurfaceView;

import java.io.IOException;

public class AndroidMediaPlayer extends HybridMediaPlayer {

    private MediaPlayer mediaPlayer;
    private Context context;
    private float volume;


    public AndroidMediaPlayer(Context context) {
        this.context = context;
        mediaPlayer = new MediaPlayer();
        volume = 1;
    }

    @Override
    public void setDataSource(String path) {
        try {
            mediaPlayer.setDataSource(path);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    @Override
    public void release() {
        mediaPlayer.release();
    }

    @Override
    public void setPlayerView(Context context, SurfaceView surfaceView) {
        // not supported
    }

    @Override
    public boolean hasVideo() {
        // not supported
        return false;
    }

    @Override
    public void setPlaybackParams(float speed, float pitch) {

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            PlaybackParams params = new PlaybackParams();
            params.setSpeed(speed);
            params.setPitch(pitch);
            mediaPlayer.setPlaybackParams(params);
        }
    }

    @Override
    public void play() {
        mediaPlayer.start();
    }

    @Override
    public void pause() {
        mediaPlayer.pause();
    }

    @Override
    public void seekTo(int msec) {
        mediaPlayer.seekTo(msec);
    }

    @Override
    public int getDuration() {
        return mediaPlayer.getDuration();
    }

    @Override
    public void prepare() {
        mediaPlayer.prepareAsync();
    }

    @Override
    public int getCurrentPosition() {
        return mediaPlayer.getCurrentPosition();
    }

    @Override
    public float getVolume() {
        return volume;
    }

    @Override
    public void setVolume(float level) {
        volume = level;
        mediaPlayer.setVolume(level, level);
    }

    @Override
    public boolean isPlaying() {
        return mediaPlayer.isPlaying();
    }


}
