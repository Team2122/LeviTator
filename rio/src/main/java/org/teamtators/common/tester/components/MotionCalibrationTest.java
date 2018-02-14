package org.teamtators.common.tester.components;

import org.teamtators.common.control.ControllerInput;
import org.teamtators.common.control.ControllerOutput;
import org.teamtators.common.control.TrapezoidalProfileFollower;
import org.teamtators.common.datalogging.DataCollector;
import org.teamtators.common.datalogging.LogDataProvider;
import org.teamtators.common.controllers.LogitechF310;
import org.teamtators.common.tester.ManualTest;

import java.util.Arrays;
import java.util.List;

public class MotionCalibrationTest extends ManualTest {
    private final TrapezoidalProfileFollower follower;
    private final ControllerInput positionProvider;
    private final ControllerInput velocityProvider;
    private final ControllerOutput outputConsumer;

    private double stickInput;
    private boolean applyStick;
    private boolean run;
    private double power;
    private double position;
    private double velocity;
    private double lastVelocity;
    private double velocityPower;
    private double acceleration;
    private double acclerationPower;

    private final DataCollector dataCollector;
    private LogDataProvider logDataProvider = new LogDataProvider() {
        @Override
        public String getName() {
            return MotionCalibrationTest.this.getName();
        }

        @Override
        public List<Object> getKeys() {
            return Arrays.asList("power", "position", "velocity", "velocityPower", "acceleration", "accelerationPower");
        }

        @Override
        public List<Object> getValues() {
            return Arrays.asList(power, position, velocity, velocityPower, acceleration, acclerationPower);
        }
    };


    public MotionCalibrationTest(String name, TrapezoidalProfileFollower follower) {
        super(name);
        this.follower = follower;
        this.positionProvider = follower.getPositionProvider();
        this.velocityProvider = follower.getVelocityProvider();
        this.outputConsumer = follower.getOutputConsumer();
        dataCollector = DataCollector.getDataCollector();
    }

    public MotionCalibrationTest(TrapezoidalProfileFollower follower) {
        this(follower.getName() + "Calibration", follower);
    }

    @Override
    public void start() {
        run = false;
        applyStick = false;
        logger.info("A to get info, B to set power from stick, hold X to run at set power, hold Y to run at joystick power");
    }

    @Override
    public void update(double delta) {
        position = positionProvider.getControllerInput();
        velocity = velocityProvider.getControllerInput();
        acceleration = (velocity - lastVelocity) / delta;
        lastVelocity = velocity;
        TrapezoidalProfileFollower.Config config = follower.getConfig();
        velocityPower = config.kfV * velocity + Math.copySign(config.kMinOutput, velocity) + config.kHoldPower;
        acclerationPower = config.kfA * acceleration;
        if (applyStick) {
            power = stickInput;
        }
        if (run) {
            outputConsumer.controllerWrite(power);
        } else {
            outputConsumer.controllerWrite(0.0);
        }
    }

    @Override
    public void onButtonDown(LogitechF310.Button button) {
        switch (button) {
            case A:
                logger.info(String.format("power=%.3f, position=%.3f, velocity=%.3f, acceleration=%.3f, velPower=%.3f, accelPower=%.3f",
                        power, position, velocity, acceleration, velocityPower, acclerationPower));
                break;
            case B:
                power = stickInput;
                logger.info(String.format("Set stick power to %.3f", power));
                break;
            case X:
                logger.info(String.format("Running at power %.3f", power));
                run = true;
                dataCollector.startProvider(logDataProvider);
                break;
            case Y:
                logger.info("Running at stick power");
                run = applyStick = true;
                dataCollector.startProvider(logDataProvider);
                break;
        }
    }

    @Override
    public void onButtonUp(LogitechF310.Button button) {
        switch (button) {
            case X:
            case Y:
                logger.info("Stopped running");
                run = applyStick = false;
                dataCollector.stopProvider(logDataProvider);
                break;
        }
    }

    @Override
    public void updateAxis(double value) {
        this.stickInput = value;
    }

    @Override
    public void stop() {
        dataCollector.stopProvider(logDataProvider);
        outputConsumer.controllerWrite(0.0);
    }
}
