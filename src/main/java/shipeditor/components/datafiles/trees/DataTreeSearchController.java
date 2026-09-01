package shipeditor.components.datafiles.trees;

import shipeditor.utility.components.UIConstants;

import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class DataTreeSearchController {

    private final DataTreePanel treePanel;
    private final JTree tree;
    private JTextField searchField;

    public DataTreeSearchController(DataTreePanel treePanel, JTree tree) {
        this.treePanel = treePanel;
        this.tree = tree;
    }

    public JPanel createSearchContainer() {
        JPanel searchContainer = new JPanel(new GridBagLayout());
        searchContainer.setBorder(UIConstants.EMPTY_BORDER);
        searchField = new JTextField();
        // Set the constraints for the search field.
        GridBagConstraints gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0; // Allow horizontal expansion.
        gridBagConstraints.insets = new Insets(0, 0, 0, 0); // Set padding.
        // Add the search field to the container with the specified constraints.
        searchContainer.add(searchField, gridBagConstraints);
        
        javax.swing.Timer timer = new javax.swing.Timer(300, e -> {
            String query = searchField.getText();
            if (query.isEmpty()) {
                tree.clearSelection();
                return;
            }
            List<DefaultMutableTreeNode> nodes = getMatchingNodes(query);
            if (!nodes.isEmpty()) {
                selectMatchedNodes(nodes);
            } else {
                tree.clearSelection();
            }
        });
        timer.setRepeats(false);
        
        searchField.getDocument().addDocumentListener(new TimerRestartListener(timer));
        
        return searchContainer;
    }

    private List<DefaultMutableTreeNode> getMatchingNodes(String input) {
        DefaultMutableTreeNode rootNode = treePanel.getRootNode();
        Enumeration<TreeNode> allNodes = rootNode.depthFirstEnumeration();
        Spliterator<TreeNode> spliterator = Spliterators.spliteratorUnknownSize(
                allNodes.asIterator(), Spliterator.ORDERED);
        Stream<TreeNode> stream = StreamSupport.stream(spliterator, false);
        return stream
                .filter(node -> node instanceof DefaultMutableTreeNode)
                .map(node -> (DefaultMutableTreeNode) node)
                .filter(node -> {
                    Object userObject = node.getUserObject();
                    if (userObject == null) return false;
                    String toString = userObject.toString().toLowerCase(Locale.ROOT);
                    return toString.contains(input.toLowerCase(java.util.Locale.ROOT));
                })
                .collect(Collectors.toList());
    }

    private void selectMatchedNodes(List<DefaultMutableTreeNode> nodes) {
        TreePath[] paths = nodes.stream()
                .map(node -> new TreePath(node.getPath()))
                .toArray(TreePath[]::new);
        tree.setSelectionPaths(paths);
        tree.scrollPathToVisible(paths[0]);
    }

    private static class TimerRestartListener implements javax.swing.event.DocumentListener {
        private final javax.swing.Timer timer;

        public TimerRestartListener(javax.swing.Timer timer) {
            this.timer = timer;
        }

        @Override
        public void insertUpdate(javax.swing.event.DocumentEvent e) { timer.restart(); }
        @Override
        public void removeUpdate(javax.swing.event.DocumentEvent e) { timer.restart(); }
        @Override
        public void changedUpdate(javax.swing.event.DocumentEvent e) { timer.restart(); }
    }
}
