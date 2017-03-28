package hybridmediaplayer;

import android.content.Context;
import android.media.MediaPlayer;
import android.media.PlaybackParams;
import android.os.Build;
import android.support.annotation.RequiresApi;

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

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    public void setPlaybackParams(PlaybackParams playbackParams) {
        mediaPlayer.setPlaybackParams(playbackParams);
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
