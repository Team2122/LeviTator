package org.teamtators.levitator.commands;

import org.teamtators.common.config.Configurable;
import org.teamtators.common.control.Timer;
import org.teamtators.common.scheduler.Command;
import org.teamtators.common.util.FieldSide;
import org.teamtators.levitator.TatorRobot;
import org.teamtators.levitator.subsystems.Auto;

public class AutoSelector extends Command implements Configurable<AutoSelector.Config> {
    private final Auto auto;
    private final TatorRobot robot;
    private SelectorType type;
    private Config config;
    private Command selected;
    private boolean hasStarted;
    private Timer waitForChildTimer = new Timer();

    public AutoSelector(TatorRobot robot) {
        super("AutoSelector");
        this.robot = robot;
        this.auto = robot.getSubsystems().getAuto();
    }

    @Override
    protected void initialize() {
        super.initialize();
        selected = null;
        hasStarted = false;
    }

    @Override
    public void configure(Config config) {
        this.type = config.type;
        this.config = config;
        waitForChildTimer.restart();
    }

    @Override
    protected boolean step() {
        if (selected != null) {
            if (!hasStarted) {
                try {
                    logger.info("Starting chosen command: {}", selected.getName());
                    startWithContext(selected, this);
                    hasStarted = true;
                } catch (IllegalArgumentException e) {
                    logger.warn("Chosen command not found", e);
                    return true;
                }
            }
            return !selected.isRunning() && waitForChildTimer.hasPeriodElapsed(10);
        }

        String toStart = "$NoAuto";
        if (type == SelectorType.FIELD_CONFIGURATION) {
            FieldSide side = auto.getFieldConfiguration(config.object);
            switch (side) {
                case LEFT:
                    toStart = config.L;
                    break;
                case RIGHT:
                    toStart = config.R;
                    break;
            }
        } else if (type == SelectorType.STARTING_POSITION) {
            String startedAt = auto.getStartingPosition().toLowerCase();
            if (startedAt.equals("left")) {
                toStart = config.left;
            }
            if (startedAt.equals("center")) {
                toStart = config.center;
            }
            if (startedAt.equals("right")) {
                toStart = config.right;
            }
        }
        selected = robot.getCommandStore().getCommand(toStart);
        return false;
    }

    public static class Config {
        public SelectorType type;

        //start pos
        public String left;
        public String center;
        public String right;
        //field config
        public int object;
        public String L;
        public String R;

    }

    public enum SelectorType {
        STARTING_POSITION,
        FIELD_CONFIGURATION
    }
}
