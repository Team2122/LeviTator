package org.teamtators.common.hw;

import edu.wpi.first.wpilibj.AnalogInput;
import edu.wpi.first.wpilibj.ControllerPower;

public class DistanceLaser {
    private double distance0V;
    private double distance5V;

    private AnalogInput distanceLaser;

    /**
     * @param distanceLaser the input that represents this distanceLaser
     * @param distance0V    the distance represented by a reading of 0V (in cm)
     * @param distance5V    the distance represented by a reading of 5V (in cm)
     */
    public DistanceLaser(AnalogInput distanceLaser, double distance0V, double distance5V) {
        this.distanceLaser = distanceLaser;
        this.distance0V = distance0V;
        this.distance5V = distance5V;
    }

    /**
     * @param channel    the channel of the input that represents this distanceLaser
     * @param distance0V the distance represented by a reading of 0V (in cm)
     * @param distance5V the distance represented by a reading of 5V (in cm)
     */
    public DistanceLaser(int channel, double distance0V, double distance5V) {
        this(new AnalogInput(channel), distance0V, distance5V);
    }

    public double getDistance() {
        double prop = distanceLaser.getVoltage() / ControllerPower.getVoltage5V();
        return (prop * (distance5V - distance0V)) + distance0V;
    }

    public double getVoltage() {
        return distanceLaser.getVoltage();
    }

    public double getDistance0V() {
        return distance0V;
    }

    public double getDistance5V() {
        return distance5V;
    }

    AnalogInput getAnalogInput() {
        return distanceLaser;
    }
}
