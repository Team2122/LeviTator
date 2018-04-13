package org.teamtators.common;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import edu.wpi.first.networktables.EntryListenerFlags;
import edu.wpi.first.networktables.EntryNotification;
import edu.wpi.first.networktables.NetworkTableEntry;
import edu.wpi.first.networktables.NetworkTableInstance;
import edu.wpi.first.wpilibj.DriverStation;
import edu.wpi.first.wpilibj.PowerDistributionPanel;
import edu.wpi.first.wpilibj.livewindow.LiveWindow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.profiler.Profiler;
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
import org.teamtators.common.controllers.Controller;
import org.teamtators.common.controllers.TriggerBinder;
import org.teamtators.common.datalogging.Dashboard;
import org.teamtators.common.datalogging.DashboardUpdatable;
import org.teamtators.common.datalogging.DashboardUpdater;
import org.teamtators.common.datalogging.DataCollector;
import org.teamtators.common.scheduler.*;
import org.teamtators.common.tester.AutomatedTester;
import org.teamtators.common.tester.ManualTester;
import org.teamtators.common.util.FMSData;

import java.util.Collections;
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

    protected final Updater updater = new Updater(this, 1 / 100.0);
    protected final UpdatableCollection controllers = new UpdatableCollection("Controllers");
    protected final UpdatableCollection motors = new UpdatableCollection("Motors");
    protected final Updater motorUpdater = new Updater(motors, 1 / 100.0);
    protected final Updater controllerUpdater = new Updater(controllers, 1 / 100.0);
    protected final DashboardUpdater smartDashboardUpdater = new DashboardUpdater(this, Dashboard.Type.TATOR_DASHBOARD);
    protected final Updater dashboardUpdater = new Updater(smartDashboardUpdater, 1 / 10.0);
    protected final DataCollector dataCollector = DataCollector.getDataCollector();
    protected final Updater dataCollectorUpdater = new Updater(dataCollector, 1.0 / 50.0);
    protected List<Controller<?, ?>> gameControllers = Collections.emptyList();
    protected final Timer stateTimer = new Timer();
    protected double lastDelta = 0.0;

    protected boolean reinitialize = false;

    private PowerDistributionPanel pdp;
    private DriverStation driverStation;
    private Command autoCommand;
    private List<Subsystem> subsystemList;

    private FMSData fmsData = new FMSData();

    private NetworkTableEntry reinitializeEntry;
    protected int reinitializeListener;

    protected Profiler profiler;

    public TatorRobotBase(String configDir) {
        configMapper.configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true);
        configLoader = new ConfigLoader(configDir, configMapper);

        reinitializeEntry = NetworkTableInstance.getDefault()
                .getTable("SmartDashboard")
                .getEntry("reinitialize");
    }

    protected void reinitializeListener(EntryNotification entryNotification) {
        logger.trace(entryNotification.name + ": " + entryNotification.value.getString());
        reinitialize = true;
    }

    protected void addReinitializeListener() {
        reinitializeEntry.setString("initialized");
        reinitializeListener = reinitializeEntry
                .addListener(this::reinitializeListener, EntryListenerFlags.kNew | EntryListenerFlags.kUpdate);
        logger.trace("Listening for reinitialization signal at {}", reinitializeEntry.getName());
    }

    protected void removeReinitializeListener() {
        reinitializeEntry.removeListener(reinitializeListener);
    }

    protected void initialize() {
        try {
            doInitialize();
        } catch (Throwable e) {
            logger.error("Robot initialization failed. Fix your code/config!", e);
        } finally {
            addReinitializeListener();
        }
    }

    protected void doInitialize() {
        configureSubsystems();
        configureCommands();
        configureTriggers();
        configureTests();
        setUpDashboards();
        startThreads();
        postInitialize();
    }

    protected void deinitialize() {
        logger.info("Deinitializing " + getName());
        stopThreads();
        deconfigureTests();
        deconfigureTriggers();
        deconfigureCommands();
        deconfigureSubsystems();
        removeReinitializeListener();
        System.gc();
    }

    protected void reinitialize() {
        deinitialize();
        initialize();
    }

    public void addSmartDashboardUpdatable(DashboardUpdatable smartDashboardUpdatable) {
        this.smartDashboardUpdater.add(smartDashboardUpdatable);
    }


    protected void configureSubsystems() {
        pdp = new PowerDistributionPanel();
        driverStation = DriverStation.getInstance();

        LiveWindow.add(pdp);
        LiveWindow.disableTelemetry(pdp);

        SubsystemsBase subsystems = getSubsystemsBase();
        logger.debug("Configuring subsystems");

        subsystems.configure(configLoader);
        this.controllers.addAll(subsystems.getUpdatables());
        this.motors.addAll(subsystems.getMotorUpdatables());

        subsystemList = subsystems.getSubsystemList();
        for (Subsystem subsystem : subsystemList) {
            getScheduler().registerStateListener(subsystem);
            getScheduler().registerFMSDataListener(subsystem);
            getTester().registerTestGroup(subsystem.createManualTests());
            getAutomatedTester().addTests(subsystem.createAutomatedTests());
        }
    }

    protected void deconfigureSubsystems() {
        logger.debug("Deconfiguring subsystems");
        pdp.free();
        LiveWindow.remove(pdp);
        this.controllers.clear();
        this.motors.clear();

        getScheduler().clearStateListeners();
        getScheduler().clearFMSDataListeners();
        getTester().clearTestGroups();
        getAutomatedTester().clearTests();

        getSubsystemsBase().deconfigure();
    }

    protected void configureCommands() {
        logger.debug("Creating commands");
        registerCommands(getCommandStore());
        ObjectNode commandsConfig = (ObjectNode) configLoader.load("Commands.yaml");
        getCommandStore().createCommandsFromConfig(commandsConfig);
    }

    protected void deconfigureCommands() {
        logger.debug("Deconfiguring commands");
        getCommandStore().clearCommands();
        getCommandStore().clearRegistrations();
    }

    protected void configureTests() {
        logger.debug("Configuring tests");
        getTester().setJoystick(getSubsystemsBase().getTestModeController());
        automatedTester.registerWith(getTester());
        getScheduler().registerDefaultCommand(getTester());
        //getAutomatedTester().initialize(configLoader);
    }

    protected void deconfigureTests() {
        logger.debug("Deconfiguring tests");
    }

    protected void configureTriggers() {
        logger.debug("Configuring triggers");
        gameControllers = getSubsystemsBase().getControllers();
        getTriggerBinder().putControllers(gameControllers);
        ObjectNode triggersConfig = (ObjectNode) configLoader.load("Triggers.yaml");
        triggerBinder.bindTriggers(triggersConfig);
    }

    protected void deconfigureTriggers() {
        logger.debug("Deconfiguring triggers");
        getTriggerBinder().clearControllers();
        getScheduler().clearTriggers();
    }

    protected void startThreads() {
        logger.debug("Starting threads");
        updater.start();
        controllerUpdater.start();
        dataCollectorUpdater.start();
        dashboardUpdater.start();
    }

    protected void stopThreads() {
        logger.debug("Stopping threads");
        updater.stop();
        controllerUpdater.stop();
        dataCollectorUpdater.stop();
        dashboardUpdater.stop();
    }

    @Override
    public void onEnterRobotState(RobotState state) {
        logger.info("==> Robot is in " + state + " <==");
        stateTimer.start();
        this.getScheduler().onEnterRobotState(state);

        if (state == RobotState.AUTONOMOUS) {
            this.autoCommand = this.getAutoCommand();
            if (autoCommand == null) {
                logger.warn("No auto command was specified");
            } else {
                this.getScheduler().startCommand(autoCommand);
            }
        }
        if (state == RobotState.AUTONOMOUS || state == RobotState.TELEOP) {
            motorUpdater.start();
        } else {
            motorUpdater.stop();
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
        profiler = new Profiler(getRobotName());
        lastDelta = delta;
        getScheduler().setProfiler(profiler.startNested("Scheduler"));
        getScheduler().execute();

        if (getState() != RobotState.TEST) {
            for (Subsystem subsystem : subsystemList) {
                profiler.start(subsystem.getName());
                subsystem.update(delta);
            }
        }
        profiler.stop();
    }

    @Override
    public Profiler getProfiler() {
        return profiler;
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
        if (reinitialize) {
            reinitialize();
            reinitialize = false;
        }
        FMSData fmsDataCurrent = FMSData.fromDriverStation(driverStation);
        if (!fmsDataCurrent.equals(fmsData)) {
            logger.info("FMS Data updated: " + fmsDataCurrent);
            fmsData = fmsDataCurrent;
            this.onFMSData(fmsData);
        }
        for (Controller<?, ?> gameController : gameControllers) {
            gameController.onDriverStationData();
        }
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
