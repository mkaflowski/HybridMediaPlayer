/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package hybridmediaplayer;

import android.content.Context;
import android.net.Uri;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.DefaultRenderersFactory;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.Player.DefaultEventListener;
import com.google.android.exoplayer2.Player.DiscontinuityReason;
import com.google.android.exoplayer2.RenderersFactory;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.Timeline.Period;
import com.google.android.exoplayer2.ext.cast.CastPlayer;
import com.google.android.exoplayer2.extractor.ExtractorsFactory;
import com.google.android.exoplayer2.source.ConcatenatingMediaSource;
import com.google.android.exoplayer2.source.DynamicConcatenatingMediaSource;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.dash.DashMediaSource;
import com.google.android.exoplayer2.source.dash.DefaultDashChunkSource;

import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;

import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSource;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory;
import com.google.android.exoplayer2.util.MimeTypes;
import com.google.android.exoplayer2.util.Util;
import com.google.android.gms.cast.MediaInfo;
import com.google.android.gms.cast.MediaMetadata;
import com.google.android.gms.cast.MediaQueueItem;
import com.google.android.gms.cast.framework.CastContext;
import com.google.android.gms.common.images.WebImage;
import com.socks.library.KLog;

import java.util.ArrayList;

/**
 * Manages players and an internal media queue for the ExoPlayer/Cast demo app.
 */
/* package */ public final class PlayerManager extends DefaultEventListener
        implements CastPlayer.SessionAvailabilityListener {

  private static Context context;
  private boolean isInitialPlay = true;

  /**
   * Listener for changes in the media queue playback position.
   */
  public interface QueuePositionListener {

    /**
     * Called when the currently played item of the media queue changes.
     */
    void onQueuePositionChanged(int previousIndex, int newIndex);

  }

  private static final String USER_AGENT = "ExoCastDemoPlayer";
  private static final DefaultBandwidthMeter BANDWIDTH_METER = new DefaultBandwidthMeter();
  private static final DefaultHttpDataSourceFactory DATA_SOURCE_FACTORY =
          new DefaultHttpDataSourceFactory(USER_AGENT, BANDWIDTH_METER);


  private final SimpleExoPlayer exoPlayer;
  private final CastPlayer castPlayer;
  private final ArrayList<MediaSourceInfo> mediaQueue;
  private final QueuePositionListener queuePositionListener;

  private DynamicConcatenatingMediaSource dynamicConcatenatingMediaSource;
  private boolean castMediaQueueCreationPending;
  private int currentItemIndex;
  private Player currentPlayer;

  public static PlayerManager createPlayerManager(
          QueuePositionListener queuePositionListener,
          Context context,
          CastContext castContext) {
    PlayerManager playerManager =
            new PlayerManager(
                    queuePositionListener, context, castContext);
    playerManager.init();
    return playerManager;
  }

  private PlayerManager(
          QueuePositionListener queuePositionListener,
          Context context,
          CastContext castContext) {

    PlayerManager.context = context;

    this.queuePositionListener = queuePositionListener;

    mediaQueue = new ArrayList<>();
    currentItemIndex = C.INDEX_UNSET;

    DefaultTrackSelector trackSelector = new DefaultTrackSelector(BANDWIDTH_METER);
    RenderersFactory renderersFactory = new DefaultRenderersFactory(context, null);
    exoPlayer = ExoPlayerFactory.newSimpleInstance(renderersFactory, trackSelector);
    exoPlayer.addListener(this);


    castPlayer = new CastPlayer(castContext);
    castPlayer.addListener(this);
    castPlayer.setSessionAvailabilityListener(this);

  }

  // Queue manipulation methods.

  /**
   * Plays a specified queue item in the current player.
   *
   * @param itemIndex The index of the item to play.
   */
  public void selectQueueItem(int itemIndex) {
    setCurrentItem(itemIndex, C.TIME_UNSET, true);
  }

  /**
   * Returns the index of the currently played item.
   */
  public int getCurrentItemIndex() {
    return currentItemIndex;
  }


  public void addItem(MediaSourceInfo sample) {
    mediaQueue.add(sample);
    if (currentPlayer == exoPlayer) {
      dynamicConcatenatingMediaSource.addMediaSource(buildMediaSource(sample));
    } else {
      castPlayer.addItems(buildMediaQueueItem(sample));
    }
  }

  /**
   * Returns the size of the media queue.
   */
  public int getMediaQueueSize() {
    return mediaQueue.size();
  }

  /**
   * Returns the item at the given index in the media queue.
   *
   * @param position The index of the item.
   * @return The item at the given index in the media queue.
   */
  public MediaSourceInfo getItem(int position) {
    return mediaQueue.get(position);
  }

  /**
   * Removes the item at the given index from the media queue.
   *
   * @param itemIndex The index of the item to remove.
   * @return Whether the removal was successful.
   */
  public boolean removeItem(int itemIndex) {
    if (currentPlayer == exoPlayer) {
      dynamicConcatenatingMediaSource.removeMediaSource(itemIndex);
    } else {
      if (castPlayer.getPlaybackState() != Player.STATE_IDLE) {
        Timeline castTimeline = castPlayer.getCurrentTimeline();
        if (castTimeline.getPeriodCount() <= itemIndex) {
          return false;
        }
        castPlayer.removeItem((int) castTimeline.getPeriod(itemIndex, new Period()).id);
      }
    }
    mediaQueue.remove(itemIndex);
    if (itemIndex == currentItemIndex && itemIndex == mediaQueue.size()) {
      maybeSetCurrentItemAndNotify(C.INDEX_UNSET);
    } else if (itemIndex < currentItemIndex) {
      maybeSetCurrentItemAndNotify(currentItemIndex - 1);
    }
    return true;
  }

  /**
   * Moves an item within the queue.
   *
   * @param fromIndex The index of the item to move.
   * @param toIndex   The target index of the item in the queue.
   * @return Whether the item move was successful.
   */
  public boolean moveItem(int fromIndex, int toIndex) {
    // Player update.
    if (currentPlayer == exoPlayer) {
      dynamicConcatenatingMediaSource.moveMediaSource(fromIndex, toIndex);
    } else if (castPlayer.getPlaybackState() != Player.STATE_IDLE) {
      Timeline castTimeline = castPlayer.getCurrentTimeline();
      int periodCount = castTimeline.getPeriodCount();
      if (periodCount <= fromIndex || periodCount <= toIndex) {
        return false;
      }
      int elementId = (int) castTimeline.getPeriod(fromIndex, new Period()).id;
      castPlayer.moveItem(elementId, toIndex);
    }

    mediaQueue.add(toIndex, mediaQueue.remove(fromIndex));

    // Index update.
    if (fromIndex == currentItemIndex) {
      maybeSetCurrentItemAndNotify(toIndex);
    } else if (fromIndex < currentItemIndex && toIndex >= currentItemIndex) {
      maybeSetCurrentItemAndNotify(currentItemIndex - 1);
    } else if (fromIndex > currentItemIndex && toIndex <= currentItemIndex) {
      maybeSetCurrentItemAndNotify(currentItemIndex + 1);
    }

    return true;
  }

  // Miscellaneous methods.


  /**
   * Releases the manager and the players that it holds.
   */
  public void release() {
    currentItemIndex = C.INDEX_UNSET;
    mediaQueue.clear();
    castPlayer.setSessionAvailabilityListener(null);
    castPlayer.release();
    exoPlayer.release();
  }

  // Player.EventListener implementation.


  @Override
  public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
    Log.d("abc", playbackState +" "+ currentPlayer.toString()+"");
    updateCurrentItemIndex();
  }

  @Override
  public void onPositionDiscontinuity(@DiscontinuityReason int reason) {
    updateCurrentItemIndex();
  }

  @Override
  public void onTimelineChanged(Timeline timeline, Object manifest) {
    updateCurrentItemIndex();
  }

  // CastPlayer.SessionAvailabilityListener implementation.

  @Override
  public void onCastSessionAvailable() {
    setCurrentPlayer(castPlayer);
  }

  @Override
  public void onCastSessionUnavailable() {
    Log.d("time", currentPlayer.getDuration()+ "");
    setCurrentPlayer(exoPlayer);
  }

  // Internal methods.

  private void init() {
    setCurrentPlayer(exoPlayer);
  }

  private void updateCurrentItemIndex() {
    int playbackState = currentPlayer.getPlaybackState();
    maybeSetCurrentItemAndNotify(
            playbackState != Player.STATE_IDLE && playbackState != Player.STATE_ENDED
                    ? currentPlayer.getCurrentWindowIndex() : C.INDEX_UNSET);
  }

  private void setCurrentPlayer(Player currentPlayer) {
    if (this.currentPlayer == currentPlayer) {
      return;
    }

    if(isInitialPlay)
    {
      KLog.d("abc "+currentPlayer);
//      isInitialPlay = false;
//      setCurrentPlayer(exoPlayer);
//
//      setCurrentPlayer(castPlayer);
//      return;
    }

    // Player state management.
    long playbackPositionMs = C.TIME_UNSET;
    int windowIndex = C.INDEX_UNSET;
    boolean playWhenReady = false;
    if (this.currentPlayer != null) {
      int playbackState = this.currentPlayer.getPlaybackState();
      if (playbackState != Player.STATE_ENDED) {
        playbackPositionMs = this.currentPlayer.getCurrentPosition();
        playWhenReady = this.currentPlayer.getPlayWhenReady();
        windowIndex = this.currentPlayer.getCurrentWindowIndex();
        if (windowIndex != currentItemIndex) {
          playbackPositionMs = C.TIME_UNSET;
          windowIndex = currentItemIndex;
        }
      }
      this.currentPlayer.stop(true);
    } else {
      // This is the initial setup. No need to save any state.
    }

    this.currentPlayer = currentPlayer;

    // Media queue management.
    castMediaQueueCreationPending = currentPlayer == castPlayer;
    if (currentPlayer == exoPlayer) {
      dynamicConcatenatingMediaSource = new DynamicConcatenatingMediaSource();
      for (int i = 0; i < mediaQueue.size(); i++) {
        dynamicConcatenatingMediaSource.addMediaSource(buildMediaSource(mediaQueue.get(i)));
      }
      exoPlayer.prepare(dynamicConcatenatingMediaSource);
    }

    // Playback transition.
    if (windowIndex != C.INDEX_UNSET) {
      setCurrentItem(windowIndex, playbackPositionMs, playWhenReady);
    }
  }

  /**
   * Starts playback of the item at the given position.
   *
   * @param itemIndex     The index of the item to play.
   * @param positionMs    The position at which playback should start.
   * @param playWhenReady Whether the player should proceed when ready to do so.
   */
  private void setCurrentItem(int itemIndex, long positionMs, boolean playWhenReady) {
    setCurrentPlayer(castPlayer.isCastSessionAvailable() ? castPlayer : exoPlayer);

    maybeSetCurrentItemAndNotify(itemIndex);
    if (castMediaQueueCreationPending) {
      MediaQueueItem[] items = new MediaQueueItem[mediaQueue.size()];
      for (int i = 0; i < items.length; i++) {
        items[i] = buildMediaQueueItem(mediaQueue.get(i));
      }
      castMediaQueueCreationPending = false;
      castPlayer.loadItems(items, itemIndex, positionMs, Player.REPEAT_MODE_OFF);
    } else {
      currentPlayer.seekTo(itemIndex, positionMs);
      currentPlayer.setPlayWhenReady(playWhenReady);
    }
  }

  private void maybeSetCurrentItemAndNotify(int currentItemIndex) {
    if (this.currentItemIndex != currentItemIndex) {
      int oldIndex = this.currentItemIndex;
      this.currentItemIndex = currentItemIndex;
      queuePositionListener.onQueuePositionChanged(oldIndex, currentItemIndex);
    }
  }

  private static MediaSource buildMediaSource(MediaSourceInfo sample) {
    String userAgent = Util.getUserAgent(context, "yourApplicationName");
    DefaultHttpDataSourceFactory httpDataSourceFactory = new DefaultHttpDataSourceFactory(
            userAgent,
            null /* listener */,
            DefaultHttpDataSource.DEFAULT_CONNECT_TIMEOUT_MILLIS,
            DefaultHttpDataSource.DEFAULT_READ_TIMEOUT_MILLIS,
            true /* allowCrossProtocolRedirects */
    );
    // Produces DataSource instances through which media data is loaded.
    DataSource.Factory dataSourceFactory = new DefaultDataSourceFactory(context, null, httpDataSourceFactory);
    // Produces Extractor instances for parsing the media data.
    ExtractorsFactory extractorsFactory = new SeekableExtractorsFactory();
    return new ExtractorMediaSource(Uri.parse(sample.getUrl()),
            dataSourceFactory, extractorsFactory, null, null);
  }

  private static MediaQueueItem buildMediaQueueItem(MediaSourceInfo sample) {
    MediaMetadata movieMetadata = new MediaMetadata(MediaMetadata.MEDIA_TYPE_MOVIE);
    movieMetadata.putString(MediaMetadata.KEY_TITLE, sample.getTitle());
    movieMetadata.addImage(new WebImage(Uri.parse(sample.getImageUrl())));
    MediaInfo mediaInfo = new MediaInfo.Builder(sample.getUrl())
            .setStreamType(MediaInfo.STREAM_TYPE_BUFFERED).setContentType( MimeTypes.AUDIO_UNKNOWN)
            .setMetadata(movieMetadata).build();
    return new MediaQueueItem.Builder(mediaInfo).build();
  }

}
