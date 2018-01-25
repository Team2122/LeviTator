package org.teamtators.common.datalogging;

public class NoopDashboardAdapter implements Dashboard {
    @Override
    public void putBoolean(String name, boolean val) {

    }

    @Override
    public void putNumber(String name, double val) {

    }

    @Override
    public void putString(String name, String val) {

    }
}
