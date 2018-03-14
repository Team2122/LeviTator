package org.teamtators.common.drive;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.teamtators.common.math.Pose2d;
import org.teamtators.common.math.Rotation;
import org.teamtators.common.math.Translation2d;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.teamtators.common.math.Epsilon.isEpsilonPositive;
import static org.teamtators.common.math.Epsilon.isEpsilonZero;

public class DrivePath {
    private static final Logger logger = LoggerFactory.getLogger(DrivePath.class);

    private final String name;
    private List<PathPoint> points;

    public DrivePath(String name, Collection<PathPoint> points) {
        this.name = name;
        this.points = new ArrayList<>(points);
    }

    public DrivePath(String name) {
        this.name = name;
        this.points = new ArrayList<>();
    }

    public String getName() {
        return name;
    }

    public void addPoint(PathPoint point) {
        point.check();
        this.points.add(point);
    }

    public List<PathPoint> getPoints() {
        return points;
    }


    public DriveSegments toSegments() {
        List<PathPoint> points = this.getPoints();
        DriveSegments segments = new DriveSegments();
        int numPoints = points.size();
        double speed = 0.0, lastSpeed = 0.0;
        Rotation heading, nextHeading = null;
        double takeOffLength, lastTakeOffLength = 0.0;

        for (int i = 0; i < numPoints - 1; i++) {
            PathPoint point1 = points.get(i);
            PathPoint point2 = points.get(i + 1);
            Translation2d trans = point2.getTranslation().minus(point1.getTranslation());
            double length = trans.getMagnitude();
            heading = trans.getDirection();
            StraightSegment straight = new StraightSegment();
            ArcSegment arc = null;
            double radius = point2.getRadius();
            boolean isRadius = isEpsilonPositive(radius) && i < numPoints - 2;
            straight.setStartSpeed(speed);
            straight.setTravelSpeed(point1.getSpeed());
            Pose2d startPose = new Pose2d(point1.getTranslation(), heading)
                    .extend(lastTakeOffLength);
            straight.setStartPose(startPose);
            straight.setEndSpeed(point2.getArcSpeed());
            straight.setReverse(point1.isReverse());
            Rotation deltaHeading = null, angle = null;
            Translation2d trans2 = null;
            double length2 = 0;
            boolean isStraight = false;
            if (isRadius) {
                PathPoint point3 = points.get(i + 2);
                trans2 = point3.getTranslation().minus(point2.getTranslation());
                length2 = trans2.getMagnitude();
                nextHeading = trans2.getDirection();
                deltaHeading = nextHeading.sub(heading);
                angle = deltaHeading.complement();
                if (!isEpsilonZero(trans.getMagnitude()) &&
                        !isEpsilonZero(trans2.getMagnitude())) {
                    if (isEpsilonZero(deltaHeading.toRadians())) {
                        isStraight = true;
                    }
                } else {
                    isRadius = false;
                }
            }
            if (isRadius && !isStraight) {
                Rotation halfAngle = angle.mult(0.5);
                double availableTakeOffLength = Math.min(length - lastTakeOffLength, length2);
                takeOffLength = Math.abs(radius / halfAngle.tan());
                if (availableTakeOffLength < takeOffLength) {
                    logger.warn("Decreasing radius on arc because distance between points is too small: " +
                            availableTakeOffLength + " < " + takeOffLength);
                    radius = Math.abs(availableTakeOffLength * halfAngle.tan());
                    takeOffLength = Math.abs(radius / halfAngle.tan());
                }

                double centerToPointLength = Math.abs(radius / halfAngle.sin());
                Rotation halfHeading = heading.inverse().sub(halfAngle);
                Translation2d arcCenter = new Pose2d(point2.getTranslation(), halfHeading)
                        .extend(centerToPointLength).getTranslation();
                arc = new ArcSegment();
                arc.setStartSpeed(point1.getArcSpeed());
                arc.setTravelSpeed(point2.getArcSpeed());
                arc.setEndSpeed(point2.getArcSpeed());
                arc.setCenter(arcCenter);
                arc.setRadius(radius);
                arc.setStartAngle(heading);
                arc.setEndAngle(nextHeading);
                arc.setReverse(point1.isReverse());
                speed = point2.getArcSpeed();
            } else {
                if (isStraight) {
                    speed = point2.getSpeed();
                } else {
                    speed = 0;
                }
                takeOffLength = 0.0;
            }
            straight.setEndSpeed(speed);
            double newLength = length - lastTakeOffLength - takeOffLength;
            if (newLength < 0) {
                throw new RuntimeException("Distance between path points is too short with arcs included: "
                        + length + " - " + lastTakeOffLength + " - " + takeOffLength + " = " + newLength);
            }
            if (!isEpsilonZero(newLength)) {
                straight.setLength(newLength);
                segments.addSegment(straight);
            } else {
                if (arc != null) {
                    arc.setStartSpeed(lastSpeed);
                }
            }
            if (arc != null) {
                segments.addSegment(arc);
            }
            lastTakeOffLength = takeOffLength;
            lastSpeed = speed;
        }

        return segments;
    }

    @Override
    public String toString() {
        return "DrivePath{" +
                "points=" + points +
                '}';
    }
}
