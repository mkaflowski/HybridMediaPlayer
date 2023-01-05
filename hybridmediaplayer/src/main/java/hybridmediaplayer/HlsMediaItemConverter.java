package hybridmediaplayer;

import android.net.Uri;

import androidx.annotation.Nullable;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.MediaMetadata;
import com.google.android.exoplayer2.ext.cast.DefaultMediaItemConverter;
import com.google.android.exoplayer2.ext.cast.MediaItemConverter;
import com.google.android.exoplayer2.util.Assertions;
import com.google.android.exoplayer2.util.MimeTypes;
import com.google.android.gms.cast.HlsSegmentFormat;
import com.google.android.gms.cast.MediaInfo;
import com.google.android.gms.cast.MediaQueueItem;
import com.google.android.gms.common.images.WebImage;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Iterator;
import java.util.UUID;

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

    public MediaItem toMediaItem(MediaQueueItem mediaQueueItem) {
        MediaInfo mediaInfo = mediaQueueItem.getMedia();

        Assertions.checkNotNull(mediaInfo);
        MediaMetadata.Builder metadataBuilder = new MediaMetadata.Builder();
        com.google.android.gms.cast.MediaMetadata metadata = mediaInfo.getMetadata();
        if (metadata != null) {
            if (metadata.containsKey("com.google.android.gms.cast.metadata.TITLE")) {
                metadataBuilder.setTitle(metadata.getString("com.google.android.gms.cast.metadata.TITLE"));
            }

            if (metadata.containsKey("com.google.android.gms.cast.metadata.SUBTITLE")) {
                metadataBuilder.setSubtitle(metadata.getString("com.google.android.gms.cast.metadata.SUBTITLE"));
            }

            if (metadata.containsKey("com.google.android.gms.cast.metadata.ARTIST")) {
                metadataBuilder.setArtist(metadata.getString("com.google.android.gms.cast.metadata.ARTIST"));
            }

            if (metadata.containsKey("com.google.android.gms.cast.metadata.ALBUM_ARTIST")) {
                metadataBuilder.setAlbumArtist(metadata.getString("com.google.android.gms.cast.metadata.ALBUM_ARTIST"));
            }

            if (metadata.containsKey("com.google.android.gms.cast.metadata.ALBUM_TITLE")) {
                metadataBuilder.setArtist(metadata.getString("com.google.android.gms.cast.metadata.ALBUM_TITLE"));
            }

            if (!metadata.getImages().isEmpty()) {
                metadataBuilder.setArtworkUri(((WebImage) metadata.getImages().get(0)).getUrl());
            }

            if (metadata.containsKey("com.google.android.gms.cast.metadata.COMPOSER")) {
                metadataBuilder.setComposer(metadata.getString("com.google.android.gms.cast.metadata.COMPOSER"));
            }

            if (metadata.containsKey("com.google.android.gms.cast.metadata.DISC_NUMBER")) {
                metadataBuilder.setDiscNumber(metadata.getInt("com.google.android.gms.cast.metadata.DISC_NUMBER"));
            }

            if (metadata.containsKey("com.google.android.gms.cast.metadata.TRACK_NUMBER")) {
                metadataBuilder.setTrackNumber(metadata.getInt("com.google.android.gms.cast.metadata.TRACK_NUMBER"));
            }
        }

        return getMediaItem((JSONObject) Assertions.checkNotNull(mediaInfo.getCustomData()), metadataBuilder.build());
    }

    public MediaQueueItem toMediaQueueItem(MediaItem mediaItem) {
        Assertions.checkNotNull(mediaItem.localConfiguration);
        if (mediaItem.localConfiguration.mimeType == null) {
            throw new IllegalArgumentException("The item must specify its mimeType");
        } else {
            com.google.android.gms.cast.MediaMetadata metadata = new com.google.android.gms.cast.MediaMetadata(MimeTypes.isAudio(mediaItem.localConfiguration.mimeType) ? 3 : 1);
            if (mediaItem.mediaMetadata.title != null) {
                metadata.putString("com.google.android.gms.cast.metadata.TITLE", mediaItem.mediaMetadata.title.toString());
            }

            if (mediaItem.mediaMetadata.subtitle != null) {
                metadata.putString("com.google.android.gms.cast.metadata.SUBTITLE", mediaItem.mediaMetadata.subtitle.toString());
            }

            if (mediaItem.mediaMetadata.artist != null) {
                metadata.putString("com.google.android.gms.cast.metadata.ARTIST", mediaItem.mediaMetadata.artist.toString());
            }

            if (mediaItem.mediaMetadata.albumArtist != null) {
                metadata.putString("com.google.android.gms.cast.metadata.ALBUM_ARTIST", mediaItem.mediaMetadata.albumArtist.toString());
            }

            if (mediaItem.mediaMetadata.albumTitle != null) {
                metadata.putString("com.google.android.gms.cast.metadata.ALBUM_TITLE", mediaItem.mediaMetadata.albumTitle.toString());
            }

            if (mediaItem.mediaMetadata.artworkUri != null) {
                metadata.addImage(new WebImage(mediaItem.mediaMetadata.artworkUri));
            }

            if (mediaItem.mediaMetadata.composer != null) {
                metadata.putString("com.google.android.gms.cast.metadata.COMPOSER", mediaItem.mediaMetadata.composer.toString());
            }

            if (mediaItem.mediaMetadata.discNumber != null) {
                metadata.putInt("com.google.android.gms.cast.metadata.DISC_NUMBER", mediaItem.mediaMetadata.discNumber);
            }

            if (mediaItem.mediaMetadata.trackNumber != null) {
                metadata.putInt("com.google.android.gms.cast.metadata.TRACK_NUMBER", mediaItem.mediaMetadata.trackNumber);
            }

            String contentUrl = mediaItem.localConfiguration.uri.toString();
            String contentId = mediaItem.mediaId.equals("") ? contentUrl : mediaItem.mediaId;
            MediaInfo.Builder mediaInfoBuilder = new MediaInfo.Builder(contentId);
            if (contentUrl.contains(".m3u8")) {
                mediaInfoBuilder.setHlsSegmentFormat(HlsSegmentFormat.AAC);
                mediaInfoBuilder.setStreamType(MediaInfo.STREAM_TYPE_BUFFERED);
                mediaInfoBuilder.setContentType("application/x-mpegURL");
            }

            MediaInfo mediaInfo = mediaInfoBuilder.setStreamType(1).setContentType(mediaItem.localConfiguration.mimeType).setContentUrl(contentUrl).setMetadata(metadata).setCustomData(getCustomData(mediaItem)).build();


            return (new com.google.android.gms.cast.MediaQueueItem.Builder(mediaInfo)).build();
        }
    }

    private static MediaItem getMediaItem(JSONObject customData, com.google.android.exoplayer2.MediaMetadata mediaMetadata) {
        try {
            JSONObject mediaItemJson = customData.getJSONObject("mediaItem");
            com.google.android.exoplayer2.MediaItem.Builder builder = (new com.google.android.exoplayer2.MediaItem.Builder()).setUri(Uri.parse(mediaItemJson.getString("uri"))).setMediaId(mediaItemJson.getString("mediaId")).setMediaMetadata(mediaMetadata);
            if (mediaItemJson.has("mimeType")) {
                builder.setMimeType(mediaItemJson.getString("mimeType"));
            }

            if (mediaItemJson.has("drmConfiguration")) {
                populateDrmConfiguration(mediaItemJson.getJSONObject("drmConfiguration"), builder);
            }

            return builder.build();
        } catch (JSONException var4) {
            throw new RuntimeException(var4);
        }
    }

    private static void populateDrmConfiguration(JSONObject json, com.google.android.exoplayer2.MediaItem.Builder mediaItem) throws JSONException {
        com.google.android.exoplayer2.MediaItem.DrmConfiguration.Builder drmConfiguration = (new com.google.android.exoplayer2.MediaItem.DrmConfiguration.Builder(UUID.fromString(json.getString("uuid")))).setLicenseUri(json.getString("licenseUri"));
        JSONObject requestHeadersJson = json.getJSONObject("requestHeaders");
        HashMap<String, String> requestHeaders = new HashMap();
        Iterator iterator = requestHeadersJson.keys();

        while (iterator.hasNext()) {
            String key = (String) iterator.next();
            requestHeaders.put(key, requestHeadersJson.getString(key));
        }

        drmConfiguration.setLicenseRequestHeaders(requestHeaders);
        mediaItem.setDrmConfiguration(drmConfiguration.build());
    }

    private static JSONObject getCustomData(MediaItem mediaItem) {
        JSONObject json = new JSONObject();

        try {
            json.put("mediaItem", getMediaItemJson(mediaItem));
            JSONObject playerConfigJson = getPlayerConfigJson(mediaItem);
            if (playerConfigJson != null) {
                json.put("exoPlayerConfig", playerConfigJson);
            }

            return json;
        } catch (JSONException var3) {
            throw new RuntimeException(var3);
        }
    }

    private static JSONObject getMediaItemJson(MediaItem mediaItem) throws JSONException {
        Assertions.checkNotNull(mediaItem.localConfiguration);
        JSONObject json = new JSONObject();
        json.put("mediaId", mediaItem.mediaId);
        json.put("title", mediaItem.mediaMetadata.title);
        json.put("uri", mediaItem.localConfiguration.uri.toString());
        json.put("mimeType", mediaItem.localConfiguration.mimeType);
        if (mediaItem.localConfiguration.drmConfiguration != null) {
            json.put("drmConfiguration", getDrmConfigurationJson(mediaItem.localConfiguration.drmConfiguration));
        }

        return json;
    }

    private static JSONObject getDrmConfigurationJson(MediaItem.DrmConfiguration drmConfiguration) throws JSONException {
        JSONObject json = new JSONObject();
        json.put("uuid", drmConfiguration.scheme);
        json.put("licenseUri", drmConfiguration.licenseUri);
        json.put("requestHeaders", new JSONObject(drmConfiguration.licenseRequestHeaders));
        return json;
    }

    @Nullable
    private static JSONObject getPlayerConfigJson(MediaItem mediaItem) throws JSONException {
        if (mediaItem.localConfiguration != null && mediaItem.localConfiguration.drmConfiguration != null) {
            MediaItem.DrmConfiguration drmConfiguration = mediaItem.localConfiguration.drmConfiguration;
            String drmScheme;
            if (C.WIDEVINE_UUID.equals(drmConfiguration.scheme)) {
                drmScheme = "widevine";
            } else {
                if (!C.PLAYREADY_UUID.equals(drmConfiguration.scheme)) {
                    return null;
                }

                drmScheme = "playready";
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
        } else {
            return null;
        }
    }
}
