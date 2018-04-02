package org.teamtators.levitator.subsystems;

import edu.wpi.first.wpilibj.Encoder;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import org.teamtators.common.config.Configurable;
import org.teamtators.common.config.helpers.DigitalSensorConfig;
import org.teamtators.common.config.helpers.EncoderConfig;
import org.teamtators.common.config.helpers.SpeedControllerConfig;
import org.teamtators.common.config.helpers.SpeedControllerGroupConfig;
import org.teamtators.common.control.*;
import org.teamtators.common.controllers.LogitechF310;
import org.teamtators.common.hw.DigitalSensor;
import org.teamtators.common.hw.SpeedControllerGroup;
import org.teamtators.common.math.Epsilon;
import org.teamtators.common.scheduler.RobotState;
import org.teamtators.common.scheduler.Subsystem;
import org.teamtators.common.tester.ManualTest;
import org.teamtators.common.tester.ManualTestGroup;
import org.teamtators.common.tester.components.DigitalSensorTest;
import org.teamtators.common.tester.components.EncoderTest;
import org.teamtators.common.tester.components.MotionCalibrationTest;
import org.teamtators.common.tester.components.SpeedControllerTest;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@SuppressWarnings({"WeakerAccess", "unused"})
public class Lift extends Subsystem implements Configurable<Lift.Config> {
    private static final boolean WAIT_FOR_HOME_SENSOR = false;
    private Pivot pivot;

    private SpeedControllerGroup liftMotor;
    private MotorPowerUpdater liftMotorUpdater;
    private Encoder liftEncoder;
    private DigitalSensor limitSensorTop;
    private DigitalSensor limitSensorBottom;

    private double desiredHeight;
    private double targetHeight;
    private double lastAttemptedHeight;

    private TrapezoidalProfileFollower liftController;
    private PreUpdatable preUpdatable;
    private NetworkTablesUpdater networkTablesUpdater;

    private Config config;

    private boolean heightForced = false;
    private boolean homed;
    private Timer homeTimer = new Timer();
    private double savedHeight;

    public Lift() {
        super("Lift");

        liftController = new TrapezoidalProfileFollower("liftController");
        liftController.setPositionProvider(this::getCurrentHeight);
        liftController.setVelocityProvider(this::getLiftVelocity);
        liftController.setOutputConsumer(this::setLiftPower);
        liftController.setOnTargetPredicate(ControllerPredicates.alwaysFalse());

        preUpdatable = new PreUpdatable();
        networkTablesUpdater = new NetworkTablesUpdater();
    }

    public Pivot getPivot() {
        return pivot;
    }

    @SuppressWarnings("WeakerAccess")
    public void setPivot(Pivot pivot) {
        this.pivot = pivot;
    }

    public void linkTo(Pivot pivot) {
        pivot.setLift(this);
        this.setPivot(pivot);
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
    @SuppressWarnings("WeakerAccess")
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
    public void setDesiredHeight(double desiredHeight, boolean force) {
        if (desiredHeight < config.heightController.minPosition) {
            logger.warn("Lift desired height exceeded bottom height limit ({} < {})", desiredHeight,
                    config.heightController.minPosition);
            desiredHeight = config.heightController.minPosition;
        } else if (desiredHeight > config.heightController.maxPosition) {
            logger.warn("Lift desired height exceeded top height limit ({} > {})", desiredHeight,
                    config.heightController.maxPosition);
            desiredHeight = config.heightController.maxPosition;
        }
        if (heightForced && !force) {
            return;
        }
        if (force) {
            logger.info("Setting desired lift height to {}", desiredHeight);
        }
        this.desiredHeight = desiredHeight;
        heightForced = force;
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
        double safeHeight = getSafeLiftHeight(height);
        if (safeHeight != height) {
            if (targetHeight == safeHeight && lastAttemptedHeight == height) {
                return;
            } else {
                logger.warn("Target height is unsafe with current picker conditions (angle: {}): {}. Moving to {}",
                        pivot.getCurrentPivotAngle(), height, safeHeight);
                lastAttemptedHeight = height;
                height = safeHeight;
            }
        } else {
            if (targetHeight == height) {
                return;
            }
        }
        double distance = height - getCurrentHeight();
        logger.debug(String.format("Setting lift target height to %.3f (distance to move: %.3f)",
                height, distance));
        targetHeight = height;
        liftController.moveToPosition(height);
    }

    public void enable() {
        preUpdatable.start();
    }

    public void disable() {
        liftController.stop();
        preUpdatable.stop();
    }

    private void enableLiftController() {
        if (!liftController.isRunning()) {
            targetHeight = getCurrentHeight();
            liftController.moveToPosition(targetHeight);
            liftController.start();
        }
    }

    private void disableLiftController() {
        liftController.stop();
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


    public boolean isAtDesiredHeight() {
        return liftController.isOnTarget();
    }

    public TrapezoidalProfileFollower getLiftController() {
        return liftController;
    }

    public List<Updatable> getUpdatables() {
        return Arrays.asList(preUpdatable, liftController, networkTablesUpdater);
    }

    public void clearForceHeightFlag() {
        logger.debug("Clearing force height flag");
        this.heightForced = false;
    }

    public boolean isHeightForced() {
        return heightForced;
    }

    public boolean isHomed() {
        return homed;
    }

    public void saveCurrentHeight() {
        logger.debug("Saving height {}", getCurrentHeight());
        this.savedHeight = getCurrentHeight();
    }

    public void recallHeight() {
        setDesiredHeight(savedHeight, true);
    }

    public boolean isAtHeight(double height) {
        return Math.abs(getCurrentHeight() - height) < config.heightTolerance;
    }

    public boolean isAtHeight(HeightPreset preset) {
        return isAtHeight(getHeightPreset(preset));
    }

    public boolean isAtHeight() {
        return isAtHeight(getDesiredHeight());
    }

    public boolean isBelowHeight(double height) {
        return getCurrentHeight() < height;
    }

    public boolean isBelowHeight(HeightPreset preset) {
        return isBelowHeight(getHeightPreset(preset));
    }

    public boolean isAtOrBelowHeight(double height) {
        return isAtHeight(height) || isBelowHeight(height);
    }

    public boolean isAtOrBelowHeight(HeightPreset preset) {
        return isAtOrBelowHeight(getHeightPreset(preset));
    }

    public double getSafeLiftHeight(double desiredHeight) {
        double currentLiftHeight = getCurrentHeight();
        double needLockHeight = getHeightPreset(HeightPreset.NEED_LOCK);
        double needCenterHeight = getHeightPreset(HeightPreset.NEED_CENTER);
        if (currentLiftHeight < 0) {
            currentLiftHeight = 0;
        }
        if (desiredHeight < 0) {
            desiredHeight = 0;
        }
        if (desiredHeight > config.heightController.maxPosition) {
            desiredHeight = config.heightController.maxPosition;
        }
        if (!pivot.isPivotLocked()) { // if the pivot is not locked
            if (isAtHeight(HeightPreset.HOME) && // if we are already below NEED_LOCK
                    pivot.isWithinTab()) { // and we are able to lock
                return currentLiftHeight; // then don't move
            }
            if (desiredHeight < needLockHeight) { // if we want to descend to below NEED_LOCK
                return needLockHeight; // then descend to the minimum height at which we can be unlocked
            }
        }
        if (!pivot.isPivotInCenter()) { // if the picker is out far enough that we can't go below NEED_CENTER
            if (Epsilon.isEpsilonLessThan(currentLiftHeight, needCenterHeight,
                    config.heightTolerance)) { // if we are not above the elevators
                return currentLiftHeight; // don't move
            }
            if (desiredHeight < needCenterHeight) { // if we want to descend to below the elevators
                return needCenterHeight; // then descend to the minimum height at which we can rotate
            }
        }
        return desiredHeight; // if picker is all good, go wherever we need to
    }

    public List<Updatable> getMotorUpdatables() {
        return Collections.singletonList(liftMotorUpdater);
    }

    @Override
    public void onEnterRobotState(RobotState state) {
        switch (state) {
            case DISABLED:
                disable();
                homeTimer.stop();
                break;
        }
        heightForced = true;
    }

    @Override
    public ManualTestGroup createManualTests() {
        ManualTestGroup tests = super.createManualTests();
        tests.addTest(new SpeedControllerTest("liftMotor", liftMotor));
        tests.addTest(new EncoderTest("liftEncoder", liftEncoder));
        tests.addTest(new DigitalSensorTest("limitSensorTop", limitSensorTop));
        tests.addTest(new DigitalSensorTest("limitSensorBottom", limitSensorBottom));

        tests.addTest(new MotionCalibrationTest(liftController));

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

        this.liftController.configure(config.heightController);

        liftMotor.setName("Lift", "liftMotor");
        liftEncoder.setName("Lift", "liftEncoder");
        limitSensorTop.setName("Lift", "limitSensorTop");
        limitSensorBottom.setName("Lift", "limitSensorBottom");

        liftMotorUpdater = new MotorPowerUpdater(liftMotor);

        homed = false;
        homeTimer.stop();
    }

    @SuppressWarnings("Duplicates")
    @Override
    public void deconfigure() {
        super.deconfigure();

        SpeedControllerConfig.free(liftMotor);
        liftEncoder.free();
        limitSensorTop.free();
        limitSensorBottom.free();
    }

    private void updateHeight() {
        if (!homed) {
            if (!homeTimer.isRunning()) {
                homeTimer.start();
            }
            boolean homeTimeout = false;
            if (homeTimer.hasPeriodElapsed(config.homingTimeout)) {
                homeTimeout = true;
                logger.warn("Homing timed out");
            }
            disableLiftController();
            if (!pivot.isPivotLocked() && !homeTimeout) {
                setLiftPower(0.0);
                return;
            }
            setLiftPower(config.homingPower);
            if (!isAtBottomLimit() && WAIT_FOR_HOME_SENSOR && !homeTimeout) {
                return;
            }
            logger.info("Lift homed");
            liftEncoder.reset();
            enableLiftController();
            homed = true;
        } else {
            enableLiftController();
        }
        //Set the lift target height to the desired height
        this.setTargetHeight(desiredHeight);
    }

    @SuppressWarnings("unused")
    public enum HeightPreset {
        HOME,
        PICK,
        NEED_LOCK,
        NEED_CENTER,
        SWITCH_LOW,
        SWITCH,
        SCALE_LOW,
        SCALE_HIGH
    }

    @SuppressWarnings("WeakerAccess")
    public static class Config {
        public SpeedControllerGroupConfig liftMotor;
        public EncoderConfig liftEncoder;
        public DigitalSensorConfig limitSensorTop;
        public DigitalSensorConfig limitSensorBottom;

        public TrapezoidalProfileFollower.Config heightController;

        public Map<HeightPreset, Double> heightPresets;

        public double bumpHeightValue;

        public double heightTolerance;
        public double homePower;
        public double homeTolerance;
        public double homingPower;
        public double homingTimeout;
    }

    private class LiftTest extends ManualTest {
        private double axisValue;

        LiftTest() {
            super("LiftTest");
        }

        @Override
        public void start() {
            logger.info("Press A to set lift target to joystick value. Hold X to enable lift profiler");
            disable();
        }


        @Override
        public void onButtonDown(LogitechF310.Button button) {
            switch (button) {
                case A:
                    double height = (config.heightController.maxPosition - config.heightController.minPosition)
                            * ((axisValue + 1) / 2) + config.heightController.minPosition;
                    setTargetHeight(height);
                    break;
                case X:
                    enable();
                    break;
            }
        }

        @Override
        public void onButtonUp(LogitechF310.Button button) {
            switch (button) {
                case X:
                    disable();
                    break;
            }
        }

        @Override
        public void updateAxis(double value) {
            this.axisValue = value;
        }

        @Override
        public void stop() {
            disable();
        }
    }

    private class PreUpdatable extends AbstractUpdatable {
        @Override
        public String getName() {
            return "Lift.preUpdatable";
        }

        @Override
        protected void doUpdate(double delta) {
            if (Lift.this.getCurrentHeight() < config.homeTolerance) {
                Lift.this.liftController.setHoldPower(config.homePower);
            } else {
                Lift.this.liftController.setHoldPower(Lift.this.config.heightController.kHoldPower);
            }
            Lift.this.updateHeight();
        }
    }

    private class NetworkTablesUpdater implements Updatable {
        @Override
        public String getName() {
            return "Lift.networkTablesUpdater";
        }

        @Override
        public void update(double delta) {
            SmartDashboard.putNumber("liftTarget", Lift.this.getTargetHeight());
            SmartDashboard.putBoolean("move", Lift.this.isHeightForced());
        }
    }
}
