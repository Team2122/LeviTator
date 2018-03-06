package org.teamtators.common.controllers;

import org.teamtators.common.config.Configurable;
import org.teamtators.common.scheduler.TriggerSource;

public class LogitechF310
        extends ControllerBase<LogitechF310.Button, LogitechF310.Axis, LogitechF310.Config>
        implements Configurable<LogitechF310.Config> {
    private double rightTriggerDeadzone;
    private double leftTriggerDeadzone;

    public LogitechF310(String name) {
        super(name);
        setAxisCount(6);
        setButtonCount(10);
    }

    @Override
    public Class<Button> getButtonClass() {
        return Button.class;
    }

    @Override
    public Class<Axis> getAxisClass() {
        return Axis.class;
    }

    @Override
    public double getAxisValue(Axis axis) {
        return getRawAxisValue(axis.getAxisID());
    }

    @Override
    public boolean isButtonDown(Button button) {
        switch (button) {
            case TRIGGER_LEFT:
                return getAxisValue(Axis.LEFT_TRIGGER) >= leftTriggerDeadzone;
            case TRIGGER_RIGHT:
                return getAxisValue(Axis.RIGHT_TRIGGER) >= rightTriggerDeadzone;
            case POV_UP:
                return getPov() == 0;
            case POV_RIGHT:
                return getPov() == 90;
            case POV_DOWN:
                return getPov() == 180;
            case POV_LEFT:
                return getPov() == 270;
            default:
                return super.isRawButtonDown(button.getButtonID());
        }
    }

    @Override
    public boolean isButtonPressed(Button button) {
        return isRawButtonPressed(button.getButtonID());
    }

    @Override
    public boolean isButtonReleased(Button button) {
        return isRawButtonDown(button.getButtonID());
    }

    @Override
    public TriggerSource getTriggerSource(Button button) {
        return () -> isButtonDown(button);
    }

    @Override
    public void configure(Config config) {
        super.configure(config);
        this.leftTriggerDeadzone = config.leftTriggerDeadzone;
        this.rightTriggerDeadzone = config.rightTriggerDeadzone;
    }

    /**
     * Enum to reference buttons
     */
    public enum Button {
        A(1),
        B(2),
        X(3),
        Y(4),
        BUMPER_LEFT(5),
        BUMPER_RIGHT(6),
        BACK(7),
        START(8),
        STICK_LEFT(9),
        STICK_RIGHT(10),
        TRIGGER_LEFT(11),
        TRIGGER_RIGHT(12),
        POV_UP(13),
        POV_DOWN(14),
        POV_LEFT(15),
        POV_RIGHT(16);

        private int buttonID;

        Button(int buttonID) {
            this.buttonID = buttonID;
        }

        /**
         * Get a button by name
         *
         * @param name the button's name
         * @return the button's enum value
         */
        public static Button fromName(String name) {
            return Button.valueOf(name);
        }

        /**
         * Gets the button ID
         *
         * @return the button ID
         */
        public int getButtonID() {
            return buttonID;
        }
    }

    /**
     * Enum to reference axis
     */
    public enum Axis {
        LEFT_STICK_X(0),
        LEFT_STICK_Y(1),
        LEFT_TRIGGER(2),
        RIGHT_TRIGGER(3),
        RIGHT_STICK_X(4),
        RIGHT_STICK_Y(5);

        private int axisID;

        Axis(int axisID) {
            this.axisID = axisID;
        }

        /**
         * Gets the axis ID
         *
         * @return the axis ID
         */
        public int getAxisID() {
            return axisID;
        }
    }

    public static class Config extends ControllerBase.Config {
        public double leftTriggerDeadzone = 0.1;
        public double rightTriggerDeadzone = 0.1;
    }
}
