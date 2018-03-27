package org.teamtators.common.control;

/**
 * A basic timer utility
 */
public class Timer {
    private double startTime;

    public Timer() {
        startTime = Double.NEGATIVE_INFINITY;
    }

    /**
     * Get current timestamp in seconds
     *
     * @return Current timestamp
     */
    public static double getTimestamp() {
        return edu.wpi.first.wpilibj.Timer.getFPGATimestamp();
    }

    /**
     * Initialize the timer
     */
    public void start() {
        startTime = getTimestamp();
    }

    /**
     * Reset the timer and get current time
     *
     * @return Current time
     */
    public double restart() {
        double time = get();
        start();
        return time;
    }

    /**
     * Get the elapsed time
     *
     * @return Elapsed time since start
     */
    public double get() {
        return getTimestamp() - startTime;
    }

    /**
     * Set start time to a value such that any timeout will finish
     */
    public void stop() {
        startTime = Double.NEGATIVE_INFINITY;
    }

    /**
     * Check if a period has passed, and reset timer if it has
     *
     * @return Whether or not the period has passed
     */
    public boolean periodically(double period) {
        boolean hasPassed = false;
        if (get() > period) {
            hasPassed = true;
            start();
        }
        return hasPassed;
    }

    public boolean hasPeriodElapsed(double period) {
        return get() > period;
    }

    /**
     * Checks if the timer is running
     *
     * @return True if the timer has been started and not reset
     */
    public boolean isRunning() {
        return startTime != Double.NEGATIVE_INFINITY;
    }

    public void startOrStop(boolean shouldStart) {
        if (shouldStart && !isRunning()) {
            start();
        } else if (!shouldStart) {
            stop();
        }
    }
}
