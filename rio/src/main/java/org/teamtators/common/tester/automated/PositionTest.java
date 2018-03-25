package org.teamtators.common.tester.automated;

import org.teamtators.common.config.Configurable;
import org.teamtators.common.control.Timer;
import org.teamtators.common.tester.AutomatedTest;
import org.teamtators.common.tester.AutomatedTestMessage;

import java.util.function.DoubleConsumer;
import java.util.function.DoubleSupplier;

/**
 * Tests that a motor gets an encoder to a certain position
 */
public class PositionTest extends AutomatedTest implements Configurable<PositionTest.Config> {
    private Config config;
    private DoubleConsumer motor;
    private DoubleSupplier encoder;
    private double position;
    private Timer timer = new Timer();

    public PositionTest(String name, DoubleConsumer motor, DoubleSupplier encoder, double position) {
        super(name, false);
        this.motor = motor;
        this.encoder = encoder;
        this.position = position;
    }

    public PositionTest(String name, DoubleConsumer motor, DoubleSupplier encoder) {
        this(name, motor, encoder, Double.NaN);
    }

    @Override
    protected void initialize() {
        timer.start();
        motor.accept(config.power * (config.invertMotor ? -1 : 1));
    }

    @Override
    public boolean step() {
        double position = encoder.getAsDouble();
        if ((position - config.targetPosition) * Math.signum(config.power) >= 0) {
            sendMessage("Encoder measured correct position of " + position
                    + " (target " + config.targetPosition + ")", AutomatedTestMessage.Level.INFO);
        } else if (timer.hasPeriodElapsed(config.timeout)) {
            sendMessage("Encoder failed to reach target of " + config.targetPosition
                    + " (ended at " + position + ")", AutomatedTestMessage.Level.ERROR);
        } else {
            return false;
        }
        return true;
    }

    @Override
    protected void finish(boolean interrupted) {
        super.finish(interrupted);
        motor.accept(0.0);
    }

    @Override
    public void configure(Config config) {
        this.config = config;
        if (!Double.isNaN(position)) {
            config.targetPosition = position;
        }
    }

    public static class Config {
        public double power;
        public double targetPosition;
        public double timeout;
        public boolean invertMotor = false;
    }
}
