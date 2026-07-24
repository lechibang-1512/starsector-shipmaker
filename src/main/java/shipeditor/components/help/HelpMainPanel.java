package shipeditor.components.help;

import com.fasterxml.jackson.databind.ObjectMapper;
import shipeditor.parsing.FileUtilities;
import shipeditor.persistence.SettingsManager;

import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import java.awt.BorderLayout;
import java.io.File;
import java.io.IOException;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class HelpMainPanel extends JPanel {

    private final ArticleTreePanel articlePanel;

    public HelpMainPanel() {
        this.setLayout(new BorderLayout());
        articlePanel = new ArticleTreePanel(this::populateArticles);

        this.populateArticles();

        JTree articlePanelTree = articlePanel.getTree();
        articlePanelTree.expandPath(new TreePath(articlePanel.getRootNode()));

        this.add(articlePanel, BorderLayout.CENTER);
    }

    private void populateArticles() {
        DefaultMutableTreeNode rootNode = articlePanel.getRootNode();
        rootNode.removeAllChildren();

        File articlesRoot = SettingsManager.getApplicationDirectory().resolve("help").toFile();

        if (!articlesRoot.exists() || !articlesRoot.isDirectory()) return;
        File[] sectionFolders = articlesRoot.listFiles(a -> a.isDirectory());

        if (sectionFolders == null) return;
        for (File sectionFolder : sectionFolders) {
            addArticleSection(sectionFolder);
        }
    }

    private void addArticleSection(File sectionFolder) {
        DefaultMutableTreeNode sectionNode = new DefaultMutableTreeNode(sectionFolder.getName());
        DefaultMutableTreeNode rootNode = articlePanel.getRootNode();
        rootNode.add(sectionNode);

        File[] articleFiles = sectionFolder.listFiles((dir, name) -> name.endsWith(".json"));

        if (articleFiles != null) {
            for (File articleFile : articleFiles) {
                HelpArticle article = this.readArticleFromFile(articleFile);
                sectionNode.add(new DefaultMutableTreeNode(article));
            }
        }
    }

    @SuppressWarnings({"CallToPrintStackTrace"})
    private HelpArticle readArticleFromFile(File file) {
        ObjectMapper objectMapper = FileUtilities.getConfigured();
        try {
            return objectMapper.readValue(file, HelpArticle.class);
        } catch (IOException e) {
            log.error("Failed to load help article", e);
            JOptionPane.showMessageDialog(shipeditor.PrimaryWindow.getInstance(),
                    "Encountered an error while trying to deserialize file with a help article: " + file.getName(),
                    "Failed to load help article!",
                    JOptionPane.ERROR_MESSAGE);
            return new HelpArticle("Error: " + file.getName(), new java.util.ArrayList<>());
        }
    }

}
