package org.teamtators.common.control;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.profiler.Profiler;

public abstract class AbstractUpdatable implements Updatable {
    protected volatile boolean running = false;
    protected Logger logger;
    protected String name;
    protected Profiler profiler;
    private double lastDelta;

    public AbstractUpdatable() {
        this("");
    }

    public AbstractUpdatable(String name) {
        setName(name);
    }

    protected abstract void doUpdate(double delta);

    @Override
    public final void update(double delta) {
        this.lastDelta = delta;
        if (running) {
            this.doUpdate(delta);
        }
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
        String loggerName = String.format("%s(%s)", this.getClass().getName(), name);
        logger = LoggerFactory.getLogger(loggerName);
    }

    public double getLastDelta() {
        return lastDelta;
    }

    public Profiler getProfiler() {
        return profiler;
    }

    public synchronized void start() {
        if (!running) {
            running = true;
        }
    }

    public synchronized void stop() {
        if (running) {
            running = false;
        }
    }

    public boolean isRunning() {
        return running;
    }
}
