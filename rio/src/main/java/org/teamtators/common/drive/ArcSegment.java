package org.teamtators.common.drive;

import org.teamtators.common.math.Pose2d;
import org.teamtators.common.math.Rotation;
import org.teamtators.common.math.Translation2d;

import static org.teamtators.common.math.Epsilon.*;

public class ArcSegment extends DriveSegmentBase {
    private Translation2d center;
    private Rotation startAngle;
    private Rotation endAngle;
    private double radius;

    public Translation2d getCenter() {
        return center;
    }

    public void setCenter(Translation2d center) {
        this.center = center;
    }

    public Rotation getStartAngle() {
        return startAngle;
    }

    public void setStartAngle(Rotation startAngle) {
        this.startAngle = startAngle;
    }

    public Rotation getEndAngle() {
        return endAngle;
    }

    public void setEndAngle(Rotation endAngle) {
        this.endAngle = endAngle;
    }

    public double getRadius() {
        return radius;
    }

    public void setRadius(double radius) {
        this.radius = radius;
    }

    public Rotation getDeltaAngle() {
        return endAngle.sub(startAngle);
    }

    public boolean isCounterClockwise() {
        return getDeltaAngle().toRadians() > 0;
    }

    public boolean isClockwise() {
        return getDeltaAngle().toRadians() < 0;
    }

    private Rotation headingToNormal(Rotation angle) {
        return isCounterClockwise() ? angle.cwNormal() : angle.ccwNormal();
    }

    private Rotation normalToHeading(Rotation angle) {
        return isCounterClockwise() ? angle.ccwNormal() : angle.cwNormal();
    }

    public Rotation getStartNormal() {
        return headingToNormal(startAngle);
    }

    public Rotation getEndNormal() {
        return headingToNormal(endAngle);
    }

    public Pose2d getStartPose() {
        return new Pose2d(center.add(getStartNormal().toTranslation(radius)),
                startAngle);
    }

    public Pose2d getEndPose() {
        return new Pose2d(center.add(getEndNormal().toTranslation(radius)),
                endAngle);
    }

    @Override
    public double getArcLength() {
        return Math.abs(getDeltaAngle().toRadians() * radius);
    }

    @Override
    protected Pose2d getNearestPoint(Translation2d point) {
        Translation2d diff = point.sub(center);
        if (isEpsilonZero(diff.getMagnitude())) { //wtf?
            return getStartPose();
        }
        Rotation direction = diff.getDirection();
        Translation2d nearest = center.add(direction.toTranslation(radius));
        if (!direction.isBetween(getStartNormal(), getEndNormal())) {
            Translation2d startTrans = getStartPose().getTranslation();
            double startDist = point.sub(startTrans).getMagnitude();
            Translation2d endTrans = getEndPose().getTranslation();
            double endDist = point.sub(endTrans).getMagnitude();
            if (startDist < endDist) {
                nearest = startTrans;
            } else {
                nearest = endTrans;
            }
        }
        return new Pose2d(nearest, normalToHeading(direction));
    }

    @Override
    public Pose2d getLookAhead(Pose2d nearestPoint, double arcDistance) {
        Translation2d diff = nearestPoint.getTranslation().sub(center);
        Rotation direction = diff.getDirection();
        Rotation angleDelta = Rotation.fromRadians(arcDistance / radius * (isClockwise() ? -1 : 1));
        Rotation newDirection = direction.add(angleDelta);
        return new Pose2d(center.add(newDirection.toTranslation(radius)),
                normalToHeading(newDirection));
    }

    @Override
    protected double getTraveledDistance(Translation2d point) {
        Translation2d diff = point.sub(center);
        Rotation direction = diff.getDirection();
        Rotation angleDelta = direction.sub(getStartNormal());
        return angleDelta.toRadians() * radius * (isClockwise() ? -1 : 1);
    }

    @Override
    protected double getRemainingDistance(Translation2d point) {
        Translation2d diff = point.sub(center);
        Rotation direction = diff.getDirection();
        Rotation angleDelta = direction.sub(getEndNormal());
        return angleDelta.toRadians() * radius * (isClockwise() ? 1 : -1);
    }

    @Override
    public String toString() {
        return "ArcSegment{" +
                "startPose=" + getStartPose() +
                ", endPose=" + getEndPose() +
                ", center=" + center +
                ", radius=" + radius +
                ", startSpeed=" + getStartSpeed() +
                ", travelSpeed=" + getTravelSpeed() +
                ", endSpeed=" + getEndSpeed() +
                ", startNormal=" + getStartNormal() +
                ", endNormal=" + getEndNormal() +
                '}';
    }
}
