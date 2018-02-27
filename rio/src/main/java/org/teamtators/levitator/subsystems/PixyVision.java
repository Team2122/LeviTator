package org.teamtators.levitator.subsystems;

import org.teamtators.common.scheduler.Subsystem;
import org.teamtators.pixyjava.PixyUSB;

public class PixyVision extends Subsystem {
    static {
//        System.setProperty("java.lib.path");
//        System.err.println(System.getProperty("java.library.path"));
        System.load("/usr/local/frc/lib/libboost_thread.so.1.65.1");
        System.loadLibrary("pixyjava");
    }

    public PixyVision() {
        super("PixyVision");
        logger.info("Initializing the pixy");
        int err = PixyUSB.pixy_init();
        if (logPixyError(err)) {
            return; //abort
        }
        int[] major = new int[1];
        int[] minor = new int[1];
        int[] build = new int[1];
        err = PixyUSB.pixy_get_firmware_version(major, minor, build);

        if (logPixyError(err)) {
            return; //abort
        }

        logger.info("Pixy firmware version {}.{} build {}", major[0], minor[0], build[0]);
    }

    public boolean logPixyError(int code) {
        if (code != 0) {
            PixyUSB.pixy_error(code);
            return true;
        }
        return false;
    }
}
