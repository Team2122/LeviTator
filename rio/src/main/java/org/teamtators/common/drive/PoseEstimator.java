package org.teamtators.common.drive;

import org.teamtators.common.control.AbstractUpdatable;
import org.teamtators.common.drive.TankKinematics;
import org.teamtators.common.math.Pose2d;
import org.teamtators.common.math.Rotation;
import org.teamtators.levitator.subsystems.Drive;

/**
 * @author Alex Mikhalev
 */
public class PoseEstimator extends AbstractUpdatable {
    private final TankDrive drive;
    private Pose2d pose;
    private Pose2d lastPose;
    private double lastCenterDistance;
    private TankKinematics kinematics;

    public PoseEstimator(TankDrive drive) {
        this.drive = drive;
        setPose(Pose2d.zero());
    }

    @Override
    public synchronized void start() {
        super.start();
        pose = pose.withYaw(getYawRotation());
        lastCenterDistance = drive.getCenterDistance();
    }

    private Rotation getYawRotation() {
        return Rotation.fromDegrees(90 - drive.getYawAngle());
    }

    public Pose2d getPose() {
        return pose;
    }

    public void setPose(Pose2d pose) {
        this.lastPose = this.pose = pose;
    }

    public Pose2d getLastPose() {
        return lastPose;
    }

    @Override
    protected void doUpdate(double delta) {
        Rotation endHeading = getYawRotation();
        double centerDistance = drive.getCenterDistance();
        double deltaWheel = centerDistance - lastCenterDistance;
        Pose2d newPose = kinematics.integratePoseChange(pose, endHeading, deltaWheel);

        lastCenterDistance = centerDistance;
        lastPose = pose;
        pose = newPose;
    }

    public void setKinematics(TankKinematics tankKinematics) {
        this.kinematics = tankKinematics;
    }

    public TankKinematics getKinematics() {
        return kinematics;
    }
}
