package org.teamtators.common.control;

import edu.wpi.first.wpilibj.Sendable;
import edu.wpi.first.wpilibj.smartdashboard.SendableBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.profiler.Profiler;

public abstract class AbstractUpdatable implements Updatable, Sendable {
    protected volatile boolean running = false;
    protected Logger logger;
    protected String name = "";
    protected String subsystem = "Ungrouped";
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

    @Override
    public void setName(String subsystem, String name) {
        setSubsystem(subsystem);
        setName(name);
    }

    @Override
    public String getSubsystem() {
        return subsystem;
    }

    @Override
    public void setSubsystem(String subsystem) {
        this.subsystem = subsystem;
    }

    @Override
    public void initSendable(SendableBuilder builder) {
        builder.addBooleanProperty("enabled", this::isRunning, this::setRunning);
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

    public void setRunning(boolean running) {
        if (running) {
            start();
        } else {
            stop();
        }
    }
}
