package org.teamtators.common.drive;

public interface TankDrive {
    void drivePowers(double left, double right);

    double getLeftDistance();
    double getRightDistance();
    default double getCenterDistance() {
        return (getLeftDistance() + getRightDistance()) / 2.0;
    }
    void resetDistances();

    double getLeftRate();
    double getRightRate();
    default double getCenterRate() {
        return (getLeftRate() + getRightRate()) / 2.0;
    }

    double getYawAngle();
    void setYawAngle(double yawAngle);
    default void resetYawAngle() {
        setYawAngle(0.0);
    }
    double getYawRate();
}
