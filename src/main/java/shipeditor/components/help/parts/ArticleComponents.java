package shipeditor.components.help.parts;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.awt.Font;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.border.EmptyBorder;
import lombok.Getter;
import shipeditor.utility.Utility;
import shipeditor.utility.components.ComponentUtilities;

public class ArticleComponents {

    @com.fasterxml.jackson.annotation.JsonTypeInfo(use = com.fasterxml.jackson.annotation.JsonTypeInfo.Id.NAME, include = com.fasterxml.jackson.annotation.JsonTypeInfo.As.PROPERTY, property = "type")

    @com.fasterxml.jackson.annotation.JsonSubTypes({
            @com.fasterxml.jackson.annotation.JsonSubTypes.Type(value = ArticleCodeBlock.class, name = "CODE_BLOCK"),
            @com.fasterxml.jackson.annotation.JsonSubTypes.Type(value = ArticleSeparator.class, name = "SEPARATOR"),
            @com.fasterxml.jackson.annotation.JsonSubTypes.Type(value = ArticleTitle.class, name = "TITLE"),
            @com.fasterxml.jackson.annotation.JsonSubTypes.Type(value = ArticleTextBlock.class, name = "TEXT_BLOCK")
    })
    public static interface ArticlePart {

        String getTitle();

        JPanel createContent();

        ArticleType getType();

    }

    @Getter
    public static class ArticleCodeBlock implements ArticlePart {

        private String code;

        @JsonCreator
        public ArticleCodeBlock(@JsonProperty("code") String inputCode) {
            this.code = inputCode;
        }

        @Override
        public String getTitle() {
            return "Code:";
        }

        @Override
        public JPanel createContent() {
            JPanel container = new JPanel();
            container.setLayout(new BoxLayout(container, BoxLayout.LINE_AXIS));

            JTextArea codeArea = new JTextArea(code);
            codeArea.setEditable(false);
            codeArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
            JScrollPane scrollPane = new JScrollPane(codeArea);

            container.add(scrollPane);

            return container;
        }

        @Override
        public ArticleType getType() {
            return ArticleType.CODE_BLOCK;
        }
    }

    @Getter
    public static class ArticleSeparator implements ArticlePart {

        private String title;

        @JsonCreator
        public ArticleSeparator(@JsonProperty("title") String title) {
            this.title = title;
        }

        @Override
        public JPanel createContent() {
            JPanel container = ComponentUtilities.createTitledSeparatorPanel(title);
            container.setAlignmentY(0);
            return container;
        }

        @Override
        public ArticleType getType() {
            return ArticleType.SEPARATOR;
        }

    }

    @Getter
    public static enum ArticleType {
        TITLE, SEPARATOR, TEXT_BLOCK, CODE_BLOCK
    }

    @Getter
    public static class ArticleTitle implements ArticlePart {

        private String title;

        @JsonCreator
        public ArticleTitle(@JsonProperty("title") String title) {
            this.title = title;
        }

        @Override
        public JPanel createContent() {
            JPanel container = new JPanel();
            container.setLayout(new BoxLayout(container, BoxLayout.LINE_AXIS));
            JLabel titleLabel = new JLabel(title);
            titleLabel.setBorder(new EmptyBorder(4, 0, 6, 0));
            titleLabel.setFont(Utility.getOrbitron(14));
            container.add(titleLabel);
            container.setAlignmentY(0);

            return container;
        }

        @Override
        public ArticleType getType() {
            return ArticleType.TITLE;
        }

    }

    @Getter
    public static class ArticleTextBlock implements ArticlePart {

        private String text;

        private int pad;

        @JsonCreator
        public ArticleTextBlock(@JsonProperty("text") String text, @JsonProperty("pad") int pad) {
            this.text = text;
            this.pad = pad;
        }

        @Override
        public String getTitle() {
            return null;
        }

        @Override
        public JPanel createContent() {
            return ComponentUtilities.createTextPanel(text, pad);
        }

        @Override
        public ArticleType getType() {
            return ArticleType.TEXT_BLOCK;
        }

    }

}
