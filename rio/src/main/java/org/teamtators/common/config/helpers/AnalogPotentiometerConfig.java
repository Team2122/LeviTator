package org.teamtators.common.config.helpers;

import org.teamtators.common.hw.AnalogPotentiometer;

public class AnalogPotentiometerConfig implements ConfigHelper<AnalogPotentiometer> {
    public int channel;
    public double fullRange = 360;
    public double offset = 0;
    public double minValue = 0.0;
    public boolean continuous = false;
    public int averageBits = 1;
    public int oversampleBits = 1;

    public AnalogPotentiometer create() {
        AnalogPotentiometer pot = new AnalogPotentiometer(channel);
        pot.setFullRange(fullRange);
        pot.setOffset(offset);
        pot.setMinValue(minValue);
        pot.setContinuous(true);
        pot.setAverageBits(averageBits);
        pot.setOversampleBits(oversampleBits);
        return pot;
    }
}
