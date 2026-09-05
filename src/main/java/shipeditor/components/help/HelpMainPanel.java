package shipeditor.components.help;

import shipeditor.utility.text.StringManager;

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

    private static final String[][] BUILT_IN_ARTICLES = {
        {"General", "getting_started.json"},
        {"General", "keyboard_shortcuts.json"},
        {"Guides", "station_modules.json"},
        {"User Documentation", "creating_help_articles.json"}
    };

    private void populateArticles() {
        DefaultMutableTreeNode rootNode = articlePanel.getRootNode();
        rootNode.removeAllChildren();

        java.util.Map<String, DefaultMutableTreeNode> sectionMap = new java.util.LinkedHashMap<>();

        loadClasspathArticles(rootNode, sectionMap);

        File articlesRoot = SettingsManager.getApplicationDirectory().resolve("help").toFile();
        if (articlesRoot.exists() && articlesRoot.isDirectory()) {
            File[] sectionFolders = articlesRoot.listFiles(a -> a.isDirectory());
            if (sectionFolders != null) {
                for (File sectionFolder : sectionFolders) {
                    addArticleSection(sectionFolder, rootNode, sectionMap);
                }
            }
        }
    }

    private void loadClasspathArticles(DefaultMutableTreeNode rootNode,
                                       java.util.Map<String, DefaultMutableTreeNode> sectionMap) {
        ObjectMapper objectMapper = FileUtilities.getConfigured();
        for (String[] entry : BUILT_IN_ARTICLES) {
            String sectionName = entry[0];
            String fileName = entry[1];
            String path = "/help/" + sectionName + "/" + fileName;
            try (java.io.InputStream is = HelpMainPanel.class.getResourceAsStream(path)) {
                if (is != null) {
                    HelpArticle article = objectMapper.readValue(is, HelpArticle.class);
                    DefaultMutableTreeNode sectionNode = sectionMap.computeIfAbsent(sectionName, s -> {
                        DefaultMutableTreeNode node = new DefaultMutableTreeNode(s);
                        rootNode.add(node);
                        return node;
                    });
                    sectionNode.add(new DefaultMutableTreeNode(article));
                }
            } catch (Exception e) {
                log.warn("Failed to load packaged help article: {}", path, e);
            }
        }
    }

    private void addArticleSection(File sectionFolder, DefaultMutableTreeNode rootNode,
                                   java.util.Map<String, DefaultMutableTreeNode> sectionMap) {
        String sectionName = sectionFolder.getName();
        DefaultMutableTreeNode sectionNode = sectionMap.computeIfAbsent(sectionName, s -> {
            DefaultMutableTreeNode node = new DefaultMutableTreeNode(s);
            rootNode.add(node);
            return node;
        });

        File[] articleFiles = sectionFolder.listFiles((dir, name) -> name.endsWith(".json"));

        if (articleFiles != null) {
            for (File articleFile : articleFiles) {
                HelpArticle article = this.readArticleFromFile(articleFile);
                if (article != null) {
                    sectionNode.add(new DefaultMutableTreeNode(article));
                }
            }
        }
    }

    private HelpArticle readArticleFromFile(File file) {
        ObjectMapper objectMapper = FileUtilities.getConfigured();
        try {
            return objectMapper.readValue(file, HelpArticle.class);
        } catch (IOException e) {
            log.error("Failed to load help article", e);
            JOptionPane.showMessageDialog(shipeditor.PrimaryWindow.getInstance(),
                    StringManager.getString("ENCOUNTERED_AN_ERROR_WHILE_TRYING_TO_DES_MSG") + file.getName(),
                    "Failed to load help article!",
                    JOptionPane.ERROR_MESSAGE);
            return new HelpArticle("Error: " + file.getName(), new java.util.ArrayList<>());
        }
    }

}
