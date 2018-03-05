package org.teamtators.levitator.subsystems;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.teamtators.common.SubsystemsBase;
import org.teamtators.common.config.ConfigException;
import org.teamtators.common.config.ConfigLoader;
import org.teamtators.common.config.Configurable;
import org.teamtators.common.config.Deconfigurable;
import org.teamtators.common.control.Updatable;
import org.teamtators.common.controllers.Controller;
import org.teamtators.common.controllers.LogitechF310;
import org.teamtators.common.scheduler.Subsystem;
import org.teamtators.levitator.TatorRobot;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Subsystems extends SubsystemsBase
        implements Configurable<Subsystems.Config>, Deconfigurable {
    private static final String SUBSYSTEMS_CONFIG_FILE = "Subsystems.yaml";
    private final List<Subsystem> subsystems;
    private final List<Updatable> updatables;
    private final List<Updatable> motorUpdatables;

    private OperatorInterface oi;
    private Drive drive;
    private Picker picker;
    private Lift lift;
    private Auto auto;
    private Vision vision;
    private Elevators elevators;
    //private YourSubsystem yourSubsystem;

    public Subsystems(TatorRobot robot) {
        oi = new OperatorInterface();
        drive = new Drive();
        picker = new Picker();
        lift = new Lift();
        auto = new Auto(robot);
        vision = new Vision(drive);
        elevators = new Elevators();
        //your subsystems here
        subsystems = Arrays.asList(oi, drive, picker, lift, auto, vision, elevators /*, yourSubsystem */);

        updatables = new ArrayList<>();
        motorUpdatables = new ArrayList<>();
    }

    @Override
    public List<Subsystem> getSubsystemList() {
        return subsystems;
    }

    @Override
    public List<Updatable> getUpdatables() {
        return updatables;
    }

    @Override
    public List<Updatable> getMotorUpdatables() {
        return motorUpdatables;
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

    @Override
    public void configure(Config config) {
        TatorRobot.logger.trace("Configuring subsystems...");
        oi.configure(config.operatorInterface);
        drive.configure(config.drive);
        picker.configure(config.picker);
        lift.configure(config.lift);
        auto.configure(config.auto);
        vision.configure(config.vision);
        elevators.configure(config.elevators);

        updatables.addAll(drive.getUpdatables());
        updatables.addAll(lift.getUpdatables());

        motorUpdatables.addAll(picker.getMotorUpdatables());
        motorUpdatables.addAll(lift.getMotorUpdatables());
        motorUpdatables.addAll(elevators.getMotorUpdatables());
    }

    @Override
    public void deconfigure() {
        TatorRobot.logger.trace("Deconfiguring subsystems...");
        oi.deconfigure();
        drive.deconfigure();
        picker.deconfigure();
        lift.deconfigure();
        auto.deconfigure();
        vision.deconfigure();
        elevators.deconfigure();

        updatables.clear();
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

    public Elevators getElevators() {
        return elevators;
    }

    @Override
    public List<Controller<?, ?>> getControllers() {
        return oi.getAllControllers();
    }

    @Override
    public LogitechF310 getTestModeController() {
        return oi.getDriverJoystick();
    }

    public Auto getAuto() {
        return auto;
    }

    public Vision getVision() {
        return vision;
    }

    public static class Config {
        public OperatorInterface.Config operatorInterface;
        public Drive.Config drive;
        public Picker.Config picker;
        public Lift.Config lift;
        public Auto.Config auto;
        public Vision.Config vision;
        public Elevators.Config elevators;
    }
}
