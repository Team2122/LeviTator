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
        return Epsilon.isEpsilonEqual(deltaX, other.deltaX) && deltaYaw.equals(other.deltaYaw);
    }
}
