package org.teamtators.levitator.subsystems;

import edu.wpi.first.wpilibj.*;
import org.teamtators.common.config.Configurable;
import org.teamtators.common.config.helpers.EncoderConfig;
import org.teamtators.common.config.helpers.SpeedControllerConfig;
import org.teamtators.common.control.*;
import org.teamtators.common.hw.ADXRS453;
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
public class Drive extends Subsystem implements Configurable<Drive.Config> {

    public static final Predicate<TrapezoidalProfileFollower> DEFAULT_PREDICATE = ControllerPredicates.finished();
    private SpeedController leftMotor;
    private SpeedController rightMotor;
    private Encoder rightEncoder;
    private Encoder leftEncoder;
    private ADXRS453 gyro;

    private DriveArcController arcController = new DriveArcController(this);


    private Config config;

    public Drive() {
        super("Drive");
    }

    /**
     * Drives with certain powers
     *
     * @param leftPower  the power for the left side
     * @param rightPower the power for the right side
     */
    public void drivePowers(double leftPower, double rightPower) {
        arcController.stop();
        setRightMotorPower(rightPower);
        setLeftMotorPower(leftPower);
    }

    public void stop() {
        drivePowers(0, 0);
    }

    public void driveStraightProfile(double heading, double distance) {
        double deltaHeading = heading - getYawAngle();
        arcController.setInitialYawAngle(heading);
        arcController.setDeltaHeading(deltaHeading);
        arcController.setDeltaCenterDistance(distance);
        arcController.start();
    }

    public void driveRotationProfile(double heading) {
        double deltaHeading = heading - getYawAngle();
        arcController.setInitialYawAngle(getYawAngle());
        arcController.setDeltaHeading(deltaHeading);
        arcController.setDeltaCenterDistance(0.0);
        arcController.start();
    }

    public void driveArcProfile(double arcLength, double endAngle) {
        double deltaHeading = endAngle - getYawAngle();
        arcController.setInitialYawAngle(getYawAngle());
        arcController.setDeltaHeading(deltaHeading);
        arcController.setDeltaCenterDistance(arcLength);
        arcController.start();
    }

    public boolean isArcOnTarget() {
        return arcController.isOnTarget();
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

    public double getAverageRate() {
        return (getRightRate() + getLeftRate()) / 2.0;
    }

    public double getLeftDistance() {
        return leftEncoder.getDistance();
    }

    public double getRightDistance() {
        return rightEncoder.getDistance();
    }

    public double getAverageDistance() {
        return (getLeftDistance() + getRightDistance()) / 2.0;
    }

    public void resetDistance() {
        leftEncoder.reset();
        rightEncoder.reset();
    }

    public double getYawAngle() {
        return gyro.getAngle();
    }

    public double getYawRate() {
        return gyro.getRate();
    }

    public void resetYawAngle() {
        gyro.resetAngle();
    }

    public DriveArcController getArcController() {
        return arcController;
    }

    public List<Updatable> getUpdatables() {
        return Arrays.asList(
                gyro, arcController
        );
    }

    @Override
    public ManualTestGroup createManualTests() {
        ManualTestGroup tests = super.createManualTests();

        tests.addTests(new SpeedControllerTest("LeftMotor", leftMotor));
        tests.addTests(new EncoderTest("LeftEncoder", leftEncoder));
        tests.addTests(new SpeedControllerTest("RightMotor", rightMotor));
        tests.addTests(new EncoderTest("RightEncoder", rightEncoder));

        tests.addTests(new ADXRS453Test("gyro", gyro));
        tests.addTests(new ControllerTest(arcController.getYawAngleController(), 180));
        tests.addTests(new MotionCalibrationTest(arcController.getLeftMotionFollower()));
        tests.addTests(new MotionCalibrationTest(arcController.getRightMotionFollower()));

        return tests;
    }

    @Override
    public void onEnterRobotState(RobotState state) {
        if (state == RobotState.TELEOP || state == RobotState.AUTONOMOUS || state == RobotState.TEST) {
            gyro.finishCalibration();
            if (state == RobotState.AUTONOMOUS) {
                gyro.resetAngle();
            }
        } else {
            gyro.startCalibration();
        }
    }

    @Override
    public void configure(Config config) {
        super.configure();
        this.config = config;
        this.leftMotor = config.leftMotor.create();
        this.rightMotor = config.rightMotor.create();
        this.leftEncoder = config.leftEncoder.create();
        this.rightEncoder = config.rightEncoder.create();

        gyro = new ADXRS453(SPI.Port.kOnboardCS0);
        gyro.start();
        gyro.startCalibration();

        ((Sendable) leftMotor).setName("Drive", "leftMotor");
        ((Sendable) rightMotor).setName("Drive", "rightMotor");
        leftEncoder.setName("Drive", "leftEncoder");
        rightEncoder.setName("Drive", "rightEncoder");
        gyro.setName("Drive", "gyro");

        arcController.configure(config.arcController);
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

    public static class Config {
        public SpeedControllerConfig leftMotor;
        public SpeedControllerConfig rightMotor;
        public EncoderConfig leftEncoder;
        public EncoderConfig rightEncoder;
        public DriveArcController.Config arcController;
    }

}
