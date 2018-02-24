package org.teamtators.common.tester;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.teamtators.common.config.ConfigLoader;
import org.teamtators.common.config.Configurable;
import org.teamtators.common.config.Configurables;
import org.teamtators.common.controllers.LogitechF310;
import org.teamtators.common.scheduler.Command;
import org.teamtators.common.scheduler.Scheduler;
import org.teamtators.common.scheduler.SequentialCommand;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * A ManualTest which coordinates automated tests
 */
public class AutomatedTester extends ManualTest {
    public static final String AUTOTESTS_CONFIG_FILE = "AutoTests.yaml";
    private List<AutomatedTest> testList = new ArrayList<>();
    private SequentialCommand autoTests;
    private SequentialCommand hybridTests;
    private Scheduler scheduler;
    private boolean running = false;
    private boolean testingHybrids;

    public AutomatedTester(Scheduler scheduler) {
        super("AutomatedTester");
        this.scheduler = scheduler;
    }

    public void registerWith(ManualTester tester) {
        tester.registerTestGroup(new ManualTestGroup("AutomatedTester", this));
    }

    public void addTests(List<AutomatedTest> newTests) {
        testList.addAll(newTests);
    }

    public void clearTests() {
        testList.clear();
    }

    @Override
    public void start() {
        super.start();
        printTestInstructions("Press A to run all automated tests.");
        printTestInstructions("Press B to stop testing.");
        printTestInstructions("Press Start to begin semi-automated tests once fully automated tests are done.");
        printTestInstructions("Press X to skip a semi-automated test.");
        initialize();
    }

    private void initialize() {
        for (AutomatedTest test : testList) {
            test.clearMessages();
            test.resetSkip();
        }
        Map<Boolean, List<Command>> tests = testList.stream()
                .collect(Collectors.groupingBy(AutomatedTest::isHybrid, Collectors.toList()));
        //autoTests = new SequentialCommand("AutomatedTests", tests.get(false));
        //hybridTests = new SequentialCommand("HybridTests", tests.get(true));
        testingHybrids = false;
    }

    @Override
    public void onButtonDown(LogitechF310.Button button) {
        switch (button) {
            case A:
                if (!running) {
                    running = true;
                    scheduler.startCommand(autoTests);
                    initialize();
                    printTestInfo("Beginning automated tests.");
                }
                break;
            case B:
                if (running) stop();
                else printTestInfo("AutomatedTester not running");
                break;
            case X:
                if (hybridTests.isRunning()) ((AutomatedTest) hybridTests.currentCommand()).skip();
                else printTestInfo("No semi-automated test currently running");
                break;
            case START:
                if (testingHybrids && !running) {
                    printTestInfo("Beginning semi-automated tests");
                    scheduler.startCommand(hybridTests);
                    running = true;
                } else {
                    printTestInfo("Not currently waiting to begin semi-automated tests, cannot trigger them");
                }
                break;
            default:
                if (!testingHybrids)
                    ((AutomatedTest) autoTests.currentCommand()).setLastButton(button);
                else
                    ((AutomatedTest) hybridTests.currentCommand()).setLastButton(button);

        }
    }

    @Override
    public void update(double delta) {
        if (running) {
            if (!testingHybrids && !scheduler.containsCommand(autoTests.getName())) {
                //beginHybrids();
            } else if (!scheduler.containsCommand(autoTests.getName()) && !scheduler.containsCommand(hybridTests.getName())) {
                printTestInfo("stop got called. tesinghybrids: {} running: {}", testingHybrids, running);
                stop();
            }
        }
    }

    private void beginHybrids() {
        testingHybrids = true;
        running = false;
        printTestInfo("All AutomatedTests completed. Press Start to begin semi-automated (human interaction) tests");
    }

    @Override
    public void stop() {
        printTestInfo("AutomatedTester stopping.");
        if (autoTests != null && autoTests.isRunning()) {
            autoTests.cancel();
        }
        if (hybridTests != null && hybridTests.isRunning()) {
            hybridTests.cancel();
        }
        if (running) outputMessages();
        running = false;
    }

    private void outputMessages() {
        printTestInfo("Listing warnings/errors from tests:");
        for (AutomatedTest test : testList) {
            for (AutomatedTestMessage message : test.getMessages()) {
                printTestInfo("{} in {}: {}", message.getLevel(), test.getName(), message.getMessage());
            }
        }
    }

    /**
     * Configures all automated tests from file
     *
     * @param configLoader Robot config loader
     */
    public void configure(ConfigLoader configLoader) {
        ObjectNode configNode = (ObjectNode) configLoader.load(AUTOTESTS_CONFIG_FILE);
        for (AutomatedTest test : testList) {
            if (test instanceof Configurable) {
                try {
                    Configurables.configureObject(test, configNode.get(test.getName()));
                } catch (Exception e) {
                    logger.error("Configuration of automated test {} failed", test.getName());
                    throw e;
                }
            }
        }
    }
}
