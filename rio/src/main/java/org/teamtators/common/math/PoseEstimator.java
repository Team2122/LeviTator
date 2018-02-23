package org.teamtators.common.math;

import org.teamtators.common.control.AbstractUpdatable;
import org.teamtators.common.control.Updatable;
import org.teamtators.levitator.subsystems.Drive;

/**
 * @author Alex Mikhalev
 */
public class PoseEstimator extends AbstractUpdatable {
    private final Drive drive;
    Pose2d pose = Pose2d.zero();
    double lastCenterDistance;
    TankKinematics kinematics = new TankKinematics();

    public PoseEstimator(Drive drive) {
        this.drive = drive;
    }

    @Override
    public synchronized void start() {
        super.start();
        pose = pose.withYaw(getYawRotation());
        lastCenterDistance = drive.getAverageDistance();
    }

    private Rotation getYawRotation() {
        return Rotation.fromDegrees(drive.getYawAngle());
    }

    @Override
    protected void doUpdate(double delta) {
        Rotation endHeading = getYawRotation();
        double centerDistance = drive.getAverageDistance();
        double deltaWheel = centerDistance - lastCenterDistance;
        Pose2d newPose = kinematics.integrateForwardKinematics(pose, endHeading, deltaWheel);

        lastCenterDistance = centerDistance;
        pose = newPose;
    }
}
