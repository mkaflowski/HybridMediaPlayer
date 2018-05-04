package hybridmediaplayer;

/**
 * Test javadoc
 */
public interface OnTrackChangedListener {
    /**
     * @param isFinished is track finished, if false it was changed by user
     */
    void onTrackChanged(boolean isFinished);
}