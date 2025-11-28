package hybridmediaplayer;

import androidx.media3.common.C;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.exoplayer.DefaultLoadControl;
import androidx.media3.exoplayer.upstream.DefaultAllocator;

@UnstableApi
public class MyLoadControl extends DefaultLoadControl {

    private final long backBufferDurationUs;

    public MyLoadControl(int backBufferMs) {
        super(new DefaultAllocator(true, C.DEFAULT_BUFFER_SEGMENT_SIZE),
                DEFAULT_MIN_BUFFER_MS,
                DEFAULT_MAX_BUFFER_MS,
                DEFAULT_BUFFER_FOR_PLAYBACK_MS / 5,
                DEFAULT_BUFFER_FOR_PLAYBACK_AFTER_REBUFFER_MS / 2,
                DEFAULT_TARGET_BUFFER_BYTES,
                DEFAULT_PRIORITIZE_TIME_OVER_SIZE_THRESHOLDS,
                backBufferMs,  // backBufferDurationMs parameter
                true);         // retainBackBufferFromKeyframe parameter

        this.backBufferDurationUs = backBufferMs * 1000;
    }

    @Override
    public long getBackBufferDurationUs() {
        return backBufferDurationUs;
    }

    @Override
    public boolean retainBackBufferFromKeyframe() {
        return true;
    }
}