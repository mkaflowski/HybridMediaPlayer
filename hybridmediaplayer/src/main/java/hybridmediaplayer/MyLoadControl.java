package hybridmediaplayer;

import com.google.android.exoplayer2.DefaultLoadControl;

public class MyLoadControl extends DefaultLoadControl {
    @Override
    public long getBackBufferDurationUs() {
        return 30000;
    }
}
