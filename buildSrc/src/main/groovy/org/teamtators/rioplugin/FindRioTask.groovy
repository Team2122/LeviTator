package org.teamtators.rioplugin

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.tasks.TaskAction

class FindRioTask extends DefaultTask {
    FindRioTask() {
        group "deploy"
        description "Determine the active address for the RoboRIO"
        shouldRunAfter project.tasks.getByPath("jar")
    }

    @TaskAction
    def findRio() {
        RioPluginExtension rio = project.rio
        println "==> Looking for roboRIO on network"
        def candidates = [
                ["mDNS", rio.getRioHost("local")],
                ["DNS", rio.getRioHost("lan")],
                ["USB_ADDRESS", Constants.USB_ADDRESS],
                ["Static IP", rio.getRioIp()]
        ]
        if (rio.rioAddress != "") {
            candidates.add(0, ["Manually specified", rio.rioAddress])
        }
        boolean found = candidates.any { pair ->
            String name = pair[0]
            String addr = pair[1]
            print "-> ${name} (${addr})... "
            System.out.flush()
            if (rio.runSshTest(addr)) {
                println "SUCCESS"
                rio.remote.host = addr
                println "==> Found roboRIO at ${addr}"
                return true
            } else {
                println()
                return false
            }
        }
        if (!found) {
            throw new GradleException("Could not find roboRIO on network")
        }
    }
}