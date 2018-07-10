package hybridmediaplayer;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.DefaultLoadControl;
import com.google.android.exoplayer2.upstream.DefaultAllocator;

public class MyLoadControl extends DefaultLoadControl {

    long backBufferUs;

    public MyLoadControl(long backBufferMs) {
        super(new DefaultAllocator(true, C.DEFAULT_BUFFER_SEGMENT_SIZE),
                DEFAULT_MIN_BUFFER_MS,
                DEFAULT_MAX_BUFFER_MS,
                DEFAULT_BUFFER_FOR_PLAYBACK_MS / 5,
                DEFAULT_BUFFER_FOR_PLAYBACK_AFTER_REBUFFER_MS / 5,
                DEFAULT_TARGET_BUFFER_BYTES,
                DEFAULT_PRIORITIZE_TIME_OVER_SIZE_THRESHOLDS);

        backBufferUs = backBufferMs*1000;
    }

    @Override
    public long getBackBufferDurationUs() {
        return backBufferUs;
    }

    @Override
    public boolean retainBackBufferFromKeyframe() {
        return true;
    }
}
