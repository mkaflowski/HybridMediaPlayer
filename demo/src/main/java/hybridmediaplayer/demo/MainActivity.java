package hybridmediaplayer.demo;

import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.MediaRouteButton;
import android.view.Menu;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;

import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.ext.cast.CastPlayer;
import com.google.android.exoplayer2.util.MimeTypes;
import com.google.android.gms.cast.MediaInfo;
import com.google.android.gms.cast.MediaMetadata;
import com.google.android.gms.cast.MediaQueueItem;
import com.google.android.gms.cast.framework.CastButtonFactory;
import com.google.android.gms.cast.framework.CastContext;
import com.google.android.gms.common.images.WebImage;
import com.socks.library.KLog;

import hybridmediaplayer.ExoMediaPlayer;
import hybridmediaplayer.HybridMediaPlayer;
import hybridplayer.demo.R;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, PlayerManager.QueuePositionListener {

    private ExoMediaPlayer mediaPlayer;
    private boolean isPrepared;
    private int time;
    float speed = 1;
    private SurfaceView playerView;

    private PlayerManager playerManager;


    //Chromecast
    private CastContext castContext;
    private MediaRouteButton mediaRouteButton;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button btPlay = findViewById(R.id.btPlay);
        Button btPause = findViewById(R.id.btPause);
        Button btFastForward = findViewById(R.id.fastForward);
        Button btSpeed = findViewById(R.id.btSpeed);
        Button btStop = findViewById(R.id.btStop);

        btPlay.setOnClickListener(this);
        btPause.setOnClickListener(this);
        btFastForward.setOnClickListener(this);
        btSpeed.setOnClickListener(this);
        btStop.setOnClickListener(this);

        playerView = findViewById(R.id.playerView);

        //Chromecast:
        mediaRouteButton = findViewById(R.id.media_route_button);
        CastButtonFactory.setUpMediaRouteButton(this, mediaRouteButton);
        castContext = CastContext.getSharedInstance(this);

        createPlayer();
    }


    private void createPlayer() {
        String url = "https://play.podtrac.com/npr-510289/npr.mc.tritondigital.com/NPR_510289/media/anon.npr-mp3/npr/pmoney/2017/03/20170322_pmoney_20170322_pmoney_pmpod.mp3";
        //String url = "http://stream3.polskieradio.pl:8904/";
        //String url2 = "https://github.com/mediaelement/mediaelement-files/blob/master/big_buck_bunny.mp4?raw=true";
        mediaPlayer = new ExoMediaPlayer(this);
        mediaPlayer.setDataSource(url);
        mediaPlayer.setPlayerView(this, playerView);
        mediaPlayer.setSupportingSystemEqualizer(true);
        mediaPlayer.setOnPositionDiscontinuityListener(new ExoMediaPlayer.OnPositionDiscontinuityListener() {
            @Override
            public void onPositionDiscontinuity(int currentWindowIndex) {
                KLog.d(currentWindowIndex);
            }
        });
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


                KLog.w(mediaPlayer.hasVideo());
            }
        });

        mediaPlayer.setCastPlayer(castContext);
    }


    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.btPlay) {
            if (mediaPlayer == null)
                createPlayer();
            else
                mediaPlayer.play();
        } else if (view.getId() == R.id.btPause) {
            mediaPlayer.pause();
            KLog.d(mediaPlayer.getCurrentPosition());
            KLog.i(mediaPlayer.getDuration());
        } else if (view.getId() == R.id.fastForward) {
            mediaPlayer.seekTo(mediaPlayer.getCurrentPosition() + 15 * 1000);
            mediaPlayer.prepare();
        } else if (view.getId() == R.id.btSpeed) {
            if (speed == 1)
                speed = 2f;
            else speed = 1;
            mediaPlayer.setPlaybackParams(speed, 1);
        } else if (view.getId() == R.id.btStop) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }



    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.options, menu);
        CastButtonFactory.setUpMediaRouteButton(this, menu, R.id.media_route_menu_item);
        return true;
    }

    @Override
    public void onQueuePositionChanged(int previousIndex, int newIndex) {

    }


}
