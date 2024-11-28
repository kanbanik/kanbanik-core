package com.googlecode.kanbanik.client.components.task;

import java.util.Date;

import com.google.gwt.dom.client.Style.Display;
import com.google.gwt.user.client.ui.*;

public class UITaskManager {
    public void setupDueDateLabel(HTML dueDateLabel, String dueDateText, Date dueDate, TaskGui.Style style, TaskGui.TaskGuiTemplates template) {
        if (dueDate == null) {
            dueDateLabel.setVisible(false);
            return;
        }

        dueDateLabel.setVisible(true);
        setupHumanReadableDueDateText(dueDateLabel, dueDateText, dueDate, style, template);
    }

    private void setupHumanReadableDueDateText(HTML dueDateLabel, String dueDateText, Date dueDate, TaskGui.Style style, TaskGui.TaskGuiTemplates template) {
        Date nowDate = new Date();
        nowDate.setMinutes(0);
        nowDate.setHours(0);
        nowDate.setSeconds(0);

        final long DAY_MILLIS = 1000 * 60 * 60 * 24;

        long day1 = dueDate.getTime() / DAY_MILLIS;
        long day2 = nowDate.getTime() / DAY_MILLIS;
        long diff = day1 - day2;

        if (diff < 0) {
            dueDateLabel.setTitle("Due date deadline (" + dueDateText + ") is already missed!");
            dueDateLabel.setHTML(template.messageWithLink(style.missedStyle(), "missed!"));
            return;
        }

        if (diff == 0) {
            dueDateLabel.setTitle("Due date deadline (" + dueDateText + ") is today!");
            dueDateLabel.setText("today");
            return;
        }

        dueDateLabel.setTitle("Due date deadline (" + dueDateText + ") is in " + diff + " days.");
        if (diff > 365) {
            dueDateLabel.setText(" > year");
            return;
        }

        if (diff > 31) {
            dueDateLabel.setText(" > month");
            return;
        }

        if (diff > 7) {
            dueDateLabel.setText(" > week");
            return;
        }

        if (diff == 1) {
            dueDateLabel.setText("tomorrow");
            return;
        }

        dueDateLabel.setText(diff + " days");
    }

    public void setElementDisplay(Widget widget, Display display) {
        widget.getElement().getStyle().setDisplay(display);
    }
}