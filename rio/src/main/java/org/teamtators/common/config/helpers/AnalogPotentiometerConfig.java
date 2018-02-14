package org.teamtators.common.config.helpers;

import org.teamtators.common.hw.AnalogPotentiometer;

public class AnalogPotentiometerConfig implements ConfigHelper<AnalogPotentiometer> {
    public int channel;
    public double fullRange = 360;
    public double offset = 0;
    public boolean continuous = false;

    public AnalogPotentiometer create() {
        return new AnalogPotentiometer(channel, fullRange, offset, continuous);
    }
}