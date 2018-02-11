package org.teamtators.levitator.subsystems;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.teamtators.common.SubsystemsBase;
import org.teamtators.common.config.ConfigException;
import org.teamtators.common.config.ConfigLoader;
import org.teamtators.common.control.PidController;
import org.teamtators.common.control.Updatable;
import org.teamtators.common.scheduler.Subsystem;
import org.teamtators.levitator.TatorRobot;

import java.util.Arrays;
import java.util.List;

public class Subsystems extends SubsystemsBase {
    private static final String SUBSYSTEMS_CONFIG_FILE = "Subsystems.yaml";
    private final List<Subsystem> subsystems;
    private final List<Updatable> controllers;

    private OperatorInterface oi;
    private Drive drive;
    private Picker picker;
    private Lift lift;
    //private YourSubsystem yourSubsystem;

    public Subsystems() {
        oi = new OperatorInterface();
        drive = new Drive();
        picker = new Picker();
        lift = new Lift();

        //your subsystems here
        subsystems = Arrays.asList(oi, drive, picker, lift /*, yourSubsystem */);
        controllers = Arrays.asList(
                lift.getPivotController()
        );
    }

    @Override
    public List<Subsystem> getSubsystemList() {
        return subsystems;
    }

    @Override
    public List<Updatable> getControllers() {
        return controllers;
    }

    @Override
    public void configure(ConfigLoader configLoader) {
        try {
            ObjectNode configNode = (ObjectNode) configLoader.load(SUBSYSTEMS_CONFIG_FILE);
            Config configObj = configLoader.getObjectMapper().treeToValue(configNode, Config.class);
            configure(configObj);
        } catch (Throwable e) {
            throw new ConfigException("Error configuring subsystems: ", e);
        }
    }

    public void configure(Config config) {
        TatorRobot.logger.trace("Configuring subsystems...");
        oi.configure(config.operatorInterface);
        drive.configure(config.drive);
        picker.configure(config.picker);
        lift.configure(config.lift);
    }

    public Drive getDrive() {
        return drive;
    }

    public Picker getPicker() {
        return picker;
    }

    public Lift getLift() {
        return lift;
    }

    public OperatorInterface getOI() {
        return oi;
    }

    public static class Config {
        public OperatorInterface.Config operatorInterface;
        public Drive.Config drive;
        public Picker.Config picker;
        public Lift.Config lift;
    }
}
