package org.teamtators.common.scheduler;

public class TriggerSources {
    public static TriggerSource constant(boolean active) {
        return () -> active;
    }
}
