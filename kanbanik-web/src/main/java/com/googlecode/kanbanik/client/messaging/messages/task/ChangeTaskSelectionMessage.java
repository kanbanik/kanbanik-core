package com.googlecode.kanbanik.client.messaging.messages.task;

import com.googlecode.kanbanik.client.messaging.BaseMessage;
import java.util.Arrays;
import java.util.List;
import static com.googlecode.kanbanik.client.api.Dtos.TaskDto;

public class ChangeTaskSelectionMessage extends BaseMessage<ChangeTaskSelectionParams> {

    public ChangeTaskSelectionMessage(ChangeTaskSelectionParams payload, Object source) {
        super(payload, source);
    }

    public static ChangeTaskSelectionMessage selectAll(Object source) {
        ChangeTaskSelectionParams params = ChangeTaskSelectionParams.selectAll();
        return new ChangeTaskSelectionMessage(params, source);
    }

    public static ChangeTaskSelectionMessage deselectAll(Object source) {
        ChangeTaskSelectionParams params = ChangeTaskSelectionParams.deselectAll();
        return new ChangeTaskSelectionMessage(params, source);
    }

    public static ChangeTaskSelectionMessage selectOne(TaskDto task, Object source) {
        ChangeTaskSelectionParams params = ChangeTaskSelectionParams.selectOne(task);
        return new ChangeTaskSelectionMessage(params, source);
    }

    public static ChangeTaskSelectionMessage selectList(List<TaskDto> tasks, Object source) {
        ChangeTaskSelectionParams params = ChangeTaskSelectionParams.selectList(tasks);
        return new ChangeTaskSelectionMessage(params, source);
    }
}

class ChangeTaskSelectionParams {

    private final boolean select;
    private final boolean all;
    private final boolean applyToYourself;
    private final List<TaskDto> tasks;

    private ChangeTaskSelectionParams(boolean select, boolean all, boolean applyToYourself, List<TaskDto> tasks) {
        this.select = select;
        this.all = all;
        this.applyToYourself = applyToYourself;
        this.tasks = tasks;
    }

    public static ChangeTaskSelectionParams selectAll() {
        return new ChangeTaskSelectionParams(true, true, false, null);
    }

    public static ChangeTaskSelectionParams deselectAll() {
        return new ChangeTaskSelectionParams(false, true, false, null);
    }

    public static ChangeTaskSelectionParams selectOne(TaskDto task) {
        return new ChangeTaskSelectionParams(true, false, false, Arrays.asList(task));
    }

    public static ChangeTaskSelectionParams selectList(List<TaskDto> tasks) {
        return new ChangeTaskSelectionParams(true, false, false, tasks);
    }

    public boolean isSelect() {
        return select;
    }

    public boolean isAll() {
        return all;
    }

    public List<TaskDto> getTasks() {
        return tasks;
    }

    public boolean isApplyToYourself() {
        return applyToYourself;
    }

    public void applySelection(TaskManager taskManager) {
        if (all) {
            if (select) {
                taskManager.selectAllTasks();
            } else {
                taskManager.deselectAllTasks();
            }
        } else if (tasks != null) {
            for (TaskDto task : tasks) {
                if (select) {
                    taskManager.selectTask(task);
                } else {
                    taskManager.deselectTask(task);
                }
            }
        }
    }
}