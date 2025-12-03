package hybridmediaplayer;

import static androidx.media3.common.MediaMetadata.MEDIA_TYPE_MUSIC;

import androidx.media3.common.MediaMetadata;

public class MediaSourceInfo {

    private String title;
    private String author;
    private String url;
    private String imageUrl;
    private boolean isVideo;
    private String albumTitle;
    private @MediaMetadata.MediaType int mediaType = MEDIA_TYPE_MUSIC;

    public static MediaSourceInfo PLACEHOLDER = new Builder().setTitle("HybridMediaPlayer Casting")
            .setAuthor("lib by Mateusz Kaflowski")
            .setImageUrl("https://github.com/mkaflowski/HybridMediaPlayer/blob/master/images/cover.jpg?raw=true")
            .setUrl("").build();

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public boolean isVideo() {
        return isVideo;
    }

    public String getAlbumTitle() {
        return albumTitle;
    }

    public @MediaMetadata.MediaType int getMediaType() {
        return mediaType;
    }

    private MediaSourceInfo(String title, String author, String url, String imageUrl, boolean isVideo, String albumTitle, int mediaType) {
        this.title = title;
        this.author = author;
        this.url = url;
        this.imageUrl = imageUrl;
        this.isVideo = isVideo;
    }

    public static class Builder {
        private String title;
        private String author;
        private String url;
        private String imageUrl;
        private boolean isVideo;
        private String albumTitle;
        private int mediaType = MEDIA_TYPE_MUSIC;

        public Builder setTitle(String title) {
            this.title = title;
            return this;
        }

        public Builder setAuthor(String author) {
            this.author = author;
            return this;
        }

        public Builder setUrl(String url) {
            this.url = url;
            return this;
        }

        public Builder setImageUrl(String imageUrl) {
            this.imageUrl = imageUrl;
            return this;
        }

        public Builder isVideo(boolean hasVideo) {
            this.isVideo = hasVideo;
            return this;
        }

        public Builder albumTitle(String albumTitle){
            this.albumTitle = albumTitle;
            return this;
        }

        public Builder setMediaType(@MediaMetadata.MediaType int mediaType){
            this.mediaType = mediaType;
            return this;
        }

        public MediaSourceInfo build() {
            return new MediaSourceInfo(title, author, url, imageUrl, isVideo, albumTitle, mediaType);
        }
    }

}
