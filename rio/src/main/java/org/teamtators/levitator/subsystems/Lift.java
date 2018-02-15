package org.teamtators.levitator.subsystems;

import edu.wpi.first.wpilibj.Encoder;
import edu.wpi.first.wpilibj.Sendable;
import edu.wpi.first.wpilibj.Solenoid;
import edu.wpi.first.wpilibj.SpeedController;
import org.teamtators.common.config.Configurable;
import org.teamtators.common.config.helpers.*;
import org.teamtators.common.control.*;
import org.teamtators.common.controllers.LogitechF310;
import org.teamtators.common.hw.AnalogPotentiometer;
import org.teamtators.common.hw.DigitalSensor;
import org.teamtators.common.hw.SpeedControllerGroup;
import org.teamtators.common.scheduler.RobotState;
import org.teamtators.common.scheduler.Subsystem;
import org.teamtators.common.tester.AutomatedTest;
import org.teamtators.common.tester.AutomatedTestMessage;
import org.teamtators.common.tester.ManualTest;
import org.teamtators.common.tester.ManualTestGroup;
import org.teamtators.common.tester.automated.MotorCurrentTest;
import org.teamtators.common.tester.automated.MotorEncoderTest;
import org.teamtators.common.tester.components.*;
import org.teamtators.levitator.TatorRobot;

import java.util.Arrays;
import java.util.List;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class Lift extends Subsystem implements Configurable<Lift.Config> {

    private SpeedControllerGroup liftMotor;
    private Encoder liftEncoder;
    private DigitalSensor limitSensorTop;
    private DigitalSensor limitSensorBottom;
    private SpeedController pivotMotor;
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

    private TatorRobot robot;
    private Config config;

    public Lift(TatorRobot robot) {
        super("Lift");

        this.robot = robot;

        liftController = new TrapezoidalProfileFollower("liftController");
        liftController.setPositionProvider(this::getCurrentHeight);
        liftController.setVelocityProvider(this::getLiftVelocity);
        liftController.setOutputConsumer(this::setLiftPower);
        liftController.setOnTargetPredicate(ControllerPredicates.alwaysFalse());

        pivotVelocity = new InputDerivative(this::getCurrentPivotAngle);
        pivotController = new /*TrapezoidalProfileFollower*/StupidController("pivotController");
//        pivotController.setPositionProvider(this::getCurrentPivotAngle);
//        pivotController.setVelocityProvider(pivotVelocity);
        pivotController.setInputProvider(this::getCurrentPivotAngle);
        pivotController.setOutputConsumer(this::setPivotPower);
//        pivotController.setOnTargetPredicate(ControllerPredicates.alwaysFalse());
        holdPowerApplier = delta -> {
                  if(getCurrentHeight() < 1) {
                      Lift.this.liftController.setHoldPower(-0.1);
                  } else {
                      Lift.this.liftController.setHoldPower(Lift.this.config.heightController.kHoldPower);
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
    public void setDesiredHeight(double desiredHeight) {
        if (desiredHeight < config.heightController.minPosition) {
            logger.warn("Lift desired height exceeded bottom height limit ({} < {})", desiredHeight,
                    config.heightController.minPosition);
            desiredHeight = config.heightController.minPosition;
        } else if (desiredHeight > config.heightController.maxPosition) {
            logger.warn("Lift desired height exceeded top height limit ({} > {})", desiredHeight,
                    config.heightController.maxPosition);
            desiredHeight = config.heightController.maxPosition;
        }
//        if (getSafeLiftHeight(desiredHeight) == desiredHeight) {
            logger.info("Setting desired lift height to {}", desiredHeight);
            this.desiredHeight = desiredHeight;
//        } else {
//            logger.warn("Cannot move lift to desired height {} when picker is rotated at {}!!", desiredHeight, pivotEncoder.get());
//        }
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
            case SWITCH_LOW:
                heightValue = config.heightPresetLowSwitch;
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
        double safeHeight = getSafeLiftHeight(height);
        if (safeHeight != height) {
            if (targetHeight == safeHeight && lastAttemptedHeight == height) {
                return;
            } else {
                logger.warn("Target height is unsafe with current picker conditions: {}. Moving to {}", height, safeHeight);
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

    public double getLiftCurrent() {
        return config.liftMotor.get(0).getTotalCurrent(robot.getPDP()) + config.liftMotor.get(1).getTotalCurrent(robot.getPDP());
    }

    public void setPivotPower(double pivotPower) {
        if(!isPivotLocked()) {
            pivotMotor.set(pivotPower);
        } else {
            pivotMotor.set(0);
        }
    }

    public double getPivotCurrent() {
        return config.pivotMotor.getTotalCurrent(robot.getPDP());
    }

    public TrapezoidalProfileFollower getLiftController() {
        return liftController;
    }

    public Updatable getPivotController() {
        return pivotController;
    }

    public List<Updatable> getUpdatables() {
        return Arrays.asList(holdPowerApplier, pivotVelocity, pivotController, liftController);
    }

    public boolean isPivotLocked() {
        return !pivotLockSensor.get();
    }

    public void setPivotLockSolenoid(boolean lock) {
        pivotLockSolenoid.set(lock);
    }

    @Override
    public void onEnterRobotState(RobotState state) {
        switch (state) {
            case AUTONOMOUS:
            case TELEOP:
                setDesiredHeight(getCurrentHeight());
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
    public List<AutomatedTest> createAutomatedTests() {
        return Arrays.asList(
                new MotorCurrentTest("LiftMotorCurrentTest", this::setTargetHeight, this::getLiftCurrent),
                new MotorEncoderTest("LiftMotorEncoderTest", this::setTargetHeight, this::getLiftVelocity),
                new MotorCurrentTest("LiftPivotMotorTest", this::setPivotPower, this::getPivotCurrent),
                new MotorEncoderTest("LiftPivotMotorEncoderTest", this::setPivotPower, this::getCurrentPivotAngle),
                new LiftLimitTest("LiftLowerLimitTest", true, -config.liftLimitTestPower, config.liftLimitTestTimeout),
                new LiftLimitTest("LiftUpperLimitTest", false, config.liftLimitTestPower, config.liftLimitTestTimeout)
        );
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
    }

    private double getSafePivotAngle(double desiredAngle) {
        if (getCurrentHeight() < config.heightPresetSwitch - config.heightTolerance) {
            return config.anglePresets.get(AnglePreset.CENTER);
        }
        return desiredAngle;
    }

    public double getSafeLiftHeight(double desiredHeight) {
        double currentPivotAngle = getCurrentPivotAngle();
        double currentLiftHeight = getCurrentHeight();
        if (currentPivotAngle < (config.anglePresets.get(AnglePreset.CENTER) - config.angleTolerance) ||
                currentPivotAngle > (config.anglePresets.get(AnglePreset.CENTER) + config.angleTolerance) ||
                !isPivotLocked()) { // if the picker is out far enough that we can't go below level of elevators
            if (currentLiftHeight < config.heightPresetSwitch) { // if we are not above the elevators
                return getCurrentHeight(); // don't move
            }
            if (desiredHeight < config.heightPresetSwitch) { // if we want to descend to below the elevators
                return config.heightPresetSwitch; // then descend to the minimum height at which we can rotate
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

    public enum HeightPreset {
        PICK,
        SWITCH_LOW,
        SWITCH,
        SCALE_LOW,
        SCALE_HIGH,
        HOME;
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
        public /*TrapezoidalProfileFollower*/StupidController.Config pivotController;
        public double pivotHoldPower;

        public Map<AnglePreset, Double> anglePresets;

        public double heightPresetPick;
        public double heightPresetSwitch;
        public double heightPresetLowSwitch;
        public double heightPresetScaleLow;
        public double heightPresetScaleHigh;
        public double heightPresetHome;

        public double bumpHeightValue;
        public double bumpPivotValue;

        public double heightTolerance;
        public double angleTolerance;

        public double liftLimitTestTimeout;
        public double liftLimitTestPower;
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

    private class LiftLimitTest extends AutomatedTest {
        private boolean lowerLimit;
        private double liftPower;
        private double timeOut;
        private Timer timer = new Timer();

        public LiftLimitTest(String name, boolean lowerLimit, double liftPower, double timeOut) {
            super(name, true);
            this.lowerLimit = lowerLimit;
            this.liftPower = liftPower;
            this.timeOut = timeOut;
        }

        @Override
        protected void initialize() {
            timer.start();
            sendMessage((lowerLimit ? "Lowering " : "Raising ") + "the lift", AutomatedTestMessage.Level.INFO);
        }

        @Override
        protected boolean step() {
            setLiftPower(liftPower);
            return timer.hasPeriodElapsed(timeOut);
        }

        @Override
        protected void finish(boolean interrupted) {
            super.finish(interrupted);
            if ((lowerLimit ? isAtBottomLimit() : isAtTopLimit())) {
                sendMessage("Lift has successfully reached limit", AutomatedTestMessage.Level.INFO);
            } else {
                sendMessage("Lift has failed to reached limit", AutomatedTestMessage.Level.ERROR);
            }
            setDesiredHeightPreset(HeightPreset.HOME);
        }

    }

}
