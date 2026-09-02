package shipeditor.utility.graphics;

import javax.swing.*;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.event.ActionEvent;
import java.util.Optional;
import java.util.function.Consumer;

public final class SmartColorPaste {

    private SmartColorPaste() {
    }

    /**
     * Attempts to read the system clipboard and parse it as a Hex Color.
     * Supports #RGB, #RGBA, #RRGGBB, #RRGGBBAA and their non-prefixed variants.
     * Whitespace is trimmed and parsing is case-insensitive.
     * Returns Optional.empty() if the clipboard doesn't contain a valid hex color string.
     */
    public static Optional<Color> tryGetHexFromClipboard() {
        if (!EventQueue.isDispatchThread()) {
            return Optional.empty(); // Should only be invoked from EDT
        }

        try {
            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            if (clipboard.isDataFlavorAvailable(DataFlavor.stringFlavor)) {
                String text = (String) clipboard.getData(DataFlavor.stringFlavor);
                if (text == null) return Optional.empty();
                
                text = text.trim();
                if (text.startsWith("#")) {
                    text = text.substring(1);
                }

                if (text.length() == 3 || text.length() == 4) {
                    // Expand #RGB to #RRGGBB and #RGBA to #RRGGBBAA
                    StringBuilder expanded = new StringBuilder();
                    for (char c : text.toCharArray()) {
                        expanded.append(c).append(c);
                    }
                    text = expanded.toString();
                }

                if (text.startsWith("0x") || text.startsWith("0X")) {
                    text = text.substring(2);
                }

                if (text.length() == 6) {
                    int r = Integer.parseInt(text.substring(0, 2), 16);
                    int g = Integer.parseInt(text.substring(2, 4), 16);
                    int b = Integer.parseInt(text.substring(4, 6), 16);
                    return Optional.of(new Color(r, g, b, 255));
                } else if (text.length() == 8) {
                    int r = Integer.parseInt(text.substring(0, 2), 16);
                    int g = Integer.parseInt(text.substring(2, 4), 16);
                    int b = Integer.parseInt(text.substring(4, 6), 16);
                    int a = Integer.parseInt(text.substring(6, 8), 16);
                    return Optional.of(new Color(r, g, b, a));
                }
            }
        } catch (HeadlessException | IllegalStateException | NumberFormatException | java.io.IOException | java.awt.datatransfer.UnsupportedFlavorException ignored) {
            // Do not throw, return empty if parsing or clipboard access fails
        }
        return Optional.empty();
    }

    /**
     * Installs a smart paste interceptor on the target JComponent.
     * If a valid color is in the clipboard during paste, it calls onValidColor and consumes the event.
     * Otherwise, it delegates to the original paste action.
     */
    public static void install(JComponent target, Consumer<Color> onValidColor) {
        String pasteActionName = "paste-color-interceptor";
        
        // Find existing paste action (DefaultEditorKit.pasteAction is typically "paste")
        Action existingPasteAction = target.getActionMap().get(javax.swing.text.DefaultEditorKit.pasteAction);
        if (existingPasteAction == null && target instanceof JTextComponent) {
            // For text components without explicit map overrides
            Action[] actions = ((JTextComponent) target).getActions();
            for (Action a : actions) {
                if (javax.swing.text.DefaultEditorKit.pasteAction.equals(a.getValue(Action.NAME))) {
                    existingPasteAction = a;
                    break;
                }
            }
        }

        Action finalExistingPasteAction = existingPasteAction;

        Action interceptor = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Optional<Color> pastedColor = tryGetHexFromClipboard();
                if (pastedColor.isPresent()) {
                    onValidColor.accept(pastedColor.get());
                } else if (finalExistingPasteAction != null) {
                    finalExistingPasteAction.actionPerformed(e);
                }
            }
        };

        // If it's a text component, replace the default paste action directly so Ctrl+V mapping naturally uses it
        if (target instanceof JTextComponent) {
            target.getActionMap().put(javax.swing.text.DefaultEditorKit.pasteAction, interceptor);
        } else {
            // For JLabels or other components, bind it explicitly to Ctrl+V
            target.setFocusable(true); // Must be focusable to receive key events
            KeyStroke pasteStroke = KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_V, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx());
            target.getInputMap(JComponent.WHEN_FOCUSED).put(pasteStroke, pasteActionName);
            target.getActionMap().put(pasteActionName, interceptor);
        }
    }
}
