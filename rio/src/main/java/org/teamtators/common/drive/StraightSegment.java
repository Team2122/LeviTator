package org.teamtators.common.drive;

import org.teamtators.common.math.Pose2d;

public class StraightSegment implements DriveSegment {
    private double startSpeed;
    private double travelSpeed;
    private double endSpeed;

    private Pose2d startPose;
    private double length;


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

    public double getLength() {
        return length;
    }

    public void setLength(double length) {
        this.length = length;
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
    public String toString() {
        return "StraightSegment{" +
                "startSpeed=" + startSpeed +
                ", travelSpeed=" + travelSpeed +
                ", endSpeed=" + endSpeed +
                ", startPose=" + startPose +
                ", length=" + length +
                ", endPose=" + getEndPose() +
                '}';
    }
}
