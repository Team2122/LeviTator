package org.teamtators.common.math;

/**
 * @author Alex Mikhalev
 */
public class Rotation {
    private double sin;
    private double cos;

    public Rotation(double sin, double cos) {
        this.sin = sin;
        this.cos = cos;
        this.normalize();
    }

    public Rotation(Rotation other) {
        this.sin = other.sin;
        this.cos = other.cos;
    }

    public static Rotation zero() {
        return new Rotation(0.0, 1.0);
    }

    public static Rotation fromRadians(double radians) {
        return new Rotation(Math.sin(radians), Math.cos(radians));
    }

    public static Rotation fromDegrees(double degrees) {
        return fromRadians(Math.toRadians(degrees));
    }

/*
    public void setRadians(double radians) {
        this.sin = Math.sin(radians);
        this.cos = Math.cos(radians);
        normalize();
    }

    public void setDegrees(double degrees) {
        setDegrees(Math.toRadians(degrees));
    }
*/

    public double sin() {
        return sin;
    }

    public double cos() {
        return cos;
    }

    public double tan() {
        return sin / cos;
    }

    public double csc() {
        return 1 / sin;
    }

    public double sec() {
        return 1 / cos;
    }

    public double cot() {
        return cos / sin;
    }

    public double toRadians() {
        return Math.atan2(sin, cos);
    }

    public double toDegrees() {
        return Math.toDegrees(this.toRadians());
    }

    public Rotation neg() {
        return new Rotation(-this.sin, this.cos);
    }

    public Rotation add(Rotation other) {
        return new Rotation(this.sin * other.cos + this.cos * other.sin,
                this.cos * other.cos - this.sin * other.sin);
    }

    public Rotation sub(Rotation other) {
        return new Rotation(this.sin * other.cos - this.cos * other.sin,
                this.cos * other.cos + this.sin * other.sin);
    }

    public Rotation mult(double scaleNum) {
        return Rotation.fromRadians(scaleNum * toRadians());
    }

    public Rotation normal() {
        return new Rotation(this.cos, -this.sin);
    }

    public Rotation inverse() {
        return new Rotation(-this.sin, -this.cos);
    }

    public Rotation complement() {
        return new Rotation(this.sin, -this.cos);
    }

    public Translation2d toTranslation() {
        return new Translation2d(this.cos, this.sin);
    }

    public void normalize() {
        double hyp = Math.hypot(sin, cos);
        if (hyp != 0) {
            this.sin /= hyp;
            this.cos /= hyp;
        }
    }

    @Override
    public String toString() {
        return toDegrees() + "°";
    }
}
