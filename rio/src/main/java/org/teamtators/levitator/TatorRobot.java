package org.teamtators.levitator;

import org.teamtators.common.SubsystemsBase;
import org.teamtators.common.TatorRobotBase;
import org.teamtators.common.config.ConfigCommandStore;
import org.teamtators.common.controllers.LogitechF310;
import org.teamtators.common.scheduler.Command;
import org.teamtators.levitator.commands.CommandRegistrar;
import org.teamtators.levitator.subsystems.Subsystems;

import java.util.Arrays;

public class TatorRobot extends TatorRobotBase {
    private final CommandRegistrar registrar = new CommandRegistrar(this);

    private Subsystems subsystems;

    public TatorRobot(String configDir) {
        super(configDir);

        subsystems = new Subsystems(this);
    }

    @Override
    public String getRobotName() {
        return "LeviTator";
    }

    public Subsystems getSubsystems() {
        return subsystems;
    }

    @Override
    public SubsystemsBase getSubsystemsBase() {
        return subsystems;
    }

    @Override
    protected void registerCommands(ConfigCommandStore commandStore) {
        super.registerCommands(commandStore);
        registrar.register(commandStore);
    }

    @Override
    protected Command getAutoCommand() {
        return getSubsystems().getAuto().getSelectedCommand();
    }

    @Override
    public String getName() {
        return "LeviTator";
    }
}
