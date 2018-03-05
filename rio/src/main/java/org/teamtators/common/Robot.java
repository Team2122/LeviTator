/*----------------------------------------------------------------------------*/
/* Copyright (c) FIRST 2008-2017. All Rights Reserved.                        */
/* Open Source Software - may be modified and shared by FRC teams. The code   */
/* must be accompanied by the FIRST BSD license file in the root directory of */
/* the project.                                                               */
/*----------------------------------------------------------------------------*/

package org.teamtators.common;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.joran.spi.JoranException;
import ch.qos.logback.core.util.StatusPrinter;
import edu.wpi.cscore.CameraServerJNI;
import edu.wpi.first.networktables.NetworkTableInstance;
import edu.wpi.first.wpilibj.*;
import edu.wpi.first.wpilibj.hal.FRCNetComm;
import edu.wpi.first.wpilibj.hal.HAL;
import edu.wpi.first.wpilibj.hal.HALUtil;
import edu.wpi.first.wpilibj.internal.HardwareHLUsageReporting;
import edu.wpi.first.wpilibj.internal.HardwareTimer;
import edu.wpi.first.wpilibj.livewindow.LiveWindow;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import edu.wpi.first.wpilibj.util.WPILibVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.jar.Manifest;

/**
 * Implement a Robot Program framework. The Robot class is intended to be subclassed by a user
 * creating a robot program. Overridden autonomous() and operatorControl() methods are called at the
 * appropriate time as the match proceeds. In the current implementation, the Autonomous code will
 * run to completion before the OperatorControl code could start. In the future the Autonomous code
 * might be spawned as a task, then killed at the end of the Autonomous period.
 */
public class Robot {
    public static Logger logger = LoggerFactory.getLogger(Robot.class);

    protected final DriverStation m_ds;
    protected final String configDir;
    private org.teamtators.common.scheduler.RobotState robotState = null;
    private TatorRobotBase robot;
    private NetworkTableInstance networkTables;

    /**
     * Constructor for a generic robot program. User code should be placed in the constructor that
     * runs before the Autonomous or Operator Control period starts. The constructor will run to
     * completion before Autonomous is entered.
     * <p>
     * <p>This must be used to ensure that the communications code starts. In the future it would be
     * nice
     * to put this code into it's own task that loads on boot so ensure that it runs.
     */
    protected Robot(String configDir) {
        // TODO: StartCAPI();
        // TODO: See if the next line is necessary
        // Resource.RestartProgram();
        this.configDir = configDir;
        networkTables = NetworkTableInstance.getDefault();
        networkTables.setNetworkIdentity("Robot");
        networkTables.startServer("/home/lvuser/networktables.ini");// must be before b
        m_ds = DriverStation.getInstance();
        networkTables.getTable(""); // forces network tables to initialize
        networkTables.getTable("LiveWindow").getSubTable(".status").getEntry("LW Enabled").setBoolean(false);

        LiveWindow.setEnabled(false);
    }

    /**
     * Get if the robot is a simulation.
     *
     * @return If the robot is running in simulation.
     */
    public static boolean isSimulation() {
        return !isReal();
    }

    /**
     * Get if the robot is real.
     *
     * @return If the robot is running in the real world.
     */
    public static boolean isReal() {
        return HALUtil.getHALRuntimeType() == 0;
    }

    @SuppressWarnings("JavadocMethod")
    public static boolean getBooleanProperty(String name, boolean defaultValue) {
        String propVal = System.getProperty(name);
        if (propVal == null) {
            return defaultValue;
        }
        if (propVal.equalsIgnoreCase("false")) {
            return false;
        } else if (propVal.equalsIgnoreCase("true")) {
            return true;
        } else {
            throw new IllegalStateException(propVal);
        }
    }

    /**
     * Common initialization for all robot programs.
     */
    public static void initializeHardwareConfiguration() {
        if (!HAL.initialize(500, 0)) {
            throw new IllegalStateException("HAL.initialize failed. Terminating");
        }

        // Set some implementations so that the static methods work properly
        Timer.SetImplementation(new HardwareTimer());
        HLUsageReporting.SetImplementation(new HardwareHLUsageReporting());
        RobotState.SetImplementation(DriverStation.getInstance());

        // Call a CameraServer JNI function to force OpenCV native library loading
        // Needed because all the OpenCV JNI functions don't have built in loading
        // TODO: Have a way to enable this if/when we use CameraServer
        CameraServerJNI.enumerateSinks();
    }

    /**
     * Starting point for the applications.
     */
    @SuppressWarnings("PMD.UnusedFormalParameter")
    public static void main(String... args) {
        System.out.println("********** Initializing HAL **********");
        initializeHardwareConfiguration();

        HAL.report(FRCNetComm.tResourceType.kResourceType_Language, FRCNetComm.tInstances.kLanguage_Java);

        if (args.length < 1) {
            System.err.println("Config directory must be specified as first argument");
            System.exit(1);
        }

        writeVersionsFile();

        boolean errorOnExit = false;
        try {
            System.out.println("********** Robot program starting **********");
            Robot robot = new Robot(args[0]);
            robot.startCompetition();
        } catch (Throwable throwable) {
            DriverStation.reportError(
                    "ERROR Unhandled exception: " + throwable.toString() + " at "
                            + Arrays.toString(throwable.getStackTrace()), false);
            errorOnExit = true;
        } finally {
            // startCompetition never returns unless exception occurs....
            System.err.println("WARNING: Robots don't quit!");
            if (errorOnExit) {
                System.err
                        .println("---> The startCompetition() method (or methods called by it) should have "
                                + "handled the exception above.");
            } else {
                System.err.println("---> Unexpected return from startCompetition() method.");
            }
        }
        System.exit(1);
    }

    public static void writeVersionsFile() {
        try {
            final File file = new File("/tmp/frc_versions/FRC_Lib_Version.ini");

            if (file.exists()) {
                file.delete();
            }

            file.createNewFile();

            try (FileOutputStream output = new FileOutputStream(file)) {
                output.write("Java ".getBytes());
                output.write(WPILibVersion.Version.getBytes());
            }

        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    /**
     * Free the resources for a Robot class.
     */
    public void free() {
    }

    /**
     * Determine if the Robot is currently disabled.
     *
     * @return True if the Robot is currently disabled by the field controls.
     */
    public boolean isDisabled() {
        return m_ds.isDisabled();
    }

    /**
     * Determine if the Robot is currently enabled.
     *
     * @return True if the Robot is currently enabled by the field controls.
     */
    public boolean isEnabled() {
        return m_ds.isEnabled();
    }

    /**
     * Determine if the robot is currently in Autonomous mode as determined by the field
     * controls.
     *
     * @return True if the robot is currently operating Autonomously.
     */
    public boolean isAutonomous() {
        return m_ds.isAutonomous();
    }

    /**
     * Determine if the robot is currently in Test mode as determined by the driver
     * station.
     *
     * @return True if the robot is currently operating in Test mode.
     */
    public boolean isTest() {
        return m_ds.isTest();
    }

    /**
     * Determine if the robot is currently in Operator Control mode as determined by the field
     * controls.
     *
     * @return True if the robot is currently operating in Tele-Op mode.
     */
    public boolean isOperatorControl() {
        return m_ds.isOperatorControl();
    }

    /**
     * Indicates if new data is available from the driver station.
     *
     * @return Has new data arrived over the network since the last time this function was called?
     */
    public boolean isNewDataAvailable() {
        return m_ds.isNewControlData();
    }

    public void startCompetition() {
        try {
            findLogDirectory();
            configureLogging();

            doStartCompetition();
        } catch (Throwable t) {
            logger.error("Unhandled exception thrown!", t);
        }
    }

    private void findLogDirectory() {
        String robotName = new File(configDir).getParentFile().getName();
        File logDir = new File("/media/sda1/" + robotName);
        if (logDir.exists() && logDir.canWrite()) {
            System.out.println("****** Using USB drive for logs at " + logDir.getAbsolutePath() + " ******");
        } else {
            System.out.println("ERROR: USB drive not present, or permissions are incorrect. Logging to /home/lvuser");
            logDir = new File("/home/lvuser/" + robotName);
        }
        System.setProperty("tator.logdir", logDir.getAbsolutePath());
    }

    private void configureLogging() {
        File logbackConfig = new File(this.configDir, "logback.xml");
        // assume SLF4J is bound to logback in the current environment
        LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();

        try {
            JoranConfigurator configurator = new JoranConfigurator();
            configurator.setContext(context);
            // Call context.reset() to clear any previous configuration, e.g. default
            // configuration. For multi-update configuration, omit calling context.reset().
            context.reset();
            configurator.doConfigure(logbackConfig);
        } catch (JoranException je) {
            // StatusPrinter will handle this
        }
        StatusPrinter.printInCaseOfErrorsOrWarnings(context);
    }

    private void doStartCompetition() throws Throwable {
        // Report that we are using java just to match what IterativeRobot does
        HAL.report(FRCNetComm.tResourceType.kResourceType_Framework,
                FRCNetComm.tInstances.kFramework_Iterative);

        // Actually initialize user robot code
        robotInit();

        // Tell the DS that the robot is ready to be enabled
        HAL.observeUserProgramStarting();

        // loop forever, calling the appropriate mode-dependent functions
        LiveWindow.setEnabled(false);

        //noinspection InfiniteLoopStatement
        while (true) {
            // Update the state if it has changed
            if (isDisabled()) {
                if (robotState != org.teamtators.common.scheduler.RobotState.DISABLED) {
                    robotState = org.teamtators.common.scheduler.RobotState.DISABLED;
                    robot.onEnterRobotState(robotState);
                }
            } else if (isTest()) {
                if (robotState != org.teamtators.common.scheduler.RobotState.TEST) {
                    robotState = org.teamtators.common.scheduler.RobotState.TEST;
                    robot.onEnterRobotState(org.teamtators.common.scheduler.RobotState.TEST);
                }
            } else if (isAutonomous()) {
                if (robotState != org.teamtators.common.scheduler.RobotState.AUTONOMOUS) {
                    robotState = org.teamtators.common.scheduler.RobotState.AUTONOMOUS;
                    robot.onEnterRobotState(org.teamtators.common.scheduler.RobotState.AUTONOMOUS);
                }
            } else {
                if (robotState != org.teamtators.common.scheduler.RobotState.TELEOP) {
                    robotState = org.teamtators.common.scheduler.RobotState.TELEOP;
                    robot.onEnterRobotState(org.teamtators.common.scheduler.RobotState.TELEOP);
                }
            }
            // If we have new control data, update stuff also
            if (isNewDataAvailable()) {
                switch (robotState) {
                    case DISABLED:
                        HAL.observeUserProgramDisabled();
                        break;
                    case AUTONOMOUS:
                        HAL.observeUserProgramAutonomous();
                        break;
                    case TELEOP:
                        HAL.observeUserProgramTeleop();
                        break;
                    case TEST:
                        HAL.observeUserProgramTest();
                        break;
                }
                robot.onDriverStationData();
                SmartDashboard.updateValues();
            }
            // Enable LiveWindow if in test mode
            LiveWindow.setEnabled(robotState == org.teamtators.common.scheduler.RobotState.TEST);
            // Wait for new data from the driver station. Should be at a ~20ms period
            SmartDashboard.updateValues();
            LiveWindow.updateValues();
            m_ds.waitForData();
        }
    }

    private void initialize() throws Exception {
        logger.info("Robot initializing with config directory " + this.configDir);

        String robotName = "";
        Enumeration<URL> resources = RobotBase.class.getClassLoader().getResources("META-INF/MANIFEST.MF");
        while (resources != null && resources.hasMoreElements()) {
            try {
                Manifest manifest = new Manifest(resources.nextElement().openStream());
                robotName = manifest.getMainAttributes().getValue("Robot-Class");
                break;
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }

        try {
            robot = (TatorRobotBase) Class.forName(robotName)
                    .getConstructor(String.class)
                    .newInstance(this.configDir);
        } catch (Exception e) {
            DriverStation.reportError("ERROR Unhandled exception instantiating robot " + robotName + " "
                    + e.toString() + " at " + Arrays.toString(e.getStackTrace()), false);
            logger.error("Could not instantiate robot " + robotName + "!\n" +
                    "Does the class exist, and does it have a public constructor which takes the name of the " +
                    "config directory?", e);
            throw e;
        }

        robot.initialize();
    }

    public void robotInit() throws Throwable {
        try {
            initialize();
        } catch (Throwable t) {
            logger.error("Exception during robot init", t);
            throw t;
        }
    }
}
