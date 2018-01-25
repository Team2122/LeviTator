package org.teamtators.common.scheduler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.teamtators.common.control.Updatable;
import org.teamtators.common.tester.AutomatedTest;
import org.teamtators.common.tester.AutomatedTestable;
import org.teamtators.common.tester.ManualTestGroup;
import org.teamtators.common.tester.ManualTestable;

import java.util.ArrayList;
import java.util.List;

public class Subsystem implements Updatable, RobotStateListener, ManualTestable, AutomatedTestable {
    protected final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final String name;

    private Command requiringCommand = null;

    public Subsystem(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    Command getRequiringCommand() {
        return requiringCommand;
    }

    void setRequiringCommand(Command requiringCommand) {
        this.requiringCommand = requiringCommand;
    }

    @Override
    public void onEnterRobotState(RobotState state) {
    }

    @Override
    public void update(double delta) {
    }

    /**
     * Get tests for this subsystem and its hardware
     *
     * @return A test group with this subsystem's tests
     */
    @Override
    public ManualTestGroup createManualTests() {
        // create an empty group by default
        return new ManualTestGroup(getName());
    }

    /**
     * Get automated tests for this subsystem's hardware and possible the subsystem itself
     *
     * @return A list of automated tests for this subsystem
     */
    @Override
    public List<AutomatedTest> createAutomatedTests() {
        return new ArrayList<>();
    }
}
