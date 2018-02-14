package org.teamtators.common.control;

/**
 * Trapezoidal motion profile calculation
 */
public class TrapezoidalProfile {
    private double distance = 0; // distance to travel in inches

    private double startVelocity = 0; // starting speed in in/s
    private double travelVelocity = 0; // maximum absolute speed in in/s
    private double endVelocity = 0; // end speed in in/s

    private double maxAcceleration = 0; // the max acceleration, in in^2/s

    public TrapezoidalProfile() {
    }

    public TrapezoidalProfile(double distance, double startVelocity, double travelVelocity,
                              double endVelocity, double maxAcceleration) {
        this.distance = distance;
        this.startVelocity = startVelocity;
        this.travelVelocity = travelVelocity;
        this.endVelocity = endVelocity;
        this.maxAcceleration = maxAcceleration;
    }

    public TrapezoidalProfile(TrapezoidalProfile other) {
        this.distance = other.distance;
        this.startVelocity = other.startVelocity;
        this.travelVelocity = other.travelVelocity;
        this.endVelocity = other.endVelocity;
        this.maxAcceleration = other.maxAcceleration;
    }

    public double getStartVelocity() {
        return startVelocity;
    }

    public void setStartVelocity(double startVelocity) {
        this.startVelocity = startVelocity;
    }

    public double getTravelVelocity() {
        return travelVelocity;
    }

    public void setTravelVelocity(double travelVelocity) {
        this.travelVelocity = travelVelocity;
    }

    public double getEndVelocity() {
        return endVelocity;
    }

    public void setEndVelocity(double endVelocity) {
        this.endVelocity = endVelocity;
    }

    public double getDistance() {
        return distance;
    }

    public void setDistance(double distance) {
        this.distance = distance;
    }

    public double getMaxAcceleration() {
        return maxAcceleration;
    }

    public void setMaxAcceleration(double maxAcceleration) {
        this.maxAcceleration = maxAcceleration;
    }

    public TrapezoidalProfileCalculator createCalculator() {
        return new TrapezoidalProfileCalculator(this);
    }

    public TrapezoidalProfile copy() {
        return new TrapezoidalProfile(this);
    }

    @Override
    public String toString() {
        return "TrapezoidalProfile{" +
                "distance=" + distance +
                ", startVelocity=" + startVelocity +
                ", travelVelocity=" + travelVelocity +
                ", endVelocity=" + endVelocity +
                ", maxAcceleration=" + maxAcceleration +
                '}';
    }
}
