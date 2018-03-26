package hybridmediaplayer.demo;


import android.content.Context;
import android.support.annotation.NonNull;

import com.google.android.gms.cast.CastMediaControlIntent;
import com.google.android.gms.cast.MediaMetadata;
import com.google.android.gms.cast.framework.CastOptions;
import com.google.android.gms.cast.framework.OptionsProvider;
import com.google.android.gms.cast.framework.SessionProvider;
import com.google.android.gms.cast.framework.media.CastMediaOptions;
import com.google.android.gms.cast.framework.media.ImageHints;
import com.google.android.gms.cast.framework.media.ImagePicker;
import com.google.android.gms.cast.framework.media.MediaIntentReceiver;
import com.google.android.gms.cast.framework.media.NotificationOptions;
import com.google.android.gms.common.images.WebImage;

import java.util.Arrays;
import java.util.List;

import hybridplayer.demo.R;

public class CastOptionsProvider implements OptionsProvider {
    @Override
    public CastOptions getCastOptions(Context context) {
        NotificationOptions notificationOptions =
                new NotificationOptions.Builder()
                        .setActions(Arrays.asList( // up to 5
                                MediaIntentReceiver.ACTION_REWIND,
                                MediaIntentReceiver.ACTION_TOGGLE_PLAYBACK,
                                MediaIntentReceiver.ACTION_FORWARD,
                                MediaIntentReceiver.ACTION_DISCONNECT
                        ), new int[]{0, 1, 2}) // Show in compat (condensed) view
                        .setSkipStepMs(NotificationOptions.SKIP_STEP_TEN_SECONDS_IN_MS)
                        .setTargetActivityClassName(
                                MainActivity.class.getName())
                        .build();

        CastMediaOptions mediaOptions = new CastMediaOptions.Builder()
                .setNotificationOptions(notificationOptions)
                .setExpandedControllerActivityClassName(
                        MainActivity.class.getName())
                // careful!
                //.setMediaIntentReceiverClassName(CustomReceiver.java)
                .setImagePicker(new ImagePickerImpl())
                .build();

        CastOptions castOptions = new CastOptions.Builder()
                .setReceiverApplicationId(CastMediaControlIntent.DEFAULT_MEDIA_RECEIVER_APPLICATION_ID)
                .setStopReceiverApplicationWhenEndingSession(true)
                .setCastMediaOptions(mediaOptions)
                .build();
        return castOptions;
    }

    @Override
    public List<SessionProvider> getAdditionalSessionProviders(Context context) {
        return null;
    }

    private class ImagePickerImpl extends ImagePicker {
        @Override
        public WebImage onPickImage(MediaMetadata mediaMetadata, int type) {


//            int imageIndex;
//
//            if (type == IMAGE_TYPE_MEDIA_ROUTE_CONTROLLER_DIALOG_BACKGROUND) {
//                imageIndex = 0;
//            } else {
//                imageIndex = 1;
//            }

            if (mediaMetadata.hasImages() && mediaMetadata.getImages().size() > 0) {
                return mediaMetadata.getImages().get(0);
            }
            return null;
        }

    }
}