package shipeditor.components.datafiles.trees;

import javax.swing.*;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.BorderLayout;

public class FilterDialogs {
    
    private static JDialog combinedFiltersDialog;
    private static ShipFilterPanel shipFilterPanel;
    private static WeaponFilterPanel weaponFilterPanel;
    
    public static void showCombinedFilters(Component parent) {
        if (combinedFiltersDialog == null) {
            combinedFiltersDialog = new JDialog();
            combinedFiltersDialog.setTitle("Data Filters");
            combinedFiltersDialog.setModal(false);
            combinedFiltersDialog.setDefaultCloseOperation(WindowConstants.HIDE_ON_CLOSE);
            combinedFiltersDialog.setAlwaysOnTop(true);
            
            JTabbedPane tabbedPane = new JTabbedPane();
            
            shipFilterPanel = new ShipFilterPanel();
            tabbedPane.addTab("Ship Filters", shipFilterPanel);
            
            weaponFilterPanel = new WeaponFilterPanel();
            tabbedPane.addTab("Weapon Filters", weaponFilterPanel);
            
            combinedFiltersDialog.getContentPane().add(tabbedPane, BorderLayout.CENTER);
            combinedFiltersDialog.setSize(new Dimension(400, 500));
        }
        
        if (!combinedFiltersDialog.isVisible()) {
            if (parent != null) {
                combinedFiltersDialog.setLocationRelativeTo(parent);
            }
            combinedFiltersDialog.setVisible(true);
        } else {
            combinedFiltersDialog.toFront();
        }
    }
}
