package shipeditor.components.datafiles.trees;

import shipeditor.communication.EventBus;
import shipeditor.communication.events.components.ComponentEvents.CSVEntryIDChanged;
import shipeditor.components.datafiles.entities.CSVEntry;
import shipeditor.parsing.FileUtilities;
import shipeditor.persistence.SettingsManager;

import javax.swing.DefaultCellEditor;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellEditor;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

public class DataTreeTableBuilder {

    public static JScrollPane createTableFromMap(Map<String, String> data, CSVEntry csvEntry) {
        Set<Map.Entry<String, String>> entries = data.entrySet();
        Object[][] tableData = entries.stream()
                .map(entry -> new Object[]{entry.getKey(), entry.getValue()})
                .toArray(Object[][]::new);
        DefaultTableModel model = new DefaultTableModel(tableData, new Object[]{"Property", "Value"}) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 1; // Make the value column editable.
            }
        };
        model.addTableModelListener(e -> {
            if (e.getType() == javax.swing.event.TableModelEvent.UPDATE) {
                int row = e.getFirstRow();
                int column = e.getColumn();
                if (column == 1) {
                    String property = (String) model.getValueAt(row, 0);
                    String newValue = (String) model.getValueAt(row, 1);
                    // Detect ID column change and fire sync event
                    String cleanProperty = property.replace("\uFEFF", "").trim();
                    if ("id".equalsIgnoreCase(cleanProperty) && csvEntry != null) {
                        String oldValue = data.get(property);
                        if (oldValue != null && !oldValue.equals(newValue)) {
                            data.put(property, newValue);
                            EventBus.publish(
                                    new CSVEntryIDChanged(oldValue.trim(), newValue.trim(), csvEntry)
                            );
                            return;
                        }
                    }
                    data.put(property, newValue);
                }
            }
        });
        JTable table = new JTable(model) {
            @Override
            public TableCellEditor getCellEditor(int row, int column) {
                if (column == 1) {
                    String property = (String) getValueAt(row, 0);
                    if (property != null) {
                        TableCellEditor customEditor = getCustomEditorForProperty(property, csvEntry);
                        if (customEditor != null) {
                            return customEditor;
                        }
                    }
                }
                return super.getCellEditor(row, column);
            }

            public String getToolTipText(MouseEvent event) {
                String tip = null;
                Point p = event.getPoint();
                int rowIndex = rowAtPoint(p);
                int colIndex = columnAtPoint(p);
                try {
                    Object valueAt = getValueAt(rowIndex, colIndex);
                    tip = valueAt.toString();
                } catch (RuntimeException ignored) {}
                return tip;
            }
        };
        table.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);
        return new JScrollPane(table);
    }

    private static TableCellEditor getCustomEditorForProperty(String property, CSVEntry csvEntry) {
        String cleanProp = property.replace("\uFEFF", "").trim().toLowerCase(Locale.ROOT);
        return switch (cleanProp) {
            case "system id", "defense id" -> {
                if (csvEntry instanceof shipeditor.components.datafiles.entities.ShipCSVEntry) {
                    yield new DefaultCellEditor(createSystemIdCombobox());
                }
                yield null;
            }
            case "shield type" -> {
                if (csvEntry instanceof shipeditor.components.datafiles.entities.ShipCSVEntry) {
                    yield new DefaultCellEditor(new JComboBox<>(new String[]{"NONE", "FRONT", "OMNI", "PHASE"}));
                }
                yield null;
            }
            case "tech/manufacturer" -> new DefaultCellEditor(createTechManufacturerCombobox());
            case "formation" -> {
                if (csvEntry instanceof shipeditor.components.datafiles.entities.WingCSVEntry) {
                    yield new DefaultCellEditor(new JComboBox<>(new String[]{"V", "CLAW", "DIAMOND", "BOX"}));
                }
                yield null;
            }
            case "role" -> {
                if (csvEntry instanceof shipeditor.components.datafiles.entities.WingCSVEntry) {
                    yield new DefaultCellEditor(new JComboBox<>(new String[]{"BOMBER", "FIGHTER", "INTERCEPTOR", "ASSAULT", "SUPPORT"}));
                }
                yield null;
            }
            case "type" -> {
                if (csvEntry instanceof shipeditor.components.datafiles.entities.WeaponCSVEntry) {
                    yield new DefaultCellEditor(new JComboBox<>(new String[]{"BALLISTIC", "ENERGY", "MISSILE", "COMPOSITE", "SYNERGY", "HYBRID", "UNIVERSAL", "DECORATIVE", "SYSTEM", "BUILT_IN", "LAUNCH_BAY"}));
                }
                yield null;
            }
            case "damage type" -> {
                if (csvEntry instanceof shipeditor.components.datafiles.entities.WeaponCSVEntry) {
                    yield new DefaultCellEditor(new JComboBox<>(new String[]{"KINETIC", "HIGH_EXPLOSIVE", "FRAGMENTATION", "ENERGY", "OTHER"}));
                }
                yield null;
            }
            default -> null;
        };
    }

    private static JComboBox<String> createSystemIdCombobox() {
        shipeditor.representation.GameDataRepository gameData = SettingsManager.getGameData();
        Map<String, shipeditor.components.datafiles.entities.ShipSystemCSVEntry> systems = gameData.getAllShipsystemEntries();
        String[] systemIds = systems.keySet().stream().sorted().toArray(String[]::new);
        JComboBox<String> combo = new JComboBox<>(systemIds);
        combo.setEditable(true);
        return combo;
    }

    private static JComboBox<String> createTechManufacturerCombobox() {
        shipeditor.representation.GameDataRepository gameData = SettingsManager.getGameData();
        Set<String> techValues = new TreeSet<>();
        gameData.getAllShipEntries().values().forEach(entry -> {
            String tech = entry.getRowData().get("tech/manufacturer");
            if (tech != null && !tech.isEmpty()) techValues.add(tech);
        });
        JComboBox<String> combo = new JComboBox<>(techValues.toArray(new String[0]));
        combo.setEditable(true);
        return combo;
    }

    public static JPanel createTableButtons(CSVEntry entry) {
        JPanel buttonsContainer = new JPanel();
        buttonsContainer.setLayout(new GridLayout(1, 3));

        JButton openTableButton = new JButton("Open table");
        openTableButton.addActionListener(e -> {
            Path toOpen = entry.getTableFilePath();
            FileUtilities.openPathInDesktop(toOpen);
        });
        buttonsContainer.add(openTableButton);

        JButton openFolderButton = new JButton("Open folder");
        openFolderButton.addActionListener(e -> {
            Path toOpen = entry.getTableFilePath().getParent();
            if (toOpen != null) {
                FileUtilities.openPathInDesktop(toOpen);
            }
        });
        buttonsContainer.add(openFolderButton);

        JButton saveCsvButton = new JButton("Save CSV");
        saveCsvButton.addActionListener(e -> shipeditor.communication.EventBus.publish(
                new shipeditor.communication.events.files.FileEvents.CSVSaveQueued(entry)
        ));
        buttonsContainer.add(saveCsvButton);

        return buttonsContainer;
    }
}
