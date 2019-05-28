package hybridmediaplayer;

import androidx.annotation.Nullable;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.util.Util;

/** Abstract base {@link Player} which implements common implementation independent methods. */
public abstract class BasePlayer implements Player {

    protected final Timeline.Window window;

    public BasePlayer() {
        window = new Timeline.Window();
    }

    @Override
    public final void seekToDefaultPosition() {
        seekToDefaultPosition(getCurrentWindowIndex());
    }

    @Override
    public final void seekToDefaultPosition(int windowIndex) {
        seekTo(windowIndex, /* positionMs= */ C.TIME_UNSET);
    }

    @Override
    public final void seekTo(long positionMs) {
        seekTo(getCurrentWindowIndex(), positionMs);
    }

    @Override
    public final boolean hasPrevious() {
        return getPreviousWindowIndex() != C.INDEX_UNSET;
    }

    @Override
    public final void previous() {
        int previousWindowIndex = getPreviousWindowIndex();
        if (previousWindowIndex != C.INDEX_UNSET) {
            seekToDefaultPosition(previousWindowIndex);
        }
    }

    @Override
    public final boolean hasNext() {
        return getNextWindowIndex() != C.INDEX_UNSET;
    }

    @Override
    public final void next() {
        int nextWindowIndex = getNextWindowIndex();
        if (nextWindowIndex != C.INDEX_UNSET) {
            seekToDefaultPosition(nextWindowIndex);
        }
    }

    @Override
    public final void stop() {
        stop(/* reset= */ false);
    }

    @Override
    public final int getNextWindowIndex() {
        Timeline timeline = getCurrentTimeline();
        return timeline.isEmpty()
                ? C.INDEX_UNSET
                : timeline.getNextWindowIndex(
                getCurrentWindowIndex(), getRepeatModeForNavigation(), getShuffleModeEnabled());
    }

    @Override
    public final int getPreviousWindowIndex() {
        Timeline timeline = getCurrentTimeline();
        return timeline.isEmpty()
                ? C.INDEX_UNSET
                : timeline.getPreviousWindowIndex(
                getCurrentWindowIndex(), getRepeatModeForNavigation(), getShuffleModeEnabled());
    }

    @Override
    @Nullable
    public final Object getCurrentTag() {
        int windowIndex = getCurrentWindowIndex();
        Timeline timeline = getCurrentTimeline();
        return windowIndex >= timeline.getWindowCount()
                ? null
                : timeline.getWindow(windowIndex, window, /* setTag= */ true).tag;
    }

    @Override
    public final int getBufferedPercentage() {
        long position = getBufferedPosition();
        long duration = getDuration();
        return position == C.TIME_UNSET || duration == C.TIME_UNSET
                ? 0
                : duration == 0 ? 100 : Util.constrainValue((int) ((position * 100) / duration), 0, 100);
    }

    @Override
    public final boolean isCurrentWindowDynamic() {
        Timeline timeline = getCurrentTimeline();
        return !timeline.isEmpty() && timeline.getWindow(getCurrentWindowIndex(), window).isDynamic;
    }

    @Override
    public final boolean isCurrentWindowSeekable() {
        Timeline timeline = getCurrentTimeline();
        return !timeline.isEmpty() && timeline.getWindow(getCurrentWindowIndex(), window).isSeekable;
    }

    @Override
    public final long getContentDuration() {
        Timeline timeline = getCurrentTimeline();
        return timeline.isEmpty()
                ? C.TIME_UNSET
                : timeline.getWindow(getCurrentWindowIndex(), window).getDurationMs();
    }

    @RepeatMode
    private int getRepeatModeForNavigation() {
        @RepeatMode int repeatMode = getRepeatMode();
        return repeatMode == REPEAT_MODE_ONE ? REPEAT_MODE_OFF : repeatMode;
    }

    /** Holds a listener reference. */
    protected static final class ListenerHolder {

        /**
         * The listener on which {link #invoke} will execute {@link ListenerInvocation listener
         * invocations}.
         */
        public final Player.EventListener listener;

        private boolean released;

        public ListenerHolder(Player.EventListener listener) {
            this.listener = listener;
        }

        /** Prevents any further {@link ListenerInvocation} to be executed on {@link #listener}. */
        public void release() {
            released = true;
        }

        /**
         * Executes the given {@link ListenerInvocation} on {@link #listener}. Does nothing if {@link
         * #release} has been called on this instance.
         */
        public void invoke(ListenerInvocation listenerInvocation) {
            if (!released) {
                listenerInvocation.invokeListener(listener);
            }
        }

        @Override
        public boolean equals(@Nullable Object other) {
            if (this == other) {
                return true;
            }
            if (other == null || getClass() != other.getClass()) {
                return false;
            }
            return listener.equals(((ListenerHolder) other).listener);
        }

        @Override
        public int hashCode() {
            return listener.hashCode();
        }
    }

    /** Parameterized invocation of a {@link Player.EventListener} method. */
    protected interface ListenerInvocation {

        /** Executes the invocation on the given {@link Player.EventListener}. */
        void invokeListener(Player.EventListener listener);
    }
}