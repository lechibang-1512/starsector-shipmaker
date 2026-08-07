package shipeditor.components.instrument.ship.variant;

import shipeditor.communication.EventBus;
import shipeditor.communication.events.viewer.points.PointEvents.PointSelectedConfirmed;
import shipeditor.components.datafiles.entities.WeaponCSVEntry;
import shipeditor.components.ComponentEnums.EditorInstrument;

import shipeditor.components.viewer.layers.ViewerLayer;
import shipeditor.components.viewer.layers.ship.ShipLayer;
import shipeditor.components.viewer.layers.ship.ShipPainter;
import shipeditor.components.viewer.layers.ship.data.ShipHull;
import shipeditor.components.viewer.layers.ship.data.ShipVariant;
import shipeditor.components.viewer.painters.points.ship.features.InstalledFeature;
import shipeditor.utility.components.ComponentUtilities;
import shipeditor.utility.components.dialog.DialogUtilities;
import shipeditor.utility.components.rendering.CustomTreeNode;
import shipeditor.utility.overseers.StaticController;
import shipeditor.utility.text.StringValues;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ToolTipManager;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.List;
import shipeditor.communication.events.components.ComponentEvents.InstrumentRepaintQueued;
import shipeditor.communication.events.components.ComponentEvents.WeaponEntryPicked;

public class VariantWeaponsPanel extends AbstractVariantPanel {

    private final VariantWeaponsTree weaponsTree;

    private final JPanel contentPanel;

    private final JPanel northPanel;

    private JPanel pickedWeaponPanel;



    public VariantWeaponsPanel() {
        this.setLayout(new BorderLayout());

        northPanel = new JPanel();
        northPanel.setLayout(new BorderLayout());
        this.add(northPanel, BorderLayout.PAGE_START);

        contentPanel = new JPanel();
        contentPanel.setLayout(new BorderLayout());
        this.add(contentPanel, BorderLayout.CENTER);



        CustomTreeNode weaponGroups = new CustomTreeNode("Weapon Groups");
        weaponsTree = new VariantWeaponsTree(weaponGroups, feature -> {});
        ToolTipManager.sharedInstance().registerComponent(weaponsTree);

        JScrollPane scroller = new JScrollPane(weaponsTree);
        contentPanel.add(scroller, BorderLayout.CENTER);

        ViewerLayer layer = StaticController.getActiveLayer();
        this.refreshPanel(layer);
    }



    @Override
    protected void initLayerListeners() {
        super.initLayerListeners();
        EventBus.subscribe(this, event -> {
            if (event instanceof PointSelectedConfirmed checked) {
                if (weaponsTree != null) {
                    weaponsTree.selectNode(checked.point());
                }
            }
        });
        EventBus.subscribe(this, event -> {
            if (event instanceof InstrumentRepaintQueued checked) {
                if (checked.editorMode() == EditorInstrument.VARIANT_WEAPONS) {
                    this.refreshPanel(StaticController.getActiveLayer());
                    this.refreshWeaponPicker();
                }
            }
        });
        EventBus.subscribe(this, event -> {
            if (event instanceof WeaponEntryPicked) {
                this.refreshWeaponPicker();
            }
        });
    }

    private void refreshWeaponPicker() {
        if (pickedWeaponPanel != null) {
            contentPanel.remove(pickedWeaponPanel);
        }

        WeaponCSVEntry pickedForInstall = null;
        if (StaticController.getActiveLayer() instanceof ShipLayer shipLayer) {
            pickedForInstall = shipLayer.getFeaturesOverseer().getWeaponForInstall();
        }
        if (pickedForInstall != null) {
            pickedWeaponPanel = pickedForInstall.createPickedWeaponPanel();
        } else {
            String weaponHint = StringValues.USE_RIGHT_CLICK_CONTEXT_MENU_OF_GAME_DATA_WIDGET_TO_ADD_ENTRIES;
            pickedWeaponPanel = ComponentUtilities.createHintPanel(weaponHint, "Info:");
            Insets insets = new Insets(1, 0, 0, 0);
            ComponentUtilities.outfitPanelWithTitle(pickedWeaponPanel, insets, StringValues.PICKED_WEAPON);
        }
        contentPanel.add(pickedWeaponPanel, BorderLayout.PAGE_START);

        this.revalidate();
        this.repaint();
    }

    @Override
    public void refreshPanel(ViewerLayer selected) {
        weaponsTree.clearRoot();
        northPanel.removeAll();

        if (!(selected instanceof ShipLayer checkedLayer)) {
            return;
        }

        ShipHull shipHull = checkedLayer.getHull();
        if (shipHull == null) {
            return;
        }
        ShipPainter painter = checkedLayer.getPainter();

        ShipVariant activeVariant = painter.getActiveVariant();
        if (activeVariant != null) {
            weaponsTree.setSlotPainter(painter.getWeaponSlotPainter());
            weaponsTree.repopulateTree(activeVariant, checkedLayer);

            JPanel buttonContainer = new JPanel(new BorderLayout());
            buttonContainer.setBorder(new EmptyBorder(4, 4, 0, 4));

            JButton rearrangeGroups = new JButton("Rearrange weapons");

            List<InstalledFeature> allFittedWeaponsList = activeVariant.getAllFittedWeaponsList();
            if (allFittedWeaponsList.isEmpty()) {
                rearrangeGroups.setEnabled(false);
            }

            rearrangeGroups.addActionListener(e -> DialogUtilities.showWeaponGroupsDialog(activeVariant));

            buttonContainer.add(rearrangeGroups, BorderLayout.PAGE_START);


            northPanel.add(buttonContainer, BorderLayout.PAGE_START);

            northPanel.add(VariantWeaponsPanel.createDataSummary(checkedLayer, activeVariant),
                    BorderLayout.CENTER);
        }
        this.revalidate();
        this.repaint();
    }

    private static JPanel createDataSummary(ShipLayer shipLayer, ShipVariant activeVariant) {
        JPanel container = new JPanel();
        ComponentUtilities.outfitPanelWithTitle(container, "Fitted weapons");
        container.setLayout(new GridBagLayout());

        JLabel shipOPCapLabel = new JLabel(StringValues.TOTAL_OP_CAPACITY);
        int shipOPTotalValue = shipLayer.getTotalOP();
        JLabel shipOPCap = new JLabel(String.valueOf(shipOPTotalValue));

        ComponentUtilities.addLabelAndComponent(container, shipOPCapLabel, shipOPCap, 0);

        JLabel usedOPTotalLabel = new JLabel("Used OP for ship:");
        int usedOP = shipLayer.getTotalUsedOP();
        JLabel usedOPTotal = new JLabel(String.valueOf(usedOP));

        ComponentUtilities.addLabelAndComponent(container, usedOPTotalLabel, usedOPTotal, 1);

        JLabel totalOPLabel = new JLabel("Total OP in weapons:");
        int totalOPInWeapons = activeVariant.getTotalOPInWeapons();
        JLabel value = new JLabel(String.valueOf(totalOPInWeapons));

        ComponentUtilities.addLabelAndComponent(container, totalOPLabel, value, 2);

        return container;
    }

}
