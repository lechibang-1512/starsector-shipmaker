package shipeditor.components.datafiles.trees;

import shipeditor.communication.EventBus;
import shipeditor.communication.events.BusEvent;
import shipeditor.utility.components.ComponentUtilities;
import shipeditor.utility.components.UIConstants;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

public abstract class AbstractFilterPanel extends JPanel {

    private final Set<JCheckBox> allFilterBoxes = new HashSet<>();

    protected AbstractFilterPanel() {
        this.setLayout(new BorderLayout());
    }

    protected void initUI() {
        JPanel filtersPane = new JPanel();
        filtersPane.setLayout(new BoxLayout(filtersPane, BoxLayout.PAGE_AXIS));
        filtersPane.setAlignmentY(0);

        initFilterPanelContent(filtersPane);

        filtersPane.add(Box.createVerticalGlue());

        JScrollPane scrollContainer = new JScrollPane(filtersPane);
        JScrollBar verticalScrollBar = scrollContainer.getVerticalScrollBar();
        verticalScrollBar.setUnitIncrement(12);

        this.add(scrollContainer, BorderLayout.CENTER);
    }

    protected abstract void initFilterPanelContent(JPanel filtersPane);

    protected JPanel getSelectionButtonsPanel() {
        JButton selectAll = new JButton();
        selectAll.setText("Select all");
        selectAll.addActionListener(e -> this.toggleAll(true));

        JButton deselectAll = new JButton();
        deselectAll.setText("Deselect all");
        deselectAll.addActionListener(e -> this.toggleAll(false));

        JPanel buttonContainer = new JPanel();
        buttonContainer.setLayout(new BoxLayout(buttonContainer, BoxLayout.LINE_AXIS));
        buttonContainer.setBorder(new EmptyBorder(6, 6, 2, 0));
        buttonContainer.add(selectAll);
        buttonContainer.add(deselectAll);
        return buttonContainer;
    }

    protected abstract void toggleAll(boolean enable);

    protected void updateAllFilterBoxes(boolean enable) {
        allFilterBoxes.forEach(checkBox -> checkBox.setSelected(enable));
    }

    protected <T> JPanel createFilterSection(String title, Iterable<T> items, Map<T, Boolean> filtersMap,
                                             java.util.function.Function<T, String> nameFunction,
                                             java.util.function.Function<T, JLabel> iconFunction,
                                             java.util.function.Supplier<BusEvent> eventSupplier) {
        JPanel container = new JPanel();
        container.setLayout(new BoxLayout(container, BoxLayout.PAGE_AXIS));
        container.setAlignmentX(0.5f);
        container.setAlignmentY(0);

        ComponentUtilities.outfitPanelWithTitle(container, new Insets(1, 0, 0, 0), title);

        for (T item : items) {
            JPanel buttonContainer = new JPanel();
            buttonContainer.setLayout(new BoxLayout(buttonContainer, BoxLayout.LINE_AXIS));
            buttonContainer.setBorder(new EmptyBorder(4, 0, 0, 0));

            JCheckBox checkBox = new JCheckBox();
            checkBox.setText(nameFunction.apply(item));
            Boolean selected = filtersMap.get(item);
            checkBox.setSelected(selected != null ? selected : true);
            checkBox.addActionListener(e -> {
                filtersMap.put(item, checkBox.isSelected());
                EventBus.publish(eventSupplier.get());
            });

            if (iconFunction != null) {
                JLabel iconLabel = iconFunction.apply(item);
                if (iconLabel != null) {
                    buttonContainer.add(iconLabel);
                }
            }

            buttonContainer.add(checkBox);
            buttonContainer.add(Box.createHorizontalGlue());

            allFilterBoxes.add(checkBox);
            container.add(buttonContainer);
        }
        container.add(Box.createRigidArea(UIConstants.PADDING_10_4));
        return container;
    }
}
