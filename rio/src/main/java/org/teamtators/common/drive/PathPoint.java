package org.teamtators.common.drive;

import org.teamtators.common.config.ConfigException;
import org.teamtators.common.math.Translation2d;

/**
 * @author Alex Mikhalev
 */
public class PathPoint {
    private Translation2d translation;
    private double radius = Double.NaN;
    private double speed = Double.NaN;
    private double arcSpeed = Double.NaN;
    private Boolean reverse = null;

    public PathPoint(Translation2d translation) {
        this.translation = translation;
    }

    public PathPoint() {
        this(Translation2d.zero());
    }

    public PathPoint(PathPoint point) {
        this.translation = new Translation2d(point.translation);
        this.radius = point.radius;
        this.speed = point.speed;
        this.arcSpeed = point.arcSpeed;
        this.reverse = point.reverse;
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

    public void setX(double x) {
        translation = translation.withX(x);
    }

    public double getY() {
        return translation.getY();
    }

    public void setY(double y) {
        translation = translation.withY(y);
    }

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

    public Boolean isReverse() {
        return reverse;
    }

    public void setReverse(boolean reverse) {
        this.reverse = reverse;
    }

    void check() {
        if (Double.isNaN(radius)) {
            throw new ConfigException("radius on DrivePath.Point not set");
        }
        if (Double.isNaN(arcSpeed)) {
            throw new ConfigException("arcSpeed on DrivePath.Point not set");
        }
        if (Double.isNaN(speed)) {
            throw new ConfigException("speed on DrivePath.Point not set");
        }
        if (reverse == null) {
            throw new ConfigException("reverse on DrivePath.Point not set");
        }
    }

    @Override
    public String toString() {
        return "Point{" +
                "translation=" + translation +
                ", radius=" + radius +
                ", speed=" + speed +
                ", arcSpeed=" + arcSpeed +
                ", reverse=" + reverse +
                '}';
    }

    public PathPoint copy() {
        return new PathPoint(this);
    }
}
