package hybridmediaplayer.demo;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;

import com.socks.library.KLog;

import hybridmediaplayer.HybridMediaPlayer;
import hybridplayer.demo.R;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private HybridMediaPlayer mediaPlayer;
    private boolean isPrepared;
    private int time;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button btPlay = (Button) findViewById(R.id.btPlay);
        Button btPause = (Button) findViewById(R.id.btPause);

        btPlay.setOnClickListener(this);
        btPause.setOnClickListener(this);

        String url = "https://ia801306.us.archive.org/23/items/dunwich_horror_1511_librivox/dunwichhorror_02_lovecraft.mp3";
        mediaPlayer = HybridMediaPlayer.getInstance(this);
        mediaPlayer.setDataSource(url);
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
            }
        });

    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.btPlay) {
            mediaPlayer.play();
        } else if (view.getId() == R.id.btPause) {

            mediaPlayer.pause();
        }
    }
}
