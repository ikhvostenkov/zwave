package com.oberasoftware.home.zwave.eventhandlers.actions.controller;

import com.oberasoftware.base.event.EventSubscribe;
import com.oberasoftware.home.zwave.api.ZWaveConverter;
import com.oberasoftware.home.zwave.api.actions.controller.InclusionStartAction;
import com.oberasoftware.home.zwave.api.events.ActionConvertedEvent;
import com.oberasoftware.home.zwave.api.messages.types.ControllerMessageType;
import com.oberasoftware.home.zwave.api.messages.types.MessageType;
import com.oberasoftware.home.zwave.eventhandlers.ActionConverterBuilder;
import com.oberasoftware.home.zwave.exceptions.HomeAutomationException;
import org.springframework.stereotype.Component;

@Component
public class InclusionStartActionConverter implements ZWaveConverter {

    private static final int ADD_NODE_ANY = 1;
    private static final int OPTION_HIGH_POWER = 0x80;
    private static final int OPTION_NETWORK_WIDE = 0x40;

    @EventSubscribe
    public ActionConvertedEvent convert(InclusionStartAction action) throws HomeAutomationException {
        byte command = ADD_NODE_ANY;
        if (action.isHighPower()) {
            command |= OPTION_HIGH_POWER;
        }
        if (action.isNetworkWide()) {
            command |= OPTION_NETWORK_WIDE;
        }
        return ActionConverterBuilder.create(ControllerMessageType.AddNodeToNetwork, MessageType.Request)
                .addByte(command).construct();
    }
}
