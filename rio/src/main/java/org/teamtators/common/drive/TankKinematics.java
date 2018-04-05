package org.teamtators.common.drive;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.teamtators.common.math.*;

/**
 * @author Alex Mikhalev
 */
public class TankKinematics {
    private static final Logger logger = LoggerFactory.getLogger(TankKinematics.class);
    private double effectiveTrackWidth = 0.0;

    public double getEffectiveTrackWidth() {
        return effectiveTrackWidth;
    }

    public void setEffectiveTrackWidth(double effectiveTrackWidth) {
        this.effectiveTrackWidth = effectiveTrackWidth;
    }

    public Pose2d calculatePoseChange(Rotation deltaHeading, double deltaWheel) {
        double deltaHeadingRads = deltaHeading.toRadians();
        if (Epsilon.isEpsilonZero(deltaHeadingRads)) {
            return new Pose2d(new Translation2d(deltaWheel, 0.0), deltaHeading);
        }
        double arcLength = deltaWheel;
        double radius = arcLength / deltaHeadingRads;
        double centerX = 0.0;
        double centerY = -radius;
        double dx = centerX + radius * deltaHeading.sin();
        double dy = centerY + radius * deltaHeading.cos();
        return new Pose2d(new Translation2d(dx, dy), deltaHeading);
    }

    public Pose2d calculatePoseChange(Rotation deltaHeading, double deltaLeftWheel, double deltaRightWheel) {
        double deltaWheel = (deltaLeftWheel + deltaRightWheel) / 2.0;
        return calculatePoseChange(deltaHeading, deltaWheel);
    }

    public Pose2d integratePoseChange(Pose2d initialPose, Rotation endHeading, double deltaWheel) {
        Rotation deltaHeading = endHeading.sub(initialPose.getYaw());
        Pose2d poseChange = calculatePoseChange(deltaHeading, deltaWheel);
        return initialPose.chain(poseChange);
    }

    public DriveOutputs calculateOutputs(Twist2d curvature, double power) {
        double yawDistance = effectiveTrackWidth * curvature.getDeltaYaw().toRadians() / 2;
        double deltaX = curvature.getDeltaX();
        DriveOutputs outputs = new DriveOutputs(deltaX - yawDistance, deltaX + yawDistance);
        outputs = outputs.scale(power / Math.abs(deltaX));
        return outputs;
    }
}
