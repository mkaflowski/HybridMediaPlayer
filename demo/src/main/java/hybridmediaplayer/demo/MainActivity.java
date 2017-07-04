package hybridmediaplayer.demo;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;

import com.socks.library.KLog;

import hybridmediaplayer.ExoMediaPlayer;
import hybridmediaplayer.HybridMediaPlayer;
import hybridplayer.demo.R;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private HybridMediaPlayer mediaPlayer;
    private boolean isPrepared;
    private int time;
    float speed = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button btPlay = (Button) findViewById(R.id.btPlay);
        Button btPause = (Button) findViewById(R.id.btPause);
        Button btFastForward = (Button) findViewById(R.id.fastForward);
        Button btSpeed = (Button) findViewById(R.id.btSpeed);

        btPlay.setOnClickListener(this);
        btPause.setOnClickListener(this);
        btFastForward.setOnClickListener(this);
        btSpeed.setOnClickListener(this);

        SurfaceView playerView = (SurfaceView) findViewById(R.id.playerView);

        String url = "https://play.podtrac.com/npr-510289/npr.mc.tritondigital.com/NPR_510289/media/anon.npr-mp3/npr/pmoney/2017/03/20170322_pmoney_20170322_pmoney_pmpod.mp3";
        //String url = "https://github.com/mediaelement/mediaelement-files/blob/master/big_buck_bunny.mp4?raw=true";
        mediaPlayer = HybridMediaPlayer.getInstance(this);
        mediaPlayer.setDataSource(url);
        mediaPlayer.setPlayerView(this, playerView);
        mediaPlayer.prepare();

        mediaPlayer.setOnPreparedListener(new HybridMediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(HybridMediaPlayer player) {
                KLog.i("prepared");

                if (!isPrepared)
                    player.seekTo(time);
                isPrepared = true;
                time = 0;
                mediaPlayer.play();

                if(android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN){
                    ExoMediaPlayer exoMediaPlayer = (ExoMediaPlayer) mediaPlayer;
                    KLog.d(exoMediaPlayer.hasVideo());
                }
            }
        });

    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.btPlay) {
            mediaPlayer.play();
        } else if (view.getId() == R.id.btPause) {
            mediaPlayer.pause();
            KLog.d(mediaPlayer.getCurrentPosition());
            KLog.i(mediaPlayer.getDuration());
        } else if (view.getId() == R.id.fastForward) {
            mediaPlayer.seekTo(mediaPlayer.getCurrentPosition() + 15 * 1000);
        } else if (view.getId() == R.id.btSpeed) {
            if (speed == 1)
                speed = 2f;
            else speed = 1;
            mediaPlayer.setPlaybackParams(speed, 1);
        }
    }
}
