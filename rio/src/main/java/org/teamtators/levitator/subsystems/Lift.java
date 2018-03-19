package org.teamtators.levitator.subsystems;

import edu.wpi.first.wpilibj.Encoder;
import edu.wpi.first.wpilibj.Sendable;
import edu.wpi.first.wpilibj.Solenoid;
import edu.wpi.first.wpilibj.SpeedController;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import org.teamtators.common.config.Configurable;
import org.teamtators.common.config.helpers.*;
import org.teamtators.common.control.*;
import org.teamtators.common.controllers.LogitechF310;
import org.teamtators.common.hw.AnalogPotentiometer;
import org.teamtators.common.hw.DigitalSensor;
import org.teamtators.common.hw.SpeedControllerGroup;
import org.teamtators.common.math.Epsilon;
import org.teamtators.common.scheduler.RobotState;
import org.teamtators.common.scheduler.Subsystem;
import org.teamtators.common.tester.ManualTest;
import org.teamtators.common.tester.ManualTestGroup;
import org.teamtators.common.tester.components.*;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class Lift extends Subsystem implements Configurable<Lift.Config> {

    private SpeedControllerGroup liftMotor;
    private MotorPowerUpdater liftMotorUpdater;
    private Encoder liftEncoder;
    private DigitalSensor limitSensorTop;
    private DigitalSensor limitSensorBottom;
    private SpeedController pivotMotor;
    private MotorPowerUpdater pivotMotorUpdater;
    private Updater pivotUpdater;
    private AnalogPotentiometer pivotEncoder;
    private Solenoid pivotLockSolenoid;
    private DigitalSensor pivotLockSensor;

    private double desiredPivotAngle;
    private double desiredHeight;
    private double targetHeight;
    private double lastAttemptedHeight;
    private double targetAngle;
    private double lastAttemptedAngle;

    private TrapezoidalProfileFollower liftController;
    private /*TrapezoidalProfileFollower*/ StupidController pivotController;
    private InputDerivative pivotVelocity;
    private Updatable holdPowerApplier;
    private Updatable networkTablesUpdater;


    private Config config;

    private boolean isMovementInitiatedByCommand = false;

    public Lift() {
        super("Lift");

        liftController = new TrapezoidalProfileFollower("liftController");
        liftController.setPositionProvider(this::getCurrentHeight);
        liftController.setVelocityProvider(this::getLiftVelocity);
        liftController.setOutputConsumer(this::setLiftPower);
        liftController.setOnTargetPredicate(ControllerPredicates.alwaysFalse());

        pivotVelocity = new InputDerivative("pivotAngleDerivative", this::getCurrentPivotAngle);
        pivotController = new /*TrapezoidalProfileFollower*/StupidController("pivotController");
//        pivotController.setPositionProvider(this::getCurrentPivotAngle);
//        pivotController.setVelocityProvider(pivotVelocity);
        pivotController.setInputProvider(this::getCurrentPivotAngle);
        pivotController.setOutputConsumer(this::setPivotPower);
//        pivotController.setOnTargetPredicate(ControllerPredicates.alwaysFalse());
        holdPowerApplier = new Updatable() {
            @Override
            public String getName() {
                return "holdPowerApplier";
            }

            @Override
            public void update(double delta) {
                if (Lift.this.getCurrentHeight() < 1) {
                    Lift.this.liftController.setHoldPower(-0.1);
                } else {

                    Lift.this.liftController.setHoldPower(Lift.this.config.heightController.kHoldPower);
                }
            }
        };
        networkTablesUpdater = new Updatable() {
            @Override
            public String getName() {
                return "networkTablesUpdater";
            }

            @Override
            public void update(double delta) {
                SmartDashboard.putNumber("liftTarget", Lift.this.getTargetHeight());
            }
        };
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
    public void setDesiredHeight(double desiredHeight, boolean commandInitiated) {
        if (desiredHeight < config.heightController.minPosition) {
            logger.warn("Lift desired height exceeded bottom height limit ({} < {})", desiredHeight,
                    config.heightController.minPosition);
            desiredHeight = config.heightController.minPosition;
        } else if (desiredHeight > config.heightController.maxPosition) {
            logger.warn("Lift desired height exceeded top height limit ({} > {})", desiredHeight,
                    config.heightController.maxPosition);
            desiredHeight = config.heightController.maxPosition;
        }

        if (isMovementInitiatedByCommand && !commandInitiated) {
            return;
        }
//        if (getSafeLiftHeight(desiredHeight) == desiredHeight) {
        logger.info("Setting desired lift height to {}", desiredHeight);
        this.desiredHeight = desiredHeight;
        isMovementInitiatedByCommand = commandInitiated;
//        } else {
//            logger.warn("Cannot move lift to desired height {} when picker is rotated at {}!!", desiredHeight, pivotEncoder.get());
//        }
    }

    public void setDesiredHeightPreset(HeightPreset desiredHeight) {
        setDesiredHeight(getHeightPreset(desiredHeight), true);
    }

    public double getHeightPreset(HeightPreset heightPreset) {
        if (!config.heightPresets.containsKey(heightPreset)) {
            return 0.0;
        }
        return config.heightPresets.get(heightPreset);
    }

    public double getTargetHeight() {
        return this.targetHeight;
    }

    public void setTargetHeight(double height) {
        if (targetHeight == height) {
            return;
        }
        double safeHeight = getSafeLiftHeight(height);
        if (safeHeight != height) {
            if (targetHeight == safeHeight && lastAttemptedHeight == height) {
                return;
            } else {
                logger.warn("Target height is unsafe with current picker conditions (angle: {}): {}. Moving to {}", getCurrentPivotAngle(),
                        height, safeHeight);
                lastAttemptedHeight = height;
                height = safeHeight;
            }
        }
        double distance = height - getCurrentHeight();
        logger.debug(String.format("Setting lift target height to %.3f (distance to move: %.3f)",
                height, distance));
        targetHeight = height;
        liftController.moveToPosition(height);
    }

    private void enableLiftController() {
        targetHeight = getCurrentHeight();
        liftController.moveToPosition(targetHeight);
        liftController.start();
    }

    private void disableLiftController() {
        liftController.stop();
    }

    public double getCurrentPivotAngle() {
        return pivotEncoder.get();
    }

    public double getDesiredPivotAngle() {
        return desiredPivotAngle;
    }

    public void setDesiredPivotAngle(double desiredAngle) {
//        if (getSafePivotAngle(desiredAngle) == desiredAngle) {
        logger.info("Setting desired pivot angle {}", desiredAngle);
        this.desiredPivotAngle = desiredAngle;
//        } else {
//            logger.warn("Rotation to desired angle {} is not allowed at the current height {}!!", desiredAngle, getCurrentHeight());
//        }
    }

    public void setDesiredAnglePreset(AnglePreset desiredPivotAngle) {
        setDesiredPivotAngle(getAnglePreset(desiredPivotAngle));
    }

    public double getAnglePreset(AnglePreset anglePreset) {
        return config.anglePresets.get(anglePreset);
    }

    public double getTargetAngle() {
        return targetAngle;
    }

    public void setTargetAngle(double angle) {
        if (targetAngle == angle) {
            return;
        }
        double safeAngle = getSafePivotAngle(angle);
        if (safeAngle != angle) {
            if (targetAngle == safeAngle && lastAttemptedAngle == angle) {
                return;
            } else {
                logger.warn("Target angle is unsafe with current lift conditions: {}. Moving to {}", angle, safeAngle);
                lastAttemptedAngle = angle;
                angle = safeAngle;
            }
        }
        double distance = angle - getCurrentPivotAngle();
        logger.debug(String.format("Setting lift target angle to %.3f (degrees to move: %.3f)",
                angle, distance));
        targetAngle = angle;
        pivotController./*moveToPosition*/setSetpoint(angle);
        pivotController.setHoldPower(Math.signum(angle) * config.pivotHoldPower);
    }

    private void enablePivotController() {
        setTargetAngle(0);
        pivotController.start();
    }

    private void disablePivotController() {
        pivotController.stop();
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
        liftMotorUpdater.set(liftPower);
    }

    public void bumpLiftUp() {
        setDesiredHeight(getDesiredHeight() + config.bumpHeightValue, true);
    }

    public void bumpLiftDown() {
        setDesiredHeight(getDesiredHeight() - config.bumpHeightValue, true);
    }

    public void bumpPivotRight() {
        setDesiredPivotAngle(getDesiredPivotAngle() + config.bumpPivotValue);
    }

    public void bumpPivotLeft() {
        setDesiredPivotAngle(getDesiredPivotAngle() - config.bumpPivotValue);
    }

    public void setPivotPower(double pivotPower) {
        if (!isPivotLocked()) {
            pivotMotorUpdater.set(pivotPower);
        } else {
            pivotMotorUpdater.set(0);
        }
    }

    public boolean isAtDesiredHeight() {
        return liftController.isOnTarget();
    }

    public double sliderToHeight(double slider) {
        return ((slider + 1) / 2) * config.heightController.maxPosition;
    }

    public TrapezoidalProfileFollower getLiftController() {
        return liftController;
    }

    public Updatable getPivotController() {
        return pivotController;
    }

    public List<Updatable> getUpdatables() {
        return Arrays.asList(holdPowerApplier, pivotVelocity, pivotController, liftController, networkTablesUpdater);
    }

    public boolean isPivotLocked() {
        return !pivotLockSensor.get();
    }

    public boolean isPivotInCenter() {
        return Epsilon.isEpsilonEqual(getCurrentPivotAngle(),
                getAnglePreset(AnglePreset.CENTER),
                config.centerTolerance);
    }

    public void setPivotLockSolenoid(boolean lock) {
        pivotLockSolenoid.set(lock);
    }

    public void clearForceMovementFlag() {
        this.isMovementInitiatedByCommand = false;
    }

    public boolean isMovementInitiatedByCommand() {
        return isMovementInitiatedByCommand;
    }

    public boolean isAtHeight() {
        return Math.abs(getCurrentHeight() - getDesiredHeight()) < config.heightTolerance;
    }

    @Override
    public void onEnterRobotState(RobotState state) {
        switch (state) {
            case AUTONOMOUS:
            case TELEOP:
                setDesiredHeight(getCurrentHeight(), true);
                setDesiredAnglePreset(AnglePreset.CENTER);
                setTargetAngle(getAnglePreset(AnglePreset.CENTER));
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
        tests.addTest(new SolenoidTest("pivotLockSolenoid", pivotLockSolenoid));
        tests.addTest(new DigitalSensorTest("pivotLockSensor", pivotLockSensor));

        tests.addTest(new MotionCalibrationTest(liftController));
        tests.addTest(new /*MotionCalibrationTest*/ControllerTest(pivotController, 90.0));

        tests.addTest(new LiftTest());

        return tests;
    }

    @Override
    public void configure(Config config) {
        super.configure();
        this.config = config;

        this.liftMotor = config.liftMotor.create();
        this.liftEncoder = config.liftEncoder.create();
        this.limitSensorTop = config.limitSensorTop.create();
        this.limitSensorBottom = config.limitSensorBottom.create();
        this.pivotMotor = config.pivotMotor.create();
        this.pivotEncoder = config.pivotEncoder.create();
        this.pivotLockSolenoid = config.pivotLockSolenoid.create();
        this.pivotLockSensor = config.pivotLockSensor.create();

        this.liftController.configure(config.heightController);
        this.pivotController.configure(config.pivotController);

        liftMotor.setName("Lift", "liftMotor");
        liftEncoder.setName("Lift", "liftEncoder");
        limitSensorTop.setName("Lift", "limitSensorTop");
        limitSensorBottom.setName("Lift", "limitSensorBottom");
        ((Sendable) pivotMotor).setName("Lift", "pivotMotor");
        pivotEncoder.setName("Lift", "pivotEncoder");

        pivotMotorUpdater = new MotorPowerUpdater(pivotMotor);
        liftMotorUpdater = new MotorPowerUpdater(liftMotor);

        pivotUpdater = new Updater(pivotMotorUpdater);

        pivotUpdater.start();
    }

    @Override
    public void deconfigure() {
        super.deconfigure();

        SpeedControllerConfig.free(liftMotor);
        liftEncoder.free();
        limitSensorTop.free();
        limitSensorBottom.free();
        SpeedControllerConfig.free(pivotMotor);
        pivotEncoder.free();
        pivotLockSolenoid.free();
        pivotLockSensor.free();

        pivotUpdater.stop();

        pivotUpdater = null; //so the GC catches these bad boys
    }

    private double getSafePivotAngle(double desiredAngle) {
        double centerAngle = getAnglePreset(AnglePreset.CENTER);
        if (Epsilon.isEpsilonLessThan(getCurrentHeight(),
                getHeightPreset(HeightPreset.NEED_LOCK),
                config.heightTolerance)) {
            return centerAngle;
        }
        if (Epsilon.isEpsilonLessThan(getCurrentHeight(),
                getHeightPreset(HeightPreset.NEED_CENTER),
                config.heightTolerance)) {
            double maxAngle = centerAngle + config.centerTolerance;
            double minAngle = centerAngle - config.centerTolerance;
            return Math.min(Math.max(desiredAngle, minAngle), maxAngle);
        }
        return desiredAngle;
    }

    public double getSafeLiftHeight(double desiredHeight) {
        double currentLiftHeight = getCurrentHeight();
        double needLockHeight = getHeightPreset(HeightPreset.NEED_LOCK);
        double needCenterHeight = getHeightPreset(HeightPreset.NEED_CENTER);
        if (!isPivotLocked()) { // if the pivot is not locked
            if (desiredHeight < needLockHeight) { // if we want to descend to below NEED_LOCK
                return needLockHeight; // then descend to the minimum height at which we can be unlocked
            }
        }
        if (!isPivotInCenter()) { // if the picker is out far enough that we can't go below NEED_CENTER
            if (currentLiftHeight < needCenterHeight) { // if we are not above the elevators
                return getCurrentHeight(); // don't move
            }
            if (desiredHeight < needCenterHeight) { // if we want to descend to below the elevators
                return needCenterHeight; // then descend to the minimum height at which we can rotate
            }
        }
        return desiredHeight; // if picker is all good, go wherever we need to
    }

    public void setPivotControllerEnabled(boolean enabled) {
        if (enabled) {
            pivotController.start();
        } else {
            pivotController.stop();
        }
    }

    public List<Updatable> getMotorUpdatables() {
        return Arrays.asList(liftMotorUpdater, pivotMotorUpdater);
    }

    public enum HeightPreset {
        HOME,
        PICK,
        NEED_LOCK,
        NEED_CENTER,
        SWITCH_LOW,
        SWITCH,
        SCALE_LOW,
        SCALE_HIGH;
    }

    public enum AnglePreset {
        LEFT,
        HALF_LEFT,
        CENTER,
        HALF_RIGHT,
        RIGHT;
    }

    public static class Config {
        public SpeedControllerGroupConfig liftMotor;
        public EncoderConfig liftEncoder;
        public DigitalSensorConfig limitSensorTop;
        public DigitalSensorConfig limitSensorBottom;
        public SpeedControllerConfig pivotMotor;
        public AnalogPotentiometerConfig pivotEncoder;
        public SolenoidConfig pivotLockSolenoid;
        public DigitalSensorConfig pivotLockSensor;

        public TrapezoidalProfileFollower.Config heightController;
        public /*TrapezoidalProfileFollower*/ StupidController.Config pivotController;
        public double pivotHoldPower;

        public Map<AnglePreset, Double> anglePresets;
        public Map<HeightPreset, Double> heightPresets;

        public double bumpHeightValue;
        public double bumpPivotValue;

        public double heightTolerance;
        public double angleTolerance;
        public double centerTolerance;
    }

    private class LiftTest extends ManualTest {
        private double axisValue;

        public LiftTest() {
            super("LiftTest");
        }

        @Override
        public void start() {
            logger.info("Press A to set lift target to joystick value. Hold X to enable lift profiler");
            logger.info("Press B to set pivot target to joystick value. Hold Y to enable pivot profiler");
            disableLiftController();
            disablePivotController();
        }

        @Override
        public void onButtonDown(LogitechF310.Button button) {
            switch (button) {
                case A:
                    double height = (config.heightController.maxPosition - config.heightController.minPosition)
                            * ((axisValue + 1) / 2) + config.heightController.minPosition;
                    setTargetHeight(height);
                    break;
                case B:
                    double angle = (config.pivotController./*maxPosition*/maxSetpoint - config.pivotController./*minPosition*/minSetpoint)
                            * ((axisValue + 1) / 2) + config.pivotController./*minPosition*/minSetpoint;
                    logger.info("Moving pivot to angle {}", angle);
                    pivotController./*moveToPosition*/setSetpoint(angle);
                    break;
                case X:
                    enableLiftController();
                    break;
                case Y:
                    enablePivotController();
                    break;
            }
        }

        @Override
        public void onButtonUp(LogitechF310.Button button) {
            switch (button) {
                case X:
                    disableLiftController();
                    break;
                case Y:
                    disablePivotController();
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
            disablePivotController();
        }
    }

}
