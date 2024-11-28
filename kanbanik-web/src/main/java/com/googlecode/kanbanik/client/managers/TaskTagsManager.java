package com.googlecode.kanbanik.client.managers;

import com.googlecode.kanbanik.client.api.DtoFactory;
import com.googlecode.kanbanik.client.api.Dtos;
import com.googlecode.kanbanik.client.messaging.Message;
import com.googlecode.kanbanik.client.messaging.MessageBus;
import com.googlecode.kanbanik.client.messaging.MessageListener;
import com.googlecode.kanbanik.client.messaging.messages.task.TaskAddedMessage;
import com.googlecode.kanbanik.client.messaging.messages.task.TaskDeletedMessage;
import com.googlecode.kanbanik.client.messaging.messages.task.TaskEditedMessage;

import java.util.*;

/**
 * Gerencia as tags das tarefas, incluindo adicionar, remover e atualizar associações de tags com tarefas.
 */
public class TaskTagsManager {

    private static final TaskTagsManager INSTANCE = new TaskTagsManager();

    private TagsChangedListener listener;
    private final List<Dtos.TaskTag> tags = new ArrayList<>();
    private final Map<String, List<String>> taskIdToTagName = new HashMap<>();
    private static Dtos.TaskTag noTag;

    private TaskTagsManager() {
        // Registrar listeners para eventos de tarefas
        MessageBus.registerListener(TaskAddedMessage.class, new TaskListener());
        MessageBus.registerListener(TaskEditedMessage.class, new TaskListener());
        MessageBus.registerListener(TaskDeletedMessage.class, new TaskRemovedListener());
    }

    public static TaskTagsManager getInstance() {
        return INSTANCE;
    }

    public Dtos.TaskTag noTag() {
        if (noTag == null) {
            noTag = DtoFactory.taskTag();
            noTag.setName("No Tag");
            noTag.setColour("white");
        }
        return noTag;
    }

    public synchronized void addTaskTag(String taskId, Dtos.TaskTag toAdd) {
        taskIdToTagName.computeIfAbsent(taskId, k -> new ArrayList<>()).add(toAdd.getName());

        if (!tags.contains(toAdd)) {
            tags.add(toAdd);
            notifyTagAdded(toAdd);
        }
    }

    public synchronized void removeTaskTag(String tagName) {
        Dtos.TaskTag tag = findTagByName(tagName);
        if (tag != null) {
            tags.remove(tag);
            notifyTagRemoved(tag);
        }
    }

    public synchronized List<Dtos.TaskTag> getTags() {
        return new ArrayList<>(tags);
    }

    public synchronized void setListener(TagsChangedListener listener) {
        this.listener = listener;
    }

    private Dtos.TaskTag findTagByName(String tagName) {
        for (Dtos.TaskTag tag : tags) {
            if (tag.getName().equals(tagName)) {
                return tag;
            }
        }
        return null;
    }

    private boolean isReferenced(String tagName) {
        for (List<String> tagList : taskIdToTagName.values()) {
            if (tagList.contains(tagName)) {
                return true;
            }
        }
        return false;
    }

    private void notifyTagAdded(Dtos.TaskTag tag) {
        if (listener != null) {
            listener.added(tag);
        }
    }

    private void notifyTagRemoved(Dtos.TaskTag tag) {
        if (listener != null) {
            listener.removed(tag);
        }
    }

    // Listeners separados para melhorar a responsabilidade da classe
    private class TaskListener implements MessageListener<Dtos.TaskDto> {
        public void messageArrived(Message<Dtos.TaskDto> message) {
            Dtos.TaskDto task = message.getPayload();
            if (task == null) {
                return;
            }

            String taskId = task.getId();
            List<Dtos.TaskTag> taskTags = task.getTaskTags();

            if (taskTags != null) {
                List<String> newTags = new ArrayList<>();
                for (Dtos.TaskTag tag : taskTags) {
                    addTaskTag(taskId, tag);
                    newTags.add(tag.getName());
                }

                List<String> existingTags = taskIdToTagName.getOrDefault(taskId, new ArrayList<>());
                for (String tagName : new ArrayList<>(existingTags)) {
                    if (!newTags.contains(tagName)) {
                        existingTags.remove(tagName);
                        if (!isReferenced(tagName)) {
                            removeTaskTag(tagName);
                        }
                    }
                }
            }
        }
    }

    private class TaskRemovedListener implements MessageListener<Dtos.TaskDto> {
        public void messageArrived(Message<Dtos.TaskDto> message) {
            Dtos.TaskDto task = message.getPayload();
            if (task == null) {
                return;
            }

            String taskId = task.getId();
            List<String> tagsToRemove = taskIdToTagName.remove(taskId);
            if (tagsToRemove != null) {
                for (String tagName : tagsToRemove) {
                    if (!isReferenced(tagName)) {
                        removeTaskTag(tagName);
                    }
                }
            }
        }
    }

    public interface TagsChangedListener {
        void added(Dtos.TaskTag tag);
        void removed(Dtos.TaskTag tag);
    }
}