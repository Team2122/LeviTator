package org.teamtators.rioplugin

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import org.hidetake.groovy.ssh.core.Service as SshService

class DeployTask extends DefaultTask {
    boolean kill = false
    boolean libs = false
    boolean jar = false
    boolean config = false
    boolean restart = false
    boolean reboot = false
    boolean execute = false

    DeployTask() {
        group "deploy"
        dependsOn project.tasks.getByPath("findRio")
    }

    @TaskAction
    def doDeploy() {
        RioPluginExtension rio = project.rio
        SshService sshService = rio.sshService

        sshService.run {
            session(rio.remote) {
                if (this.kill || this.jar || this.execute || this.restart) {
                    println "==> Killing user code"
                    // Kill user code
                    execute rio.killCommand + " -t 2> /dev/null", ignoreError: true
                    execute "killall -q netconsole-host || :"
                    execute "killall -q java || :"
                }
                if (this.libs) {
                    println "==> Deploying dependency JARs to robot (and removing old ones)"
                    execute "rm -rf ${rio.libDir} || :"
                    execute "mkdir -p ${rio.libDir}"
                    def conf = project.configurations.compile
                    conf
                            .collect {
                        println "> ${it.name} -> ${rio.libDir}"
                        put from: it, into: rio.libDir
                    }
                }
                if (this.jar) {
                    def jarFile = project.jar.archivePath
                    def rioJarPath = "${rio.deployDir}/${jarFile.name}"

                    println "==> Deploying JAR to robot in configuration ${rio.deployConfig}"
                    execute "mkdir -p ${rio.deployDir}"
                    put from: jarFile, into: rio.deployDir
                    execute "chmod +x ${rioJarPath}"

                    String robotCommand = rio.getRobotCommand(rioJarPath)
                    def rc_local = new File(project.buildDir, "robotCommand")
                    rc_local.write("${robotCommand}\n")
                    put from: rc_local, into: rio.robotCommandPath
                    execute "chmod +x ${rio.robotCommandPath}"

                    execute "sync"
                }
                if (this.config) {
                    println "==> Deploying configs to robot"
                    execute "mkdir -p ${rio.configDir}"
                    put from: rio.localConfigDir, into: rio.configDir
                }
                if (this.restart) {
                    println "==> Restarting robot code"
                    execute rio.killCommand + " -t -r", ignoreError: true
                }
                if (this.reboot) {
                    println "==> Rebooting roboRIO"
                    execute "reboot"
                }
                if (this.execute) {
                    println "==> Executing robot code"
                    execute rio.robotCommandPath, logging: "stdout"
                }
            }
        }
    }
}