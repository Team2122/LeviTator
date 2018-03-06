package org.teamtators.common.controllers;

import edu.wpi.first.wpilibj.GenericHID;
import org.teamtators.common.scheduler.TriggerSource;

/**
 * @author Alex Mikhalev
 */
public class RawController extends ControllerBase<Integer, Integer, RawController.Config> {
    public static final int DEFAULT_BUTTON_COUNT = 12;
    public static final int DEFAULT_AXIS_COUNT = 6;
    private int axisCount = DEFAULT_BUTTON_COUNT;
    private int buttonCount = DEFAULT_AXIS_COUNT;

    public RawController(String name, GenericHID hid) {
        super(name, hid);
    }

    public RawController(String name, int port) {
        super(name, port);
    }

    public RawController(String name) {
        super(name);
    }

    @Override
    public Class<Integer> getButtonClass() {
        return Integer.class;
    }

    @Override
    public Class<Integer> getAxisClass() {
        return Integer.class;
    }

    @Override
    public double getAxisValue(Integer axis) {
        return getRawAxisValue(axis);
    }

    @Override
    public boolean isButtonDown(Integer button) {
        return isRawButtonDown(button);
    }

    @Override
    public boolean isButtonPressed(Integer button) {
        return isRawButtonPressed(button);
    }

    @Override
    public boolean isButtonReleased(Integer button) {
        return isRawButtonReleased(button);
    }

    @Override
    public TriggerSource getTriggerSource(Integer button) {
        return getRawTriggerSource(button);
    }

    public void setAxisCount(int axisCount) {
        this.axisCount = axisCount;
    }

    public void setButtonCount(int buttonCount) {
        this.buttonCount = buttonCount;
    }

    @Override
    public int getAxisCount() {
        return axisCount;
    }

    @Override
    public int getButtonCount() {
        return buttonCount;
    }

    @Override
    public void configure(Config config) {
        super.configure(config);
        setButtonCount(config.buttonCount);
        setAxisCount(config.axisCount);
    }

    public static class Config extends ControllerBase.Config {
        public int buttonCount = DEFAULT_BUTTON_COUNT;
        public int axisCount = DEFAULT_AXIS_COUNT;
    }
}
