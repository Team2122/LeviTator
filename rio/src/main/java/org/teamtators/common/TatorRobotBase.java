package org.teamtators.common;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import edu.wpi.first.wpilibj.DriverStation;
import edu.wpi.first.wpilibj.PowerDistributionPanel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.teamtators.common.config.ConfigCommandStore;
import org.teamtators.common.config.ConfigLoader;
import org.teamtators.common.config.TriggerBinder;
import org.teamtators.common.control.Timer;
import org.teamtators.common.control.Updatable;
import org.teamtators.common.control.UpdatableCollection;
import org.teamtators.common.control.Updater;
import org.teamtators.common.datalogging.Dashboard;
import org.teamtators.common.datalogging.DashboardUpdatable;
import org.teamtators.common.datalogging.DashboardUpdater;
import org.teamtators.common.datalogging.DataCollector;
import org.teamtators.common.hw.LogitechF310;
import org.teamtators.common.scheduler.*;
import org.teamtators.common.tester.AutomatedTester;
import org.teamtators.common.tester.ManualTester;

import java.util.List;

public abstract class TatorRobotBase implements RobotStateListener, Updatable {
    public static final Logger logger = LoggerFactory.getLogger(TatorRobotBase.class);
    protected final ObjectMapper configMapper = new ObjectMapper(new YAMLFactory());
    protected final ConfigLoader configLoader;
    protected final Scheduler scheduler = new Scheduler();
    protected final ConfigCommandStore commandStore = new ConfigCommandStore();
    protected final TriggerBinder triggerBinder = new TriggerBinder(getScheduler(), getCommandStore(), configMapper);
    protected final ManualTester tester = new ManualTester();
    protected final AutomatedTester automatedTester = new AutomatedTester(getScheduler());

    protected final Updater updater = new Updater(this, 1 / 120.0);
    protected final UpdatableCollection controllers = new UpdatableCollection("Controllers");
    protected final Updater controllerUpdater = new Updater(controllers, 1 / 120.0);
    protected final DashboardUpdater smartDashboardUpdater = new DashboardUpdater(this, Dashboard.Type.TATOR_DASHBOARD);
    protected final Updater dashboardUpdater = new Updater(smartDashboardUpdater, 1 / 10.0);
    protected final DataCollector dataCollector = DataCollector.getDataCollector();
    protected final Updater dataCollectorUpdater = new Updater(dataCollector, 1.0 / 60.0);
    protected final Timer stateTimer = new Timer();
    protected double lastDelta = 0.0;

    private PowerDistributionPanel pdp = new PowerDistributionPanel();
    private DriverStation driverStation = DriverStation.getInstance();
    private Command autoCommand;
    private List<Subsystem> subsystemList;

    public TatorRobotBase(String configDir) {
        configMapper.configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true);
        configLoader = new ConfigLoader(configDir, configMapper);
    }

    public void initialize() {
        configureSubsystems();
        configureCommands();
        configureTriggers();
        configureTests();
        setUpDashboards();
        startThreads();
        postInitialize();
    }

    public void addSmartDashboardUpdatable(DashboardUpdatable smartDashboardUpdatable) {
        this.smartDashboardUpdater.add(smartDashboardUpdatable);
    }


    protected void configureSubsystems() {
        SubsystemsBase subsystems = getSubsystemsBase();
        logger.debug("Configuring subsystems");

        subsystems.configure(configLoader);
        this.controllers.addAll(subsystems.getControllers());

        subsystemList = subsystems.getSubsystemList();
        for (Subsystem subsystem : subsystemList) {
            getScheduler().registerStateListener(subsystem);
            getTester().registerTestGroup(subsystem.createManualTests());
            getAutomatedTester().addTests(subsystem.createAutomatedTests());
        }
    }

    protected void configureCommands() {
        logger.debug("Creating commands");
        registerCommands();
        ObjectNode commandsConfig = (ObjectNode) configLoader.load("Commands.yaml");
        getCommandStore().createCommandsFromConfig(commandsConfig);
        //autoCommand = commandStore.getCommand("$AutoChooser");
    }

    protected void configureTests() {
        logger.debug("Configuring tests");
        getTester().setJoystick(getDriverJoystick());
        automatedTester.registerWith(getTester());
        getScheduler().registerDefaultCommand(getTester());
        //getAutomatedTester().initialize(configLoader);
    }

    protected void configureTriggers() {
        logger.debug("Configuring triggers");
        ObjectNode triggersConfig = (ObjectNode) configLoader.load("Triggers.yaml");
        triggerBinder.setDriverJoystick(getDriverJoystick());
        triggerBinder.setGunnerJoystick(getGunnerJoystick());
        triggerBinder.bindTriggers(triggersConfig);
    }

    protected void startThreads() {
        logger.debug("Starting threads");
        updater.start();
        controllerUpdater.start();
        dataCollectorUpdater.start();
        dashboardUpdater.start();
    }

    @Override
    public void onEnterRobotState(RobotState state) {
        logger.info("==> Robot is in " + state + " <==");
        stateTimer.start();
        this.getScheduler().onEnterRobotState(state);

        if (state == RobotState.AUTONOMOUS) {
            this.getScheduler().startCommand(autoCommand);
        }
    }

    public double getStateTime() {
        return stateTimer.get();
    }

    public void update(double delta) {
        lastDelta = delta;
        getScheduler().execute();

        if (getState() != RobotState.TEST) {
            for (Subsystem subsystem : subsystemList) {
                subsystem.update(delta);
            }
        }
        //DriverStation.getInstance().isDSAttached();
    }

    private void setUpDashboards() {
        smartDashboardUpdater.setUpDashboards();
    }

    public ObjectMapper getConfigMapper() {
        return configMapper;
    }

    public ConfigLoader getConfigLoader() {
        return configLoader;
    }

    public ConfigCommandStore getCommandStore() {
        return commandStore;
    }

    public Scheduler getScheduler() {
        return scheduler;
    }

    public ManualTester getTester() {
        return tester;
    }

    public AutomatedTester getAutomatedTester() {
        return automatedTester;
    }

    public TriggerBinder getTriggerBinder() {
        return triggerBinder;
    }

    public RobotState getState() {
        return scheduler.getRobotState();
    }

    public PowerDistributionPanel getPDP() {
        return pdp;
    }

    protected void onDriverStationData() {
    }

    public abstract SubsystemsBase getSubsystemsBase();

    protected abstract void registerCommands();

    protected abstract LogitechF310 getGunnerJoystick();

    protected abstract LogitechF310 getDriverJoystick();

    protected void postInitialize() {
        logger.info("==> Initialized TatorRobot");
    }

    public DriverStation getDriverStation() {
        return driverStation;
    }
}
