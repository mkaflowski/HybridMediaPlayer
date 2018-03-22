# HybridMediaPlayer
Android music player from URL. Uses ExoPlayer and MediaPlayer for lower APIs.

## Installation

To use the library, first include it your project using Gradle

    allprojects {
        repositories {
            jcenter()
            maven { url "https://jitpack.io" }
        }
    }

	dependencies {
	        compile 'com.github.mkaflowski:HybridMediaPlayer:1.x'
	}
	

## How to use

```java
        HybridMediaPlayer mediaPlayer = HybridMediaPlayer.getInstance(context);
        mediaPlayer.setDataSource(url);
	mediaPlayer.setPlayerView(this, playerView);
	
        mediaPlayer.prepare();

        mediaPlayer.setOnCompletionListener(this);
        mediaPlayer.setOnErrorListener(this);
        mediaPlayer.setOnPreparedListener(this);
        
        mediaPlayer.play();
        mediaPlayer.seekTo(1500);
        mediaPlayer.pause();
	
	mediaPlayer.setVolume(0.5f);
        
        mediaPlayer.release();
```

### Methods for ExoPlayer only

```java
        ExoMediaPlayer mediaPlayer = new ExoMediaPlayer(this)
        mediaPlayer.setDataSource(url1, url2, url3, ...);
	
	//for proper Chromecast handling
	mediaPlayer.setDataSource(mediaSourceInfoList);
	mediaPlayer.setCastPlayer(castContext);
	
	//setting video view:
	mediaPlayer.setPlayerView(this, playerView);
	
        mediaPlayer.prepare();

        mediaPlayer.setOnPositionDiscontinuityListener(this);
	mediaPlayer.setOnTracksChangedListener(this);
        
	mediaPlayer.hasVideo();
        mediaPlayer.seekTo(windowPosition,time);
	
	mediaPlayer.setSupportingSystemEqualizer(true);
	//FOR EDITING PLAYLIST
	mediaPlayer.getMediaSource();
	
        mediaPlayer.release();
```
### Creating MediaSourceInfo for proper Chromecast handling example

```java
        MediaSourceInfo source1 = new MediaSourceInfo.Builder().setUrl(url)
                .setTitle("Podcast Stream")
                .setImageUrl("https://cdn.dribbble.com/users/20781/screenshots/573506/podcast_logo.jpg")
                .build();
        MediaSourceInfo source2 = new MediaSourceInfo.Builder().setUrl(url3)
                .setTitle("Movie")
                .setImageUrl("http://www.pvhc.net/img29/amkulkkbogfvmihgspru.png")
                .isVideo(true)
                .build();

        List<MediaSourceInfo> sources = new ArrayList<>();
        sources.add(source1);
        sources.add(source2);

        mediaPlayer.setDataSource(sources);
	mediaPlayer.setCastPlayer(castContext);
```
