package org.teamtators.common.config.helpers;

import org.teamtators.common.hw.DistanceLaser;

public class DistanceLaserConfig implements ConfigHelper<DistanceLaser> {
    private int channel;
    private double distance0V;
    private double distance5V;

    public int getChannel() {
        return channel;
    }

    public void setChannel(int channel) {
        this.channel = channel;
    }

    double getDistance0V() {
        return distance0V;
    }

    public void setDistance0V(double distance0V) {
        this.distance0V = distance0V;
    }

    double getDistance5V() {
        return distance5V;
    }

    public void setDistance5V(double distance5V) {
        this.distance5V = distance5V;
    }

    public DistanceLaser create() {
        return new DistanceLaser(channel, distance0V, distance5V);
    }
}
