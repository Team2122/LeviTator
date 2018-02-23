package org.teamtators.common.math;

import com.google.common.base.Preconditions;

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

    public static Pose2d zero() {
        return new Pose2d(Translation2d.zero(), Rotation.zero());
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

    public Pose2d sub(Pose2d other) {
        return new Pose2d(this.translation.sub(other.translation), this.yaw.sub(other.yaw));
    }

    public Pose2d neg() {
        return new Pose2d(this.translation.neg(), this.yaw.neg());
    }

    public Pose2d concat(Pose2d other) {
        return new Pose2d(this.translation.add(other.translation.rotateBy(this.yaw)),
                this.yaw.add(other.yaw));
    }

    @Override
    public String toString() {
        return "Pose2d{" +
                "translation=" + translation +
                ", yaw=" + yaw +
                '}';
    }
}
