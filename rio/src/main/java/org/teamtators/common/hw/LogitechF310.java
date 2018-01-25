package org.teamtators.common.hw;

import edu.wpi.first.wpilibj.GenericHID;
import edu.wpi.first.wpilibj.Joystick;
import org.teamtators.common.config.Configurable;
import org.teamtators.common.control.Timer;
import org.teamtators.common.control.Updatable;
import org.teamtators.common.scheduler.TriggerSource;

public class LogitechF310 implements Configurable<LogitechF310.Config>, Updatable {
    private Joystick joystick;
    private double rightTriggerDeadzone;
    private double leftTriggerDeadzone;
    private double targetTime;

    private Timer timer = new Timer();

    public double getAxisValue(Axis axis) {
        return joystick.getRawAxis(axis.getAxisID());
    }

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
                return joystick.getRawButton(button.getButtonID());
        }
    }

    /**
     * Gets a trigger source for the specified button on the joystick
     *
     * @param button Button to get the trigger source for
     * @return A TriggerSource
     */
    public TriggerSource getTriggerSource(Button button) {
        return new LogitechTrigger(this, button);
    }

    private int getPov() {
        return joystick.getPOV();
    }

    public void setRumble(RumbleType rumbleType, double value, double time) {
        targetTime = time;
        timer.restart();
//        setRumbleValue(rumbleType, value);
    }

    private void setRumbleValue(RumbleType rumbleType, double value) {
        if (rumbleType == RumbleType.LEFT || rumbleType == RumbleType.BOTH) {
            joystick.setRumble(GenericHID.RumbleType.kLeftRumble, value);
        }
        if (rumbleType == RumbleType.RIGHT || rumbleType == RumbleType.BOTH) {
            joystick.setRumble(GenericHID.RumbleType.kRightRumble, value);
        }
    }

    @Override
    public void configure(Config config) {
        joystick = new Joystick(config.index);
        this.leftTriggerDeadzone = config.leftTriggerDeadzone;
        this.rightTriggerDeadzone = config.rightTriggerDeadzone;
    }

    @Override
    public void update(double delta) {
        if(timer.hasPeriodElapsed(targetTime)) {
            joystick.setRumble(GenericHID.RumbleType.kLeftRumble, 0.0);
            joystick.setRumble(GenericHID.RumbleType.kRightRumble, 0.0);
        }
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
    public static enum Axis {
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

    /**
     * An enum to rumble the left, right or both sides of a controller
     */
    public enum RumbleType {
        LEFT,
        RIGHT,
        BOTH
    }

    /**
     * A class representing a button on a WPILibLogitechF310 gamepad, used for binding commands to buttons
     */
    public static class LogitechTrigger implements TriggerSource {
        private LogitechF310 joystick;
        private Button button;

        public LogitechTrigger(LogitechF310 joystick, Button button) {
            this.joystick = joystick;
            this.button = button;
        }

        @Override
        public boolean getActive() {
            return joystick.isButtonDown(button);
        }
    }

    public static class Config {
        public int index;
        public double leftTriggerDeadzone;
        public double rightTriggerDeadzone;
    }
}
