package org.teamtators.levitator.subsystems;

import edu.wpi.first.wpilibj.Encoder;
import edu.wpi.first.wpilibj.SpeedController;
import org.teamtators.common.control.PidController;
import org.teamtators.common.hw.ADXRS453;
import org.teamtators.common.scheduler.Subsystem;

public class Drive extends Subsystem {

    private SpeedController leftMotor;
    private SpeedController rightMotor;
    private Encoder rightEncoder;
    private Encoder leftEncoder;
    private ADXRS453 gyro;
    private PidController rotationController;
    private PidController leftController;
    private PidController rightController;

    public Drive() {
        super("Drive");
    }

    public void driveHeading(double heading, double speed) {

    }

    public void setPowers(double leftPower, double rightPower) {

    }

    public void driveSpeeds(double rightSpeed, double leftSpeed) {

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
}
