package org.teamtators.levitator.subsystems;

import edu.wpi.first.wpilibj.Encoder;
import edu.wpi.first.wpilibj.SpeedController;
import org.teamtators.common.config.Configurable;
import org.teamtators.common.control.PidController;
import org.teamtators.common.hw.ADXRS453;
import org.teamtators.common.scheduler.Subsystem;

public class Drive extends Subsystem /*implements Configurable<Drive.Config>*/{

    /**
     * Has 2 sets of wheels (on the left and right) which can be independently controlled with motors. Also has 2
     * encoders and a gyroscope to measure the movements of the robot
     */
    private SpeedController leftMotor;
    private SpeedController rightMotor;
    private Encoder rightEncoder;
    private Encoder leftEncoder;
    private ADXRS453 gyro;
    private PidController rotationController = new PidController("RotationController");
    private PidController leftController = new PidController("LeftController");
    private PidController rightController = new PidController("RightController");

    private double speed;

    public Drive() {
        super("Drive");

        leftController.setInputProvider(this::getLeftRate);
        leftController.setOutputConsumer(this::setLeftMotorPower);

        rightController.setInputProvider(this::getRightRate);
        rightController.setOutputConsumer(this::setRightMotorPower);

        rotationController.setInputProvider(this::getYawAngle);
        rotationController.setOutputConsumer((double output) -> {
            setRightMotorPower(speed + output);
            setLeftMotorPower(speed -output);
        });
    }

    /**
     * Drives with a certain heading (angle) and speed
     * @param heading in degrees
     * @param speed in inches/second
     */
    public void driveHeading(double heading, double speed) {
        rightController.stop();
        leftController.stop();
        rotationController.start();
        this.speed = speed;

        rotationController.setSetpoint(heading);
    }

    /**
     * Drives with certain powers
     * @param leftPower the power for the left side
     * @param rightPower the power for the right side
     */
    public void drivePowers(double leftPower, double rightPower) {
        rightController.stop();
        leftController.stop();
        rotationController.stop();

        setRightMotorPower(rightPower);
        setLeftMotorPower(leftPower);
    }

    /**
     * Drives with certain speeds
     * @param rightSpeed the speed (inches/sec) for the right side
     * @param leftSpeed the speed (in/sec) for the left side
     */
    public void driveSpeeds(double rightSpeed, double leftSpeed) {
        rotationController.stop();
        leftController.start();
        rightController.start();

        leftController.setSetpoint(leftSpeed);
        rightController.setSetpoint(rightSpeed);
    }

    public void setRightMotorPower(double power) {
        rightMotor.set(power);
    }

    public void setLeftMotorPower(double power) {
        leftMotor.set(power);
    }

    public double getLeftDistance() {
        return leftEncoder.getDistance(); //* wheelCircumference;
    }


    public double getRightDistance() {
        return rightEncoder.getDistance();
    }

    public double getAverageDistance() {
        return (getLeftDistance() + getRightDistance()) / 2.0;
    }

    public double getLeftRate() {
        return leftEncoder.getRate();
    }

    public double getRightRate() {
        return rightEncoder.getRate();
    }

    public double getAverageRate() {
        return (getRightRate() + getLeftRate())/2.0;
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

    /*
    @Override
    public void configure(Config config) {
    }

    public class Config {

    }
    */
}
