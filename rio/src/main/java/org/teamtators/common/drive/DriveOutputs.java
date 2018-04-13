package org.teamtators.common.drive;

import java.util.Objects;

public class DriveOutputs {
    private double left;
    private double right;

    public DriveOutputs(double left, double right) {
        this.left = left;
        this.right = right;
    }

    public DriveOutputs() {
        this(0.0, 0.0);
    }

    public double getLeft() {
        return left;
    }

    public double getRight() {
        return right;
    }

    public DriveOutputs normalize() {
        return normalize(1.0);
    }

    public DriveOutputs normalize(double maxValue) {
        double greatest = Math.max(Math.abs(left), Math.abs(right));
        double scale = maxValue / Math.max(greatest, maxValue);
        return new DriveOutputs(left * scale, right * scale);
    }

    public DriveOutputs maximize() {
        return maximize(1.0);
    }

    public DriveOutputs maximize(double maxValue) {
        double max = Math.max(Math.abs(left), Math.abs(right)) / maxValue;
        return new DriveOutputs(left / max, right / max);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DriveOutputs that = (DriveOutputs) o;
        return Double.compare(that.left, left) == 0 &&
                Double.compare(that.right, right) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(left, right);
    }

    @Override
    public String toString() {
        return "DriveOutputs<" + left + ", " + right + '>';
    }

    public DriveOutputs scale(double power) {
        return new DriveOutputs(left * power, right * power);
    }
}
