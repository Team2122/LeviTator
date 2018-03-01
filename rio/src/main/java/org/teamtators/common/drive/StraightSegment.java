package org.teamtators.common.drive;

import org.teamtators.common.math.Epsilon;
import org.teamtators.common.math.Pose2d;
import org.teamtators.common.math.Translation2d;

public class StraightSegment extends DriveSegmentBase {
    private Pose2d startPose;
    private double length;

    public double getLength() {
        return length;
    }

    public void setLength(double length) {
        this.length = length;
    }

    @Override
    public double getArcLength() {
        return length;
    }

    public Pose2d getStartPose() {
        return startPose;
    }

    public void setStartPose(Pose2d startPose) {
        this.startPose = startPose;
    }

    public Pose2d getEndPose() {
        return startPose.extend(length);
    }

    @Override
    protected Pose2d getNearestPoint(Translation2d point) {
        Translation2d nearestPoint = startPose.getNearestPoint(point);
        Translation2d diff = nearestPoint.sub(startPose.getTranslation());
        if (!Epsilon.isEpsilonZero(diff.getMagnitude())) {
            if (diff.getDirection().epsilonEquals(startPose.getYaw())) {
                if (diff.getMagnitude() > length) {
//                    return getEndPose();
                }
            } else {
//                return getStartPose();
            }
        }
        return new Pose2d(nearestPoint, startPose.getYaw());
    }

    @Override
    public Pose2d getLookAhead(Pose2d nearestPoint, double distance) {
        return new Pose2d(
                nearestPoint.getTranslation()
                        .add(startPose.getYaw().toTranslation(distance)),
                startPose.getYaw());
    }

    @Override
    protected double getTraveledDistance(Translation2d point) {
        return getStartPose().getDistanceAhead(point);
    }

    @Override
    protected double getRemainingDistance(Translation2d point) {
        return -getEndPose().getDistanceAhead(point);
    }

    @Override
    public String toString() {
        return "StraightSegment{" +
                "startPose=" + startPose +
                ", endPose=" + getEndPose() +
                ", length=" + length +
                ", startSpeed=" + getStartSpeed() +
                ", travelSpeed=" + getTravelSpeed() +
                ", endSpeed=" + getEndSpeed() +
                '}';
    }
}
