package org.teamtators.levitator;

import edu.wpi.cscore.MjpegServer;
import edu.wpi.cscore.UsbCamera;
import edu.wpi.cscore.UsbCameraInfo;
import edu.wpi.cscore.VideoSource;
import edu.wpi.first.wpilibj.CameraServer;
import org.teamtators.common.SubsystemsBase;
import org.teamtators.common.TatorRobotBase;
import org.teamtators.common.config.ConfigCommandStore;
import org.teamtators.common.controllers.LogitechF310;
import org.teamtators.common.scheduler.Command;
import org.teamtators.levitator.commands.CommandRegistrar;
import org.teamtators.levitator.subsystems.Subsystems;

import java.util.Arrays;

public class TatorRobot extends TatorRobotBase {
    private final CommandRegistrar registrar = new CommandRegistrar(this);

    private Subsystems subsystems;

    public TatorRobot(String configDir) {
        super(configDir);

        subsystems = new Subsystems(this);
    }

    @Override
    public String getRobotName() {
        return "LeviTator";
    }

    public Subsystems getSubsystems() {
        return subsystems;
    }

    @Override
    protected void initialize() {
        super.initialize();
        setupCameras();
    }

    public void setupCameras() {
        UsbCameraInfo[] usbCameras = UsbCamera.enumerateUsbCameras();
        logger.info("CameraServer reports {} usb cameras", usbCameras.length);
        if (usbCameras.length > 0) {
            logger.info("Using camera source: " + usbCameras[0].name);
            UsbCamera usbCamera = CameraServer.getInstance().startAutomaticCapture("USBWebCam_Pick", usbCameras[0].dev);
            MjpegServer server = CameraServer.getInstance().addServer("CameraServer2122");
            usbCamera.setResolution(320, 240);
        } else {
            logger.warn("No USB webcam!");
        }
    }

    @Override
    public SubsystemsBase getSubsystemsBase() {
        return subsystems;
    }

    @Override
    protected void registerCommands(ConfigCommandStore commandStore) {
        super.registerCommands(commandStore);
        registrar.register(commandStore);
    }

    @Override
    protected Command getAutoCommand() {
        return getSubsystems().getAuto().getSelectedCommand();
    }

    @Override
    public String getName() {
        return "LeviTator";
    }
}
