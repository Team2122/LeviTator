package org.teamtators.common;

import org.teamtators.common.config.ConfigLoader;
import org.teamtators.common.config.Deconfigurable;
import org.teamtators.common.control.Updatable;
import org.teamtators.common.controllers.Controller;
import org.teamtators.common.controllers.LogitechF310;
import org.teamtators.common.scheduler.Subsystem;

import java.util.List;

/**
 * Created by TatorsDriverStation on 10/1/2017.
 */
public abstract class SubsystemsBase implements Deconfigurable {
    public abstract List<Subsystem> getSubsystemList();

    public abstract void configure(ConfigLoader configLoader);

    public abstract List<Updatable> getUpdatables();

    public abstract List<Updatable> getMotorUpdatables();

    public abstract List<Controller<?, ?>> getControllers();

    public abstract LogitechF310 getTestModeController();
}
