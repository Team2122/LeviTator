package org.teamtators.levitator;

import org.teamtators.common.SubsystemsBase;
import org.teamtators.common.TatorRobotBase;
import org.teamtators.common.hw.LogitechF310;
import org.teamtators.levitator.commands.CommandRegistrar;
import org.teamtators.levitator.subsystems.Subsystems;

public class TatorRobot extends TatorRobotBase {
    private final CommandRegistrar registrar = new CommandRegistrar(this);

    private Subsystems subsystems;

    private LogitechF310 driver;

    private LogitechF310 gunner;

    public TatorRobot(String configDir) {
        super(configDir);

        subsystems = new Subsystems();
    }

    public Subsystems getSubsystems() {
        return subsystems;
    }

    @Override
    public SubsystemsBase getSubsystemsBase() {
        return subsystems;
    }

    @Override
    protected void registerCommands() {
        registrar.register(getCommandStore());
    }

    @Override
    protected LogitechF310 getGunnerJoystick() {
        return driver;
    }

    @Override
    protected LogitechF310 getDriverJoystick() {
        return gunner;
    }

    @Override
    public String getName() {
        return "LeviTator";
    }

    @Override
    protected void configureSubsystems() {
        super.configureSubsystems();
        this.driver = subsystems.getOI().getDriverJoystick();
        this.gunner = subsystems.getOI().getGunnerJoystick();
    }
}
