package shipeditor.components.datafiles;

import shipeditor.communication.EventBus;
import shipeditor.communication.events.components.ComponentEvents.SelectReferenceDataTab;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import java.awt.Dimension;

/**
 * Floating window for reference data (Hullmods, Ship Systems, Hull/Engine Styles).
 * Toggled via the "Show Reference Data" toolbar button.
 */
public class GameDataReferenceWindow {

    private static JFrame window;

    private GameDataReferenceWindow() {
    }

    public static void toggleWindow() {
        if (window == null) {
            createWindow();
        }
        window.setVisible(!window.isVisible());
        if (window.isVisible()) {
            window.toFront();
        }
    }

    private static void createWindow() {
        window = new JFrame("Reference Data");
        window.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);

        DataReferencePanel referencePanel = new DataReferencePanel();
        window.setContentPane(referencePanel);

        window.setMinimumSize(new Dimension(400, 500));
        window.setPreferredSize(new Dimension(600, 700));
        window.pack();
        window.setLocationRelativeTo(null);

        EventBus.subscribe(GameDataReferenceWindow.class, event -> {
            if (event instanceof SelectReferenceDataTab) {
                SwingUtilities.invokeLater(() -> {
                    if (window != null) {
                        window.setVisible(true);
                        window.toFront();
                    }
                });
            }
        });
    }

}
