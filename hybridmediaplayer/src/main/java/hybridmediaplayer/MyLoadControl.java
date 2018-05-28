package hybridmediaplayer;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.DefaultLoadControl;
import com.google.android.exoplayer2.upstream.DefaultAllocator;

public class MyLoadControl extends DefaultLoadControl {

    public MyLoadControl() {
        super(new DefaultAllocator(true, C.DEFAULT_BUFFER_SEGMENT_SIZE),
                DEFAULT_MIN_BUFFER_MS,
                DEFAULT_MAX_BUFFER_MS,
                DEFAULT_BUFFER_FOR_PLAYBACK_MS / 5,
                DEFAULT_BUFFER_FOR_PLAYBACK_AFTER_REBUFFER_MS / 5,
                DEFAULT_TARGET_BUFFER_BYTES,
                DEFAULT_PRIORITIZE_TIME_OVER_SIZE_THRESHOLDS);
    }

    @Override
    public long getBackBufferDurationUs() {
        return 20000000;
    }

    @Override
    public boolean retainBackBufferFromKeyframe() {
        return true;
    }
}
