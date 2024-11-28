package com.googlecode.kanbanik.client.components.task;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.googlecode.kanbanik.client.api.Dtos;
import com.googlecode.kanbanik.client.messaging.Message;
import com.googlecode.kanbanik.client.messaging.MessageBus;
import com.googlecode.kanbanik.client.messaging.MessageListener;
import com.googlecode.kanbanik.client.messaging.messages.task.ChangeTaskSelectionMessage;
import com.googlecode.kanbanik.client.messaging.messages.task.TaskChangedMessage;
import com.googlecode.kanbanik.client.messaging.messages.task.TaskDeletedMessage;
import com.googlecode.kanbanik.client.messaging.messages.task.TaskEditedMessage;

public class EventManager implements MessageListener<Dtos.TaskDto>, ClickHandler {
    private TaskGui taskGui;

    public EventManager(TaskGui taskGui) {
        this.taskGui = taskGui;
    }

    @Override
    public void onClick(ClickEvent event) {
        event.stopPropagation();
        event.preventDefault();
    }

    @Override
    public void messageArrived(Message<Dtos.TaskDto> message) {
        if (message instanceof TaskEditedMessage || message instanceof TaskChangedMessage) {
            taskGui.doTaskChanged(message);
        } else if (message instanceof TaskDeletedMessage) {
            if (message.getPayload().equals(taskGui.getDto())) {
                taskGui.unregisterListeners();
            }
        }
    }

    public void registerListeners() {
        MessageBus.registerListener(TaskEditedMessage.class, this);
        MessageBus.registerListener(TaskChangedMessage.class, this);
        MessageBus.registerListener(TaskDeletedMessage.class, this);
        MessageBus.registerListener(ChangeTaskSelectionMessage.class, taskGui.getTaskSelectionChangeListener());
    }

    public void unregisterListeners() {
        MessageBus.unregisterListener(TaskEditedMessage.class, this);
        MessageBus.unregisterListener(TaskChangedMessage.class, this);
        MessageBus.unregisterListener(TaskDeletedMessage.class, this);
        MessageBus.unregisterListener(ChangeTaskSelectionMessage.class, taskGui.getTaskSelectionChangeListener());
    }
}