package com.googlecode.kanbanik.client.components.filter;

import java.awt.Color;
import javax.swing.JLabel;

public class UIManager {
    public void changeLabelColor(JLabel label, Color color) {
        try {
            label.setForeground(color);
        } catch (Exception e) {
            // Log e continuar
        }
    }

    public void setDueDateTextBoxVisibility(int condition, JTextField dueDateFromBox, JTextField dueDateToBox) {
        if (condition == BoardsFilter.DATE_CONDITION_BETWEEN) {
            dueDateToBox.setVisible(true);
        } else {
            dueDateToBox.setVisible(false);
        }

        if (condition == BoardsFilter.DATE_CONDITION_UNSET || condition == BoardsFilter.DATE_CONDITION_ONLY_WITHOUT) {
            dueDateToBox.setVisible(false);
            dueDateFromBox.setVisible(false);
        } else {
            dueDateFromBox.setVisible(true);
        }
    }
}