package shipeditor.utility.components;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;

/**
 * Centralized UI constants to eliminate magic numbers.
 */
public final class UIConstants {

    private UIConstants() {}

    /**
     * Standard minimal panel size.
     */
    public static final Dimension MINIMUM_PANEL_SIZE = new Dimension(120, 100);

    /**
     * Standard padding dimension used across UI filter and properties panels.
     */
    public static final Dimension PADDING_10_4 = new Dimension(10, 4);

    /**
     * Common zero padding border.
     */
    public static final Border EMPTY_BORDER = BorderFactory.createEmptyBorder();

    public static final Insets INSETS_NONE = new Insets(0, 0, 0, 0);
    public static final Insets INSETS_TOP_1 = new Insets(1, 0, 0, 0);
    public static final Insets INSETS_SMALL = new Insets(2, 2, 2, 2);
    public static final Insets INSETS_MEDIUM = new Insets(4, 4, 4, 4);

}
