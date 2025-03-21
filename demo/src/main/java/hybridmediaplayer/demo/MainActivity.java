package hybridmediaplayer.demo;


import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.service.notification.StatusBarNotification;
import android.view.Menu;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;

import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.mediarouter.app.MediaRouteButton;

import com.google.android.exoplayer2.MediaMetadata;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.metadata.Metadata;
import com.google.android.exoplayer2.metadata.icy.IcyHeaders;
import com.google.android.exoplayer2.metadata.icy.IcyInfo;
import com.google.android.gms.cast.framework.CastButtonFactory;
import com.google.android.gms.cast.framework.CastContext;
import com.google.android.gms.cast.framework.CastState;
import com.google.android.gms.cast.framework.CastStateListener;

import java.util.ArrayList;
import java.util.List;

import hybridmediaplayer.ExoMediaPlayer;
import hybridmediaplayer.HybridMediaPlayer;
import hybridmediaplayer.MediaSourceInfo;
import hybridplayer.demo.R;
import timber.log.Timber;

public class MainActivity extends FragmentActivity implements View.OnClickListener {

    public static final String TESTCHANNEL = "testchannel5";
    private static final String GROUP_1 = "GROUP_1";
    private ExoMediaPlayer mediaPlayer;
    private boolean isPrepared;
    private int time;
    float speed = 1;
    private SurfaceView playerView;


    //Chromecast
    private CastContext castContext;
    private MediaRouteButton mediaRouteButton;
    private List<MediaSourceInfo> sources;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button btPlay = findViewById(R.id.btPlay);
        Button btPause = findViewById(R.id.btPause);
        Button btFastForward = findViewById(R.id.fastForward);
        Button btSpeed = findViewById(R.id.btSpeed);
        Button btStop = findViewById(R.id.btStop);
        Button btNext = findViewById(R.id.btNext);
        Button btCreatePlayer = findViewById(R.id.btCreatePlayer);
        Button btSetSources = findViewById(R.id.btSetSources);


        btPlay.setOnClickListener(this);
        btPause.setOnClickListener(this);
        btFastForward.setOnClickListener(this);
        btSpeed.setOnClickListener(this);
        btStop.setOnClickListener(this);
        btNext.setOnClickListener(this);
        btCreatePlayer.setOnClickListener(this);
        btSetSources.setOnClickListener(this);

        playerView = findViewById(R.id.playerView);

        //Chromecast:
        mediaRouteButton = findViewById(R.id.media_route_button);

//        // Ensure volume control is updated
//        setVolumeControlStream(AudioManager.STREAM_MUSIC);

        castContext = CastContext.getSharedInstance(this);
        castContext.addCastStateListener(new CastStateListener() {
            @Override
            public void onCastStateChanged(int state) {
                if (state == CastState.NO_DEVICES_AVAILABLE)
                    mediaRouteButton.setVisibility(View.GONE);
                else {
                    if (mediaRouteButton.getVisibility() == View.GONE)
                        mediaRouteButton.setVisibility(View.VISIBLE);
                }
            }
        });

        CastButtonFactory.setUpMediaRouteButton(this, mediaRouteButton);

        setVolumeControlStream(AudioManager.STREAM_MUSIC);

        mediaPlayer = new ExoMediaPlayer(this, castContext, 0);

    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mediaPlayer != null)
            mediaPlayer.pause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mediaPlayer != null)
            mediaPlayer.release();
    }

    private void createPlayer() {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.release();
        }

        createSources();
        mediaPlayer = new ExoMediaPlayer(this, castContext, 0);

        mediaPlayer.setPlayerView(this, playerView);
        mediaPlayer.setSupportingSystemEqualizer(true);
        mediaPlayer.setOnTrackChangedListener(isFinished -> {
            Timber.w("onTrackChanged isFinished " + isFinished + " " + mediaPlayer.getDuration() + " window = " + mediaPlayer.getCurrentWindow());
        });


        mediaPlayer.setOnPreparedListener(player -> {
            Timber.w(String.valueOf(mediaPlayer.hasVideo()));
            Timber.d("onPrepared " + mediaPlayer.getCurrentPlayer());
        });

        mediaPlayer.setOnPlayerStateChanged((playWhenReady, playbackState) -> {
//            Timber.d("onPlayerStateChanged playbackState " + playbackState + " position " + mediaPlayer.getCurrentWindow());
        });

        mediaPlayer.setOnCompletionListener(player -> Timber.i("onCompletion"));

        mediaPlayer.setOnLoadingChanged(isLoading -> Timber.d("setOnLoadingChanged " + isLoading));

        mediaPlayer.setOnErrorListener(new HybridMediaPlayer.OnErrorListener() {
            @Override
            public void onError(Exception error, HybridMediaPlayer player) {
                Timber.e(error);
                Timber.e(String.valueOf(player));
            }
        });

        mediaPlayer.setDataSource(sources, sources, 0);
        mediaPlayer.setOnAudioSessionIdSetListener(audioSessionId -> Timber.d("onAudioSessionIdset audio session id = " + audioSessionId));

        mediaPlayer.setOnPositionDiscontinuityListener((reason, currentWindowIndex) -> Timber.w("onPositionDiscontinuity reason " + reason + " position " + mediaPlayer.getCurrentWindow() + " currentWindowIndex " + currentWindowIndex));
//        mediaPlayer.setInitialWindowNum(2);
//        mediaPlayer.setInitialWindowNum(2);
        mediaPlayer.prepare();
        mediaPlayer.seekTo(0,1000*30);
        mediaPlayer.play();

        Timber.w(String.valueOf(mediaPlayer.getWindowCount()));


        mediaPlayer.getExoPlayer().addListener(new Player.Listener() {
            @Override
            public void onMediaMetadataChanged(MediaMetadata mediaMetadata) {
                Player.Listener.super.onMediaMetadataChanged(mediaMetadata);
                Timber.w((String) mediaMetadata.artist);
                Timber.w((String) mediaMetadata.title);
                Timber.w((String) mediaMetadata.displayTitle);
            }

            @Override
            public void onMetadata(Metadata metadata) {
                Player.Listener.super.onMetadata(metadata);
                Timber.d("metadata: " + metadata);
                if (metadata.length() > 0) {
                    if (metadata.get(0) instanceof IcyInfo) {

                        IcyInfo data = (IcyInfo) metadata.get(0);
                        String metadataTitle = data.title;
                        Timber.d(data.toString());
                        String track = data.title;
                        //updateMediaSessionMetadata(true);
                        Timber.e(metadataTitle);
                        Timber.e(track);
                    }
                    if (metadata.get(0) instanceof IcyHeaders) {
                        IcyHeaders data = (IcyHeaders) metadata.get(0);
                        Timber.d(data.name);
                        Timber.d(data.genre);
                        Timber.d(String.valueOf(data.bitrate));
                        Timber.d(String.valueOf(data.metadataInterval));
                        Timber.d(data.url);
                    }
                }
            }

        });

    }

    private void createSources() {
//        String url = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3";
        String url = "https://streaming.live365.com/a72282";
//        String url = "https://storage.googleapis.com/shaka-demo-assets/raw-hls-audio-only/manifest.m3u8";
//        url = "http://devimages.apple.com/iphone/samples/bipbop/gear1/prog_index.m3u8";

        String url2 = "https://stream.rcs.revma.com/an1ugyygzk8uv";
        String url3 = "https://github.com/mediaelement/mediaelement-files/blob/master/big_buck_bunny.mp4?raw=true";
        String url4 = "http://rss.art19.com/episodes/d93a35f0-e171-4a92-887b-35cee645f835.mp3";
        //mediaPlayer.setDataSource(url);
        MediaSourceInfo source1 = new MediaSourceInfo.Builder().setUrl(url4)
                .setTitle("Source 1")
                .setImageUrl("https://cdn.dribbble.com/users/20781/screenshots/573506/podcast_logo.jpg")
                .build();
        MediaSourceInfo source2 = new MediaSourceInfo.Builder().setUrl(url2)
                .setTitle("Source 2")
                .setImageUrl("https://www.benq.com/content/dam/b2c/en-au/campaign/4k-monitor/kv-city-m.jpg")
                .build();
        MediaSourceInfo source3 = new MediaSourceInfo.Builder().setUrl("https://sample-videos.com/audio/mp3/crowd-cheering.mp3") //http://stream3.polskieradio.pl:8904/;
                .setTitle("Source 3")
                .setImageUrl("https://s3-us-west-2.amazonaws.com/anchor-generated-image-bank/production/podcast_uploaded400/1415185/1415185-1549732984963-ac8825f57f7a6.jpg")
                .build();
        MediaSourceInfo source4 = new MediaSourceInfo.Builder().setUrl(url3) //http://stream3.polskieradio.pl:8904/;
                .setTitle("Source 4")
                .isVideo(true)
                .setImageUrl("https://images.podigee.com/0x,sHM1hqLl0xbqhcudNgPC1zIAQxNU0Zegm6V3mEOyurEc=/https://cdn.podigee.com/uploads/u10930/194e8b53-e0c3-449f-9c36-3a522f1c5e3c.png")
                .build();
        MediaSourceInfo source5 = new MediaSourceInfo.Builder().setUrl("http://rss.art19.com/episodes/d93a35f0-e171-4a92-887b-35cee645f835.mp3") //http://stream3.polskieradio.pl:8904/;
                .setTitle("Source 5")
                .setImageUrl("https://apynews.pl/~i/2019/08/Calcio_Truck-1.jpg")
                .build();
//        MediaSourceInfo source4 = new MediaSourceInfo.Builder().setUrl(url2)
//                .setTitle("Source 4")
//                .setImageUrl("http://www.pvhc.net/img29/amkulkkbogfvmihgspru.png")
//                .isVideo(true)
//                .build();

        sources = new ArrayList<>();
//        sources.add(source1);
        sources.add(source1);
        sources.add(source2);
        sources.add(source3);
        sources.add(source4);
        sources.add(source5);
    }

    private void createSources2() {
        String url = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3";
//        String url = "https://radio357.s3.eu-central-1.amazonaws.com/stream/c4223621-2600-45e4-93f1-ebe936f3c9b5.m3u8";
//        url = "http://devimages.apple.com/iphone/samples/bipbop/gear1/prog_index.m3u8";

        String url2 = "https://stream.rcs.revma.com/an1ugyygzk8uv";
        String url3 = "https://github.com/mediaelement/mediaelement-files/blob/master/big_buck_bunny.mp4?raw=true";
        String url4 = "http://rss.art19.com/episodes/d93a35f0-e171-4a92-887b-35cee645f835.mp3";
        //mediaPlayer.setDataSource(url);
        MediaSourceInfo source1 = new MediaSourceInfo.Builder().setUrl(url)
                .setTitle("Source 1")
                .setImageUrl("https://cdn.dribbble.com/users/20781/screenshots/573506/podcast_logo.jpg")
                .build();
        MediaSourceInfo source2 = new MediaSourceInfo.Builder().setUrl(url2)
                .setTitle("Source 2")
                .setImageUrl("https://www.benq.com/content/dam/b2c/en-au/campaign/4k-monitor/kv-city-m.jpg")
                .build();
        MediaSourceInfo source3 = new MediaSourceInfo.Builder().setUrl("https://sample-videos.com/audio/mp3/crowd-cheering.mp3") //http://stream3.polskieradio.pl:8904/;
                .setTitle("Source 3")
                .setImageUrl("https://s3-us-west-2.amazonaws.com/anchor-generated-image-bank/production/podcast_uploaded400/1415185/1415185-1549732984963-ac8825f57f7a6.jpg")
                .build();
        MediaSourceInfo source4 = new MediaSourceInfo.Builder().setUrl(url3) //http://stream3.polskieradio.pl:8904/;
                .setTitle("Source 4")
                .isVideo(true)
                .setImageUrl("https://images.podigee.com/0x,sHM1hqLl0xbqhcudNgPC1zIAQxNU0Zegm6V3mEOyurEc=/https://cdn.podigee.com/uploads/u10930/194e8b53-e0c3-449f-9c36-3a522f1c5e3c.png")
                .build();
        MediaSourceInfo source5 = new MediaSourceInfo.Builder().setUrl("http://rss.art19.com/episodes/d93a35f0-e171-4a92-887b-35cee645f835.mp3") //http://stream3.polskieradio.pl:8904/;
                .setTitle("Source 5")
                .setImageUrl("https://apynews.pl/~i/2019/08/Calcio_Truck-1.jpg")
                .build();
//        MediaSourceInfo source4 = new MediaSourceInfo.Builder().setUrl(url2)
//                .setTitle("Source 4")
//                .setImageUrl("http://www.pvhc.net/img29/amkulkkbogfvmihgspru.png")
//                .isVideo(true)
//                .build();

        sources = new ArrayList<>();
//        sources.add(source1);
        sources.add(source5);
        sources.add(source4);
        sources.add(source3);
        sources.add(source2);
        sources.add(source1);
    }


    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.btPlay) {
            if (mediaPlayer == null)
                createPlayer();
            else {
                mediaPlayer.play();
            }
        } else if (view.getId() == R.id.btPause) {
//            showTestNotification();

            if (mediaPlayer != null) {
                mediaPlayer.pause();

                Timber.d(String.valueOf(mediaPlayer.getCurrentPosition()));
                Timber.i(String.valueOf(mediaPlayer.getDuration()));
            }
        } else if (view.getId() == R.id.btSetSources) {
            createSources2();
            mediaPlayer.setDataSource(sources, sources, 0);
            mediaPlayer.prepare();
            mediaPlayer.seekTo(0,0);
        } else if (view.getId() == R.id.fastForward) {
            mediaPlayer.seekTo(mediaPlayer.getCurrentPosition() + 1500);
            Timber.e(String.valueOf(mediaPlayer.getCurrentPlayer().getCurrentWindowIndex()));
//            mediaPlayer.seekTo(mediaPlayer.getCurrentPosition() + 2000);
        } else if (view.getId() == R.id.btSpeed) {
//            loadOtherSources();
//            if (speed == 1)
//                speed = 2f;
//            else speed = 1;
//            mediaPlayer.setPlaybackParams(speed, 1);


//            int msec = mediaPlayer.getCurrentPosition() - 2000;
//            if(msec<0)
//                msec = 1;
//            mediaPlayer.seekTo(msec);

//            loadOtherSources();
            Timber.w(String.valueOf(mediaPlayer.getCurrentPosition()));
            Timber.i(String.valueOf(mediaPlayer.getCurrentWindow()));
            Timber.w(String.valueOf(mediaPlayer.getWindowCount()));
            Timber.d("duration " + mediaPlayer.getDuration());
        } else if (view.getId() == R.id.btStop) {
            mediaPlayer.release();
            mediaPlayer = null;
        } else if (view.getId() == R.id.btNext) {
//            pm.selectQueueItem(pm.getCurrentItemIndex()+1);
            Timber.d(String.valueOf(mediaPlayer.getCurrentWindow()));
            Timber.i("abc " + (mediaPlayer.getCurrentWindow() + 1) % mediaPlayer.getWindowCount() + " / " + mediaPlayer.getWindowCount());
            mediaPlayer.seekTo((mediaPlayer.getCurrentWindow() + 1) % mediaPlayer.getWindowCount(), 0);
            Timber.i(String.valueOf(mediaPlayer.getCurrentPlayer().getPlaybackState()));
//            Timber.i(mediaPlayer.getCurrentPlayer().getPlaybackError());
        } else if (view.getId() == R.id.btCreatePlayer) {
//            pm = PlayerManager.createPlayerManager(new PlayerManager.QueuePositionListener() {
//                @Override
//                public void onQueuePositionChanged(int previousIndex, int newIndex) {
//
//                }
//            }, this, castContext);
//            MediaSourceInfo source2 = new MediaSourceInfo.Builder().setUrl("http://rss.art19.com/episodes/d93a35f0-e171-4a92-887b-35cee645f835.mp3")
//                    .setTitle("Movie")
//                    .setImageUrl("http://www.pvhc.net/img29/amkulkkbogfvmihgspru.png")
//                    .build();
//            MediaSourceInfo source3 = new MediaSourceInfo.Builder().setUrl("http://rss.art19.com/episodes/d93a35f0-e171-4a92-887b-35cee645f835.mp3")
//                    .setTitle("Source 3")
//                    .setImageUrl("http://www.pvhc.net/img29/amkulkkbogfvmihgspru.png")
//                    .build();
//            pm.addItem(source2);
//            pm.addItem(source2);
//            pm.selectQueueItem(0);

            createPlayer();

        }
    }

    private int notificationCounter = 1;

    private void showTestNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createChannel(this);
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, TESTCHANNEL)
                .setSmallIcon(R.drawable.exo_notification_small_icon)
                .setContentTitle(Integer.toString(notificationCounter))
                .setContentText("noti")
                .setGroup(GROUP_1)
                .setColor(ContextCompat.getColor(this, R.color.cast_expanded_controller_ad_container_white_stripe_color));


        NotificationCompat.Builder gruobuilder = new NotificationCompat.Builder(this, TESTCHANNEL)
                .setSmallIcon(R.drawable.exo_notification_small_icon)
                .setContentTitle("GROUP " + Integer.toString(notificationCounter))
                .setContentText("noti")
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setGroup(GROUP_1)
                .setGroupSummary(true)
                .setColor(ContextCompat.getColor(this, R.color.cast_expanded_controller_ad_container_white_stripe_color));


        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationCounter == 1)
            manager.notify(999, gruobuilder.build());
        manager.notify(notificationCounter, builder.build());
//        manager.notify(id, builder.build());


        notificationCounter++;

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            StatusBarNotification[] statusBarNotifications = manager.getActiveNotifications();
            int counter = 0;
            for (StatusBarNotification statusBarNotification : statusBarNotifications) {
                if (statusBarNotification.getGroupKey().contains(GROUP_1))
                    counter++;
            }

            if (counter == 1) {
                manager.cancel(999);
            }
        }

    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private static void createChannel(Context context) {
        NotificationManager
                mNotificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        // The id of the channel.
        String id = TESTCHANNEL;
        // The user-visible name of the channel.
        CharSequence name = id;
        // The user-visible description of the channel.
        String description = id;
        int importance = NotificationManager.IMPORTANCE_HIGH;

        NotificationChannel mChannel = new NotificationChannel(id, name, importance);
        // Configure the notification channel.
        mChannel.setDescription(description);
        mChannel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
        mNotificationManager.createNotificationChannel(mChannel);
    }

    private void loadOtherSources() {
        List<MediaSourceInfo> sources2 = new ArrayList<>();
        MediaSourceInfo source = new MediaSourceInfo.Builder().setUrl("http://api.spreaker.com/download/episode/14404535/dlaczego_rezygnujemy.mp3")
                .setTitle("Source 1")
                .setImageUrl("https://github.com/mkaflowski/HybridMediaPlayer/blob/master/images/cover.jpg?raw=true")
                .build();
        MediaSourceInfo source2 = new MediaSourceInfo.Builder().setUrl("http://api.spreaker.com/download/episode/14404535/dlaczego_rezygnujemy.mp3")
                .setTitle("Source 2")
                .setImageUrl("https://github.com/mkaflowski/HybridMediaPlayer/blob/master/images/cover.jpg?raw=true")
                .build();
        MediaSourceInfo source3 = new MediaSourceInfo.Builder().setUrl("http://api.spreaker.com/download/episode/14404535/dlaczego_rezygnujemy.mp3")
                .setTitle("Source 3")
                .setImageUrl("https://github.com/mkaflowski/HybridMediaPlayer/blob/master/images/cover.jpg?raw=true")
                .build();

        sources2.add(source);
        sources2.add(source2);
//        sources2.add(source3);
        mediaPlayer.setInitialWindowNum(1);
        mediaPlayer.setDataSource(sources2, sources2, 0);
        mediaPlayer.prepare();
        mediaPlayer.seekTo(10000);

    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.options, menu);
        CastButtonFactory.setUpMediaRouteButton(this, menu, R.id.media_route_menu_item);
        return true;
    }


}