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

        try {
            java.net.URL url = HelpMainPanel.class.getResource("/help");
            if (url == null) {
                log.error("Could not find /help resource.");
                return;
            }
            java.net.URI uri = url.toURI();
            java.nio.file.Path myPath;
            if (uri.getScheme().equals("jar")) {
                java.nio.file.FileSystem fileSystem;
                try {
                    fileSystem = java.nio.file.FileSystems.getFileSystem(uri);
                } catch (java.nio.file.FileSystemNotFoundException e) {
                    fileSystem = java.nio.file.FileSystems.newFileSystem(uri, java.util.Collections.emptyMap());
                }
                myPath = fileSystem.getPath("/help");
            } else {
                myPath = java.nio.file.Paths.get(uri);
            }
            
            try (java.util.stream.Stream<java.nio.file.Path> stream = java.nio.file.Files.list(myPath)) {
                stream.filter(java.nio.file.Files::isDirectory).forEach(this::addArticleSection);
            }
        } catch (Exception e) {
            log.error("Failed to load help articles from classpath", e);
        }
    }

    private void addArticleSection(java.nio.file.Path sectionFolder) {
        String folderName = sectionFolder.getFileName().toString();
        // ZipFileSystem might have trailing slashes in directory names
        if (folderName.endsWith("/")) {
            folderName = folderName.substring(0, folderName.length() - 1);
        }
        DefaultMutableTreeNode sectionNode = new DefaultMutableTreeNode(folderName);
        DefaultMutableTreeNode rootNode = articlePanel.getRootNode();
        rootNode.add(sectionNode);

        try (java.util.stream.Stream<java.nio.file.Path> stream = java.nio.file.Files.list(sectionFolder)) {
            stream.filter(p -> p.toString().endsWith(".json")).forEach(articleFile -> {
                HelpArticle article = this.readArticleFromFile(articleFile);
                if (article != null) {
                    sectionNode.add(new DefaultMutableTreeNode(article));
                }
            });
        } catch (Exception e) {
            log.error("Failed to read section folder: " + sectionFolder, e);
        }
    }

    @SuppressWarnings({"CallToPrintStackTrace"})
    private HelpArticle readArticleFromFile(java.nio.file.Path file) {
        ObjectMapper objectMapper = FileUtilities.getConfigured();
        try (java.io.InputStream is = java.nio.file.Files.newInputStream(file)) {
            return objectMapper.readValue(is, HelpArticle.class);
        } catch (Exception e) {
            log.error("Failed to load help article", e);
            JOptionPane.showMessageDialog(shipeditor.PrimaryWindow.getInstance(),
                    "Encountered an error while trying to deserialize file with a help article: " + file.getFileName().toString(),
                    "Failed to load help article!",
                    JOptionPane.ERROR_MESSAGE);
            return new HelpArticle("Error: " + file.getFileName().toString(), new java.util.ArrayList<>());
        }
    }

}
