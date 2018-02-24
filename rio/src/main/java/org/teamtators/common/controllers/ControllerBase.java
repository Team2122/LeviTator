package org.teamtators.common.controllers;

import edu.wpi.first.wpilibj.GenericHID;
import edu.wpi.first.wpilibj.Joystick;
import org.teamtators.common.config.Configurable;
import org.teamtators.common.config.Deconfigurable;
import org.teamtators.common.control.Timer;
import org.teamtators.common.control.Updatable;
import org.teamtators.common.scheduler.TriggerSource;

/**
 * @author Alex Mikhalev
 */
public abstract class ControllerBase<TButton, TAxis, TConfig extends ControllerBase.Config>
        implements Controller<TButton, TAxis>, Updatable, Configurable<TConfig>, Deconfigurable {
    private String name;
    private GenericHID hid = null;

    private Timer rumbleTimer = new Timer();
    private double rumbleTime;

    public ControllerBase(String name, GenericHID hid) {
        this.name = name;
        this.hid = hid;
    }

    public ControllerBase(String name, int port) {
        this(name, new Joystick(port));
    }

    public ControllerBase(String name) {
        this(name, null);
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public double getRawAxisValue(int axis) {
        return hid.getRawAxis(axis);
    }

    @Override
    public boolean isRawButtonDown(int button) {
        return hid.getRawButton(button);
    }

    @Override
    public boolean isRawButtonPressed(int button) {
        return hid.getRawButtonPressed(button);
    }

    @Override
    public boolean isRawButtonReleased(int button) {
        return hid.getRawButtonReleased(button);
    }

    @Override
    public TriggerSource getRawTriggerSource(int button) {
        return new ControllerTrigger(button);
    }

    @Override
    public int getPov(int pov) {
        return hid.getPOV(pov);
    }

    @Override
    public void setOutput(int outputNumber, boolean value) {
        hid.setOutput(outputNumber, value);
    }

    @Override
    public void setOutputs(int outputs) {
        hid.setOutputs(outputs);
    }

    @Override
    public void setRumble(RumbleType type, double value) {
        if (type == RumbleType.LEFT || type == RumbleType.BOTH) {
            hid.setRumble(GenericHID.RumbleType.kLeftRumble, value);
        }
        if (type == RumbleType.RIGHT || type == RumbleType.BOTH) {
            hid.setRumble(GenericHID.RumbleType.kRightRumble, value);
        }
    }

    public void setRumbleTime(RumbleType rumbleType, double value, double time) {
        rumbleTime = time;
        rumbleTimer.restart();
        setRumble(rumbleType, value);
    }

    @Override
    public void update(double delta) {
        if (rumbleTimer.hasPeriodElapsed(rumbleTime)) {
            setRumble(RumbleType.BOTH, 0.0);
        }
    }

    @Override
    public void configure(TConfig config) {
        this.hid = new Joystick(config.port);
    }

    public static class Config {
        public int port;
    }

    public class ControllerTrigger implements TriggerSource {
        private int button;

        public ControllerTrigger(int button) {
            this.button = button;
        }

        @Override
        public boolean getActive() {
            return ControllerBase.this.isRawButtonDown(button);
        }
    }
}
