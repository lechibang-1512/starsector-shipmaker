package oth.shipeditor.components;

import oth.shipeditor.communication.EventBus;
import oth.shipeditor.communication.events.components.LoadingActionFired;
import oth.shipeditor.communication.events.components.LoadingTaskCompleted;
import oth.shipeditor.communication.events.components.LoadingTaskStarted;

import javax.swing.*;
import java.awt.*;
import java.util.LinkedHashSet;
import java.util.Set;

public class ProgressBarPanel extends JPanel {

    private final JProgressBar progressBar;
    private final JLabel statusLabel;
    private final Set<String> activeTasks = new LinkedHashSet<>();

    ProgressBarPanel() {
        this.setLayout(new BoxLayout(this, BoxLayout.LINE_AXIS));
        progressBar = new JProgressBar(SwingConstants.HORIZONTAL);
        progressBar.setIndeterminate(true);
        statusLabel = new JLabel("");
        
        EventBus.subscribe(this, event -> {
            if (event instanceof LoadingActionFired checked) {
                if (checked.started()) {
                    this.add(statusLabel);
                    this.add(Box.createRigidArea(new Dimension(5, 0)));
                    this.add(progressBar);
                } else {
                    this.removeAll();
                    activeTasks.clear();
                    updateStatusLabel();
                }
                this.revalidate();
                this.repaint();
            } else if (event instanceof LoadingTaskStarted started) {
                activeTasks.add(started.taskName());
                updateStatusLabel();
            } else if (event instanceof LoadingTaskCompleted completed) {
                activeTasks.remove(completed.taskName());
                updateStatusLabel();
            }
        });
    }

    private void updateStatusLabel() {
        if (activeTasks.isEmpty()) {
            statusLabel.setText("");
        } else {
            statusLabel.setText("Loading: " + String.join(", ", activeTasks));
        }
    }

}
