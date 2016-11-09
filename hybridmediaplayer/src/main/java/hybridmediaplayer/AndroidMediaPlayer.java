package hybridmediaplayer;

import android.content.Context;
import android.media.MediaPlayer;

import java.io.IOException;

public class AndroidMediaPlayer extends HybridMediaPlayer {

    private MediaPlayer player;
    private Context context;


    public AndroidMediaPlayer(Context context) {
        this.context = context;
        player = new MediaPlayer();
    }

    @Override
    public void setDataSource(String path) {
        try {
            player.setDataSource(path);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override

    public void prepare() {
        player.prepareAsync();
    }

    @Override
    public void release() {
        player.release();
    }

    @Override
    public void play() {
        player.start();
    }

    @Override
    public void pause() {
        player.pause();
    }

    @Override
    public void seekTo(int msec) {
        player.seekTo(msec);
    }

    @Override
    public int getDuration() {
        return player.getDuration();
    }

    @Override
    public int getCurrentPosition() {
        return player.getCurrentPosition();
    }

    @Override
    public boolean isPlaying() {
        return player.isPlaying();
    }



}
