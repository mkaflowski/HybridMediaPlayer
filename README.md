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
        HybridMediaPlayer mediaPLayer = HybridMediaPLayer.getInstance(context);
        mediaPlayer = new ExoMediaPlayer(this);
        mediaPlayer.setDataSource(url);
        mediaPlayer.prepare();

        mediaPLayer.setOnCompletionListener(this);
        mediaPLayer.setOnErrorListener(this);
        mediaPLayer.setOnPreparedListener(this);
        
        mediaPLayer.play();
        mediaPLayer.seekTo(1500);
        mediaPLayer.pause();
        
        mediaPLayer.release();
```

