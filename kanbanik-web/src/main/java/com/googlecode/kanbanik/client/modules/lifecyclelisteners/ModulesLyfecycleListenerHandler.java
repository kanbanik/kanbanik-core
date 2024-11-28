package com.googlecode.kanbanik.client.modules.lifecyclelisteners;

import com.googlecode.kanbanik.client.Modules;
import com.googlecode.kanbanik.client.messaging.Message;
import com.googlecode.kanbanik.client.messaging.MessageBus;
import com.googlecode.kanbanik.client.messaging.MessageListener;
import com.googlecode.kanbanik.client.messaging.messages.modules.ModuleActivatedMessage;
import com.googlecode.kanbanik.client.messaging.messages.modules.ModuleDeactivatedMessage;

/**
 * Manages the lifecycle listeners for modules, handling module activation
 * and deactivation events.
 *
 * This class registers listeners for messages related to module activation
 * and deactivation, ensuring that the appropriate lifecycle methods are
 * triggered.
 */
public class ModulesLifecycleListenerHandler {

    private final Class<?> listenOnModule;
    private ModulesLifecycleListener listener;

    /**
     * Creates a new instance of the lifecycle listener handler for the specified module.
     *
     * @param listenOnModule the module to listen for lifecycle events
     * @param listener       the listener that handles activation and deactivation events
     */
    public ModulesLifecycleListenerHandler(Modules listenOnModule, ModulesLifecycleListener listener) {
        this.listenOnModule = listenOnModule.toClass();
        this.listener = listener;

        ActivatedListener activatedListener = new ActivatedListener();
        MessageBus.registerListener(ModuleActivatedMessage.class, activatedListener);
        MessageBus.registerListener(ModuleDeactivatedMessage.class, new DeactivatedListener(activatedListener));
    }

    /**
     * Listener for module activation events.
     */
    class ActivatedListener implements MessageListener<Class<?>> {

        @Override
        public void messageArrived(Message<Class<?>> message) {
            if (message.getPayload() == listenOnModule) {
                listener.activated();
            }
        }
    }

    /**
     * Listener for module deactivation events.
     */
    class DeactivatedListener implements MessageListener<Class<?>> {

        private ActivatedListener activatedListener;

        /**
         * Creates a new deactivated listener with the specified activated listener.
         *
         * @param activatedListener the activated listener to unregister upon deactivation
         */
        public DeactivatedListener(ActivatedListener activatedListener) {
            this.activatedListener = activatedListener;
        }

        @Override
        public void messageArrived(Message<Class<?>> message) {
            if (message.getPayload() == listenOnModule) {
                // Unregister the listeners for this module
                MessageBus.unregisterListener(ModuleActivatedMessage.class, activatedListener);
                MessageBus.unregisterListener(ModuleDeactivatedMessage.class, this);

                // Notify the listener of the deactivation
                listener.deactivated();

                // Prevent potential memory leaks
                listener = null;
                activatedListener = null;
            }
        }
    }
}
