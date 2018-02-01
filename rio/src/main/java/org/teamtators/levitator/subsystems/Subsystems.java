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
    //private YourSubsytem yourSubsystem;

    public Subsystems() {
        drive = new Drive();

        //your subsystems here


        subsystems = Arrays.asList(drive /*, yourSubsystem */);
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
}
