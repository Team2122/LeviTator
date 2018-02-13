package org.teamtators.common.controllers;

import edu.wpi.first.wpilibj.GenericHID;
import org.teamtators.common.scheduler.TriggerSource;

/**
 * @author Alex Mikhalev
 */
public class RawController extends ControllerBase<Integer, Integer, ControllerBase.Config> {
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
}
