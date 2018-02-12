package org.teamtators.levitator.subsystems;

import edu.wpi.first.wpilibj.Encoder;
import edu.wpi.first.wpilibj.SPI;
import edu.wpi.first.wpilibj.SpeedController;
import org.teamtators.common.config.Configurable;
import org.teamtators.common.config.helpers.EncoderConfig;
import org.teamtators.common.config.helpers.SpeedControllerConfig;
import org.teamtators.common.control.PidController;
import org.teamtators.common.control.Updatable;
import org.teamtators.common.hw.ADXRS453;
import org.teamtators.common.scheduler.Subsystem;
import org.teamtators.common.tester.ManualTestGroup;
import org.teamtators.common.tester.components.ADXRS453Test;
import org.teamtators.common.tester.components.ControllerTest;
import org.teamtators.common.tester.components.EncoderTest;
import org.teamtators.common.tester.components.SpeedControllerTest;

import java.util.Arrays;
import java.util.List;

/**
 * Has 2 sets of wheels (on the left and right) which can be independently controlled with motors. Also has 2
 * encoders and a gyroscope to measure the movements of the robot
 */
public class Drive extends Subsystem implements Configurable<Drive.Config>{

    private SpeedController leftMotor;
    private SpeedController rightMotor;
    private Encoder rightEncoder;
    private Encoder leftEncoder;
    private ADXRS453 gyro;
    private PidController rotationController = new PidController("RotationController");

    private Config config;
    private double speed;

    public Drive() {
        super("Drive");

        rotationController.setInputProvider(this::getYawAngle);
        rotationController.setOutputConsumer((double output) -> {
            setRightMotorPower(speed + output);
            setLeftMotorPower(speed - output);
        });
    }

    /**
     * Drives with a certain heading (angle) and speed
     *
     * @param heading in degrees
     * @param speed   in inches/second
     */
    public void driveHeading(double heading, double speed) {
        rotationController.start();
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

        setRightMotorPower(rightPower);
        setLeftMotorPower(leftPower);
    }

    /**
     * Drives with certain speeds
     *
     * @param rightSpeed the speed (inches/sec) for the right side
     * @param leftSpeed  the speed (in/sec) for the left side
     */
    public void driveSpeeds(double leftSpeed, double rightSpeed) {
        rotationController.stop();
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

    public List<Updatable> getUpdatables() {
        return Arrays.asList(rotationController);
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

        return tests;
    }

    @Override
    public void configure(Config config) {
        this.config = config;
        this.leftMotor = config.leftMotor.create();
        this.rightMotor = config.rightMotor.create();
        this.leftEncoder = config.leftEncoder.create();
        this.rightEncoder = config.rightEncoder.create();
        gyro = new ADXRS453(SPI.Port.kOnboardCS0);
        this.rotationController.configure(config.rotationController);
        gyro.start();
        gyro.startCalibration();
    }

    public static class Config {
        public SpeedControllerConfig leftMotor;
        public SpeedControllerConfig rightMotor;
        public EncoderConfig leftEncoder;
        public EncoderConfig rightEncoder;
        public PidController.Config rotationController;
    }
}
