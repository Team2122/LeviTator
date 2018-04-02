package org.teamtators.levitator.commands;

import org.teamtators.common.config.ConfigCommandStore;
import org.teamtators.common.config.Configurable;
import org.teamtators.common.scheduler.Command;
import org.teamtators.common.scheduler.RobotState;
import org.teamtators.common.util.FieldSide;
import org.teamtators.levitator.TatorRobot;
import org.teamtators.levitator.subsystems.Auto;

import java.util.ArrayList;
import java.util.List;

public class AutoSelector extends Command implements Configurable<AutoSelector.Config> {
    private final Auto auto;
    private SelectorType type;
    private Config config;
    private Command selected;
    private ConfigCommandStore commandStore;
    private boolean initialized;
    private boolean cancel;

    AutoSelector(TatorRobot robot) {
        super("AutoSelector");
        this.auto = robot.getSubsystems().getAuto();
        commandStore = robot.getCommandStore();
        validIn(RobotState.AUTONOMOUS);
    }

    @Override
    protected void initialize() {
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
        initialized = false;
        cancel = false;
        try {
            selected = commandStore.getCommand(toStart);
            logger.info("Running chosen command: {}", selected.getName());
        } catch (IllegalArgumentException e) {
            logger.warn("Chosen command not found", e);
            selected = null;
        }
    }

    @Override
    public boolean step() {
        if (selected == null) {
            return true;
        }
        if (cancel) {
            selected.finishRun(true);
            this.cancel();
            return true;
        }
        if (!initialized) {
            releaseRequirements(selected.getRequirements());
            if (selected.isRunning()) {
                if (selected.getContext() == this && selected.checkRequirements()) {
                    logger.trace("Command was already initialized");
                    initialized = true;
                } else {
                    logger.trace("Command was already running, canceling");
                    selected.cancel();
                    return false;
                }
            } else if (selected.startRun(this)) {
                logger.trace("Command initialized");
                initialized = true;
            }
        }
        if (initialized) {
            boolean finished = selected.step();
            if (finished) {
                selected.finishRun(false);
                return true;
            }
        } else {
            logger.trace("Command was not initialized");
        }
        if (cancel) {
            selected.finishRun(true);
            this.cancel();
            return true;
        }
        return false;
    }

    @Override
    protected void finish(boolean interrupted) {
        super.finish(interrupted);
        if (interrupted && selected != null && selected.isRunning()) {
            selected.finishRun(true);
        }
    }

    @Override
    public void cancelCommand(Command command) {
        if (command == selected) {
            cancel = true;
        } else {
            super.cancelCommand(command);
        }
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
        switch (config.type) {
            case FIELD_CONFIGURATION:
                commandNames.add(config.left);
                commandNames.add(config.right);
                commandNames.add(config.center);
                break;
            case STARTING_POSITION:
                commandNames.add(config.L);
                commandNames.add(config.R);
                break;
            default:
                logger.error("Invalid selector type: " + config.type);
                break;
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

    @SuppressWarnings("WeakerAccess")
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
