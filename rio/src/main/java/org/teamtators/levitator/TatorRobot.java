package org.teamtators.levitator;

import org.teamtators.common.SubsystemsBase;
import org.teamtators.common.TatorRobotBase;
import org.teamtators.common.hw.LogitechF310;

public class TatorRobot extends TatorRobotBase {
    public TatorRobot(String configDir) {
        super(configDir);
    }

    @Override
    public SubsystemsBase getSubsystemsBase() {
        return null;
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
