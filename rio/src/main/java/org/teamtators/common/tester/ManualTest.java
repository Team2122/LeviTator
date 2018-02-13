package org.teamtators.common.tester;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.teamtators.common.control.Updatable;
import org.teamtators.common.controllers.LogitechF310;

/**
 * Class to represent a component test
 */
public abstract class ManualTest implements Updatable {
    protected Logger logger;
    private String name;

    /**
     * Construct a new ManualTest with given name
     *
     * @param name Name for the ManualTest
     */
    public ManualTest(String name) {
        setName(name);
    }

    /**
     * Executed when the test is selected
     */
    public void start() {
        logger.info("Starting {} {}", getClass(), getName());
    }

    /**
     * Executed repeatedly while test is selected
     *
     * @param delta
     */
    @Override
    public void update(double delta) {
    }

    /**
     * Called when a button is pressed
     *
     * @param button The button that was pressed
     */
    public void onButtonDown(LogitechF310.Button button) {
    }

    /**
     * Called when a button is released
     *
     * @param button The button that was released
     */
    public void onButtonUp(LogitechF310.Button button) {
    }

    /**
     * Called repeatedly with the value of the analog axis for tests
     *
     * @param value The value of the axis
     */
    public void updateAxis(double value) {
    }

    /**
     * Executed when the test is stopped (navigated away from)
     */
    public void stop() {
    }

    /**
     * @return the name of the test
     */
    public String getName() {
        return name;
    }

    /**
     * Set the name of the test
     *
     * @param name the name of the test
     */
    public void setName(String name) {
        this.name = name;
        String loggerName = String.format("%s(%s)", this.getClass().getName(), name);
        this.logger = LoggerFactory.getLogger(loggerName);
    }

    /**
     * Print instructions about how to use a test
     *
     * @param message   Message to print
     * @param arguments Arguments to put into message
     */
    public void printTestInstructions(String message, Object... arguments) {
        logger.info("==> " + message + " <==", arguments);
    }

    /**
     * Print relevant info during execution of a test
     *
     * @param message   Message to print
     * @param arguments Arguments to put into message
     */
    public void printTestInfo(String message, Object... arguments) {
        logger.info(">> " + message + " <<", arguments);
    }
}
