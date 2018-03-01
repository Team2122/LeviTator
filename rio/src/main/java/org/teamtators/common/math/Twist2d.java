package org.teamtators.common.math;

public class Twist2d {
    private double deltaX;
    private Rotation deltaYaw;

    public Twist2d(Rotation deltaYaw, double deltaX) {
        this.deltaYaw = deltaYaw;
        this.deltaX = deltaX;
    }

    public Twist2d() {
        this(Rotation.identity(), 0.0);
    }

    public double getDeltaX() {
        return deltaX;
    }

    public void setDeltaX(double deltaX) {
        this.deltaX = deltaX;
    }

    public Rotation getDeltaYaw() {
        return deltaYaw;
    }

    public void setDeltaYaw(Rotation deltaYaw) {
        this.deltaYaw = deltaYaw;
    }

    @Override
    public String toString() {
        return "Twist2d{" +
                "deltaX=" + deltaX +
                ", deltaYaw=" + deltaYaw +
                '}';
    }

    public boolean epsilonEquals(Twist2d other) {
        return Epsilon.isEpsilonEqual(deltaX, other.deltaX) && deltaYaw.epsilonEquals(other.deltaYaw);
    }

    public static Twist2d fromTangentArc(Pose2d startPose, Translation2d endPoint) {
        Translation2d diff = endPoint.sub(startPose.getTranslation());
        Translation2d halfDiff = diff.scale(0.5);
        Pose2d perpBisector = new Pose2d(startPose.getTranslation().add(halfDiff), diff.getDirection().ccwNormal());
        Rotation startHeading = startPose.getYaw();
        Rotation startNormal = startHeading.ccwNormal();
        Pose2d startNormalLine = new Pose2d(startPose.getTranslation(), startNormal);
        Translation2d center = perpBisector.getIntersection(startNormalLine);
        Twist2d twist = new Twist2d();
        if (center.isNaN()) { // if the radii don't intersect, it is a straight line
            twist.setDeltaYaw(Rotation.identity());
            twist.setDeltaX(diff.getMagnitude());
        } else {
            boolean isCcw = startNormalLine.getDistanceAhead(center) > 0;
            double radius = endPoint.sub(center).getMagnitude();
            Rotation endNormal = endPoint.sub(center).getDirection();
            Rotation endHeading = isCcw ? endNormal.ccwNormal() : endNormal.cwNormal();
            Rotation deltaHeading = endHeading.sub(startHeading);
            double arcLength = deltaHeading.toRadians() * radius;
            twist.setDeltaYaw(deltaHeading);
            twist.setDeltaX(Math.abs(arcLength));
        }
        return twist;
    }

    public Twist2d invert() {
        return new Twist2d(deltaYaw.neg(), -deltaX);
    }
}
