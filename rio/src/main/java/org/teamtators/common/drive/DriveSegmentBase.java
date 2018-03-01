package org.teamtators.common.drive;

import org.teamtators.common.control.TrapezoidalProfile;
import org.teamtators.common.math.Pose2d;
import org.teamtators.common.math.Translation2d;

/**
 * @author Alex Mikhalev
 */
public abstract class DriveSegmentBase implements DriveSegment {
    private double startSpeed;
    private double travelSpeed;
    private double endSpeed;
    private boolean isReverse = false;

    @Override
    public double getStartSpeed() {
        return startSpeed;
    }

    public void setStartSpeed(double startSpeed) {
        this.startSpeed = startSpeed;
    }

    @Override
    public double getTravelSpeed() {
        return travelSpeed;
    }

    public void setTravelSpeed(double travelSpeed) {
        this.travelSpeed = travelSpeed;
    }

    @Override
    public double getEndSpeed() {
        return endSpeed;
    }

    public void setEndSpeed(double endSpeed) {
        this.endSpeed = endSpeed;
    }

    @Override
    public boolean isReverse() {
        return isReverse;
    }

    public void setReverse(boolean reverse) {
        isReverse = reverse;
    }

    protected abstract Pose2d getNearestPoint(Translation2d point);

    public abstract Pose2d getLookAhead(Pose2d nearestPoint, double distance);

    protected abstract double getTraveledDistance(Translation2d point);

    protected abstract double getRemainingDistance(Translation2d point);

    public LookaheadReport getLookaheadReport(Pose2d currentPose, double lookaheadDistance) {
        LookaheadReport report = new LookaheadReport();
        report.nearestPoint = getNearestPoint(currentPose.getTranslation());
        report.trackError = report.nearestPoint.getTranslation().sub(currentPose.getTranslation()).getMagnitude();
        report.yawError = currentPose.getYaw().sub(report.nearestPoint.getYaw());
        report.lookaheadPoint = getLookAhead(report.nearestPoint, lookaheadDistance);
        report.traveledDistance = getTraveledDistance(report.nearestPoint.getTranslation());
        report.remainingDistance = getRemainingDistance(report.nearestPoint.getTranslation());
        report.lookaheadTraveledDistance = getTraveledDistance(report.lookaheadPoint.getTranslation());
        report.lookaheadRemainingDistance = getRemainingDistance(report.lookaheadPoint.getTranslation());
        return report;
    }
}
