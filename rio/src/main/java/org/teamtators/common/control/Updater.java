package org.teamtators.common.control;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.profiler.Profiler;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Updates an Updatable at a constant period.
 *
 * @author Alex Mikhalev
 */
public class Updater implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(Updater.class);
    private static final double DEFAULT_PERIOD = 1.0 / 120.0;
    private static final double S_TO_NS = 1000000000.0;
    private final Updatable updatable;
    private double period;
    private boolean running = false;
    private double lastStepTime;

    public Updater(Updatable updatable) {
        this(updatable, DEFAULT_PERIOD);
    }

    public Updater(Updatable updatable, double period) {
        this(updatable, period, Executors.newSingleThreadScheduledExecutor((r) -> new Thread(r,
                "Updater-" + updatable.getName())));
    }

    public Updater(Updatable updatable, double period, ScheduledExecutorService executorService) {
        if (updatable == null)
            throw new NullPointerException("updatable cannot be null");
        this.updatable = updatable;
        this.period = period;
        executorService.scheduleAtFixedRate(this, 0, (long) (S_TO_NS * this.period), TimeUnit.NANOSECONDS);
    }

    public void start() {
        if (isRunning()) return;
        running = true;
        lastStepTime = getTime();
    }

    private double getTime() {
        return System.nanoTime() / S_TO_NS;
    }

    public void stop() {
        running = false;
    }

    public boolean isRunning() {
        return running;
    }

    public Updatable getUpdatable() {
        return updatable;
    }

    @Override
    public void run() {
        if (!running) return;
        double time = getTime();
        double delta = time - lastStepTime;
        lastStepTime = time;
        try {
            updatable.update(delta);
            double elapsed = getTime() - time;
            if (elapsed > period && elapsed > .1) {
                logger.warn("Updatable " + updatable.getClass().getName() + " exceeded period ({} > {})", elapsed, period);
                Profiler profiler = updatable.getProfiler();
                if (profiler != null) {
                    profiler.setLogger(logger);
                    profiler.log();
                }
            }
        } catch (Throwable t) {
            logger.error("Exception in updatable: ", t);
        }
    }

    public double getPeriod() {
        return period;
    }

    public void setPeriod(double period) {
        this.period = period;
    }
}