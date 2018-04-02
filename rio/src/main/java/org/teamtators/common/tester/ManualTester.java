package org.teamtators.common.tester;

import org.teamtators.common.control.Timer;
import org.teamtators.common.controllers.LogitechF310;
import org.teamtators.common.scheduler.Command;
import org.teamtators.common.scheduler.RobotState;
import org.teamtators.common.util.JoystickModifiers;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * A class that allows manually running ManualTest's
 */
@SuppressWarnings("unused")
public class ManualTester extends Command {
    private static final LogitechF310.Axis TEST_AXIS = LogitechF310.Axis.RIGHT_STICK_Y;
    private static final double DEAD_ZONE = 0.05;
    private static final double EXPONENT = 2.0;
    private int testGroupIndex = 0;
    private int testIndex = 0;
    private Timer timer = new Timer();

    private LogitechF310 joystick;

    private Map<LogitechF310.Button, Boolean> lastStates = new EnumMap<>(LogitechF310.Button.class);
    private List<ManualTestGroup> testGroups = new ArrayList<>();

    public ManualTester() {
        super("ManualTester");
        validIn(RobotState.TEST);
    }

    public void setJoystick(LogitechF310 joystick) {
        this.joystick = joystick;
    }

    @Override
    public void initialize() {
        logger.info("==> Starting Manual Tester <==");
        logger.trace("ManualTester has " + testGroups.size() + " groups and is currently at " + testGroupIndex + ", " + testIndex);
        if (joystick == null) {
            logger.error("Joystick must be set before using ManualTester");
            this.cancel();
        }
        lastStates.clear();
        beginTestGroup(testGroupIndex, testIndex);
        timer.start();
    }

    @Override
    public boolean step() {
        double delta = timer.restart();
        ManualTest test = getCurrentTest();
        if (test != null) {
            double axisValue = -joystick.getAxisValue(TEST_AXIS);
            axisValue = JoystickModifiers.applyDriveModifiers(axisValue, DEAD_ZONE, EXPONENT);
            test.updateAxis(axisValue);
        }
        for (LogitechF310.Button button : LogitechF310.Button.values()) {
            boolean value = joystick.isButtonDown(button);
            Boolean lastValue = lastStates.get(button);
            if (lastValue == null) lastValue = false;
            if (value && !lastValue) {
                switch (button) {
                    case POV_DOWN:
                        nextTestGroup();
                        break;
                    case POV_UP:
                        previousTestGroup();
                        break;
                    case POV_RIGHT:
                        nextTest();
                        break;
                    case POV_LEFT:
                        previousTest();
                        break;
                    default:
                        if (test != null) {
                            test.onButtonDown(button);
                        }
                        break;
                }
            } else if (lastValue && !value) {
                if (test != null) {
                    test.onButtonUp(button);
                }
            }
            lastStates.put(button, value);
        }
        if (test != null) {
            test.update(delta);
        }
        return false;
    }

    private ManualTest getCurrentTest() {
        ManualTestGroup group = getCurrentTestGroup();
        if (group == null) return null;
        if (testIndex >= group.getTests().size()) return null;
        return group.getTests().get(testIndex);
    }

    private ManualTestGroup getCurrentTestGroup() {
        if (testGroupIndex >= testGroups.size()) return null;
        return testGroups.get(testGroupIndex);
    }

    @Override
    protected void finish(boolean interrupted) {
        ManualTest currentTest = getCurrentTest();
        if (currentTest != null)
            currentTest.stop();
        logger.debug("--> ManualTester finished <--");
    }

    private void stopTest() {
        ManualTest test = getCurrentTest();
        if (test != null)
            test.stop();
    }

    private void beginTest(int index) {
        if (getCurrentTestGroup() == null) return;
        stopTest();
        testIndex = index;
        startTest();
    }

    private void nextTest() {
        if (getCurrentTestGroup() == null) return;
        int newTestIndex = testIndex + 1;
        if (newTestIndex >= getCurrentTestGroup().getTests().size())
            newTestIndex = 0;
        beginTest(newTestIndex);
    }

    private void previousTest() {
        if (getCurrentTestGroup() == null) return;
        int newTestIndex = testIndex - 1;
        if (getCurrentTestGroup().getTests().size() == 0)
            newTestIndex = 0;
        else if (newTestIndex < 0)
            newTestIndex = getCurrentTestGroup().getTests().size() - 1;
        beginTest(newTestIndex);
    }

    private void beginTestGroup(int index) {
        beginTestGroup(index, 0);
    }

    private void beginTestGroup(int groupIndex, int testIndex) {
        testGroupIndex = groupIndex;
        if (getCurrentTestGroup() == null) {
            logger.info("--> There are no test groups <--");
            return;
        }
        logger.info("--> Entering Test Group '{}' <--", getCurrentTestGroup().getName());
        beginTest(testIndex);
    }

    private void nextTestGroup() {
        if (testGroups.isEmpty()) return;
        int nextGroupIndex = testGroupIndex + 1;
        if (nextGroupIndex >= testGroups.size())
            nextGroupIndex = 0;
        beginTestGroup(nextGroupIndex);
    }

    private void previousTestGroup() {
        if (testGroups.isEmpty()) return;
        int nextGroupIndex = testGroupIndex - 1;
        if (nextGroupIndex < 0)
            nextGroupIndex = testGroups.size() - 1;
        beginTestGroup(nextGroupIndex);
    }

    private void startTest() {
        ManualTestGroup group = getCurrentTestGroup();
        if (group == null) {
            logger.info("-> There are no test groups! <-");
            return;
        }
        ManualTest test = getCurrentTest();
        if (test == null) {
            logger.info("-> Test group '{}' is empty! <-", group.getName());
            return;
        }
        logger.info("-> Testing '{}' <-", test.getName());
        test.start();
    }

    /**
     * Register a new test group
     *
     * @param group the test group to register
     */
    public void registerTestGroup(ManualTestGroup group) {
        testGroups.add(group);
    }

    public void unregisterTestGroup(ManualTestGroup group) {
        testGroups.remove(group);
    }

    public void clearTestGroups() {
        testGroups.clear();
    }
}
