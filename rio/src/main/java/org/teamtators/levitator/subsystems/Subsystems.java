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
    private Pivot pivot;
    private Auto auto;
    private Vision vision;
    private Climber climber;

    public Subsystems(TatorRobot robot) {
        oi = new OperatorInterface();
        drive = new Drive();
        picker = new Picker();
        lift = new Lift();
        pivot = new Pivot();
        lift.linkTo(pivot);
        auto = new Auto(robot);
        vision = new Vision(drive);
        climber = new Climber(robot);
        subsystems = Arrays.asList(oi, drive, picker, lift, pivot, auto, vision, climber);

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
        pivot.configure(config.pivot);
        auto.configure(config.auto);
        vision.configure(config.vision);
        climber.configure(config.climber);

        updatables.addAll(drive.getUpdatables());
        updatables.addAll(pivot.getUpdatables());
        updatables.addAll(lift.getUpdatables());

        motorUpdatables.addAll(picker.getMotorUpdatables());
        motorUpdatables.addAll(lift.getMotorUpdatables());
        motorUpdatables.addAll(pivot.getMotorUpdatables());
        motorUpdatables.addAll(climber.getMotorUpdatables());
    }

    @Override
    public void deconfigure() {
        TatorRobot.logger.trace("Deconfiguring subsystems...");
        oi.deconfigure();
        drive.deconfigure();
        picker.deconfigure();
        lift.deconfigure();
        pivot.deconfigure();
        auto.deconfigure();
        vision.deconfigure();
        climber.deconfigure();

        updatables.clear();
        motorUpdatables.clear();
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

    public Pivot getPivot() {
        return pivot;
    }

    public OperatorInterface getOI() {
        return oi;
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

    public Climber getClimber() {return climber;}

    public static class Config {
        public OperatorInterface.Config operatorInterface;
        public Drive.Config drive;
        public Picker.Config picker;
        public Lift.Config lift;
        public Pivot.Config pivot;
        public Auto.Config auto;
        public Vision.Config vision;
        public Climber.Config climber;
    }
}
