package org.teamtators.common.paths;

import org.teamtators.common.math.Pose2d;
import org.teamtators.common.math.Rotation;
import org.teamtators.common.math.Translation2d;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class DrivePath {
    private static final double EPSILON = 1E-9;

    public static void main(String[] argv) {
        DrivePath path = new DrivePath();
        DrivePath.Point point = new DrivePath.Point();
        point.setTranslation(new Translation2d(0, 0));
        point.setRadius(12);
        point.setSpeed(30);
        point.setArcSpeed(20);
        path.addPoint(point.copy());
        point.setTranslation(new Translation2d(0, 10));
        path.addPoint(point.copy());
        point.setTranslation(new Translation2d(30, 10));
        path.addPoint(point.copy());
        point.setTranslation(new Translation2d(30, 40));
        path.addPoint(point.copy());
        point.setTranslation(new Translation2d(-30, 40));
        path.addPoint(point.copy());
        point.setTranslation(new Translation2d(-30, 10));
        path.addPoint(point.copy());
        point.setTranslation(new Translation2d(0, 10));
        path.addPoint(point.copy());
        point.setTranslation(new Translation2d(0, 0));
        path.addPoint(point.copy());

        DriveSegments segments = path.toSegments();
        System.out.println(segments.getSegments().stream().map(Object::toString).collect(Collectors.joining("\n")));
    }

    public static class Point {
        private Translation2d translation;
        private double radius;
        private double speed;
        private double arcSpeed;

        public Point(Translation2d translation) {
            this.translation = translation;
        }

        public Point() {
            this(Translation2d.zero());
        }

        public Point(Point point) {
            this.translation = new Translation2d(point.translation);
            this.radius = point.radius;
            this.speed = point.speed;
            this.arcSpeed = point.arcSpeed;
        }

        public void setTranslation(Translation2d translation) {
            this.translation = translation;
        }

        public Translation2d getTranslation() {
            return translation;
        }

        public double getX() {
            return translation.getX();
        }

//        public void setX(double x) {
//            translation.setX(x);
//        }

        public double getY() {
            return translation.getY();
        }

//        public void setY(double y) {
//            translation.setY(y);
//        }

        public double getRadius() {
            return radius;
        }

        public void setRadius(double radius) {
            this.radius = radius;
        }

        public double getSpeed() {
            return speed;
        }

        public void setSpeed(double speed) {
            this.speed = speed;
        }

        public double getArcSpeed() {
            return arcSpeed;
        }

        public void setArcSpeed(double arcSpeed) {
            this.arcSpeed = arcSpeed;
        }

        @Override
        public String toString() {
            return "Point{" +
                    "translation=" + translation +
                    ", radius=" + radius +
                    ", speed=" + speed +
                    ", arcSpeed=" + arcSpeed +
                    '}';
        }

        public Point copy() {
            return new Point(this);
        }
    }

    private List<Point> points;

    public DrivePath(Collection<Point> points) {
        this.points = new ArrayList<>(points);
    }

    public DrivePath() {
        this.points = new ArrayList<>();
    }

    public void addPoint(Point point) {
        this.points.add(point);
    }

    public List<Point> getPoints() {
        return points;
    }


    public DriveSegments toSegments() {
        List<DrivePath.Point> points = this.getPoints();
        DriveSegments segments = new DriveSegments();
        int numPoints = points.size();
        double speed = 0.0, lastSpeed = 0.0;
        Rotation heading, nextHeading = null;
        double takeOffLength, lastTakeOffLength = 0.0;

        for (int i = 0; i < numPoints - 1; i++) {
            DrivePath.Point point1 = points.get(i);
            DrivePath.Point point2 = points.get(i + 1);
            Translation2d trans = point2.getTranslation().sub(point1.getTranslation());
            double length = trans.getMagnitude();
            heading = trans.getDirection();
            StraightSegment straight = new StraightSegment();
            ArcSegment arc = null;
            double radius = point2.getRadius();
            boolean isRadius = radius > EPSILON && i < numPoints - 2;
            straight.setStartSpeed(speed);
            straight.setTravelSpeed(point1.getSpeed());
            Pose2d startPose = new Pose2d(point1.getTranslation(), heading)
                    .extend(lastTakeOffLength);
            straight.setStartPose(startPose);
            straight.setEndSpeed(point2.getArcSpeed());
            Rotation deltaHeading = null, angle = null;
            Translation2d trans2 = null;
            double length2 = 0;
            boolean isStraight = false;
            if (isRadius) {
                DrivePath.Point point3 = points.get(i + 2);
                trans2 = point3.getTranslation().sub(point2.getTranslation());
                length2 = trans2.getMagnitude();
                nextHeading = trans2.getDirection();
                deltaHeading = nextHeading.sub(heading);
                angle = deltaHeading.complement();
                if (Math.abs(trans.getMagnitude()) > EPSILON &&
                        Math.abs(trans2.getMagnitude()) > EPSILON) {
                    if (Math.abs(deltaHeading.toRadians()) < EPSILON) {
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
                    System.out.println("Warning: Decreasing radius on arc because distance between points is too small: " +
                            availableTakeOffLength + " < " + takeOffLength);
                    radius = Math.abs(availableTakeOffLength * halfAngle.tan());
                    takeOffLength = Math.abs(radius / halfAngle.tan());
                }

                double centerToPointLength = Math.abs(radius / halfAngle.sin());
                Rotation halfHeading = heading.add(halfAngle).inverse();
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
            if (Math.abs(newLength) > EPSILON) {
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
