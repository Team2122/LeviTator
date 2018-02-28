package org.teamtators.common.drive;

import org.teamtators.common.math.Pose2d;
import org.teamtators.common.math.Twist2d;

public interface TankDrive {
    void setPowers(double left, double right);
    default void setPowers(DriveOutputs outputs) {
        setPowers(outputs.getLeft(), outputs.getRight());
    }

    void stop();

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

    Pose2d getPose();

    TankKinematics getTankKinematics();
}
