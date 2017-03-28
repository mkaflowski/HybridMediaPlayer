package hybridmediaplayer;

import android.content.Context;
import android.media.MediaPlayer;

import java.io.IOException;

public class AndroidMediaPlayer extends HybridMediaPlayer {

    private MediaPlayer mediaPlayer;
    private Context context;


    public AndroidMediaPlayer(Context context) {
        this.context = context;
        mediaPlayer = new MediaPlayer();

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

    public void prepare() {
        mediaPlayer.prepareAsync();
    }

    @Override
    public void release() {
        mediaPlayer.release();
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
    public int getCurrentPosition() {
        return mediaPlayer.getCurrentPosition();
    }

    @Override
    public boolean isPlaying() {
        return mediaPlayer.isPlaying();
    }



}
