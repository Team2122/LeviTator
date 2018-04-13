package org.teamtators.common.control;

import edu.wpi.first.wpilibj.Notifier;
import edu.wpi.first.wpilibj.RobotController;
import edu.wpi.first.wpilibj.hal.NotifierJNI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.profiler.Profiler;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Updates an Updatable at a constant period.
 *
 * @author Alex Mikhalev
 */
public class Updater {
    private static final Logger logger = LoggerFactory.getLogger(Updater.class);
    private static final double DEFAULT_PERIOD = 1.0 / 100.0;
    private final Thread m_thread;
    private final ReentrantLock m_processLock = new ReentrantLock();
    private final AtomicInteger m_notifier = new AtomicInteger();
    private final AtomicBoolean m_running = new AtomicBoolean(false);
    private double period;
    private double m_expirationTime;
    private final Updatable updatable;
    private long lastStepTime;

    public Updater(Updatable updatable) {
        this(updatable, DEFAULT_PERIOD);
    }

    public Updater(Updatable updatable, double period) {
        if (updatable == null)
            throw new NullPointerException("updatable cannot be null");
        this.updatable = updatable;
        this.period = period;

        m_notifier.set(NotifierJNI.initializeNotifier());

        m_thread = new Thread(this::updaterThread, "Updater-" + updatable.getName());
        m_thread.setDaemon(true);
    }

    private void updateAlarm() {
        int notifier = this.m_notifier.get();
        if (notifier == 0) {
            return;
        }
        NotifierJNI.updateNotifierAlarm(notifier, (long) (m_expirationTime * 1e6));
    }

    public void start() {
        if (isRunning()) return;
        if (!m_thread.isAlive()) {
            m_thread.start();
        }
        m_running.set(true);
        lastStepTime = RobotController.getFPGATime();
        m_processLock.lock();
        try {
            m_expirationTime = RobotController.getFPGATime() * 1e-6 + period;
            updateAlarm();
        } finally {
            m_processLock.unlock();
        }
    }

    public void stop() {
        m_running.set(false);
        NotifierJNI.cancelNotifierAlarm(m_notifier.get());
    }

    public boolean isRunning() {
        return m_running.get();
    }

    public Updatable getUpdatable() {
        return updatable;
    }

    private void updaterThread() {
        while (!Thread.interrupted()) {
            int notifier = this.m_notifier.get();
            if (notifier == 0) {
                break;
            }
            long curTime = NotifierJNI.waitForNotifierAlarm(notifier);
            if (curTime == 0) {
                break;
            }

            m_processLock.lock();
            try {
                m_expirationTime += period;
                updateAlarm();
            } finally {
                m_processLock.unlock();
            }

            run();
        }

    }

    private void run() {
        if (!isRunning()) return;
        long time = RobotController.getFPGATime();
        long delta = time - lastStepTime;
        double deltaSeconds = delta / 1000000.0;
        lastStepTime = time;
        try {
            updatable.update(deltaSeconds);
            long elapsed = RobotController.getFPGATime() - time;
            double elapsedSeconds = elapsed / 1000000.0;
            if (elapsedSeconds > 10 * period) {
                logger.warn("Updatable " + updatable.getClass().getName() + " exceeded period ({} > {})", elapsedSeconds, period);
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