package org.teamtators.levitator.subsystems;

import edu.wpi.first.wpilibj.*;
import org.teamtators.common.config.Configurable;
import org.teamtators.common.config.helpers.*;
import org.teamtators.common.control.*;
import org.teamtators.common.control.Timer;
import org.teamtators.common.controllers.LogitechF310;
import org.teamtators.common.hw.AnalogPotentiometer;
import org.teamtators.common.hw.DigitalSensor;
import org.teamtators.common.math.Epsilon;
import org.teamtators.common.scheduler.RobotState;
import org.teamtators.common.scheduler.Subsystem;
import org.teamtators.common.tester.ManualTest;
import org.teamtators.common.tester.ManualTestGroup;
import org.teamtators.common.tester.components.*;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class Pivot extends Subsystem implements Configurable<Pivot.Config> {
    private Lift lift;

    private SpeedController pivotMotor;
    private MotorPowerUpdater pivotMotorUpdater;
    private AbstractUpdatable pivotUpdatable;
    private DigitalInput pivotEncoderA;
    private DigitalInput pivotEncoderB;
    private Encoder pivotEncoder;
    private Solenoid pivotLockSolenoid;
    private DigitalSensor pivotLockSensor;

    private TrapezoidalProfileFollower pivotController;
    private BooleanSampler locked = new BooleanSampler(this::isPivotLockedRaw);

    private boolean homed = false;
    private double desiredPivotAngle;
    private double targetAngle;
    private double lastAttemptedAngle;
    private boolean rotationForced = false;
    private boolean locking;
    private double sweepTarget;
    private Timer sweepTimer = new Timer();

    private Config config;
    private boolean manualOverride;
    private AnalogPotentiometer pivotAnalog;

    private double encoderOffset = 0.0;

    public Pivot() {
        super("Pivot");

        pivotController = new TrapezoidalProfileFollower("pivotController");
        pivotController.setPositionProvider(this::getCurrentPivotAngle);
        pivotController.setVelocityProvider(() -> pivotController.getTargetVelocity());
        pivotController.setOutputConsumer(this::setPivotPower);
        pivotController.setOnTargetPredicate(ControllerPredicates.alwaysFalse());

        pivotUpdatable = new PivotUpdatable();
    }

    public Lift getLift() {
        return lift;
    }

    public void setLift(Lift lift) {
        this.lift = lift;
    }

    public boolean isHomed() {
        return homed;
    }

    public double getCurrentPivotAngle() {
        return pivotEncoder.getDistance() - encoderOffset;
//        return getAngleAbsolute();
    }

    public double getAngleQuadrature() {
        return pivotEncoder.getDistance();
    }

    public double getAngleAbsolute() {
        return pivotAnalog.get();
    }

    private void resetPivotAngle() {
        logger.debug("Reset pivot angle");
        pivotEncoder.reset();
        targetAngle = Double.NaN;
    }

    public double getCurrentPivotVelocity() {
        double rate = pivotEncoder.getRate();
        if (rate > 300.0) {
            rate = 300;
        } else if (rate < -300) {
            rate = -300;
        }
        return rate;
    }

    public double getDesiredPivotAngle() {
        return desiredPivotAngle;
    }

    public void setDesiredPivotAngle(double desiredAngle, boolean force) {
        if (rotationForced && !force) {
            return;
        }
        if (desiredPivotAngle != desiredAngle) {
            if (force) {
                logger.info("Setting desired pivot angle {}", desiredAngle);
            }
            this.desiredPivotAngle = desiredAngle;
            this.rotationForced = force;
        }
    }

    public void setDesiredAnglePreset(AnglePreset desiredPivotAngle) {
        setDesiredPivotAngle(getAnglePreset(desiredPivotAngle), true);
    }

    public double getAnglePreset(AnglePreset anglePreset) {
        return config.anglePresets.get(anglePreset);
    }

    public double getTargetAngle() {
        return targetAngle;
    }

    public void setTargetAngle(double angle) {
//        sync();
        double safeAngle = getSafePivotAngle(angle);
        if (safeAngle != angle) {
            if (targetAngle == safeAngle && lastAttemptedAngle == angle) {
                return;
            } else {
                logger.warn("Target angle is unsafe with current lift conditions: {}. Moving to {}", angle, safeAngle);
                lastAttemptedAngle = angle;
                angle = safeAngle;
            }
        } else {
            if (targetAngle == angle) {
                return;
            }
        }
        double distance = angle - getCurrentPivotAngle();
        logger.debug(String.format("Setting pivot angle to %.3f (degrees to move: %.3f)",
                angle, distance));
        targetAngle = angle;
        pivotController.moveToPosition(angle);
        logger.trace("Profile: {}", pivotController.getCalculator().getProfile());
        pivotController.setHoldPower(Math.signum(angle) * config.pivotHoldPower);
    }

    public void sync() {
        double distance = getCurrentPivotAngle();
        double analogValue = getAngleAbsolute();
        double delta = (distance - analogValue);
        if (Math.abs(delta) >= 2) {
            encoderOffset += delta;
            logger.warn("!~Resynchronizing incremental encoder and potentiometer. quad: {}, analog: {}, newOffset: {}",
                    distance, analogValue, encoderOffset);
            targetAngle = Double.NaN;
        }
    }

    public void enable() {
        pivotUpdatable.start();
    }

    public void disable() {
        pivotController.stop();
        pivotUpdatable.stop();
    }

    private void enablePivotController() {
        pivotController.start();
    }

    private void disablePivotController() {
        pivotController.stop();
    }

    public void bumpPivotRight() {
        setDesiredPivotAngle(getDesiredPivotAngle() + config.bumpPivotValue, true);
    }

    public void bumpPivotLeft() {
        setDesiredPivotAngle(getDesiredPivotAngle() - config.bumpPivotValue, true);
    }

    public void setPivotPower(double pivotPower) {
        if (!isPivotLocked()) {
            /*pivotMotorUpdater*/
            pivotMotor.set(pivotPower);
        } else {
            /*pivotMotorUpdater*/
            pivotMotor.set(0);
        }
    }


    public Updatable getPivotController() {
        return pivotController;
    }

    public List<Updatable> getUpdatables() {
        return Arrays.asList(pivotUpdatable, pivotController);
    }

    public boolean isPivotLockedRaw() {
        return !pivotLockSensor.get();
    }

    public boolean isPivotLocked() {
        return locked.get();
    }

    public boolean isPivotInCenter() {
        return Epsilon.isEpsilonEqual(getCurrentPivotAngle(),
                getAnglePreset(AnglePreset.CENTER),
                config.centerTolerance);
    }

    public boolean isWithinTab() {
        return Epsilon.isEpsilonEqual(getCurrentPivotAngle(),
                getAnglePreset(AnglePreset.CENTER),
                4);
    }

    public boolean isLockable() {
        double currentAngle = getCurrentPivotAngle();
        return currentAngle > -config.lockAngle && currentAngle < config.lockAngle;
    }

    public void setPivotLockSolenoid(boolean lock) {
        pivotLockSolenoid.set(lock);
    }

    public void clearForceRotationFlag() {
        logger.debug("Clearing force rotation flag");
        rotationForced = false;
    }

    public boolean isRotationForced() {
        return rotationForced;
    }


    public double getSafePivotAngle(double desiredAngle) {
        double centerAngle = getAnglePreset(AnglePreset.CENTER);
        if (lift.isBelowHeight(Lift.HeightPreset.NEED_LOCK)) {
            return centerAngle;
        }
        if (lift.isBelowHeight(Lift.HeightPreset.NEED_CENTER)) {
//            double maxAngle = centerAngle + config.centerTolerance - 5.0;
//            double minAngle = centerAngle - config.centerTolerance + 5.0;
//            return Math.min(Math.max(desiredAngle, minAngle), maxAngle);
            return centerAngle;
        }
        return desiredAngle;
    }

    public List<Updatable> getMotorUpdatables() {
        return Arrays.asList(/*pivotMotorUpdater*/);
    }


    @Override
    public void onEnterRobotState(RobotState state) {
        switch (state) {
            case AUTONOMOUS:
            case TELEOP:
                setDesiredAnglePreset(AnglePreset.CENTER);
                setTargetAngle(getAnglePreset(AnglePreset.CENTER));
                enable();
                break;
            case DISABLED:
                disable();
                targetAngle = Double.NaN;
                break;
        }
    }

    @Override
    public ManualTestGroup createManualTests() {
        ManualTestGroup tests = super.createManualTests();
        tests.addTest(new SpeedControllerTest("pivotMotor", pivotMotor));
        tests.addTest(new EncoderTest("pivotEncoder", pivotEncoder));
        tests.addTest(new AnalogPotentiometerTest("pivotAnalog", pivotAnalog));
        tests.addTest(new SolenoidTest("pivotLockSolenoid", pivotLockSolenoid));
        tests.addTest(new DigitalSensorTest("pivotLockSensor", pivotLockSensor));

        tests.addTest(new MotionCalibrationTest(pivotController));

        tests.addTest(new PivotTest());
        return tests;
    }

    @Override
    public void configure(Config config) {
        super.configure();
        this.config = config;

        this.pivotMotor = config.pivotMotor.create();
        this.pivotAnalog = config.pivotAnalog.create();
//        this.pivotEncoder = config.pivotEncoder.create();
        this.pivotEncoderA = new DigitalInput(config.pivotEncoder.getaChannel());
        this.pivotEncoderB = new DigitalInput(config.pivotEncoder.getbChannel());
        this.pivotEncoder = new Encoder(pivotEncoderA, pivotEncoderB, config.pivotEncoder.isReverse(),
                config.pivotEncoder.getEncodingType());
        this.pivotEncoder.setDistancePerPulse(config.pivotEncoder.getDistancePerPulse());
        this.pivotLockSolenoid = config.pivotLockSolenoid.create();
        this.pivotLockSensor = config.pivotLockSensor.create();

        this.pivotController.configure(config.pivotController);

        locked.setPeriod(config.lockedPeriod);

        ((Sendable) pivotMotor).setName("Pivot", "pivotMotor");
        pivotEncoder.setName("Pivot", "pivotEncoder");
        pivotAnalog.setName("Pivot", "pivotAnalog");
        pivotEncoderA.setName("Pivot", "pivotEncoderA");
        pivotEncoderB.setName("Pivot", "pivotEncoderB");
        pivotLockSolenoid.setName("Pivot", "pivotLockSolenoid");
        pivotLockSensor.setName("Pivot", "pivotLockSensor");

        pivotMotorUpdater = new MotorPowerUpdater(pivotMotor);

        homed = false;
    }

    @Override
    public void deconfigure() {
        super.deconfigure();

        SpeedControllerConfig.free(pivotMotor);
        pivotEncoder.free();
        pivotAnalog.free();
        pivotEncoderA.free();
        pivotEncoderB.free();
        pivotLockSolenoid.free();
        pivotLockSensor.free();
    }

    public boolean isManualOverride() {
        return manualOverride;
    }

    public void setManualOverride(boolean override) {
        this.manualOverride = override;
        if (!override) {
            enable();
            homed = false;
        }
    }

    @SuppressWarnings("WeakerAccess")
    public static class Config {
        public SpeedControllerConfig pivotMotor;
        public EncoderConfig pivotEncoder;
        public AnalogPotentiometerConfig pivotAnalog;
        public SolenoidConfig pivotLockSolenoid;
        public DigitalSensorConfig pivotLockSensor;

        public TrapezoidalProfileFollower.Config pivotController;
        public double pivotHoldPower;

        public Map<AnglePreset, Double> anglePresets;

        public double bumpPivotValue;

        public double angleTolerance;
        public double centerTolerance;

        public double pivotSweepPower;
        public double startSweepAngle;
        public double sweepTimeoutSeconds;
        public double lockedPeriod;
        public double lockAngle;
    }

    @SuppressWarnings("unused")
    public enum AnglePreset {
        LEFT,
        HALF_LEFT,
        CENTER,
        HALF_RIGHT,
        RIGHT;
    }

    private class PivotTest extends ManualTest {
        private double axisValue;

        public PivotTest() {
            super("PivotTest");
        }

        @Override
        public void start() {
            logger.info("Press B to set pivot target to joystick value. Hold Y to enable pivot profiler");
            disable();
        }

        @Override
        public void onButtonDown(LogitechF310.Button button) {
            switch (button) {
                case B:
                    double angle = (config.pivotController.maxPosition - config.pivotController.minPosition)
                            * ((axisValue + 1) / 2) + config.pivotController.minPosition;
                    logger.info("Moving pivot to angle {}", angle);
                    pivotController.moveToPosition(angle);
                    break;
                case Y:
                    enable();
                    break;
            }
        }

        @Override
        public void onButtonUp(LogitechF310.Button button) {
            switch (button) {
                case Y:
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

    private class PivotUpdatable extends AbstractUpdatable {
        PivotUpdatable() {
            super("Pivot.pivotUpdatable");
        }

        @Override
        public synchronized void start() {
            super.start();
            sweepTimer.start();
        }

        @Override
        public void doUpdate(double delta) {
            if (manualOverride) {
                pivotLockSolenoid.set(false);
                disablePivotController();
                return;
            }
            Pivot pivot = Pivot.this;
            if (!pivot.isHomed()) {
                pivot.disablePivotController();
                pivot.setPivotPower(0.0);
                pivot.setPivotLockSolenoid(true);
                if (pivot.isPivotLocked()) {
                    pivot.resetPivotAngle();
                    pivotAnalog.setOffset(0);
                    pivotAnalog.setOffset(-getAngleAbsolute());
                    logger.info("Pivot homed");
                    pivot.setDesiredAnglePreset(AnglePreset.CENTER);
                    pivot.homed = true;
                    locking = true;
                } else {
                    return;
                }
            }
            double centerAngle = pivot.getAnglePreset(AnglePreset.CENTER);
            double currentAngle = pivot.getCurrentPivotAngle();
            double safePivotAngle = pivot.getSafePivotAngle(desiredPivotAngle);
            boolean isWithinLockAngle = isLockable();
            if ((safePivotAngle != centerAngle || !isWithinLockAngle) && locking) {
                locking = false;
                logger.debug("Moving away from center, disengaging lock (absolute: {}, quadrature: {})",
                        getAngleAbsolute(), getAngleQuadrature());
//                if (isWithinLockAngle) {
//                    resetPivotAngle();
//                }
            }
            if (locking) {
                //Extend the solenoid to lock
                pivot.setPivotLockSolenoid(true);
                if (sweepTarget == 0) {
                    pivot.setPivotPower(0.0);
                    pivot.disablePivotController();
//                    resetPivotAngle();
                } else {
                    /*
                    //Check if this is the first step of locking or we overshot our target
                    if (sweepTarget > 0 ? (currentAngle >= sweepTarget) : (currentAngle <= sweepTarget)) {
                        logger.warn("Pivot sweep missed lock, sweeping other direction");
                        sweepTarget = -sweepTarget;
                    }
                    //Apply the configured power with a sign that is the same as our target (i.e. positive power moves right, increasing angle)
                    pivot.setPivotPower(config.pivotSweepPower * Math.signum(sweepTarget));
                    //If our solenoid is locked in or the timer ran out
                    */
                    pivot.setTargetAngle(centerAngle);
                    pivot.enablePivotController();
                }
                if (sweepTarget != 0) {
                    if (isPivotLocked()) {
                        //Reset sweep target
                        logger.debug("Pivot locked (absolute: {}, quadrature: {})",
                                        getAngleAbsolute(), getAngleQuadrature());

                        pivot.disablePivotController();
                        pivot.setPivotPower(0.0);
                        if(Epsilon.isEpsilonZero(getAngleAbsolute(), config.angleTolerance)) {
                            encoderOffset = 0;
                            resetPivotAngle();
                        } else {
//                            sync();
                        }
                        sweepTarget = 0;
                    } /* else if (sweepTimer.periodically(config.sweepTimeoutSeconds)) {
                        logger.warn("Pivot could not lock, timeout elapsed");
                    } */
                } else {
                    if (!pivot.isPivotLocked()) {
                        sweepTarget = -Math.signum(currentAngle) * config.startSweepAngle;
                    }
                }
            } else {
                //Retract the solenoid
                pivot.setPivotLockSolenoid(false);
                //If we want to go to center and we're within the range of locking
                if (safePivotAngle == centerAngle) {
                    boolean isWithinSweepAngle = currentAngle > -config.startSweepAngle && currentAngle < config.startSweepAngle;
                    if (isWithinSweepAngle) {
                        logger.debug("Pivot moving center, will engage lock (absolute: {}, quadrature: {})",
                                        getAngleAbsolute(), getAngleQuadrature());
                        //Start locking
                        locking = true;
//                        sync();
                        sweepTarget = -Math.signum(currentAngle) * config.startSweepAngle;
                        //Disable PID
//                        pivot.disablePivotController();
                        //Start the timer
                        sweepTimer.restart();
                    } else {
                        pivot.enablePivotController();
                        pivot.setTargetAngle(0);
                    }
                }
                //If we want to go somewhere other than the center
                else {
                    //Enable the PID Controller
                    pivot.enablePivotController();
                    //Set the pivot target angle to the desired angle
                    pivot.setTargetAngle(desiredPivotAngle);
                }
            }
//            if (pivot.pivotController.isFinished()) {
//                sync();
//            }
        }
    }
}
