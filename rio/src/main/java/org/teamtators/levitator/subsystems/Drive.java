package org.teamtators.levitator.subsystems;

import edu.wpi.first.wpilibj.*;
import org.teamtators.common.config.Configurable;
import org.teamtators.common.config.helpers.EncoderConfig;
import org.teamtators.common.config.helpers.SpeedControllerConfig;
import org.teamtators.common.control.*;
import org.teamtators.common.drive.*;
import org.teamtators.common.hw.ADXRS453;
import org.teamtators.common.math.Pose2d;
import org.teamtators.common.scheduler.RobotState;
import org.teamtators.common.scheduler.Subsystem;
import org.teamtators.common.tester.ManualTestGroup;
import org.teamtators.common.tester.components.*;

import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;

/**
 * Has 2 sets of wheels (on the left and right) which can be independently controlled with motors. Also has 2
 * encoders and a gyroscope to measure the movements of the robot
 */
public class Drive extends Subsystem implements Configurable<Drive.Config>, TankDrive {

    public static final Predicate<TrapezoidalProfileFollower> DEFAULT_PREDICATE = ControllerPredicates.finished();
    private SpeedController leftMotor;
    private SpeedController rightMotor;
    private Encoder rightEncoder;
    private Encoder leftEncoder;
    private ADXRS453 gyro;
    private PidController rotationController = new PidController("RotationController");

    private TrapezoidalProfileFollower straightMotionFollower = new TrapezoidalProfileFollower("DriveMotionFollower");
    private PidController yawAngleController = new PidController("DriveYawAngleController");
    private OutputController outputController = new OutputController();

    private TrapezoidalProfileFollower rotationMotionFollower =
            new TrapezoidalProfileFollower("DriveRotationMotionFollower");

    private TankKinematics tankKinematics;
    private PoseEstimator poseEstimator = new PoseEstimator(this);
    private DriveSegmentsFollower driveSegmentsFollower = new DriveSegmentsFollower(this);

    private Config config;
    private double speed;

    public Drive() {
        super("Drive");

        rotationController.setInputProvider(this::getYawAngle);
        rotationController.setOutputConsumer((double output) -> {
            setRightMotorPower(speed + output);
            setLeftMotorPower(speed - output);
        });


        straightMotionFollower.setPositionProvider(this::getCenterDistance);
        straightMotionFollower.setVelocityProvider(this::getCenterRate);
        straightMotionFollower.setOutputConsumer(outputController::setStraightOutput);

        yawAngleController.setInputProvider(this::getYawAngle);
        yawAngleController.setOutputConsumer(outputController::setRotationOutput);

        rotationMotionFollower.setPositionProvider(this::getYawAngle);
        rotationMotionFollower.setVelocityProvider(this::getYawRate);
        rotationMotionFollower.setOutputConsumer(outputController::setRotationOutput);


    }

    /**
     * Drives with a certain heading (angle) and speed
     *
     * @param heading in degrees
     * @param speed   in inches/second
     */
    public void driveHeading(double heading, double speed) {
        rotationController.start();
        straightMotionFollower.stop();
        yawAngleController.stop();
        rotationMotionFollower.stop();
        outputController.stop();
        outputController.setMode(OutputMode.None);
        this.speed = speed;

        rotationController.setSetpoint(heading);
    }

    /**
     * Drives with certain powers
     *
     * @param leftPower  the power for the left side
     * @param rightPower the power for the right side
     */
    public void drivePowers(double leftPower, double rightPower) {
        rotationController.stop();
        straightMotionFollower.stop();
        yawAngleController.stop();
        rotationMotionFollower.stop();
        outputController.stop();
        outputController.setMode(OutputMode.None);
        driveSegmentsFollower.stop();

        setRightMotorPower(rightPower);
        setLeftMotorPower(leftPower);
    }

    public void driveStraightProfile(double heading, double distance) {
        rotationController.stop();
        rotationMotionFollower.stop();
        outputController.setMode(OutputMode.StraightAndRotation);
        driveSegmentsFollower.stop();

        yawAngleController.setSetpoint(heading);
        straightMotionFollower.moveDistance(distance);

        straightMotionFollower.start();
        yawAngleController.start();
        outputController.start();
    }

    public void driveRotationProfile(double heading) {
        rotationController.stop();
        straightMotionFollower.stop();
        yawAngleController.stop();
        outputController.setMode(OutputMode.RotationOnly);
        driveSegmentsFollower.stop();

        rotationMotionFollower.moveToPosition(heading);
        rotationMotionFollower.start();
        outputController.start();
    }

    public void driveArcProfile(double arcLength, double endAngle) {
        rotationController.stop();
        yawAngleController.stop();
        outputController.setMode(OutputMode.StraightAndRotation);
        driveSegmentsFollower.stop();

        straightMotionFollower.moveDistance(arcLength);
        rotationMotionFollower.moveToPosition(endAngle);

        straightMotionFollower.start();
        rotationMotionFollower.start();
        outputController.start();
    }

    public void driveSegments(DriveSegments segments) {
        rotationController.stop();
        straightMotionFollower.stop();
        yawAngleController.stop();
        rotationMotionFollower.stop();
        outputController.stop();
        outputController.setMode(OutputMode.None);

        driveSegmentsFollower.setSegments(segments);
        driveSegmentsFollower.start();
    }

    public boolean isStraightProfileOnTarget() {
        return straightMotionFollower.isOnTarget();
    }

    public boolean isRotationProfileOnTarget() {
        return rotationMotionFollower.isOnTarget();
    }

    public boolean isArcOnTarget() {
        return straightMotionFollower.isOnTarget() && rotationMotionFollower.isOnTarget();
    }

    public boolean isDriveSegmentsFollowerFinished() {
        return !driveSegmentsFollower.isRunning();
    }

    public TrapezoidalProfileFollower getStraightMotionFollower() {
        return straightMotionFollower;
    }

    public PidController getYawAngleController() {
        return yawAngleController;
    }

    public OutputController getOutputController() {
        return outputController;
    }

    public TrapezoidalProfileFollower getRotationMotionFollower() {
        return rotationMotionFollower;
    }

    public DriveSegmentsFollower getDriveSegmentsFollower() {
        return driveSegmentsFollower;
    }

    @Override
    public void setPowers(double left, double right) {
        setLeftMotorPower(left);
        setRightMotorPower(right);
    }

    public void setRightMotorPower(double power) {
        rightMotor.set(power);
    }

    public void setLeftMotorPower(double power) {
        leftMotor.set(power);
    }

    public double getLeftRate() {
        return leftEncoder.getRate();
    }

    public double getRightRate() {
        return rightEncoder.getRate();
    }

    public double getLeftDistance() {
        return leftEncoder.getDistance();
    }

    public double getRightDistance() {
        return rightEncoder.getDistance();
    }

    public void resetDistances() {
        leftEncoder.reset();
        rightEncoder.reset();
    }

    public double getYawAngle() {
        return gyro.getAngle();
    }

    public double getYawRate() {
        return gyro.getRate();
    }

    public void setYawAngle(double yawAngle) {
        gyro.setAngle(yawAngle);
    }

    public void resetYawAngle() {
        gyro.resetAngle();
    }

    @Override
    public TankKinematics getTankKinematics() {
        return tankKinematics;
    }

    public PoseEstimator getPoseEstimator() {
        return poseEstimator;
    }

    public Pose2d getPose() {
        return poseEstimator.getPose();
    }

    public List<Updatable> getUpdatables() {
        return Arrays.asList(
                gyro, poseEstimator, rotationController, yawAngleController,
                straightMotionFollower, yawAngleController, outputController, rotationMotionFollower,
                driveSegmentsFollower
        );
    }

    public void stop() {
        drivePowers(0, 0);
    }

    @Override
    public ManualTestGroup createManualTests() {
        ManualTestGroup tests = super.createManualTests();

        tests.addTests(new SpeedControllerTest("LeftMotor", leftMotor));
        tests.addTests(new EncoderTest("LeftEncoder", leftEncoder));
        tests.addTests(new SpeedControllerTest("RightMotor", rightMotor));
        tests.addTests(new EncoderTest("RightEncoder", rightEncoder));

        tests.addTests(new ADXRS453Test("gyro", gyro));
        tests.addTests(new ControllerTest(rotationController, 180));
        tests.addTests(new PoseEstimatorTest(poseEstimator));

        return tests;
    }

    @Override
    public void onEnterRobotState(RobotState state) {
        if (state == RobotState.TELEOP || state == RobotState.AUTONOMOUS || state == RobotState.TEST) {
            gyro.finishCalibration();
        }
        if (state == RobotState.AUTONOMOUS || state == RobotState.TELEOP) {
            gyro.resetAngle();
            poseEstimator.setPose(Pose2d.zero());
        }
    }

    @Override
    public void configure(Config config) {
        super.configure();
        this.config = config;
        this.tankKinematics = config.tankKinematics;
        poseEstimator.setKinematics(tankKinematics);
        this.leftMotor = config.leftMotor.create();
        this.rightMotor = config.rightMotor.create();
        this.leftEncoder = config.leftEncoder.create();
        this.rightEncoder = config.rightEncoder.create();
        this.straightMotionFollower.configure(config.straightMotionFollower);
        this.yawAngleController.configure(config.yawAngleController);
        this.rotationMotionFollower.configure(config.rotationMotionFollower);
        this.driveSegmentsFollower.configure(config.driveSegmentsFollower);

        gyro = new ADXRS453(SPI.Port.kOnboardCS0);
        this.rotationController.configure(config.rotationController);
        gyro.start();
        gyro.startCalibration();

        ((Sendable) leftMotor).setName("Drive", "leftMotor");
        ((Sendable) rightMotor).setName("Drive", "rightMotor");
        leftEncoder.setName("Drive", "leftEncoder");
        rightEncoder.setName("Drive", "rightEncoder");
        gyro.setName("Drive", "gyro");

        poseEstimator.start();
    }

    @Override
    public void deconfigure() {
        super.deconfigure();
        this.config = null;
        SpeedControllerConfig.free(leftMotor);
        SpeedControllerConfig.free(rightMotor);
        if (leftEncoder != null) leftEncoder.free();
        if (rightEncoder != null) rightEncoder.free();
        if (gyro != null) gyro.free();
    }

    private enum OutputMode {
        StraightOnly, RotationOnly, StraightAndRotation, None
    }

    public static class Config {
        public SpeedControllerConfig leftMotor;
        public SpeedControllerConfig rightMotor;
        public EncoderConfig leftEncoder;
        public EncoderConfig rightEncoder;
        public PidController.Config rotationController;
        public TrapezoidalProfileFollower.Config straightMotionFollower;
        public PidController.Config yawAngleController;
        public TrapezoidalProfileFollower.Config rotationMotionFollower;
        public TankKinematics tankKinematics;
        public DriveSegmentsFollower.Config driveSegmentsFollower;
    }

    private class OutputController extends AbstractUpdatable {
        private double straightOutput;
        private double rotationOutput;
        private OutputMode mode = OutputMode.StraightOnly;

        public OutputController() {
            super("Drive.OutputController");
        }

        public OutputMode getMode() {
            return mode;
        }

        public void setMode(OutputMode mode) {
            this.mode = mode;
        }

        public void setStraightOutput(double followerOutput) {
            this.straightOutput = followerOutput;
        }

        public void setRotationOutput(double rotationOutput) {
            this.rotationOutput = rotationOutput;
        }

        @Override
        protected void doUpdate(double delta) {
            double left = 0, right = 0;
            if (mode == OutputMode.StraightOnly || mode == OutputMode.StraightAndRotation) {
                left += straightOutput;
                right += straightOutput;
                straightOutput = Double.NaN;
            }
            if (mode == OutputMode.RotationOnly || mode == OutputMode.StraightAndRotation) {
                left += rotationOutput;
                right -= rotationOutput;
                rotationOutput = Double.NaN;
            }
            if (mode != OutputMode.None && !Double.isNaN(left) && !Double.isNaN(right)) {
                setLeftMotorPower(left);
                setRightMotorPower(right);
                logger.trace("driving at powers {}, {}", left, right);
            } else {
                setLeftMotorPower(0.0);
                setRightMotorPower(0.0);
                logger.trace("not driving, something was NaN: {}, {}", left, right);
            }
        }
    }
}
