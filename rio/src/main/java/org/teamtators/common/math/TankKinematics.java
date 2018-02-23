package org.teamtators.common.math;

/**
 * @author Alex Mikhalev
 */
public class TankKinematics {
    private double wheelBase = 0.0;

    public double getWheelBase() {
        return wheelBase;
    }

    public void setWheelBase(double wheelBase) {
        this.wheelBase = wheelBase;
    }

    public Pose2d relativeForwardKinematics(Rotation deltaHeading, double deltaWheel) {
        double arcLength = deltaWheel;
        double deltaHeadingRads = deltaHeading.toRadians();
        double radius = arcLength / deltaHeadingRads;
        double centerX = radius;
        double centerY = 0.0;
        double dx = centerX - radius * Math.cos(deltaHeadingRads);
        double dy = centerY + radius * Math.sin(deltaHeadingRads);
        return new Pose2d(new Translation2d(dx, dy), deltaHeading);
    }

    public Pose2d relativeForwardKinematics(Rotation deltaHeading, double deltaLeftWheel, double deltaRightWheel) {
        double deltaWheel = (deltaLeftWheel + deltaRightWheel) / 2.0;
        return relativeForwardKinematics(deltaHeading, deltaWheel);
    }

    public Pose2d forwardKinematics(Rotation initialHeading, Rotation endHeading, double deltaWheel) {
        return relativeForwardKinematics(endHeading.sub(initialHeading), deltaWheel)
                .rotateBy(initialHeading);
    }

    public Pose2d integrateForwardKinematics(Pose2d initialPose, Rotation endHeading, double deltaWheel) {
        return initialPose.add(forwardKinematics(initialPose.getYaw(), endHeading, deltaWheel));
    }


}
