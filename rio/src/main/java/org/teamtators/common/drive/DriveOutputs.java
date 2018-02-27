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
        double max = Math.max(Math.abs(left), Math.abs(right));
        max = Math.max(max, 1.0);
        return new DriveOutputs(left / max, right / max);
    }

    public DriveOutputs maximize() {
        double max = Math.max(Math.abs(left), Math.abs(right));
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
