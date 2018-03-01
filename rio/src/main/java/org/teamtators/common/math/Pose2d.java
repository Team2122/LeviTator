package org.teamtators.common.math;

import com.google.common.base.Preconditions;

import static org.teamtators.common.math.Epsilon.isEpsilonNegative;
import static org.teamtators.common.math.Epsilon.isEpsilonZero;

/**
 * @author Alex Mikhalev
 */
public class Pose2d {
    private Translation2d translation;
    private Rotation yaw;

    public Pose2d(Translation2d translation, Rotation yaw) {
        this.translation = Preconditions.checkNotNull(translation);
        this.yaw = Preconditions.checkNotNull(yaw);
    }

    public Pose2d(Pose2d other) {
        this.translation = new Translation2d(other.translation);
        this.yaw = new Rotation(other.yaw);
    }

    public static Pose2d zero() {
        return new Pose2d(Translation2d.zero(), Rotation.identity());
    }

    public Pose2d withTranslation(Translation2d translation) {
        return new Pose2d(translation, this.getYaw());
    }

    public Pose2d withYaw(Rotation yawRotation) {
        return new Pose2d(new Translation2d(this.translation), yawRotation);
    }

    public Translation2d getTranslation() {
        return translation;
    }

    public Rotation getYaw() {
        return yaw;
    }

    public double getYawDegrees() {
        return yaw.toDegrees();
    }

    public double getX() {
        return translation.getX();
    }

    public double getY() {
        return translation.getY();
    }

    public Pose2d add(Pose2d other) {
        return new Pose2d(this.translation.add(other.translation), this.yaw.add(other.yaw));
    }

    public Pose2d addTranslation(Translation2d translation) {
        return new Pose2d(this.translation.add(translation), this.yaw);
    }

    public Pose2d addYaw(Rotation yaw) {
        return new Pose2d(this.translation, this.yaw.add(yaw));
    }

    public Pose2d invertYaw() {
        return withYaw(yaw.inverse());
    }

    public Pose2d sub(Pose2d other) {
        return new Pose2d(this.translation.sub(other.translation), this.yaw.sub(other.yaw));
    }

    public Pose2d neg() {
        return new Pose2d(this.translation.neg(), this.yaw.neg());
    }

    public Pose2d rotateBy(Rotation rotation) {
        return new Pose2d(this.translation.rotateBy(rotation),
                this.yaw.add(rotation));
    }

    public Pose2d chain(Pose2d other) {
        return this.addTranslation(other.getTranslation().rotateBy(this.yaw))
                .addYaw(other.yaw);
    }

    public Pose2d extend(double distance) {
        return new Pose2d(this.translation.add(yaw.toTranslation(distance)), this.yaw);
    }

    public Translation2d getIntersection(Pose2d other) {
        if (yaw.isParallel(other.yaw)) {
            return Translation2d.nan();
        }
        if (Math.abs(yaw.cos()) < Math.abs(other.yaw.cos())) {
            return intersectionHelper(this, other);
        } else {
            return intersectionHelper(other, this);
        }
    }

    private static Translation2d intersectionHelper(Pose2d a, Pose2d b) {
        Rotation a_r = a.getYaw();
        Rotation b_r = b.getYaw();
        Translation2d a_t = a.getTranslation();
        Translation2d b_t = b.getTranslation();

        double tan_b = b_r.tan();
        double t = ((a_t.getX() - b_t.getX()) * tan_b + b_t.getY() - a_t.getY())
                / (a_r.sin() - a_r.cos() * tan_b);
        return a_t.add(a_r.toTranslation(t));
    }

    public Translation2d getNearestPoint(Translation2d point) {
        Pose2d normalPose = new Pose2d(point, yaw.ccwNormal());
        return getIntersection(normalPose);
    }

    public double getDistanceAhead(Translation2d point) {
        Translation2d diff = point.sub(translation);
        if (isEpsilonZero(diff.getMagnitude())) {
            return 0.0;
        }
        Rotation direction = diff.getDirection();
        if (direction.epsilonEquals(yaw)) {
            return diff.getMagnitude();
        } else if (direction.inverse().epsilonEquals(yaw)) {
            return -diff.getMagnitude();
        } else {
            return Double.NaN;
        }
    }

    @Override
    public String toString() {
        return "Pose2d{" +
                "translation=" + translation +
                ", yaw=" + yaw +
                '}';
    }

    public boolean epsilonEquals(Pose2d other) {
        return getTranslation().epsilonEquals(other.getTranslation()) &&
                getYaw().epsilonEquals(other.getYaw());
    }
}
