package shipeditor.components.help;

import shipeditor.communication.EventBus;
import shipeditor.components.help.parts.ArticleComponents.ArticlePart;
import shipeditor.utility.components.containers.TextScrollPanel;

import javax.swing.BoxLayout;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTree;
import javax.swing.ScrollPaneConstants;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.util.List;
import shipeditor.communication.events.components.ComponentEvents.GameDataPanelResized;

public class ArticleTreePanel extends JPanel {

    private final DefaultMutableTreeNode rootNode;
    private final JTree tree;
    private final JPanel rightPanel;
    private final Runnable reload;

    public ArticleTreePanel(Runnable reloadAction) {
        this.reload = reloadAction;
        this.setLayout(new BorderLayout());

        this.rootNode = new DefaultMutableTreeNode("Articles");
        this.tree = new JTree(rootNode);
        this.tree.setToggleClickCount(1);
        
        JScrollPane treeScroll = new JScrollPane(tree);
        treeScroll.setMinimumSize(new Dimension(120, 100));

        this.rightPanel = new JPanel();
        this.rightPanel.setLayout(new BoxLayout(rightPanel, BoxLayout.PAGE_AXIS));

        JScrollPane rightScroll = new JScrollPane(rightPanel);
        rightScroll.setBorder(null);
        rightScroll.setMinimumSize(new Dimension(120, 100));

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setOneTouchExpandable(true);
        splitPane.setResizeWeight(0.4f);
        splitPane.setLeftComponent(treeScroll);
        splitPane.setRightComponent(rightScroll);

        this.add(splitPane, BorderLayout.CENTER);

        this.initComponentListeners();
    }

    public DefaultMutableTreeNode getRootNode() {
        return rootNode;
    }

    public JTree getTree() {
        return tree;
    }

    public void reload() {
        reload.run();
    }

    private void initComponentListeners() {
        tree.addTreeSelectionListener(e -> {
            TreePath selectedNode = e.getNewLeadSelectionPath();
            if (selectedNode == null) return;
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) selectedNode.getLastPathComponent();
            if (node.getUserObject() instanceof HelpArticle checked) {
                updateEntryPanel(checked);
                EventBus.publish(new GameDataPanelResized(this.getMinimumSize()));
            }
        });
    }

    private void updateEntryPanel(HelpArticle selected) {
        rightPanel.removeAll();

        JPanel contentContainer = new TextScrollPanel(new FlowLayout());
        contentContainer.setLayout(new BoxLayout(contentContainer, BoxLayout.PAGE_AXIS));

        List<ArticlePart> articleParts = selected.getArticleParts();
        for (ArticlePart articlePart : articleParts) {
            contentContainer.add(articlePart.createContent());
        }
        
        JScrollPane scrollContainer = new JScrollPane(contentContainer);
        scrollContainer.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        JScrollBar verticalScrollBar = scrollContainer.getVerticalScrollBar();
        verticalScrollBar.setUnitIncrement(24);

        rightPanel.add(scrollContainer);
        rightPanel.revalidate();
        rightPanel.repaint();
    }
}
