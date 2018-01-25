package org.teamtators.rioplugin

import org.gradle.api.*

class RioPlugin implements Plugin<Project> {
    RioPluginExtension extension

    @Override
    void apply(Project project) {
        project.apply plugin: "java"

        def rioAddress = project.findProperty("rio.deploy.address") ?: ""
        def deployConfigStr = (project.findProperty("rio.deploy.config") as String)?.toUpperCase()
        DeployConfig deployConfig = (deployConfigStr as DeployConfig) ?: DeployConfig.NORMAL

        extension = project.extensions.create("rio", RioPluginExtension)
        extension.rioAddress = rioAddress
        extension.deployConfig = deployConfig
        extension.localConfigDir = project.rootProject.fileTree("config")

        project.configurations {
            nativeZip // zips that native .so files will be extracted from and deployed to the roboRIO
            nativeLib // .so files that will be deployed to the roboRIO
            compileJar // jar file which are deployed to the rio, separate from the main jar
            source    // source files
        }

        project.afterEvaluate {
            project.rio.validate()

            def conf = [project.configurations.nativeLib, project.configurations.nativeZip]
            conf.each { c ->
                c.dependencies.findAll { it != null }.collect {
                    def libfile = c.files(it)[0]
                }
            }

            project.jar {
                from project.configurations.compileJar.findAll { it.isDirectory() }
//                from project.configurations.compile.collect() { it.isDirectory() ? it : project.zipTree(it) }
                from project.configurations.compile.findAll { it.isDirectory() }
                manifest {
                    attributes(
                            'Main-Class': Constants.MAIN_CLASS,
                            'Robot-Class': project.rio.robotClass,
//                            'Class-Path': project.configurations.compileJar.collect {
//                                "${project.rio.libDir}/${it.getName()}"
//                            }.join(' ')
                            'Class-Path': project.configurations.compile.collect {
                                "${project.rio.libDir}/${it.getName()}"
                            }.join(' ')
                    )
                }
            }
        }

        def jarTask = project.tasks.getByPath("jar")

        project.tasks.create("findRio", FindRioTask)
        project.tasks.create("deployNativeLibs", DeployNativeLibsTask)

        project.tasks.create("restartCode", DeployTask) {
            description "Restarts the robot code on the roboRIO"
            restart = true
        }

        project.tasks.create("rebootRio", DeployTask) {
            description "Reboots the roboRIO"
            reboot = true
        }

        project.tasks.create("deployLibs", DeployTask) {
            description "Deploys all compile dependency JARs to the roboRIO"
            libs = true
        }

        project.tasks.create("deployConfig", DeployTask) {
            description "Deploys the config to the roboRIO and restarts the code"
            config = restart = true
        }

        project.tasks.create("deployJar", DeployTask) {
            description "Deploys the compiled JAR file and config to the roboRIO and restarts the code"
            dependsOn jarTask
            jar = config = restart = true
        }

        project.tasks.create("executeRio", DeployTask) {
            description "Executes the robot code on the roboRIO over an SSH connection"
            execute = true
        }

        project.tasks.create("deployConfigExecute", DeployTask) {
            description "Executes the robot code on the roboRIO over an SSH connection"
            config = execute = true
        }

        project.tasks.create("deployJarExecute", DeployTask) {
            description "Executes the robot code on the roboRIO over an SSH connection"
            dependsOn jarTask
            jar = config = execute = true
        }
    }
}




