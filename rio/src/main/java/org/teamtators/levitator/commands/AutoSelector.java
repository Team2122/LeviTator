package org.teamtators.levitator.commands;

import org.teamtators.common.config.ConfigCommandStore;
import org.teamtators.common.config.Configurable;
import org.teamtators.common.control.Timer;
import org.teamtators.common.scheduler.Command;
import org.teamtators.common.scheduler.RobotState;
import org.teamtators.common.util.FieldSide;
import org.teamtators.levitator.TatorRobot;
import org.teamtators.levitator.subsystems.Auto;

import java.util.ArrayList;
import java.util.List;

public class AutoSelector extends Command implements Configurable<AutoSelector.Config> {
    private final Auto auto;
    private final TatorRobot robot;
    private SelectorType type;
    private Config config;
    private Command selected;
    private boolean hasStarted;
    private ConfigCommandStore commandStore;

    public AutoSelector(TatorRobot robot) {
        super("AutoSelector");
        this.robot = robot;
        this.auto = robot.getSubsystems().getAuto();
        commandStore = robot.getCommandStore();
        validIn(RobotState.AUTONOMOUS);
    }

    @Override
    protected void initialize() {
        hasStarted = false;

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
        try {
            selected = commandStore.getCommand(toStart);
            logger.info("Starting chosen command: {}", selected.getName());
            startWithContext(selected, this);
        } catch (IllegalArgumentException e) {
            logger.warn("Chosen command not found", e);
            selected = null;
        }
    }

    @Override
    protected boolean step() {
        if (selected != null) {
            if (selected.isRunning()) {
                hasStarted = true;
            }
            return hasStarted && !selected.isRunning();
        }
        return true;
    }

    @Override
    public void configure(Config config) {
        this.type = config.type;
        this.config = config;
        updateRequirements();
    }

    @Override
    public void updateRequirements() {
        if (config == null) {
            return;
        }
        List<String> commandNames = new ArrayList<>();
        if (config.type == SelectorType.FIELD_CONFIGURATION) {
            commandNames.add(config.left);
            commandNames.add(config.right);
            commandNames.add(config.center);
        } else if (config.type == SelectorType.STARTING_POSITION) {
            commandNames.add(config.L);
            commandNames.add(config.R);
        } else {
            logger.error("Invalid selector type: " + config.type);
        }
        for (String commandName : commandNames) {
            Command command;
            try {
                command = commandStore.getCommand(commandName);
            } catch (IllegalArgumentException e) {
                continue;
            }
            requiresAll(command.getRequirements());
        }
    }

    public enum SelectorType {
        STARTING_POSITION,
        FIELD_CONFIGURATION
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
}
