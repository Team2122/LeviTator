package org.teamtators.rioplugin

class Constants {
    static final USB_ADDRESS = "172.91.22.2"
    static final MAIN_CLASS = "org.teamtators.common.Robot"

    class Resources {
        static final NETCONSOLE_HOST = "/org/teamtators/rioplugin/netconsole-host"
    }

    class Remote {
        static final NETCONSOLE_HOST = "/usr/local/frc/bin/netconsole-host"
        static final ROBOT_COMAMND = "/home/lvuser/robotCommand"
        static final KILL_COMMAND =
                ". /etc/profile.d/natinst-path.sh; /usr/local/frc/bin/frcKillRobot.sh"
        static final DEPLOY_DIR = "/home/lvuser"
        static final JAVA = "/usr/local/frc/JRE/bin/java"
        static final ROBOT_USER = "lvuser"
        static final NATIVE_LIBS = "/usr/local/frc/lib"
    }
}
