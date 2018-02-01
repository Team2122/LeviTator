package org.teamtators.levitator;

import org.teamtators.common.SubsystemsBase;
import org.teamtators.common.TatorRobotBase;
import org.teamtators.common.hw.LogitechF310;
import org.teamtators.levitator.subsystems.Subsystems;

public class TatorRobot extends TatorRobotBase {

    private Subsystems subsystems;

    public TatorRobot(String configDir) {
        super(configDir);

        subsystems = new Subsystems();
    }

    @Override
    public SubsystemsBase getSubsystemsBase() {
        return subsystems;
    }

    @Override
    protected void registerCommands() {

    }

    @Override
    protected LogitechF310 getGunnerJoystick() {
        return null;
    }

    @Override
    protected LogitechF310 getDriverJoystick() {
        return null;
    }

    @Override
    public String getName() {
        return null;
    }
}
