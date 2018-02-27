package org.teamtators.vision;

import edu.wpi.cscore.CameraServerJNI;
import edu.wpi.cscore.UsbCamera;
import edu.wpi.cscore.UsbCameraInfo;

/**
 * @author Alex Mikhalev
 */
public class VisionMain {
    public static void main(String[] args) {
        CameraServerJNI.enumerateSinks();
        UsbCameraInfo[] usbCameras = UsbCamera.enumerateUsbCameras();
        System.out.println(usbCameras.length + " USB cameras detected");
        if (usbCameras.length < 1) {
            System.err.println("Not enough cameras");
            System.exit(1);
        }
        UsbCameraInfo cameraInfo = usbCameras[0];
        System.out.println("Using camera \"" + cameraInfo.name + "\" at " + cameraInfo.path);
        UsbCamera camera = new UsbCamera("USB Camera", cameraInfo.path);
        if (camera.isConnected() && camera.isValid()) {
            System.out.println("Camera is connected and valid");
        } else {
            System.err.println("Camera is NOT connected and/or valid");
        }

        System.out.println("Shutting down...");
        camera.free();
    }
}
