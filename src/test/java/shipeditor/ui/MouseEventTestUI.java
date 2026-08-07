package shipeditor.ui;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.dnd.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class MouseEventTestUI {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Advanced Mouse Event & Drag-and-Drop Test");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

            DefaultMutableTreeNode root = new DefaultMutableTreeNode("Root");
            for (int i = 1; i <= 20; i++) {
                root.add(new DefaultMutableTreeNode("Test File " + i + ".ship"));
            }

            JTree tree = new JTree(root);
            tree.setToggleClickCount(1); // Simulate the project's setup

            JTextArea logArea = new JTextArea();
            logArea.setEditable(false);
            JScrollPane logScroll = new JScrollPane(logArea);
            
            // Helper to log and auto-scroll
            java.util.function.Consumer<String> logger = msg -> {
                logArea.append(msg + "\n");
                logArea.setCaretPosition(logArea.getDocument().getLength());
            };

            // Canvas Panel for Drag and Drop testing
            JPanel canvasPanel = new JPanel(new BorderLayout());
            canvasPanel.setBackground(new Color(40, 44, 52));
            JLabel canvasLabel = new JLabel("CANVAS (Drag and Drop Here / Right Click Here)", SwingConstants.CENTER);
            canvasLabel.setForeground(Color.WHITE);
            canvasPanel.add(canvasLabel, BorderLayout.CENTER);

            JSplitPane topSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, new JScrollPane(tree), canvasPanel);
            topSplit.setResizeWeight(0.3);

            // Context Menu for a specific Tree Node
            JPopupMenu nodeMenu = new JPopupMenu();
            JMenuItem nodeItem = new JMenuItem("Action for Selected Node");
            nodeItem.addActionListener(e -> logger.accept("[ContextMenu] Executed Node Action"));
            nodeMenu.add(nodeItem);

            // Context Menu for the empty Tree Background
            JPopupMenu treeBgMenu = new JPopupMenu();
            JMenuItem bgItem = new JMenuItem("Action for Tree Background");
            bgItem.addActionListener(e -> logger.accept("[ContextMenu] Executed Tree Background Action"));
            treeBgMenu.add(bgItem);

            // 1. Logging all Mouse Clicks on the Tree & Handling Context Menus
            tree.addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    logMouseEvent("Tree", e, logger);
                    
                    // Trigger context menu on right click
                    if (SwingUtilities.isRightMouseButton(e)) {
                        TreePath path = tree.getPathForLocation(e.getX(), e.getY());
                        if (path != null) {
                            // Clicked ON a node
                            tree.setSelectionPath(path); // Auto-select node before showing menu
                            nodeMenu.show(e.getComponent(), e.getX(), e.getY());
                            logger.accept("[TreeZone] Showing Context Menu for Node: " + path.getLastPathComponent());
                        } else {
                            // Clicked on empty space inside the tree
                            tree.clearSelection();
                            treeBgMenu.show(e.getComponent(), e.getX(), e.getY());
                            logger.accept("[TreeZone] Showing Context Menu for Tree Background");
                        }
                    }
                }
            });

            // Context Menu for the Canvas
            JPopupMenu canvasMenu = new JPopupMenu();
            JMenuItem canvasItem = new JMenuItem("Action for Canvas");
            canvasItem.addActionListener(e -> logger.accept("[ContextMenu] Executed Canvas Action"));
            canvasMenu.add(canvasItem);

            // 2. Logging all Mouse Clicks on the Canvas & Handling Context Menu
            canvasPanel.addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    logMouseEvent("Canvas", e, logger);
                    if (SwingUtilities.isRightMouseButton(e)) {
                        canvasMenu.show(e.getComponent(), e.getX(), e.getY());
                        logger.accept("[CanvasZone] Showing Context Menu for Canvas Area");
                    }
                }
            });

            // 3. Drag and Drop Setup
            // Setup Drag from Tree
            DragSource dragSource = DragSource.getDefaultDragSource();
            dragSource.createDefaultDragGestureRecognizer(tree, DnDConstants.ACTION_COPY, dge -> {
                TreePath path = tree.getSelectionPath();
                if (path != null) {
                    logger.accept("[DragSource] Drag gesture recognized for: " + path.getLastPathComponent());
                    StringSelection transferData = new StringSelection(path.getLastPathComponent().toString());
                    dge.startDrag(DragSource.DefaultCopyDrop, transferData);
                } else {
                    logger.accept("[DragSource] Drag gesture recognized but no item selected.");
                }
            });

            // Setup Drop to Canvas
            new DropTarget(canvasPanel, new DropTargetAdapter() {
                @Override
                public void drop(DropTargetDropEvent dtde) {
                    try {
                        dtde.acceptDrop(DnDConstants.ACTION_COPY);
                        Object data = dtde.getTransferable().getTransferData(java.awt.datatransfer.DataFlavor.stringFlavor);
                        logger.accept("[DropTarget] Dropped successfully onto Canvas: " + data);
                        dtde.dropComplete(true);
                    } catch (Exception ex) {
                        logger.accept("[DropTarget] Drop failed: " + ex.getMessage());
                        dtde.rejectDrop();
                    }
                }
            });

            JSplitPane verticalSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, topSplit, logScroll);
            verticalSplit.setResizeWeight(0.7);

            frame.add(verticalSplit);
            frame.setSize(900, 700);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
            logger.accept("UI Initialized. Try right-clicking on different zones (Node vs Tree Background vs Canvas).");
        });
    }

    private static void logMouseEvent(String componentName, MouseEvent e, java.util.function.Consumer<String> logger) {
        String buttonName;
        switch (e.getButton()) {
            case MouseEvent.BUTTON1: buttonName = "Left Click (BUTTON1)"; break;
            case MouseEvent.BUTTON2: buttonName = "Middle Click (BUTTON2)"; break;
            case MouseEvent.BUTTON3: buttonName = "Right Click (BUTTON3)"; break;
            case 0: buttonName = "No Button"; break;
            default: buttonName = "Programmable/Extra Button (BUTTON" + e.getButton() + ")"; break;
        }
        logger.accept(String.format("[%s] MousePressed - %s | Clicks: %d | Location: (%d, %d)", 
                componentName, buttonName, e.getClickCount(), e.getX(), e.getY()));
    }
}
