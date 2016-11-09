package hybridmediaplayer.demo;

import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;

import com.google.android.exoplayer2.DefaultLoadControl;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.LoadControl;
import com.google.android.exoplayer2.SimpleExoPlayer;
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

import hybridmediaplayer.ExoMediaPlayer;
import hybridmediaplayer.HybridMediaPLayer;
import hybridplayer.demo.R;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private HybridMediaPLayer mediaPlayer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button btPlay = (Button) findViewById(R.id.btPlay);
        Button btPause = (Button) findViewById(R.id.btPause);

        btPlay.setOnClickListener(this);
        btPause.setOnClickListener(this);

        String url = "https://ia801306.us.archive.org/23/items/dunwich_horror_1511_librivox/dunwichhorror_02_lovecraft.mp3";
        mediaPlayer = HybridMediaPLayer.getInstance(this);
        mediaPlayer.setDataSource(url);
        mediaPlayer.prepare();

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
