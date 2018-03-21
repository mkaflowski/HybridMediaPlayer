package hybridmediaplayer;

public class MediaSourceInfo {

    private String title;
    private String author;
    private String url;
    private String imageUrl;

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

    private MediaSourceInfo(String title, String author, String url, String imageUrl) {
        this.title = title;
        this.author = author;
        this.url = url;
        this.imageUrl = imageUrl;
    }

    public static class Builder {
        private String title;
        private String author;
        private String url;
        private String imageUrl;


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

        public MediaSourceInfo build() {
            return new MediaSourceInfo(title, author, url, imageUrl);
        }
    }

}
