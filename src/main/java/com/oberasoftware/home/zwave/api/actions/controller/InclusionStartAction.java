package com.oberasoftware.home.zwave.api.actions.controller;

import com.oberasoftware.home.zwave.api.ZWaveAction;

public class InclusionStartAction implements ZWaveAction {

    private boolean highPower;
    private boolean networkWide;
    public InclusionStartAction(boolean highPower, boolean networkWide) {
        this.highPower = highPower;
        this.networkWide = networkWide;
    }

    public boolean isHighPower() {
        return highPower;
    }

    public boolean isNetworkWide() {
        return networkWide;
    }

    @Override
    public String toString() {
        return "InclusionStartAction{}";
    }
}
