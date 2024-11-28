package com.googlecode.kanbanik.client.components.task;

import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.i18n.client.DateTimeFormat.PredefinedFormat;
import com.googlecode.kanbanik.client.api.Dtos;
import com.googlecode.kanbanik.client.managers.ClassOfServicesManager;

import java.util.Date;

public class TaskManager {
    private Dtos.TaskDto taskDto;

    public TaskManager(Dtos.TaskDto taskDto) {
        this.taskDto = taskDto;
    }

    public String getColorOf() {
        if (taskDto.getClassOfService() == null) {
            return "#" + ClassOfServicesManager.getInstance().getDefaultClassOfService().getColour();
        }
        return "#" + taskDto.getClassOfService().getColour();
    }

    public Date parseDueDate(String dueDate) {
        if (dueDate == null || "".equals(dueDate)) {
            return null;
        }
        try {
            return DateTimeFormat.getFormat(PredefinedFormat.DATE_SHORT).parse(dueDate);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}