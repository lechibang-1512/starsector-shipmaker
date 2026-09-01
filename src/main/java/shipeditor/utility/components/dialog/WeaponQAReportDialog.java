package shipeditor.utility.components.dialog;

import shipeditor.utility.text.StringManager;

import shipeditor.parsing.loading.FileLoading;
import shipeditor.persistence.database.DatabaseQueryService;
import shipeditor.persistence.database.IndexedFile;
import shipeditor.representation.weapon.WeaponSpecFile;
import shipeditor.utility.text.StringConstants;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingWorker;
import javax.swing.WindowConstants;
import javax.swing.table.AbstractTableModel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;

public class WeaponQAReportDialog extends JDialog {

    private final JTable table;
    private final QATableModel model;
    private final JLabel statusLabel;

    public WeaponQAReportDialog() {
        super(shipeditor.PrimaryWindow.getInstance(), "Weapon Offset QA Report", false);
        this.setLayout(new BorderLayout());
        this.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        this.setSize(700, 500);
        this.setLocationRelativeTo(shipeditor.PrimaryWindow.getInstance());

        model = new QATableModel();
        table = new JTable(model);
        table.setFillsViewportHeight(true);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getColumnModel().getColumn(0).setPreferredWidth(150); // Weapon ID
        table.getColumnModel().getColumn(1).setPreferredWidth(100); // Issue Type
        table.getColumnModel().getColumn(2).setPreferredWidth(450); // Details

        JScrollPane scrollPane = new JScrollPane(table);
        this.add(scrollPane, BorderLayout.CENTER);

        JPanel bottomPanel = new JPanel(new BorderLayout());
        statusLabel = new JLabel(StringManager.getString("SCANNING_WEAPONS"));
        statusLabel.setBorder(javax.swing.BorderFactory.createEmptyBorder(4, 4, 4, 4));
        bottomPanel.add(statusLabel, BorderLayout.WEST);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton closeButton = new JButton(StringManager.getString("CLOSE"));
        closeButton.addActionListener(e -> this.dispose());
        buttonPanel.add(closeButton);
        bottomPanel.add(buttonPanel, BorderLayout.EAST);

        this.add(bottomPanel, BorderLayout.SOUTH);

        runQAAnalysis();
    }

    private void runQAAnalysis() {
        SwingWorker<List<QAIssue>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<QAIssue> doInBackground() {
                List<QAIssue> issues = new ArrayList<>();
                List<IndexedFile> weaponFiles = DatabaseQueryService.getFilesByType(StringConstants.WEAPON_TYPE);

                for (IndexedFile f : weaponFiles) {
                    try {
                        WeaponSpecFile spec = FileLoading.loadWeaponFile(f.getFilePath().toFile());
                        if (spec == null) continue;

                        Point2D[] turret = spec.getTurretOffsets();
                        Point2D[] hardpoint = spec.getHardpointOffsets();

                        int turretBarrels = turret != null ? turret.length : 0;
                        int hardpointBarrels = hardpoint != null ? hardpoint.length : 0;

                        // 1. Check barrel count mismatch
                        if (turretBarrels != hardpointBarrels) {
                            issues.add(new QAIssue(spec.getId(), "Barrel Mismatch", 
                                "Turret has " + turretBarrels + " barrels, Hardpoint has " + hardpointBarrels + "."));
                        }

                        // 2. Check Y-symmetry
                        if (turret != null && turretBarrels > 1 && turretBarrels % 2 == 0) {
                            double ySum = 0;
                            for (Point2D pt : turret) {
                                ySum += pt.getY();
                            }
                            if (Math.abs(ySum) > 0.01) {
                                issues.add(new QAIssue(spec.getId(), "Asymmetrical Offsets", 
                                    "Turret Y-offsets sum to " + ySum + ". They might be asymmetrical."));
                            }
                        }

                    } catch (Exception e) {
                        issues.add(new QAIssue(f.getEntityId(), "Parse Error", "Failed to parse weapon file."));
                    }
                }
                return issues;
            }

            @Override
            protected void done() {
                try {
                    List<QAIssue> issues = get();
                    model.setIssues(issues);
                    if (issues.isEmpty()) {
                        statusLabel.setText(StringManager.getString("ALL_WEAPONS_PASSED_OFFSET_QA"));
                    } else {
                        statusLabel.setText(StringManager.getString("FOUND") + issues.size() + " potential offset issues.");
                    }
                } catch (Exception e) {
                    statusLabel.setText(StringManager.getString("ERROR_DURING_QA_ANALYSIS"));
                }
            }
        };
        worker.execute();
    }

    private record QAIssue(String weaponId, String issueType, String details) {}

    private static class QATableModel extends AbstractTableModel {
        private final String[] COLUMNS = {"Weapon ID", "Issue", "Details"};
        private List<QAIssue> issues = new ArrayList<>();

        public void setIssues(List<QAIssue> issues) {
            this.issues = issues;
            fireTableDataChanged();
        }

        @Override
        public int getRowCount() { return issues.size(); }
        @Override
        public int getColumnCount() { return COLUMNS.length; }
        @Override
        public String getColumnName(int column) { return COLUMNS[column]; }
        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) { return false; }
        
        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            QAIssue issue = issues.get(rowIndex);
            return switch (columnIndex) {
                case 0 -> issue.weaponId;
                case 1 -> issue.issueType;
                case 2 -> issue.details;
                default -> "";
            };
        }
    }
}
