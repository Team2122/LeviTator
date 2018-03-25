package org.teamtators.common.controllers;

import com.google.common.util.concurrent.AtomicDouble;
import edu.wpi.first.wpilibj.DriverStation;
import edu.wpi.first.wpilibj.GenericHID;
import edu.wpi.first.wpilibj.Joystick;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.teamtators.common.config.Configurable;
import org.teamtators.common.config.Deconfigurable;
import org.teamtators.common.control.Timer;
import org.teamtators.common.control.Updatable;
import org.teamtators.common.scheduler.TriggerSource;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Alex Mikhalev
 */
public abstract class ControllerBase<TButton, TAxis, TConfig extends ControllerBase.Config>
        implements Controller<TButton, TAxis>, Configurable<TConfig>, Deconfigurable {
    protected final Logger logger = LoggerFactory.getLogger(getClass());

    private String name;
    private GenericHID hid = null;
    private DriverStation driverStation = DriverStation.getInstance();

    private final AtomicInteger buttonsState = new AtomicInteger(0);
    private final AtomicInteger povState = new AtomicInteger(0);
    private final List<AtomicDouble> axisStates;

    private final AtomicDouble leftRumble = new AtomicDouble(0);
    private final AtomicDouble rightRumble = new AtomicDouble(0);
    private final AtomicInteger outputs = new AtomicInteger(0);

    private int lastAxisCount = -1;
    private int lastButtonCount = -1;

    private Timer rumbleTimer = new Timer();
    private double rumbleTime;
    private int axisCount = 0;
    private int buttonCount = 0;

    public ControllerBase(String name, GenericHID hid) {
        this.name = name;
        this.hid = hid;

        axisStates = Collections.synchronizedList(new ArrayList<>(getAxisCount()));
    }

    public void reset() {
        buttonsState.set(0);
        povState.set(0);
        for (AtomicDouble axisState : axisStates) {
            axisState.set(0);
        }
        leftRumble.set(0);
        rightRumble.set(0);
        outputs.set(0);
        lastAxisCount = 0;
        lastButtonCount = 0;
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
        if (axis >= axisStates.size()) {
            logger.warn("Invalid axis number: {}", axis);
            return 0.0;
        }
        return axisStates.get(axis).get();
    }

    @Override
    public boolean isRawButtonDown(int button) {
        if (button > getButtonCount()) {
            logger.warn("Invalid button number: {}", button);
            return false;
        }
        return (buttonsState.get() & (1 << (button - 1))) != 0;
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
        return povState.get();
    }

    @Override
    public void setOutput(int outputNumber, boolean value) {
        this.outputs.getAndUpdate(outputs -> {
            if (value) {
                return outputs | (1 << (outputNumber - 1));
            } else {
                return outputs & ~(1 << (outputNumber - 1));
            }
        });
    }

    @Override
    public void setOutputs(int outputs) {
        this.outputs.set(outputs);
    }

    @Override
    public void setRumble(RumbleType type, double value) {
        if (type == RumbleType.LEFT || type == RumbleType.BOTH) {
            leftRumble.set(value);
        }
        if (type == RumbleType.RIGHT || type == RumbleType.BOTH) {
            rightRumble.set(value);
        }
    }

    public void setRumbleTime(RumbleType rumbleType, double value, double time) {
        rumbleTime = time;
        rumbleTimer.restart();
        setRumble(rumbleType, value);
    }

    protected final void setAxisCount(int axisCount) {
        this.axisCount = axisCount;
        axisStates.clear();
        for (int i = 0; i < axisCount; i++) {
            axisStates.add(new AtomicDouble(0.0));
        }
    }

    protected final void setButtonCount(int buttonCount) {
        this.buttonCount = buttonCount;
    }

    @Override
    public final int getAxisCount() {
        return axisCount;
    }

    @Override
    public final int getButtonCount() {
        return buttonCount;
    }

    @Override
    public void configure(TConfig config) {
        this.hid = new Joystick(config.port);
    }

    @Override
    public void deconfigure() {
        this.hid = null;
    }

    @Override
    public void onDriverStationData() {
        int axisCount = hid.getAxisCount();
        int buttonCount = hid.getButtonCount();
        if (lastAxisCount != axisCount ||
                lastButtonCount != buttonCount) {
            lastAxisCount = axisCount;
            lastButtonCount = buttonCount;
            if (axisCount < getAxisCount()) {
                logger.error("Joystick {} does not have enough axes ({} < {})",
                        hid.getPort(), axisCount, getAxisCount());
            }
            if (buttonCount < getButtonCount()) {
                logger.error("Joystick {} does not have enough buttons ({} < {})",
                        hid.getPort(), buttonCount, getButtonCount());
            }
        }
        buttonsState.set(driverStation.getStickButtons(hid.getPort()));
        if (hid.getPOVCount() > 0) {
            povState.set(hid.getPOV());
        }
        for (int i = 0; i < axisCount && i < axisStates.size(); i++) {
            axisStates.get(i).set(hid.getRawAxis(i));
        }
        if (rumbleTimer.hasPeriodElapsed(rumbleTime)) {
            setRumble(RumbleType.BOTH, 0.0);
        }
        hid.setRumble(GenericHID.RumbleType.kLeftRumble, leftRumble.get());
        hid.setRumble(GenericHID.RumbleType.kRightRumble, rightRumble.get());
        hid.setOutputs(outputs.get());
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
