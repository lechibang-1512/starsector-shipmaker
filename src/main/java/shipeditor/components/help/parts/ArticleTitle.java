package shipeditor.components.help.parts;

import lombok.Getter;
import lombok.Setter;
import shipeditor.utility.Utility;

import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

@Getter @Setter
public class ArticleTitle implements ArticlePart {

    private String title;

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
