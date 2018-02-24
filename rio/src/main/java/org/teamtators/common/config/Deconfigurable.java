package org.teamtators.common.config;

/**
 * @author Alex Mikhalev
 */
public interface Deconfigurable {
    /**
     * Called to release any resources and reset any state allocated when the object
     * was configured
     */
    void deconfigure();
}
