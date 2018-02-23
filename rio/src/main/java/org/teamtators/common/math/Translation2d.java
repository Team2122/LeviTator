package org.teamtators.common.math;

/**
 * @author Alex Mikhalev
 */
public class Translation2d {
    private double x;
    private double y;

    public Translation2d(double x, double y) {
        this.x = x;
        this.y = y;
    }

    public static Translation2d zero() {
        return new Translation2d(0.0, 0.0);
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public Translation2d rotateBy(Rotation rotation) {
        return new Translation2d(this.x * rotation.cos() - this.y * rotation.sin(),
                this.x * rotation.sin() + this.y * rotation.cos());
    }

    public Rotation getDirection() {
        return new Rotation(this.x, this.y);
    }

    public double getMagnitude() {
        return Math.hypot(this.x, this.y);
    }

    public Translation2d add(Translation2d other) {
        return new Translation2d(this.x + other.x, this.y + other.y);
    }

    public Translation2d sub(Translation2d other) {
        return new Translation2d(this.x - other.x, this.y - other.y);
    }

    public Translation2d neg() {
        return new Translation2d(-this.x, -this.y);
    }

    public Translation2d scale(double scalar) {
        return new Translation2d(this.x * scalar, this.y * scalar);
    }

    public double dot(Translation2d other) {
        return this.x * other.x + this.y * other.y;
    }

    @Override
    public String toString() {
        return "<" + x + ", " + y + ">";
    }
}
