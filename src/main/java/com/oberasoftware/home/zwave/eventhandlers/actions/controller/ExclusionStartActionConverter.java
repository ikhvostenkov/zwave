package com.oberasoftware.home.zwave.eventhandlers.actions.controller;

import com.oberasoftware.base.event.EventSubscribe;
import com.oberasoftware.home.zwave.api.ZWaveConverter;
import com.oberasoftware.home.zwave.api.actions.controller.ExclusionStartAction;
import com.oberasoftware.home.zwave.api.events.ActionConvertedEvent;
import com.oberasoftware.home.zwave.api.messages.types.ControllerMessageType;
import com.oberasoftware.home.zwave.api.messages.types.MessageType;
import com.oberasoftware.home.zwave.eventhandlers.ActionConverterBuilder;
import com.oberasoftware.home.zwave.exceptions.HomeAutomationException;
import org.springframework.stereotype.Component;

@Component
public class ExclusionStartActionConverter implements ZWaveConverter {

    private final int REMOVE_NODE_ANY = 1;

    @EventSubscribe
    public ActionConvertedEvent convert(ExclusionStartAction action) throws HomeAutomationException {
        return ActionConverterBuilder.create(ControllerMessageType.RemoveNodeFromNetwork, MessageType.Request)
                .addInt(REMOVE_NODE_ANY).construct();
    }
}
