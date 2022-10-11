package com.oberasoftware.home.zwave;

import com.oberasoftware.base.event.EventHandler;
import com.oberasoftware.base.event.EventSubscribe;
import com.oberasoftware.home.zwave.api.ZWaveSession;
import com.oberasoftware.home.zwave.api.actions.SwitchAction;
import com.oberasoftware.home.zwave.api.actions.controller.ExclusionStartAction;
import com.oberasoftware.home.zwave.api.actions.controller.InclusionStartAction;
import com.oberasoftware.home.zwave.api.actions.controller.InclusionStopAction;
import com.oberasoftware.home.zwave.api.events.devices.SwitchEvent;
import com.oberasoftware.home.zwave.api.messages.ZWaveRawMessage;
import com.oberasoftware.home.zwave.api.messages.types.ControllerMessageType;
import com.oberasoftware.home.zwave.exceptions.HomeAutomationException;
import com.oberasoftware.home.zwave.local.LocalZwaveSession;
import org.slf4j.Logger;

import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import static com.google.common.util.concurrent.Uninterruptibles.sleepUninterruptibly;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.slf4j.LoggerFactory.getLogger;

public class InclusionExclusionTest {
    private static final Logger LOG = getLogger(InclusionExclusionTest.class);

    public static void main(String[] args) {
        LOG.info("Starting Local ZWAVE App");
        try {
            doZwaveStuff();
        } catch (HomeAutomationException e) {
            LOG.error("", e);
        }
    }

    /**
     * Initialises the binding. This is called after the 'updated' method
     * has been called and all configuration has been passed.
     */
    public static void doZwaveStuff() throws HomeAutomationException {
        LOG.info("Application startup");
        ZWaveSession s = new LocalZwaveSession();
        s.subscribe(new MyEventListener(
                (switchId) -> {
                    try {
                        sleepUninterruptibly(15, TimeUnit.SECONDS);

                        LOG.info("--2-- Discovery Finished and Disabled.");
                        s.doAction(new InclusionStopAction());

                        sleepUninterruptibly(20, TimeUnit.SECONDS);


                        LOG.info("--3-- Switch {} added, switching it on...", switchId);
                        s.doAction(new SwitchAction(switchId, SwitchAction.STATE.ON));
                        sleepUninterruptibly(3, TimeUnit.SECONDS);

                        LOG.info("--4-- Switch {} in state ON, switching it off...", switchId);
                        s.doAction(new SwitchAction(switchId, SwitchAction.STATE.OFF));
                        sleepUninterruptibly(3, TimeUnit.SECONDS);

                        LOG.info("--5-- Enable device removal");
                        s.doAction(new ExclusionStartAction());
                    } catch (HomeAutomationException e) {
                        LOG.error("Could not switch off the switch {}: {}", switchId, e);
                    }
                }));
        s.connect();

        while (!s.isNetworkReady()) {
            LOG.info("Network not ready yet, sleeping");
            sleepUninterruptibly(1, TimeUnit.SECONDS);
        }

        LOG.info("--1-- Enable Discovery");
        s.doAction(new InclusionStartAction(true, true));
        sleepUninterruptibly(3, SECONDS);
    }

    public static class MyEventListener implements EventHandler {

        private final int ADD_NODE_STATUS_LEARN_READY = 1;
        private final int ADD_NODE_STATUS_NODE_FOUND = 2;
        private final int ADD_NODE_STATUS_ADDING_SLAVE = 3;
        private final int ADD_NODE_STATUS_ADDING_CONTROLLER = 4;
        private final int ADD_NODE_STATUS_PROTOCOL_DONE = 5;
        private final int ADD_NODE_STATUS_DONE = 6;
        private final int ADD_NODE_STATUS_FAILED = 7;

        private final int REMOVE_NODE_STATUS_LEARN_READY = 1;
        private final int REMOVE_NODE_STATUS_NODE_FOUND = 2;
        private final int REMOVE_NODE_STATUS_REMOVING_SLAVE = 3;
        private final int REMOVE_NODE_STATUS_REMOVING_CONTROLLER = 4;
        private final int REMOVE_NODE_STATUS_DONE = 6;
        private final int REMOVE_NODE_STATUS_FAILED = 7;

        private final Consumer<Integer> switchAdded;

        public MyEventListener(Consumer<Integer> switchAdded) {
            this.switchAdded = switchAdded;
        }

        @EventSubscribe
        public void handleEvent(ZWaveRawMessage event) {
            if (event.getControllerMessageType().equals(ControllerMessageType.AddNodeToNetwork)) {
                handleAddNodeEvent(event);
            }
            if (event.getControllerMessageType().equals(ControllerMessageType.RemoveNodeFromNetwork)) {
                handleRemoveNodeEvent(event);
            }
        }

        @EventSubscribe
        public void handleEvent(SwitchEvent event) {
            LOG.info("Received switch change for node: {} value: {}", event.getNodeId(), event.isOn() ? "Switch ON" : "Switch OFF");

            if (event.isTriggered()) {
                LOG.info("Received switch is triggered for node: {}", event.getNodeId());
            }
        }

        private void handleAddNodeEvent(ZWaveRawMessage source) {
            LOG.info("Handling add node to the network event: {}", source);

            int controllerNodeId = source.getMessageByte(0);
            LOG.info("Received node information from controller: {} based on last message: {}", controllerNodeId, source);

            switch (source.getMessageByte(1)) {
                case ADD_NODE_STATUS_LEARN_READY:
                    LOG.info("Add Node: Learn ready.");
                    break;

                case ADD_NODE_STATUS_NODE_FOUND:
                    LOG.info("Add Node: New node found.");
                    break;

                case ADD_NODE_STATUS_ADDING_SLAVE:
                    if (source.getMessageByte(2) < 1 || source.getMessageByte(2) > 232) {
                        break;
                    }
                    LOG.info("NODE {}: Adding Device.", source.getMessageByte(2));
                    break;
                case ADD_NODE_STATUS_ADDING_CONTROLLER:
                    // we never get here, as controller has been added before
                    if (source.getMessageByte(2) < 1 || source.getMessageByte(2) > 232) {
                        break;
                    }
                    LOG.info("NODE {}: Adding Controller.", source.getMessageByte(2));
                    break;

                case ADD_NODE_STATUS_PROTOCOL_DONE:
                    LOG.info("NODE {}: Add Node: Protocol done.", source.getMessageByte(2));
                    final int deviceNodeId = source.getMessageByte(2);
                    LOG.info("DEVICE_NODE {}: Adding slave.", deviceNodeId);
                    switchAdded.accept(deviceNodeId);
                    break;

                case ADD_NODE_STATUS_DONE:
                    LOG.info("Discovery: Finished.");
                    break;

                case ADD_NODE_STATUS_FAILED:
                    LOG.info("Add Node: Failed.");
                    break;

                default:
                    LOG.info("Add Node: Unknown request ({}).", source.getMessageByte(1));
                    break;
            }
        }

        private void handleRemoveNodeEvent(ZWaveRawMessage source) {
            LOG.info("Handling remove node to the network event: {}", source);

            switch (source.getMessageByte(1)) {
                case REMOVE_NODE_STATUS_LEARN_READY:
                    LOG.info("Remove Node: Learn ready.");
                    break;
                case REMOVE_NODE_STATUS_NODE_FOUND:
                    LOG.info("Remove Node: Node found for removal.");
                    break;
                case REMOVE_NODE_STATUS_REMOVING_SLAVE:
                    if (source.getMessageByte(2) < 0 || source.getMessageByte(2) > 232) {
                        break;
                    }
                    LOG.info("NODE {}: Removing slave.", source.getMessageByte(2));
                    break;
                case REMOVE_NODE_STATUS_REMOVING_CONTROLLER:
                    if (source.getMessageByte(2) < 0 || source.getMessageByte(2) > 232) {
                        break;
                    }
                    LOG.info("NODE {}: Removing controller.", source.getMessageByte(2));
                    break;
                case REMOVE_NODE_STATUS_DONE:
                    if (source.getMessageByte(2) < 0 || source.getMessageByte(2) > 232) {
                        break;
                    }
                    LOG.info("Remove Node: Done.");
                    break;
                case REMOVE_NODE_STATUS_FAILED:
                    LOG.info("Remove Node: Failed.");
                    break;
                default:
                    LOG.info("Remove Node: Unknown request ({}).", source.getMessageByte(1));
                    break;
            }
        }
    }
}
