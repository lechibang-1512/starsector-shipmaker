package shipeditor.utility.components;

import shipeditor.utility.themes.Themes;

import javax.swing.*;

public final class UIFactory {

    private UIFactory() {}

    public static JButton createButton(String text) {
        return new JButton(text);
    }

    public static JLabel createLabel(String text) {
        return new JLabel(text);
    }

    public static JPanel createPanel() {
        return new JPanel();
    }
}
