package org.teamtators.common.datalogging;

public interface Dashboard {
    void putBoolean(String name, boolean val);

    void putNumber(String name, double val);

    void putString(String name, String val);

    enum Type {
        SMART_DASHBOARD,
        TATOR_DASHBOARD,
        NONE
    }
}
