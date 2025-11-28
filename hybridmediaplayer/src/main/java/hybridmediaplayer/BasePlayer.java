package hybridmediaplayer;

import androidx.annotation.Nullable;
import androidx.media3.common.C;
import androidx.media3.common.Player;
import androidx.media3.common.Timeline;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.common.util.Util;

/** Abstract base {@link Player} which implements common implementation independent methods. */
@UnstableApi
public abstract class BasePlayer implements Player {

    protected final Timeline.Window window;

    public BasePlayer() {
        window = new Timeline.Window();
    }

    @Override
    public final void seekToDefaultPosition() {
        seekToDefaultPosition(getCurrentMediaItemIndex());
    }

    @Override
    public final void seekToDefaultPosition(int mediaItemIndex) {
        seekTo(mediaItemIndex, /* positionMs= */ C.TIME_UNSET);
    }

    @Override
    public final void seekTo(long positionMs) {
        seekTo(getCurrentMediaItemIndex(), positionMs);
    }

    @Override
    public final boolean hasPreviousMediaItem() {
        return getPreviousMediaItemIndex() != C.INDEX_UNSET;
    }

    @Override
    public final void seekToPreviousMediaItem() {
        int previousIndex = getPreviousMediaItemIndex();
        if (previousIndex != C.INDEX_UNSET) {
            seekToDefaultPosition(previousIndex);
        }
    }

    @Override
    public final boolean hasNextMediaItem() {
        return getNextMediaItemIndex() != C.INDEX_UNSET;
    }

    @Override
    public final void seekToNextMediaItem() {
        int nextIndex = getNextMediaItemIndex();
        if (nextIndex != C.INDEX_UNSET) {
            seekToDefaultPosition(nextIndex);
        }
    }

    @Override
    public final int getNextMediaItemIndex() {
        Timeline timeline = getCurrentTimeline();
        return timeline.isEmpty()
                ? C.INDEX_UNSET
                : timeline.getNextWindowIndex(
                getCurrentMediaItemIndex(),
                getRepeatModeForNavigation(),
                getShuffleModeEnabled());
    }

    @Override
    public final int getPreviousMediaItemIndex() {
        Timeline timeline = getCurrentTimeline();
        return timeline.isEmpty()
                ? C.INDEX_UNSET
                : timeline.getPreviousWindowIndex(
                getCurrentMediaItemIndex(),
                getRepeatModeForNavigation(),
                getShuffleModeEnabled());
    }

    @Nullable
    public final Object getCurrentTag() {
        int mediaItemIndex = getCurrentMediaItemIndex();
        Timeline timeline = getCurrentTimeline();
        return mediaItemIndex >= timeline.getWindowCount()
                ? null
                : timeline.getWindow(mediaItemIndex, window).tag;
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
    public final boolean isCurrentMediaItemDynamic() {
        Timeline timeline = getCurrentTimeline();
        return !timeline.isEmpty() && timeline.getWindow(getCurrentMediaItemIndex(), window).isDynamic;
    }

    @Override
    public final boolean isCurrentMediaItemSeekable() {
        Timeline timeline = getCurrentTimeline();
        return !timeline.isEmpty() && timeline.getWindow(getCurrentMediaItemIndex(), window).isSeekable;
    }

    @Override
    public final long getContentDuration() {
        Timeline timeline = getCurrentTimeline();
        return timeline.isEmpty()
                ? C.TIME_UNSET
                : timeline.getWindow(getCurrentMediaItemIndex(), window).getDurationMs();
    }

    @RepeatMode
    private int getRepeatModeForNavigation() {
        @RepeatMode int repeatMode = getRepeatMode();
        return repeatMode == REPEAT_MODE_ONE ? REPEAT_MODE_OFF : repeatMode;
    }

    /** Holds a listener reference. */
    protected static final class ListenerHolder {

        /**
         * The listener on which {@link #invoke} will execute {@link ListenerInvocation listener
         * invocations}.
         */
        public final Player.Listener listener;

        private boolean released;

        public ListenerHolder(Player.Listener listener) {
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

    /** Parameterized invocation of a {@link Player.Listener} method. */
    protected interface ListenerInvocation {

        /** Executes the invocation on the given {@link Player.Listener}. */
        void invokeListener(Player.Listener listener);
    }
}