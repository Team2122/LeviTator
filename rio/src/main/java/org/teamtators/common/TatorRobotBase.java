package org.teamtators.common;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import edu.wpi.first.wpilibj.DriverStation;
import edu.wpi.first.wpilibj.PowerDistributionPanel;
import edu.wpi.first.wpilibj.livewindow.LiveWindow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.teamtators.common.commands.CancelCommand;
import org.teamtators.common.commands.LogCommand;
import org.teamtators.common.commands.WaitCommand;
import org.teamtators.common.commands.WaitForCommand;
import org.teamtators.common.config.ConfigCommandStore;
import org.teamtators.common.config.ConfigLoader;
import org.teamtators.common.control.Timer;
import org.teamtators.common.control.Updatable;
import org.teamtators.common.control.UpdatableCollection;
import org.teamtators.common.control.Updater;
import org.teamtators.common.controllers.TriggerBinder;
import org.teamtators.common.datalogging.Dashboard;
import org.teamtators.common.datalogging.DashboardUpdatable;
import org.teamtators.common.datalogging.DashboardUpdater;
import org.teamtators.common.datalogging.DataCollector;
import org.teamtators.common.scheduler.*;
import org.teamtators.common.tester.AutomatedTester;
import org.teamtators.common.tester.ManualTester;
import org.teamtators.common.util.FMSData;

import java.util.List;

public abstract class TatorRobotBase implements RobotStateListener, Updatable, FMSDataListener {
    public static final Logger logger = LoggerFactory.getLogger(TatorRobotBase.class);
    public static final ObjectMapper configMapper = new ObjectMapper(new YAMLFactory());
    protected final ConfigLoader configLoader;
    protected final Scheduler scheduler = new Scheduler();
    protected final ConfigCommandStore commandStore = new ConfigCommandStore();
    protected final TriggerBinder triggerBinder = new TriggerBinder(getScheduler(), getCommandStore());
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

    private PowerDistributionPanel pdp;
    private DriverStation driverStation;
    private Command autoCommand;
    private List<Subsystem> subsystemList;

    private FMSData fmsData = new FMSData();

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
        pdp = new PowerDistributionPanel();
        driverStation = DriverStation.getInstance();

        LiveWindow.add(pdp);

        SubsystemsBase subsystems = getSubsystemsBase();
        logger.debug("Configuring subsystems");

        subsystems.configure(configLoader);
        this.controllers.addAll(subsystems.getUpdatables());

        subsystemList = subsystems.getSubsystemList();
        for (Subsystem subsystem : subsystemList) {
            getScheduler().registerStateListener(subsystem);
            getScheduler().registerDataListener(subsystem);
            getTester().registerTestGroup(subsystem.createManualTests());
            getAutomatedTester().addTests(subsystem.createAutomatedTests());
        }
    }

    protected void configureCommands() {
        logger.debug("Creating commands");
        registerCommands(getCommandStore());
        ObjectNode commandsConfig = (ObjectNode) configLoader.load("Commands.yaml");
        getCommandStore().createCommandsFromConfig(commandsConfig);
        this.autoCommand = this.getAutoCommand();
    }

    protected void configureTests() {
        logger.debug("Configuring tests");
        getTester().setJoystick(getSubsystemsBase().getTestModeController());
        automatedTester.registerWith(getTester());
        getScheduler().registerDefaultCommand(getTester());
        //getAutomatedTester().initialize(configLoader);
    }

    protected void configureTriggers() {
        logger.debug("Configuring triggers");
        getTriggerBinder().putControllers(getSubsystemsBase().getControllers());
        ObjectNode triggersConfig = (ObjectNode) configLoader.load("Triggers.yaml");
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
            if (autoCommand == null) {
                logger.warn("No auto command was specified");
            } else {
                this.getScheduler().startCommand(autoCommand);
            }
        }
    }

    @Override
    public void onFMSData(FMSData data) {
        this.getScheduler().onFMSData(data);
    }

    public double getStateTime() {
        return stateTimer.get();
    }

    public void update(double delta) {
        lastDelta = delta;
        getScheduler().execute();

        FMSData fmsDataCurrent = FMSData.fromDriverStation(driverStation);
        if (!fmsDataCurrent.equals(fmsData)) {
            logger.info("FMS Data updated: " + fmsDataCurrent);
            fmsData = fmsDataCurrent;
            this.onFMSData(fmsData);
        }

        if (getState() != RobotState.TEST) {
            for (Subsystem subsystem : subsystemList) {
                subsystem.update(delta);
            }
        }
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

    public DriverStation getDriverStation() {
        return driverStation;
    }

    protected void onDriverStationData() {
    }

    public String getRobotName() {
        return "TatorRobot";
    }

    public abstract SubsystemsBase getSubsystemsBase();

    protected void registerCommands(ConfigCommandStore commandStore) {
        commandStore.registerCommand("Cancel", () -> new CancelCommand(this));
        commandStore.registerCommand("Wait", () -> new WaitCommand(this));
        commandStore.registerCommand("WaitFor", () -> new WaitForCommand(this));
        commandStore.registerCommand("Log", LogCommand::new);
    }

    protected Command getAutoCommand() {
        return null;
    }

    protected void postInitialize() {
        logger.info("==> Initialized " + getRobotName());
    }
}
