package hybridmediaplayer;

import android.net.Uri;

import androidx.annotation.Nullable;
import androidx.media3.common.C;
import androidx.media3.common.MediaItem;
import androidx.media3.common.MediaMetadata;
import androidx.media3.common.MimeTypes;
import androidx.media3.common.util.Assertions;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.cast.MediaItemConverter;

import com.google.android.gms.cast.HlsSegmentFormat;
import com.google.android.gms.cast.MediaInfo;
import com.google.android.gms.cast.MediaQueueItem;
import com.google.android.gms.common.images.WebImage;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Iterator;
import java.util.UUID;

@UnstableApi
public class HlsMediaItemConverter implements MediaItemConverter {
    private static final String KEY_MEDIA_ITEM = "mediaItem";
    private static final String KEY_PLAYER_CONFIG = "exoPlayerConfig";
    private static final String KEY_MEDIA_ID = "mediaId";
    private static final String KEY_URI = "uri";
    private static final String KEY_TITLE = "title";
    private static final String KEY_MIME_TYPE = "mimeType";
    private static final String KEY_DRM_CONFIGURATION = "drmConfiguration";
    private static final String KEY_UUID = "uuid";
    private static final String KEY_LICENSE_URI = "licenseUri";
    private static final String KEY_REQUEST_HEADERS = "requestHeaders";

    public HlsMediaItemConverter() {
    }

    @Override
    public MediaItem toMediaItem(MediaQueueItem mediaQueueItem) {
        MediaInfo mediaInfo = mediaQueueItem.getMedia();
        Assertions.checkNotNull(mediaInfo);

        MediaMetadata.Builder metadataBuilder = new MediaMetadata.Builder();
        com.google.android.gms.cast.MediaMetadata metadata = mediaInfo.getMetadata();

        if (metadata != null) {
            if (metadata.containsKey(com.google.android.gms.cast.MediaMetadata.KEY_TITLE)) {
                metadataBuilder.setTitle(metadata.getString(com.google.android.gms.cast.MediaMetadata.KEY_TITLE));
            }

            if (metadata.containsKey(com.google.android.gms.cast.MediaMetadata.KEY_SUBTITLE)) {
                metadataBuilder.setSubtitle(metadata.getString(com.google.android.gms.cast.MediaMetadata.KEY_SUBTITLE));
            }

            if (metadata.containsKey(com.google.android.gms.cast.MediaMetadata.KEY_ARTIST)) {
                metadataBuilder.setArtist(metadata.getString(com.google.android.gms.cast.MediaMetadata.KEY_ARTIST));
            }

            if (metadata.containsKey(com.google.android.gms.cast.MediaMetadata.KEY_ALBUM_ARTIST)) {
                metadataBuilder.setAlbumArtist(metadata.getString(com.google.android.gms.cast.MediaMetadata.KEY_ALBUM_ARTIST));
            }

            if (metadata.containsKey(com.google.android.gms.cast.MediaMetadata.KEY_ALBUM_TITLE)) {
                metadataBuilder.setAlbumTitle(metadata.getString(com.google.android.gms.cast.MediaMetadata.KEY_ALBUM_TITLE));
            }

            if (!metadata.getImages().isEmpty()) {
                metadataBuilder.setArtworkUri(((WebImage) metadata.getImages().get(0)).getUrl());
            }

            if (metadata.containsKey(com.google.android.gms.cast.MediaMetadata.KEY_COMPOSER)) {
                metadataBuilder.setComposer(metadata.getString(com.google.android.gms.cast.MediaMetadata.KEY_COMPOSER));
            }

            if (metadata.containsKey(com.google.android.gms.cast.MediaMetadata.KEY_DISC_NUMBER)) {
                metadataBuilder.setDiscNumber(metadata.getInt(com.google.android.gms.cast.MediaMetadata.KEY_DISC_NUMBER));
            }

            if (metadata.containsKey(com.google.android.gms.cast.MediaMetadata.KEY_TRACK_NUMBER)) {
                metadataBuilder.setTrackNumber(metadata.getInt(com.google.android.gms.cast.MediaMetadata.KEY_TRACK_NUMBER));
            }
        }

        return getMediaItem((JSONObject) Assertions.checkNotNull(mediaInfo.getCustomData()), metadataBuilder.build());
    }

    @Override
    public MediaQueueItem toMediaQueueItem(MediaItem mediaItem) {
        MediaItem.LocalConfiguration localConfig = Assertions.checkNotNull(mediaItem.localConfiguration);

        if (localConfig.mimeType == null) {
            throw new IllegalArgumentException("The item must specify its mimeType");
        }

        String contentUrl = localConfig.uri.toString();

        // Określ typ metadata (audio vs video)
        int metadataType = MimeTypes.isAudio(localConfig.mimeType) || contentUrl.contains("radio")
                ? com.google.android.gms.cast.MediaMetadata.MEDIA_TYPE_MUSIC_TRACK
                : com.google.android.gms.cast.MediaMetadata.MEDIA_TYPE_MOVIE;

        com.google.android.gms.cast.MediaMetadata metadata = new com.google.android.gms.cast.MediaMetadata(metadataType);

        if (mediaItem.mediaMetadata.title != null) {
            metadata.putString(com.google.android.gms.cast.MediaMetadata.KEY_TITLE,
                    mediaItem.mediaMetadata.title.toString());
        }

        if (mediaItem.mediaMetadata.subtitle != null) {
            metadata.putString(com.google.android.gms.cast.MediaMetadata.KEY_SUBTITLE,
                    mediaItem.mediaMetadata.subtitle.toString());
        }

        if (mediaItem.mediaMetadata.artist != null) {
            metadata.putString(com.google.android.gms.cast.MediaMetadata.KEY_ARTIST,
                    mediaItem.mediaMetadata.artist.toString());
        }

        if (mediaItem.mediaMetadata.albumArtist != null) {
            metadata.putString(com.google.android.gms.cast.MediaMetadata.KEY_ALBUM_ARTIST,
                    mediaItem.mediaMetadata.albumArtist.toString());
        }

        if (mediaItem.mediaMetadata.albumTitle != null) {
            metadata.putString(com.google.android.gms.cast.MediaMetadata.KEY_ALBUM_TITLE,
                    mediaItem.mediaMetadata.albumTitle.toString());
        }

        if (mediaItem.mediaMetadata.artworkUri != null) {
            metadata.addImage(new WebImage(mediaItem.mediaMetadata.artworkUri));
        }

        if (mediaItem.mediaMetadata.composer != null) {
            metadata.putString(com.google.android.gms.cast.MediaMetadata.KEY_COMPOSER,
                    mediaItem.mediaMetadata.composer.toString());
        }

        if (mediaItem.mediaMetadata.discNumber != null) {
            metadata.putInt(com.google.android.gms.cast.MediaMetadata.KEY_DISC_NUMBER,
                    mediaItem.mediaMetadata.discNumber);
        }

        if (mediaItem.mediaMetadata.trackNumber != null) {
            metadata.putInt(com.google.android.gms.cast.MediaMetadata.KEY_TRACK_NUMBER,
                    mediaItem.mediaMetadata.trackNumber);
        }

        String contentId = mediaItem.mediaId.isEmpty() ? contentUrl : mediaItem.mediaId;
        MediaInfo.Builder mediaInfoBuilder = new MediaInfo.Builder(contentId);

        // Obsługa HLS streamów
        if (contentUrl.contains(".m3u8")) {
            mediaInfoBuilder.setHlsSegmentFormat(HlsSegmentFormat.AAC);
            mediaInfoBuilder.setStreamType(MediaInfo.STREAM_TYPE_BUFFERED);
            mediaInfoBuilder.setContentType("application/x-mpegURL");
        } else {
            mediaInfoBuilder.setStreamType(MediaInfo.STREAM_TYPE_BUFFERED);
            mediaInfoBuilder.setContentType(localConfig.mimeType);
        }

        MediaInfo mediaInfo = mediaInfoBuilder
                .setContentUrl(contentUrl)
                .setMetadata(metadata)
                .setCustomData(getCustomData(mediaItem))
                .build();

        return new MediaQueueItem.Builder(mediaInfo).build();
    }

    private static MediaItem getMediaItem(JSONObject customData, MediaMetadata mediaMetadata) {
        try {
            JSONObject mediaItemJson = customData.getJSONObject(KEY_MEDIA_ITEM);
            MediaItem.Builder builder = new MediaItem.Builder()
                    .setUri(Uri.parse(mediaItemJson.getString(KEY_URI)))
                    .setMediaId(mediaItemJson.getString(KEY_MEDIA_ID))
                    .setMediaMetadata(mediaMetadata);

            if (mediaItemJson.has(KEY_MIME_TYPE)) {
                builder.setMimeType(mediaItemJson.getString(KEY_MIME_TYPE));
            }

            if (mediaItemJson.has(KEY_DRM_CONFIGURATION)) {
                populateDrmConfiguration(mediaItemJson.getJSONObject(KEY_DRM_CONFIGURATION), builder);
            }

            return builder.build();
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    private static void populateDrmConfiguration(JSONObject json, MediaItem.Builder mediaItem) throws JSONException {
        MediaItem.DrmConfiguration.Builder drmConfiguration = new MediaItem.DrmConfiguration.Builder(
                UUID.fromString(json.getString(KEY_UUID)))
                .setLicenseUri(json.getString(KEY_LICENSE_URI));

        JSONObject requestHeadersJson = json.getJSONObject(KEY_REQUEST_HEADERS);
        HashMap<String, String> requestHeaders = new HashMap<>();
        Iterator<String> iterator = requestHeadersJson.keys();

        while (iterator.hasNext()) {
            String key = iterator.next();
            requestHeaders.put(key, requestHeadersJson.getString(key));
        }

        drmConfiguration.setLicenseRequestHeaders(requestHeaders);
        mediaItem.setDrmConfiguration(drmConfiguration.build());
    }

    private static JSONObject getCustomData(MediaItem mediaItem) {
        JSONObject json = new JSONObject();

        try {
            json.put(KEY_MEDIA_ITEM, getMediaItemJson(mediaItem));
            JSONObject playerConfigJson = getPlayerConfigJson(mediaItem);
            if (playerConfigJson != null) {
                json.put(KEY_PLAYER_CONFIG, playerConfigJson);
            }

            return json;
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    private static JSONObject getMediaItemJson(MediaItem mediaItem) throws JSONException {
        MediaItem.LocalConfiguration localConfig = Assertions.checkNotNull(mediaItem.localConfiguration);

        JSONObject json = new JSONObject();
        json.put(KEY_MEDIA_ID, mediaItem.mediaId);
        json.put(KEY_TITLE, mediaItem.mediaMetadata.title);
        json.put(KEY_URI, localConfig.uri.toString());
        json.put(KEY_MIME_TYPE, localConfig.mimeType);

        if (localConfig.drmConfiguration != null) {
            json.put(KEY_DRM_CONFIGURATION, getDrmConfigurationJson(localConfig.drmConfiguration));
        }

        return json;
    }

    private static JSONObject getDrmConfigurationJson(MediaItem.DrmConfiguration drmConfiguration) throws JSONException {
        JSONObject json = new JSONObject();
        json.put(KEY_UUID, drmConfiguration.scheme);
        json.put(KEY_LICENSE_URI, drmConfiguration.licenseUri);
        json.put(KEY_REQUEST_HEADERS, new JSONObject(drmConfiguration.licenseRequestHeaders));
        return json;
    }

    @Nullable
    private static JSONObject getPlayerConfigJson(MediaItem mediaItem) throws JSONException {
        if (mediaItem.localConfiguration != null && mediaItem.localConfiguration.drmConfiguration != null) {
            MediaItem.DrmConfiguration drmConfiguration = mediaItem.localConfiguration.drmConfiguration;
            String drmScheme;

            if (C.WIDEVINE_UUID.equals(drmConfiguration.scheme)) {
                drmScheme = "widevine";
            } else if (C.PLAYREADY_UUID.equals(drmConfiguration.scheme)) {
                drmScheme = "playready";
            } else {
                return null;
            }

            JSONObject playerConfigJson = new JSONObject();
            playerConfigJson.put("withCredentials", false);
            playerConfigJson.put("protectionSystem", drmScheme);

            if (drmConfiguration.licenseUri != null) {
                playerConfigJson.put("licenseUrl", drmConfiguration.licenseUri);
            }

            if (!drmConfiguration.licenseRequestHeaders.isEmpty()) {
                playerConfigJson.put("headers", new JSONObject(drmConfiguration.licenseRequestHeaders));
            }

            return playerConfigJson;
        }

        return null;
    }
}