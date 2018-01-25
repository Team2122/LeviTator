package org.teamtators.rioplugin

import org.gradle.api.GradleException
import org.gradle.api.file.FileTree
import org.gradle.api.plugins.ExtensionAware
import org.hidetake.groovy.ssh.Ssh
import org.hidetake.groovy.ssh.connection.AllowAnyHosts
import org.hidetake.groovy.ssh.core.Remote
import org.hidetake.groovy.ssh.core.Service as SshService

class RioPluginExtension {
    SshService sshService = Ssh.newService()

    String teamNumber
    String robotClass

    String rioAddress
    Remote remote = new Remote(
            name: "roboRIO",
            timeoutSec: 3,
            user: Constants.Remote.ROBOT_USER,
            password: "",
            knownHosts: AllowAnyHosts.instance,
    )

    // remote paths
    String remoteNativeLibsDir = Constants.Remote.NATIVE_LIBS
    String robotCommandPath = Constants.Remote.ROBOT_COMAMND
    String deployDir
    String configDir
    String libDir

    def setDeployDir(String deployDir) {
        this.deployDir = deployDir
        configDir = "$deployDir/config"
        libDir = "$deployDir/libs"
    }

    FileTree localConfigDir

    String killCommand = Constants.Remote.KILL_COMMAND
    String runArgs = ""
    String jvmArgs = ""
    String robotCommand
    int debugPort = 8348

    DeployConfig deployConfig

    RioPluginExtension() {
        this.deployDir = Constants.Remote.DEPLOY_DIR
        (this as ExtensionAware).getExtensions().add(Remote.class, "remote", this.remote)
    }

    def validate() {
        if (!this.teamNumber) {
            throw new GradleException("RioPlugin requires that project.rio.teamNumber be set")
        }
        if (!this.robotClass) {
            throw new GradleException("RioPlugin requires that project.rio.robotClass be set")
        }
    }

    String getRioHost(String tld = "local") {
        return "roboRIO-${this.teamNumber}-FRC.${tld}"
    }

    String getRioIp() {
        String team = this.teamNumber
        def teamLen = team.length()
        if (teamLen < 4)
            for (int i = 0; i < 4 - teamLen; i++)
                team = "0" + team
        return "10.${team.substring(0, 2)}.${team.substring(2, 4)}.2"
    }

    boolean runSshTest(String addr) {
        assert addr
        def remote = this.cloneRemote()
        remote.host = addr
        try {
            this.sshService.run {
                session(remote) {
                }
            }
            return true
        } catch (ignored) {
            print(ignored.toString())
            return false
        }
    }

    String getRobotCommand(String jarFile) {
        if (this.robotCommand) {
            return this.robotCommand
        }
        String jvmArgs = this.jvmArgs ?: "";
        switch (this.deployConfig) {
            case DeployConfig.DEBUG:
            case DeployConfig.DEBUG_SUSPEND:
                def suspend = (this.deployConfig == DeployConfig.DEBUG_SUSPEND) ? 'y' : 'n'
                jvmArgs += " -agentlib:jdwp=transport=dt_socket,address=${this.debugPort},server=y,suspend=${suspend}"
                break;
        }
        Constants.Remote.NETCONSOLE_HOST + " " + Constants.Remote.JAVA +
                " -Djava.library.path=${Constants.Remote.NATIVE_LIBS} " + jvmArgs +
                " -jar ${jarFile} ${this.runArgs}"
    }

    Remote cloneRemote(Remote remote = this.remote) {
        def props = Remote.metaClass.getProperties()
                .findAll { it.getSetter() != null }
                .collectEntries {
            [it.getName(), it.getProperty(remote)]
        }
        new Remote(props)
    }
}