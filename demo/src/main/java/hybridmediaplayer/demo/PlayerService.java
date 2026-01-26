package hybridmediaplayer.demo;

import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.media3.common.AudioAttributes;
import androidx.media3.common.C;
import androidx.media3.common.MediaItem;
import androidx.media3.common.MediaMetadata;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.session.LibraryResult;
import androidx.media3.session.MediaLibraryService;
import androidx.media3.session.MediaSession;
import androidx.media3.session.SessionError;

import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@UnstableApi
public class PlayerService extends MediaLibraryService {

    private MediaLibrarySession mediaLibrarySession;
    private ExoPlayer player;

    private static final String ROOT_ID = "root";
    private static final String PLAYLIST_ID = "playlist";

    private static final List<MediaItem> SAMPLE_ITEMS = Arrays.asList(
            new MediaItem.Builder()
                    .setMediaId("1")
                    .setUri("https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3")
                    .setMediaMetadata(
                            new MediaMetadata.Builder()
                                    .setTitle("SoundHelix Song 1")
                                    .setArtist("SoundHelix")
                                    .setIsPlayable(true)
                                    .build()
                    )
                    .build(),
            new MediaItem.Builder()
                    .setMediaId("2")
                    .setUri("https://www.soundhelix.com/examples/mp3/SoundHelix-Song-2.mp3")
                    .setMediaMetadata(
                            new MediaMetadata.Builder()
                                    .setTitle("SoundHelix Song 2")
                                    .setArtist("SoundHelix")
                                    .setIsPlayable(true)
                                    .build()
                    )
                    .build(),
            new MediaItem.Builder()
                    .setMediaId("3")
                    .setUri("https://www.soundhelix.com/examples/mp3/SoundHelix-Song-3.mp3")
                    .setMediaMetadata(
                            new MediaMetadata.Builder()
                                    .setTitle("SoundHelix Song 3")
                                    .setArtist("SoundHelix")
                                    .setIsPlayable(true)
                                    .build()
                    )
                    .build()
    );

    @Override
    public void onCreate() {
        super.onCreate();

        AudioAttributes audioAttributes = new AudioAttributes.Builder()
                .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                .setUsage(C.USAGE_MEDIA)
                .build();

        player = new ExoPlayer.Builder(this)
                .setAudioAttributes(audioAttributes, true)
                .setHandleAudioBecomingNoisy(true)
                .build();

        Intent launchIntent = getPackageManager().getLaunchIntentForPackage(getPackageName());
        PendingIntent sessionActivityPendingIntent = null;
        if (launchIntent != null) {
            sessionActivityPendingIntent = PendingIntent.getActivity(
                    this,
                    0,
                    launchIntent,
                    PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT
            );
        }

        MediaLibrarySession.Builder builder = new MediaLibrarySession.Builder(
                this,
                player,
                new LibrarySessionCallback()
        );

        if (sessionActivityPendingIntent != null) {
            builder.setSessionActivity(sessionActivityPendingIntent);
        }

        mediaLibrarySession = builder.build();

        // Add tracks to player and start playback
        player.setMediaItems(SAMPLE_ITEMS);
        player.prepare();
        player.play();
    }

    @Nullable
    @Override
    public MediaLibrarySession onGetSession(@NonNull MediaSession.ControllerInfo controllerInfo) {
        return mediaLibrarySession;
    }

    @Override
    public void onDestroy() {
        if (mediaLibrarySession != null) {
            if (player != null) {
                player.release();
                player = null;
            }
            mediaLibrarySession.release();
            mediaLibrarySession = null;
        }
        super.onDestroy();
    }

    private class LibrarySessionCallback implements MediaLibrarySession.Callback {

        @NonNull
        @Override
        public ListenableFuture<LibraryResult<MediaItem>> onGetLibraryRoot(
                @NonNull MediaLibrarySession session,
                @NonNull MediaSession.ControllerInfo browser,
                @Nullable LibraryParams params
        ) {
            MediaItem rootItem = new MediaItem.Builder()
                    .setMediaId(ROOT_ID)
                    .setMediaMetadata(
                            new MediaMetadata.Builder()
                                    .setTitle("Root")
                                    .setIsBrowsable(true)
                                    .setIsPlayable(false)
                                    .setMediaType(MediaMetadata.MEDIA_TYPE_FOLDER_MIXED)
                                    .build()
                    )
                    .build();
            return Futures.immediateFuture(LibraryResult.ofItem(rootItem, params));
        }

        @SuppressLint("WrongConstant")
        @NonNull
        @Override
        public ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> onGetChildren(
                @NonNull MediaLibrarySession session,
                @NonNull MediaSession.ControllerInfo browser,
                @NonNull String parentId,
                int page,
                int pageSize,
                @Nullable LibraryParams params
        ) {
            switch (parentId) {
                case ROOT_ID: {
                    MediaItem playlistItem = new MediaItem.Builder()
                            .setMediaId(PLAYLIST_ID)
                            .setMediaMetadata(
                                    new MediaMetadata.Builder()
                                            .setTitle("Sample Playlist")
                                            .setIsBrowsable(true)
                                            .setIsPlayable(false)
                                            .setMediaType(MediaMetadata.MEDIA_TYPE_PLAYLIST)
                                            .build()
                            )
                            .build();
                    return Futures.immediateFuture(
                            LibraryResult.ofItemList(ImmutableList.of(playlistItem), params)
                    );
                }
                case PLAYLIST_ID: {
                    return Futures.immediateFuture(
                            LibraryResult.ofItemList(ImmutableList.copyOf(SAMPLE_ITEMS), params)
                    );
                }
                default: {
                    return Futures.immediateFuture(
                            LibraryResult.ofError(SessionError.ERROR_BAD_VALUE)
                    );
                }
            }
        }

        @NonNull
        @Override
        public ListenableFuture<LibraryResult<MediaItem>> onGetItem(
                @NonNull MediaLibrarySession session,
                @NonNull MediaSession.ControllerInfo browser,
                @NonNull String mediaId
        ) {
            for (MediaItem item : SAMPLE_ITEMS) {
                if (item.mediaId.equals(mediaId)) {
                    return Futures.immediateFuture(LibraryResult.ofItem(item, null));
                }
            }
            return Futures.immediateFuture(LibraryResult.ofError(SessionError.ERROR_BAD_VALUE));
        }

        @NonNull
        @Override
        public ListenableFuture<List<MediaItem>> onAddMediaItems(
                @NonNull MediaSession mediaSession,
                @NonNull MediaSession.ControllerInfo controller,
                @NonNull List<MediaItem> mediaItems
        ) {
            List<MediaItem> resolvedItems = new ArrayList<>();
            for (MediaItem requestedItem : mediaItems) {
                MediaItem foundItem = null;
                for (MediaItem sampleItem : SAMPLE_ITEMS) {
                    if (sampleItem.mediaId.equals(requestedItem.mediaId)) {
                        foundItem = sampleItem;
                        break;
                    }
                }
                resolvedItems.add(foundItem != null ? foundItem : requestedItem);
            }
            return Futures.immediateFuture(resolvedItems);
        }
    }
}