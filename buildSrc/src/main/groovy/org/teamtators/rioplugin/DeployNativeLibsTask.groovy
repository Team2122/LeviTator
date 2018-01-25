package org.teamtators.rioplugin

import org.gradle.api.DefaultTask
import org.gradle.api.file.FileVisitDetails
import org.gradle.api.tasks.TaskAction

class DeployNativeLibsTask extends DefaultTask {
    DeployNativeLibsTask() {
        group "deploy"
        description "Deploys all nativeLib and nativeZip dependencies to the roboRIO"
        dependsOn project.tasks.getByPath("findRio")
    }

    @TaskAction
    def deployNativeLibs() {
        RioPluginExtension rio = project.rio
        def sshService = rio.sshService
        def adminRemote = rio.cloneRemote()
        adminRemote.user = "admin"

        sshService.run {
            session(adminRemote) {
                println "==> Deploying native libraries to roboRIO"
                def putLibFile = { File libFile ->
                    println "-> ${libFile.name}"
                    put from: libFile, into: rio.remoteNativeLibsDir
                }

                def conf = project.configurations.nativeLib
                conf.dependencies.findAll { it != null }.collect {
                    putLibFile conf.files(it)[0]
                }

                def confZip = project.configurations.nativeZip
                confZip.dependencies.findAll { it != null }.collect {
                    def zipFile = confZip.files(it)[0]
                    def unzipped = new File(project.buildDir, "depUnzip/${zipFile.name}")
                    project.ant.unzip(src: zipFile,
                            dest: unzipped,
                            overwrite: "true")
                    project.fileTree(unzipped)
                            .include("*.so*", "lib/*.so*", "java/lib/*.so*")
                            .visit { FileVisitDetails visit ->
                        if (!visit.directory)
                            putLibFile visit.file
                    }
                }

                execute "killall -q netconsole-host 2> /dev/null || :", ignoreError: true // Kill netconsole
                def netConsoleHost = getClass().getResourceAsStream(Constants.Resources.NETCONSOLE_HOST)
                put from: netConsoleHost, into: Constants.Remote.NETCONSOLE_HOST
                execute "chmod +x ${Constants.Remote.NETCONSOLE_HOST}"

                execute "ldconfig"
            }
        }
    }
}