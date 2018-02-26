package org.teamtators.common.paths;

import org.teamtators.common.math.Pose2d;
import org.teamtators.common.math.Rotation;
import org.teamtators.common.math.Translation2d;

public class ArcSegment implements DriveSegment {
    private double startSpeed;
    private double travelSpeed;
    private double endSpeed;

    private Translation2d center;
    private Rotation startAngle;
    private Rotation endAngle;
    private double radius;

    public double getStartSpeed() {
        return startSpeed;
    }

    public void setStartSpeed(double startSpeed) {
        this.startSpeed = startSpeed;
    }

    public double getTravelSpeed() {
        return travelSpeed;
    }

    public void setTravelSpeed(double travelSpeed) {
        this.travelSpeed = travelSpeed;
    }

    public double getEndSpeed() {
        return endSpeed;
    }

    public void setEndSpeed(double endSpeed) {
        this.endSpeed = endSpeed;
    }

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

    public boolean isTurningLeft() {
        return endAngle.sub(startAngle).toRadians() > 0;
    }

    public boolean isTurningRight() {
        return endAngle.sub(startAngle).toRadians() < 0;
    }

    public Rotation getStartNormal() {
        return isTurningLeft() ? startAngle.normal().inverse() : startAngle.normal();
    }

    public Rotation getEndNormal() {
        return isTurningLeft() ? endAngle.normal().inverse() : endAngle.normal();
    }

    public Pose2d getStartPose() {
        return new Pose2d(center.add(getStartNormal().toTranslation().scale(radius)),
                startAngle);
    }

    public Pose2d getEndPose() {
        return new Pose2d(center.add(getEndNormal().inverse().toTranslation().scale(radius)),
                endAngle);
    }

    @Override
    public String toString() {
        return "ArcSegment{" +
                "startSpeed=" + startSpeed +
                ", travelSpeed=" + travelSpeed +
                ", endSpeed=" + endSpeed +
                ", center=" + center +
                ", startAngle=" + startAngle +
                ", endAngle=" + endAngle +
                ", radius=" + radius +
                ", startPose=" + getStartPose() +
                ", endPose=" + getEndPose() +
                '}';
    }
}
