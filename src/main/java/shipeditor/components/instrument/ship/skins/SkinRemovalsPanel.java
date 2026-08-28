package shipeditor.components.instrument.ship.skins;

import shipeditor.communication.EventBus;
import shipeditor.communication.events.viewer.layers.LayerEvents.ActiveLayerUpdated;
import shipeditor.communication.events.viewer.layers.LayerEvents.LayerWasSelected;
import shipeditor.components.ComponentEnums.EditorInstrument;
import shipeditor.components.datafiles.entities.HullmodCSVEntry;
import shipeditor.components.datafiles.entities.WingCSVEntry;
import shipeditor.components.viewer.layers.ViewerLayer;
import shipeditor.components.viewer.layers.ship.ShipLayer;
import shipeditor.components.viewer.layers.ship.ShipPainter;
import shipeditor.components.viewer.layers.ship.data.ShipSkin;
import shipeditor.persistence.SettingsManager;
import shipeditor.representation.GameDataRepository;
import shipeditor.undo.UndoOverseer;
import shipeditor.undo.edits.features.SkinOverrideEdits.SkinListOverrideEdit;
import shipeditor.utility.components.ComponentUtilities;
import shipeditor.utility.components.UIConstants;
import shipeditor.communication.events.components.ComponentEvents.InstrumentRepaintQueued;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class SkinRemovalsPanel extends JPanel {

    private final JLabel statusLabel;
    private final JPanel contentContainer;
    private ShipPainter cachedPainter;

    public SkinRemovalsPanel() {
        this.setLayout(new BorderLayout());

        statusLabel = new JLabel("No skin active");
        statusLabel.setHorizontalAlignment(SwingConstants.CENTER);
        statusLabel.setBorder(new EmptyBorder(8, 8, 8, 8));

        contentContainer = new JPanel();
        contentContainer.setLayout(new BoxLayout(contentContainer, BoxLayout.Y_AXIS));

        JScrollPane scrollPane = new JScrollPane(contentContainer);
        scrollPane.setBorder(UIConstants.EMPTY_BORDER);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);

        this.add(statusLabel, BorderLayout.PAGE_START);
        this.add(scrollPane, BorderLayout.CENTER);

        this.initEventListening();
    }

    private void initEventListening() {
        EventBus.subscribe(this, event -> {
            if (event instanceof LayerWasSelected checked) {
                handleLayerChange(checked.selected());
            } else if (event instanceof ActiveLayerUpdated checked) {
                handleLayerChange(checked.updated());
            }
        });
        EventBus.subscribe(this, event -> {
            if (event instanceof InstrumentRepaintQueued checked) {
                if (checked.editorMode() == EditorInstrument.SKIN_REMOVALS) {
                    refreshContent();
                }
            }
        });
    }

    private void handleLayerChange(ViewerLayer layer) {
        if (layer instanceof ShipLayer shipLayer) {
            ShipPainter painter = shipLayer.getPainter();
            if (painter != null && !painter.isUninitialized()) {
                this.cachedPainter = painter;
                refreshContent();
                return;
            }
        }
        this.cachedPainter = null;
        refreshContent();
    }

    private void refreshContent() {
        contentContainer.removeAll();

        if (cachedPainter == null || cachedPainter.isUninitialized()) {
            statusLabel.setText("No ship layer selected");
            contentContainer.revalidate();
            contentContainer.repaint();
            return;
        }

        ShipSkin activeSkin = cachedPainter.getActiveSkin();
        if (activeSkin == null || activeSkin.isBase()) {
            statusLabel.setText("No skin active — select a skin in the Skin Data tab");
            contentContainer.revalidate();
            contentContainer.repaint();
            return;
        }

        statusLabel.setText("Skin: " + activeSkin);

        contentContainer.add(createRemovalListPanel("Remove Weapon Slots", activeSkin,
                activeSkin::getRemoveWeaponSlots, activeSkin::setRemoveWeaponSlots,
                String.class, "Enter Slot ID"));

        contentContainer.add(createRemovalListPanel("Remove Engine Slots", activeSkin,
                activeSkin::getRemoveEngineSlots, activeSkin::setRemoveEngineSlots,
                Integer.class, "Enter Engine Index (e.g. 0)"));

        contentContainer.add(createRemovalListPanel("Remove Built-in Weapons", activeSkin,
                activeSkin::getRemoveBuiltInWeapons, activeSkin::setRemoveBuiltInWeapons,
                String.class, "Enter Slot ID"));

        contentContainer.add(createRemovalListPanel("Remove Built-in Hullmods", activeSkin,
                activeSkin::getRemoveBuiltInMods, activeSkin::setRemoveBuiltInMods,
                HullmodCSVEntry.class, "Enter Hullmod ID"));

        contentContainer.add(createRemovalListPanel("Remove Built-in Wings", activeSkin,
                activeSkin::getRemoveBuiltInWings, activeSkin::setRemoveBuiltInWings,
                WingCSVEntry.class, "Enter Wing ID"));

        contentContainer.add(Box.createVerticalGlue());
        contentContainer.revalidate();
        contentContainer.repaint();
    }

    private <T> JPanel createRemovalListPanel(String title, ShipSkin skin,
                                              Supplier<List<T>> getter, Consumer<List<T>> setter,
                                              Class<T> typeClass, String inputHint) {
        JPanel panel = new JPanel(new BorderLayout());
        ComponentUtilities.outfitPanelWithTitle(panel, title);

        List<T> currentList = getter.get();
        if (currentList == null) currentList = new ArrayList<>();

        DefaultListModel<String> listModel = new DefaultListModel<>();
        for (T item : currentList) {
            if (item instanceof HullmodCSVEntry entry) {
                listModel.addElement(entry.getID());
            } else if (item instanceof WingCSVEntry entry) {
                listModel.addElement(entry.getID());
            } else {
                listModel.addElement(String.valueOf(item));
            }
        }

        JList<String> jList = new JList<>(listModel);
        jList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        jList.setVisibleRowCount(4);
        JScrollPane scrollPane = new JScrollPane(jList);
        panel.add(scrollPane, BorderLayout.CENTER);

        JPanel controlPanel = new JPanel(new BorderLayout(4, 0));
        JTextField inputField = new JTextField();
        inputField.setToolTipText(inputHint);
        JButton addButton = new JButton("Add");
        JButton removeButton = new JButton("Remove");

        addButton.addActionListener(e -> {
            String text = inputField.getText().trim();
            if (text.isEmpty() || listModel.contains(text)) return;

            T item = parseInput(text, typeClass);
            if (item != null) {
                List<T> newList = new ArrayList<>(getter.get() != null ? getter.get() : new ArrayList<>());
                newList.add(item);

                var edit = new SkinListOverrideEdit<>(setter, getter.get(), newList, EditorInstrument.SKIN_REMOVALS, skin);
                UndoOverseer.post(edit);
                edit.redo();
            }
        });

        removeButton.addActionListener(e -> {
            int selected = jList.getSelectedIndex();
            if (selected != -1) {
                List<T> newList = new ArrayList<>(getter.get());
                newList.remove(selected);
                if (newList.isEmpty()) newList = null;

                var edit = new SkinListOverrideEdit<>(setter, getter.get(), newList, EditorInstrument.SKIN_REMOVALS, skin);
                UndoOverseer.post(edit);
                edit.redo();
            }
        });

        JPanel btnPanel = new JPanel(new GridLayout(1, 2, 4, 0));
        btnPanel.add(addButton);
        btnPanel.add(removeButton);

        controlPanel.add(inputField, BorderLayout.CENTER);
        controlPanel.add(btnPanel, BorderLayout.LINE_END);
        controlPanel.setBorder(new EmptyBorder(4, 0, 0, 0));

        panel.add(controlPanel, BorderLayout.PAGE_END);
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 160));

        return panel;
    }

    @SuppressWarnings("unchecked")
    private <T> T parseInput(String input, Class<T> typeClass) {
        if (typeClass == String.class) {
            return (T) input;
        } else if (typeClass == Integer.class) {
            try {
                return (T) Integer.valueOf(input);
            } catch (NumberFormatException e) {
                return null;
            }
        } else if (typeClass == HullmodCSVEntry.class) {
            HullmodCSVEntry entry = GameDataRepository.retrieveHullmodCSVEntryByID(input);
            if (entry == null) {
                JOptionPane.showMessageDialog(this, "Unknown Hullmod ID: " + input, "Error", JOptionPane.ERROR_MESSAGE);
            }
            return (T) entry;
        } else if (typeClass == WingCSVEntry.class) {
            WingCSVEntry entry = GameDataRepository.retrieveWingCSVEntryByID(input);
            if (entry == null) {
                JOptionPane.showMessageDialog(this, "Unknown Wing ID: " + input, "Error", JOptionPane.ERROR_MESSAGE);
            }
            return (T) entry;
        }
        return null;
    }
}
