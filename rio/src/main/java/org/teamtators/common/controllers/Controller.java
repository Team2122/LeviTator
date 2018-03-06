package org.teamtators.common.controllers;

import org.teamtators.common.scheduler.TriggerSource;

/**
 * @author Alex Mikhalev
 */
public interface Controller<TButton, TAxis> {
    String getName();
    Class<TButton> getButtonClass();
    Class<TAxis> getAxisClass();

    double getAxisValue(TAxis axis);
    boolean isButtonDown(TButton button);
    boolean isButtonPressed(TButton button);
    boolean isButtonReleased(TButton button);
    /**
     * Gets a trigger source for the specified button on the joystick
     *
     * @param button Button to get the trigger source for
     * @return A TriggerSource
     */
    TriggerSource getTriggerSource(TButton button);

    int getAxisCount();
    int getButtonCount();

    double getRawAxisValue(int axis);
    boolean isRawButtonDown(int button);
    boolean isRawButtonPressed(int button);
    boolean isRawButtonReleased(int button);

    /**
     * Gets a trigger source for the specified button on the joystick
     *
     * @param button Button to get the trigger source for
     * @return A TriggerSource
     */
    TriggerSource getRawTriggerSource(int button);

    default int getPov() {
        return getPov(0);
    }
    int getPov(int pov);

    void setOutput(int outputNumber, boolean value);
    void setOutputs(int outputs);
    void setRumble(RumbleType type, double value);

    void onDriverStationData();

    /**
     * An enum to rumble the left, right or both sides of a controller
     */
    enum RumbleType {
        LEFT,
        RIGHT,
        BOTH
    }
}
