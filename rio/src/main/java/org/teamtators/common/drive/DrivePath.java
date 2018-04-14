package org.teamtators.common.drive;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.teamtators.common.math.Pose2d;
import org.teamtators.common.math.Rotation;
import org.teamtators.common.math.Translation2d;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.teamtators.common.math.Epsilon.isEpsilonNegativeOrZero;
import static org.teamtators.common.math.Epsilon.isEpsilonPositiveOrZero;
import static org.teamtators.common.math.Epsilon.isEpsilonZero;

public class DrivePath {
    public static final Logger logger = LoggerFactory.getLogger(DrivePath.class);

    private List<PathPoint> points;

    public DrivePath(Collection<PathPoint> points) {
        this.points = new ArrayList<>(points);
    }

    public DrivePath() {
        this.points = new ArrayList<>();
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
            StraightSegment straight = StraightSegment.fromPoints(point1, point2);
            heading = straight.getHeading();
            double originalLength = straight.getLength();
            ArcSegment arc = null;
            double radius = point2.getRadius();
            boolean isRadius = isEpsilonPositiveOrZero(radius) && i < numPoints - 2;
            straight.setStartSpeed(speed);
            straight.shortenStart(lastTakeOffLength);
            Rotation deltaHeading, angle = null;
            StraightSegment straight2;
            Translation2d trans2;
            double length2 = 0;
            boolean canContinueSpeed = false;
            if (i >= numPoints - 1) {
                canContinueSpeed = true;
            }
            if (isRadius) {
                PathPoint point3 = points.get(i + 2);
                straight2 = StraightSegment.fromPoints(point2, point3);
                length2 = straight2.getLength();
                nextHeading = straight2.getHeading();
                deltaHeading = nextHeading.sub(heading);
                angle = deltaHeading.complement();
                if (straight.isValid() && straight2.isValid()) {
                    if (isEpsilonZero(deltaHeading.toRadians())) {
                        canContinueSpeed = true;
                    }
                } else {
                    isRadius = false;
                }
            }
            if (isRadius && !canContinueSpeed) {
                Rotation halfAngle = angle.mult(0.5);
                double availableTakeOffLength = Math.min(straight.getLength(), length2);
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
                if (canContinueSpeed) {
                    speed = point2.getSpeed();
                } else {
                    speed = 0;
                }
                takeOffLength = 0.0;
            }
            straight.setEndSpeed(speed);
            straight.shortenEnd(takeOffLength);
            double newLength = straight.getLength();
            if (newLength < 0) {
                throw new RuntimeException("Distance between path points is too short with arcs included: "
                        + originalLength + " - " + lastTakeOffLength + " - " + takeOffLength + " < " + newLength);
            }
            if (!isEpsilonZero(newLength)) {
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
