package org.teamtators.levitator;

import org.teamtators.common.SubsystemsBase;
import org.teamtators.common.TatorRobotBase;
import org.teamtators.common.hw.LogitechF310;
import org.teamtators.levitator.commands.CommandRegistrar;
import org.teamtators.levitator.subsystems.Subsystems;

import org.teamtators.pixyjava.*;

public class TatorRobot extends TatorRobotBase {
    private final CommandRegistrar registrar = new CommandRegistrar(this);

    private Subsystems subsystems;

    public TatorRobot(String configDir) {
        super(configDir);

        subsystems = new Subsystems();
    }

    @Override
    public void initialize() {
        super.initialize();

        System.loadLibrary("pixyjava");

        int result = PixyUSB.pixy_init();
        if (result != 0) {
            logger.error(String.format("pixy_init failed: %d", result));
            return;
        } else {
        }
        int[] major = new int[1];
        int[] minor = new int[1];
        int[] build = new int[1];
        PixyUSB.pixy_get_firmware_version(major, minor, build);
        logger.info(String.format("pixy firmware version: %d.%d.%d",
                major[0], minor[0], build[0]));
        logger.info("Pixy camera initialized");
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
        return getSubsystems().getOI().getGunnerJoystick();
    }

    @Override
    protected LogitechF310 getDriverJoystick() {
        return getSubsystems().getOI().getDriverJoystick();
    }

    @Override
    public String getName() {
        return "LeviTator";
    }
}
