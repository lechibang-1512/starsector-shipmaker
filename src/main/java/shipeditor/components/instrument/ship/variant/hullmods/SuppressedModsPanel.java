package shipeditor.components.instrument.ship.variant.hullmods;


import shipeditor.components.viewer.layers.ViewerLayer;
import shipeditor.components.viewer.layers.ship.ShipLayer;
import shipeditor.components.viewer.layers.ship.ShipPainter;
import shipeditor.components.viewer.layers.ship.data.ShipVariant;
import shipeditor.utility.components.ComponentUtilities;
import shipeditor.utility.text.StringValues;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.border.LineBorder;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.util.List;
import shipeditor.utility.themes.Themes;

/** * Simple list editor for variant suppressedMods (raw hullmod ID strings).
 * Follows the same visual pattern as {@link VariantHullmodsListPane}.*/
class SuppressedModsPanel extends JPanel {

    private DefaultListModel<String> listModel;
    private JList<String> modsList;
    private JTextField addField;

    SuppressedModsPanel() {
        this.setLayout(new BorderLayout());
        ComponentUtilities.outfitPanelWithTitle(this, "Suppressed Mods");

        listModel = new DefaultListModel<>();
        modsList = new JList<>(listModel);
        modsList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        modsList.setBorder(new LineBorder(Themes.getBorderColor()));
        modsList.setEnabled(false);
        this.add(new JScrollPane(modsList), BorderLayout.CENTER);

        JPanel controlPanel = new JPanel(new BorderLayout(4, 0));
        addField = new JTextField();
        addField.setToolTipText("Enter hullmod ID to suppress, then press Add or Enter");
        addField.addActionListener(e -> addFromField());
        controlPanel.add(addField, BorderLayout.CENTER);

        JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 0));

        JButton addButton = new JButton("Add");
        addButton.addActionListener(e -> addFromField());
        buttonsPanel.add(addButton);

        JButton removeButton = new JButton(StringValues.REMOVE);
        removeButton.addActionListener(e -> {
            String selected = modsList.getSelectedValue();
            if (selected == null) return;
            listModel.removeElement(selected);
            syncBackToVariant();
        });
        buttonsPanel.add(removeButton);

        controlPanel.add(buttonsPanel, BorderLayout.LINE_END);
        this.add(controlPanel, BorderLayout.PAGE_END);
    }

    private void addFromField() {
        String text = addField.getText();
        if (text == null || text.isBlank()) return;
        String trimmed = text.trim();
        if (listModel.contains(trimmed)) return;
        listModel.addElement(trimmed);
        addField.setText("");
        syncBackToVariant();
    }

    /**
     * Push the current list model contents back to the active variant's suppressedMods list.
     */
    private void syncBackToVariant() {
        ViewerLayer activeLayer = shipeditor.utility.overseers.StaticController.getActiveLayer();
        if (!(activeLayer instanceof ShipLayer shipLayer)) return;
        ShipPainter painter = shipLayer.getPainter();
        if (painter == null || painter.isUninitialized()) return;
        ShipVariant variant = painter.getActiveVariant();
        if (variant == null || variant.isEmpty()) return;

        List<String> updatedList = new java.util.ArrayList<>(listModel.getSize());
        for (int i = 0; i < listModel.getSize(); i++) {
            updatedList.add(listModel.getElementAt(i));
        }
        
        List<String> oldList = variant.getSuppressedMods();
        if (oldList == null) {
            oldList = new java.util.ArrayList<>();
        } else {
            oldList = new java.util.ArrayList<>(oldList);
        }
        
        shipeditor.undo.edits.features.FeatureEdits.SuppressedModsEdit edit = new shipeditor.undo.edits.features.FeatureEdits.SuppressedModsEdit(variant, activeLayer, oldList, updatedList);
        shipeditor.undo.UndoOverseer.post(edit);
        edit.redo();
    }

    void refreshListModel(ViewerLayer selected) {
        DefaultListModel<String> newModel = new DefaultListModel<>();
        if (!(selected instanceof ShipLayer checkedLayer)) {
            this.listModel = newModel;
            this.modsList.setModel(newModel);
            this.modsList.setEnabled(false);
            return;
        }
        ShipPainter painter = checkedLayer.getPainter();
        if (painter != null && !painter.isUninitialized()) {
            ShipVariant active = painter.getActiveVariant();
            if (active != null && !active.isEmpty()) {
                List<String> entries = active.getSuppressedMods();
                if (entries != null) {
                    newModel.addAll(entries);
                }
                this.modsList.setEnabled(true);
            } else {
                this.modsList.setEnabled(false);
            }
        } else {
            this.modsList.setEnabled(false);
        }
        this.listModel = newModel;
        this.modsList.setModel(newModel);
    }

}
