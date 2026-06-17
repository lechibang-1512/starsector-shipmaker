package shipeditor.components.datafiles;

import shipeditor.PrimaryWindow;
import javax.swing.JFrame;
import javax.swing.WindowConstants;
import java.awt.BorderLayout;
import java.awt.Dimension;

public class GameDataReferenceWindow extends JFrame {

    private static GameDataReferenceWindow instance;
    private final DataReferencePanel dataReferencePanel;

    private GameDataReferenceWindow() {
        super("Game Data Reference");
        this.setDefaultCloseOperation(WindowConstants.HIDE_ON_CLOSE);
        this.setMinimumSize(new Dimension(300, 400));
        this.setPreferredSize(new Dimension(350, 800));

        this.dataReferencePanel = new DataReferencePanel();
        
        this.getContentPane().setLayout(new BorderLayout());
        this.getContentPane().add(this.dataReferencePanel, BorderLayout.CENTER);
        
        this.pack();
        this.setLocationRelativeTo(PrimaryWindow.getInstance());
    }

    public static GameDataReferenceWindow getInstance() {
        if (instance == null) {
            instance = new GameDataReferenceWindow();
        }
        return instance;
    }

    public static void showWindow() {
        GameDataReferenceWindow window = getInstance();
        if (!window.isVisible()) {
            window.setVisible(true);
        }
        window.toFront();
        window.requestFocus();
    }
    
    public static void toggleWindow() {
        GameDataReferenceWindow window = getInstance();
        if (window.isVisible()) {
            window.setVisible(false);
        } else {
            showWindow();
        }
    }
}
