package org.teamtators.levitator.subsystems;

import edu.wpi.first.wpilibj.Sendable;
import edu.wpi.first.wpilibj.Solenoid;
import edu.wpi.first.wpilibj.SpeedController;
import org.teamtators.common.config.Configurable;
import org.teamtators.common.config.helpers.*;
import org.teamtators.common.control.*;
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
    private Updater pivotUpdater;
    private AnalogPotentiometer pivotEncoder;
    private Solenoid pivotLockSolenoid;
    private DigitalSensor pivotLockSensor;

    private /*TrapezoidalProfileFollower*/ StupidController pivotController;
    private InputDerivative pivotVelocity;

    private double desiredPivotAngle;
    private double targetAngle;
    private double lastAttemptedAngle;
    private boolean rotationForced = false;

    private Config config;

    public Pivot() {
        super("Pivot");

        pivotVelocity = new InputDerivative("pivotAngleDerivative", this::getCurrentPivotAngle);
        pivotController = new /*TrapezoidalProfileFollower*/StupidController("pivotController");
//        pivotController.setPositionProvider(this::getCurrentPivotAngle);
//        pivotController.setVelocityProvider(pivotVelocity);
        pivotController.setInputProvider(this::getCurrentPivotAngle);
        pivotController.setOutputConsumer(this::setPivotPower);
//        pivotController.setOnTargetPredicate(ControllerPredicates.alwaysFalse());
    }

    public Lift getLift() {
        return lift;
    }

    public void setLift(Lift lift) {
        this.lift = lift;
    }

    public double getCurrentPivotAngle() {
        return pivotEncoder.get();
    }

    public double getDesiredPivotAngle() {
        return desiredPivotAngle;
    }

    public void setDesiredPivotAngle(double desiredAngle, boolean force) {
//        if (getSafePivotAngle(desiredAngle) == desiredAngle) {
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
//        } else {
//            logger.warn("Rotation to desired angle {} is not allowed at the current height {}!!", desiredAngle, getCurrentHeight());
//        }
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
        logger.debug(String.format("Setting lift target angle to %.3f (degrees to move: %.3f)",
                angle, distance));
        targetAngle = angle;
        pivotController./*moveToPosition*/setSetpoint(angle);
        pivotController.setHoldPower(Math.signum(angle) * config.pivotHoldPower);
    }

    public void enablePivotController() {
        pivotController.start();
    }

    public void disablePivotController() {
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
            pivotMotorUpdater.set(pivotPower);
        } else {
            pivotMotorUpdater.set(0);
        }
    }


    public Updatable getPivotController() {
        return pivotController;
    }

    public List<Updatable> getUpdatables() {
        return Arrays.asList(pivotVelocity, pivotController);
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
            double maxAngle = centerAngle + config.centerTolerance;
            double minAngle = centerAngle - config.centerTolerance;
            return Math.min(Math.max(desiredAngle, minAngle), maxAngle);
        }
        return desiredAngle;
    }

    public List<Updatable> getMotorUpdatables() {
        return Arrays.asList(pivotMotorUpdater);
    }


    @Override
    public void onEnterRobotState(RobotState state) {
        switch (state) {
            case AUTONOMOUS:
            case TELEOP:
                setDesiredAnglePreset(AnglePreset.CENTER);
                setTargetAngle(getAnglePreset(AnglePreset.CENTER));
                enablePivotController();
                break;
            case DISABLED:
                disablePivotController();
                break;
        }
    }

    @Override
    public ManualTestGroup createManualTests() {
        ManualTestGroup tests = super.createManualTests();
        tests.addTest(new SpeedControllerTest("pivotMotor", pivotMotor));
        tests.addTest(new AnalogPotentiometerTest("pivotEncoder", pivotEncoder));
        tests.addTest(new SolenoidTest("pivotLockSolenoid", pivotLockSolenoid));
        tests.addTest(new DigitalSensorTest("pivotLockSensor", pivotLockSensor));

        tests.addTest(new /*MotionCalibrationTest*/ControllerTest(pivotController, 90.0));

        tests.addTest(new PivotTest());
        return tests;
    }

    @Override
    public void configure(Config config) {
        super.configure();
        this.config = config;

        this.pivotMotor = config.pivotMotor.create();
        this.pivotEncoder = config.pivotEncoder.create();
        this.pivotLockSolenoid = config.pivotLockSolenoid.create();
        this.pivotLockSensor = config.pivotLockSensor.create();

        this.pivotController.configure(config.pivotController);

        ((Sendable) pivotMotor).setName("Lift", "pivotMotor");
        pivotEncoder.setName("Lift", "pivotEncoder");

        pivotMotorUpdater = new MotorPowerUpdater(pivotMotor);

        pivotUpdater = new Updater(pivotMotorUpdater);

        pivotUpdater.start();
    }

    @Override
    public void deconfigure() {
        super.deconfigure();

        SpeedControllerConfig.free(pivotMotor);
        pivotEncoder.free();
        pivotLockSolenoid.free();
        pivotLockSensor.free();

        pivotUpdater.stop();

        pivotUpdater = null; //so the GC catches these bad boys
    }

    public static class Config {
        public SpeedControllerConfig pivotMotor;
        public AnalogPotentiometerConfig pivotEncoder;
        public SolenoidConfig pivotLockSolenoid;
        public DigitalSensorConfig pivotLockSensor;

        public /*TrapezoidalProfileFollower*/ StupidController.Config pivotController;
        public double pivotHoldPower;

        public Map<AnglePreset, Double> anglePresets;

        public double bumpPivotValue;

        public double angleTolerance;
        public double centerTolerance;
    }

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
            disablePivotController();
        }

        @Override
        public void onButtonDown(LogitechF310.Button button) {
            switch (button) {
                case B:
                    double angle = (config.pivotController./*maxPosition*/maxSetpoint - config.pivotController./*minPosition*/minSetpoint)
                            * ((axisValue + 1) / 2) + config.pivotController./*minPosition*/minSetpoint;
                    logger.info("Moving pivot to angle {}", angle);
                    pivotController./*moveToPosition*/setSetpoint(angle);
                    break;
                case Y:
                    enablePivotController();
                    break;
            }
        }

        @Override
        public void onButtonUp(LogitechF310.Button button) {
            switch (button) {
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
            disablePivotController();
        }
    }
}
