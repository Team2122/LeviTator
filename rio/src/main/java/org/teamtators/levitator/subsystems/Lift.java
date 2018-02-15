package org.teamtators.levitator.subsystems;

import edu.wpi.first.wpilibj.Encoder;
import edu.wpi.first.wpilibj.SpeedController;
import org.teamtators.common.config.Configurable;
import org.teamtators.common.config.helpers.*;
import org.teamtators.common.control.ControllerPredicates;
import org.teamtators.common.control.PidController;
import org.teamtators.common.control.TrapezoidalProfile;
import org.teamtators.common.control.TrapezoidalProfileFollower;
import org.teamtators.common.controllers.LogitechF310;
import org.teamtators.common.hw.AnalogPotentiometer;
import org.teamtators.common.hw.DigitalSensor;
import org.teamtators.common.hw.SpeedControllerGroup;
import org.teamtators.common.scheduler.RobotState;
import org.teamtators.common.scheduler.Subsystem;
import org.teamtators.common.tester.ManualTest;
import org.teamtators.common.tester.ManualTestGroup;
import org.teamtators.common.tester.components.*;

public class Lift extends Subsystem implements Configurable<Lift.Config> {

    private SpeedControllerGroup liftMotor;
    private Encoder liftEncoder;
    private DigitalSensor limitSensorTop;
    private DigitalSensor limitSensorBottom;
    private SpeedController pivotMotor;
    private AnalogPotentiometer pivotEncoder;

    private double desiredPivotAngle;
    private double desiredHeight;
    private double targetHeight;

    private TrapezoidalProfileFollower liftController;
    private PidController pivotController;

    private Config config;
    private TrapezoidalProfile liftProfile;

    public Lift() {
        super("Lift");

        liftController = new TrapezoidalProfileFollower("liftController");
        liftController.setPositionProvider(this::getCurrentHeight);
        liftController.setVelocityProvider(this::getLiftVelocity);
        liftController.setOutputConsumer(this::setLiftPower);
        liftController.setOnTargetPredicate(ControllerPredicates.alwaysFalse());

        pivotController = new PidController("pivotController");
        pivotController.setInputProvider(this::getCurrentPivotAngle);
        pivotController.setOutputConsumer(this::setPivotPower);
    }

    /**
     * @return height in inches
     */
    public double getCurrentHeight() {
        return liftEncoder.getDistance();
    }

    /**
     * @return velocity in inches per second
     */
    public double getLiftVelocity() {
        return liftEncoder.getRate();
    }

    /**
     * @return height in inches
     */
    public double getDesiredHeight() {
        return desiredHeight;
    }

    /**
     * @param desiredHeight height in inches
     */
    public void setDesiredHeight(double desiredHeight) {
        if (getSafeLiftHeight(desiredHeight) == desiredHeight) {
            this.desiredHeight = desiredHeight;
        } else {
            logger.warn("Cannot move lift to height {} when picker is rotated at {}!!", desiredHeight, pivotEncoder.get());
        }
    }

    public void setDesiredHeightPreset(HeightPreset desiredHeight) {
        setDesiredHeight(getHeightPreset(desiredHeight));
    }

    public double getHeightPreset(HeightPreset heightPreset) {
        double heightValue = 0.0;
        switch (heightPreset) {
            case PICK:
                heightValue = config.heightPresetPick;
                break;
            case SWITCH:
                heightValue = config.heightPresetSwitch;
                break;
            case SCALE_LOW:
                heightValue = config.heightPresetScaleLow;
                break;
            case SCALE_HIGH:
                heightValue = config.heightPresetScaleHigh;
                break;
            case HOME:
                heightValue = config.heightPresetHome;
        }
        return heightValue;
    }

    public double getTargetHeight() {
        return this.targetHeight;
    }

    public void setTargetHeight(double height) {
        if (targetHeight == height) {
            return;
        }
        if (height < config.heightBottomLimit) {
            logger.warn("Lift target height exceeded bottom height limit ({} < {})", height, config.heightBottomLimit);
            height = config.heightBottomLimit;
        } else if (height > config.heightTopLimit) {
            logger.warn("Lift target height exceeded top height limit ({} > {})", height, config.heightTopLimit);
            height = config.heightTopLimit;
        }
        this.targetHeight = height;
        double distance = height - getCurrentHeight();
        logger.debug(String.format("Setting lift target height to %.3f (distance to move: %.3f)",
                height, distance));
        liftProfile.setDistance(distance);
        liftProfile.setStartVelocity(getLiftVelocity());
        liftProfile.setTravelVelocity(Math.copySign(liftProfile.getTravelVelocity(), distance));
        logger.trace("Profile: " + liftProfile);
        liftController.updateProfile();
    }

    private void enableLiftController() {
        setTargetHeight(getCurrentHeight());
        liftController.start();
    }

    private void disableLiftController() {
        setTargetHeight(getCurrentHeight());
        liftController.stop();
    }

    public double getCurrentPivotAngle() {
        return pivotEncoder.get();
    }

    public double getDesiredPivotAngle() {
        return desiredPivotAngle;
    }

    public void setDesiredPivotAngle(double desiredAngle) {
        if (getSafePivotAngle(desiredAngle) == desiredAngle) {
            this.desiredPivotAngle = desiredAngle;
        } else {
            logger.warn("Rotation to {} is not allowed at the current height {}!!", desiredAngle, getCurrentHeight());
        }
    }

    public void setDesiredAnglePreset(AnglePreset desiredPivotAngle) {
        setDesiredPivotAngle(getAnglePreset(desiredPivotAngle));
    }

    public double getAnglePreset(AnglePreset anglePreset) {
        double angleValue = 0.0;
        switch (anglePreset) {
            case LEFT:
                angleValue = config.anglePresetLeft;
                break;
            case RIGHT:
                angleValue = config.anglePresetRight;
                break;
            case CENTER:
                angleValue = config.anglePresetCenter;
                break;
        }
        return angleValue;
    }

    public double getTargetAngle() {
        return pivotController.getSetpoint();
    }

    public void setTargetAngle(double angle) {
        pivotController.setSetpoint(angle);
    }

    public boolean isAtBottomLimit() {
        return limitSensorBottom.get();
    }

    public boolean isAtTopLimit() {
        return !limitSensorTop.get();
    }

    public void setLiftPower(double liftPower) {
        //limit to max zero if max height is triggered
        if (isAtTopLimit() && liftPower > 0.0) {
            liftPower = 0.0;
        }
        if (isAtBottomLimit() && liftPower < 0.0) {
            liftPower = 0.0;
        }
        liftMotor.set(liftPower);
    }

    public void bumpLiftUp() {
        setDesiredHeight(getCurrentHeight() + config.bumpHeightValue);
    }

    public void bumpLiftDown() {
        setDesiredHeight(getCurrentHeight() - config.bumpHeightValue);
    }

    public void bumpPivotRight() {
        setDesiredPivotAngle(getCurrentPivotAngle() + config.bumpPivotValue);
    }

    public void bumpPivotLeft() {
        setDesiredPivotAngle(getCurrentPivotAngle() - config.bumpPivotValue);
    }

    public void setPivotPower(double pivotPower) {
        pivotMotor.set(pivotPower);
    }

    public TrapezoidalProfileFollower getLiftController() {
        return liftController;
    }

    public PidController getPivotController() {
        return pivotController;
    }

    @Override
    public void onEnterRobotState(RobotState state) {
        switch (state) {
            case AUTONOMOUS:
            case TELEOP:
                setDesiredHeight(getCurrentHeight());
                setDesiredPivotAngle(getCurrentPivotAngle());
                setTargetAngle(getCurrentPivotAngle());
                pivotController.start();
                enableLiftController();
                break;
            case DISABLED:
                pivotController.stop();
                disableLiftController();
                break;
        }
    }

    @Override
    public ManualTestGroup createManualTests() {
        ManualTestGroup tests = super.createManualTests();
        tests.addTest(new SpeedControllerTest("liftMotor", liftMotor));
        tests.addTest(new EncoderTest("liftEncoder", liftEncoder));
        tests.addTest(new DigitalSensorTest("limitSensorTop", limitSensorTop));
        tests.addTest(new DigitalSensorTest("limitSensorBottom", limitSensorBottom));
        tests.addTest(new SpeedControllerTest("pivotMotor", pivotMotor));
        tests.addTest(new AnalogPotentiometerTest("pivotEncoder", pivotEncoder));

        tests.addTest(new MotionCalibrationTest(liftController));
        tests.addTest(new ControllerTest(pivotController));

        tests.addTest(new LiftTest());

        return tests;
    }

    @Override
    public void configure(Config config) {
        this.config = config;

        this.liftMotor = config.liftMotor.create();
        this.liftEncoder = config.liftEncoder.create();
        this.limitSensorTop = config.limitSensorTop.create();
        this.limitSensorBottom = config.limitSensorBottom.create();
        this.pivotMotor = config.pivotMotor.create();
        this.pivotEncoder = config.pivotEncoder.create();

        this.liftController.configure(config.heightController);
        this.liftProfile = config.baseHeightProfile;
        liftController.setBaseProfile(liftProfile);
        this.pivotController.configure(config.pivotController);

        liftMotor.setName("Lift", "liftMotor");
        liftEncoder.setName("Lift", "liftEncoder");
        limitSensorTop.setName("Lift", "limitSensorTop");
        limitSensorBottom.setName("Lift", "limitSensorBottom");
        //((Sendable) pivotMotor).setName("Lift", "pivotMotor");
        pivotEncoder.setName("Lift", "pivotEncoder");
    }

    private double getSafePivotAngle(double desiredAngle) {
        if (getCurrentHeight() < config.heightPresetSwitch - config.heightTolerance) {
            return config.anglePresetCenter;
        }
        return desiredAngle;
    }

    public double getSafeLiftHeight(double desiredHeight) {
        double currentPivotAngle = getCurrentPivotAngle();
        double currentLiftHeight = getCurrentHeight();
        if (currentPivotAngle < (config.anglePresetCenter - config.angleTolerance) ||
                currentPivotAngle > (config.anglePresetCenter + config.angleTolerance)) { // if the picker is out far enough that we can't go below level of elevators
            if (currentLiftHeight < config.heightPresetSwitch) { // if we are not above the elevators
                return config.heightPresetSwitch; // don't move
            }
            if (desiredHeight < config.heightPresetSwitch) { // if we want to descend to below the elevators
                return config.heightPresetSwitch; // then descend to the minimum height at which we can rotate
            }
        }
        return desiredHeight; // if picker is all good, go wherever we need to
    }

    public enum HeightPreset {
        PICK,
        SWITCH,
        SCALE_LOW,
        SCALE_HIGH,
        HOME;
    }

    public enum AnglePreset {
        LEFT,
        CENTER,
        RIGHT;
    }

    public static class Config {
        public SpeedControllerGroupConfig liftMotor;
        public EncoderConfig liftEncoder;
        public DigitalSensorConfig limitSensorTop;
        public DigitalSensorConfig limitSensorBottom;
        public SpeedControllerConfig pivotMotor;
        public AnalogPotentiometerConfig pivotEncoder;

        public TrapezoidalProfileFollower.Config heightController;
        public TrapezoidalProfile baseHeightProfile;
        public PidController.Config pivotController;

        public double anglePresetLeft;
        public double anglePresetCenter;
        public double anglePresetRight;

        public double heightBottomLimit;
        public double heightTopLimit;
        public double heightPresetPick;
        public double heightPresetSwitch;
        public double heightPresetScaleLow;
        public double heightPresetScaleHigh;
        public double heightPresetHome;

        public double bumpHeightValue;
        public double bumpPivotValue;

        public double heightTolerance;
        public double angleTolerance;
    }

    private class LiftTest extends ManualTest {
        private double axisValue;

        public LiftTest() {
            super("LiftTest");
        }

        @Override
        public void start() {
            logger.info("Press A to set lift target to joystick value. Hold Y to enable lift profiler");
            disableLiftController();
            //temp
            pivotController.start();
            pivotController.setSetpoint(0);
        }

        @Override
        public void onButtonDown(LogitechF310.Button button) {
            switch (button) {
                case A:
                    double height = (config.heightTopLimit - config.heightBottomLimit)
                            * ((axisValue + 1) / 2) + config.heightBottomLimit;
                    setDesiredHeight(height);
                    break;
                case Y:
                    enableLiftController();
                    break;
            }
        }

        @Override
        public void onButtonUp(LogitechF310.Button button) {
            switch (button) {
                case Y:
                    disableLiftController();
                    break;
            }
        }

        @Override
        public void updateAxis(double value) {
            this.axisValue = value;
        }

        @Override
        public void stop() {
            disableLiftController();
        }
    }

}
