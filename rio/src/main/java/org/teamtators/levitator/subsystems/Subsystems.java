package org.teamtators.levitator.subsystems;

import org.teamtators.common.SubsystemsBase;
import org.teamtators.common.config.ConfigLoader;
import org.teamtators.common.control.Updatable;
import org.teamtators.common.scheduler.Subsystem;

import java.util.Arrays;
import java.util.List;

public class Subsystems extends SubsystemsBase {

    private List<Subsystem> subsystems;

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
    }


    @Override
    public List<Subsystem> getSubsystemList() {
        return subsystems;
    }

    @Override
    public void configure(ConfigLoader configLoader) {

    }

    @Override
    public List<Updatable> getControllers() {
        return Arrays.asList();
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
}
