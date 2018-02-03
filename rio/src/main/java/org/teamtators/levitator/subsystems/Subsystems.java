package org.teamtators.levitator.subsystems;

import org.teamtators.common.SubsystemsBase;
import org.teamtators.common.config.ConfigLoader;
import org.teamtators.common.control.Updatable;
import org.teamtators.common.scheduler.Subsystem;

import java.util.Arrays;
import java.util.List;

public class Subsystems extends SubsystemsBase {

    private List<Subsystem> subsystems;

    private Drive drive;
    private Picker picker;
    private Lift lift;
    //private YourSubsytem yourSubsystem;

    public Subsystems() {
        drive = new Drive();
        picker = new Picker();
        lift = new Lift();

        //your subsystems here


        subsystems = Arrays.asList(drive, picker, lift /*, yourSubsystem */);
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
        return null;
    }

    public Drive getDrive() {
        return drive;
    }

    public Picker getPicker() {
        return picker;
    }
}
