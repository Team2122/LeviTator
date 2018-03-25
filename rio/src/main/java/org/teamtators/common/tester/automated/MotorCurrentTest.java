package org.teamtators.common.tester.automated;

import org.teamtators.common.config.Configurable;
import org.teamtators.common.control.Timer;
import org.teamtators.common.tester.AutomatedTest;
import org.teamtators.common.tester.AutomatedTestMessage;

import java.util.function.DoubleConsumer;
import java.util.function.DoubleSupplier;

public class MotorCurrentTest extends AutomatedTest implements Configurable<MotorCurrentTest.Config> {
    private Config config;
    private Timer timer;
    private DoubleConsumer motor;
    private DoubleSupplier current;
    private Runnable before;
    private Timer beforeTimer;
    private double waitBefore;
    private boolean ready;
    private double currentSum;
    private int steps;

    public MotorCurrentTest(String name, DoubleConsumer motor, DoubleSupplier current, Runnable before, double waitBefore) {
        super(name);
        this.motor = motor;
        this.current = current;
        this.before = before;
        this.beforeTimer = new Timer();
        this.waitBefore = waitBefore;
        this.timer = new Timer();
    }

    public MotorCurrentTest(String name, DoubleConsumer motor, DoubleSupplier current) {
        this(name, motor, current, null, 0.0);
    }

    @Override
    protected void initialize() {
        if (before != null) {
            beforeTimer.start();
            before.run();
            ready = false;
        } else {
            ready = true;
        }
        motor.accept(config.power);
        currentSum = 0;
        steps = 0;
        timer.start();
    }

    @Override
    public boolean step() {
        if (before != null && !ready) {
            if (beforeTimer.hasPeriodElapsed(waitBefore)) {
                motor.accept(config.power);
                timer.start();
                ready = true;
            } else {
                return false;
            }
        }
        if (!ready) return false;
        currentSum += current.getAsDouble();
        steps++;
        if (timer.hasPeriodElapsed(config.timeout)) {
            double currentDrawn = currentSum / ((double) steps);
            boolean withinRange = Math.abs((currentDrawn - config.expectedCurrent) / config.expectedCurrent)
                    < config.tolerance;
            sendMessage("The current averaged " + currentDrawn + ", "
                            + (withinRange ? "within" : "outside of") + " the desired range",
                    (withinRange ? AutomatedTestMessage.Level.INFO : AutomatedTestMessage.Level.ERROR));
            return true;
        }
        return false;
    }

    @Override
    protected void finish(boolean interrupted) {
        super.finish(interrupted);
        motor.accept(0.0);
    }

    public void configure(Config config) {
        this.config = config;
    }

    public static class Config {
        public double power = 0.2;
        public double expectedCurrent;
        public double tolerance = 0.10;
        public double timeout = 1.0;
    }
}
