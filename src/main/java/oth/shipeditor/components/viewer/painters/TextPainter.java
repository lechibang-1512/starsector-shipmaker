package oth.shipeditor.components.viewer.painters;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import lombok.Getter;
import lombok.Setter;
import oth.shipeditor.utility.overseers.StaticController;
import oth.shipeditor.utility.Utility;
import oth.shipeditor.utility.graphics.DrawUtilities;

import java.awt.*;
import java.awt.font.GlyphVector;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

/** * Draws Strings in world coordinates.*/
@Getter @Setter
@SuppressFBWarnings({"EI_EXPOSE_REP", "EI_EXPOSE_REP2", "MS_EXPOSE_REP"})
public class TextPainter {

    private Point2D worldPosition;

    private String text;

    private boolean constantScreenShape;

    private final AffineTransform delegateTransform;

    private transient String cachedText;
    private transient Font cachedFont;
    private transient Shape cachedTextShape;
    private transient Shape cachedBoundsShape;

    public TextPainter() {
        this.worldPosition = new Point2D.Double();
        this.delegateTransform = new AffineTransform();
        this.constantScreenShape = true;
    }

    public void paintText(Graphics2D g, AffineTransform worldToScreen) {
        paintText(g, worldToScreen, Utility.getOrbitron(16));
    }

    public void paintText(Graphics2D g, AffineTransform worldToScreen, Font font) {
        paintText(g, worldToScreen, font, Color.WHITE);
    }

    public void paintText(Graphics2D g, AffineTransform worldToScreen, Font font, Color fill) {
        if (text == null || text.isEmpty()) return;

        double anchorOffsetX = 25;
        if (constantScreenShape) {
            delegateTransform.setToIdentity();
            Point2D screenPosition = worldToScreen.transform(worldPosition, null);
            delegateTransform.translate(screenPosition.getX(), screenPosition.getY());
            anchorOffsetX += (StaticController.getZoomLevel() * 0.25);
        } else {
            double textScale = 0.025;

            delegateTransform.setTransform(worldToScreen);
            delegateTransform.translate(worldPosition.getX(), worldPosition.getY());
            delegateTransform.scale(textScale, textScale);
            delegateTransform.rotate(StaticController.getRotationRadians());
        }

        if (cachedTextShape == null || !text.equals(cachedText) || !font.equals(cachedFont)) {
            GlyphVector glyphVector = font.createGlyphVector(g.getFontRenderContext(), text);
            cachedTextShape = glyphVector.getOutline();
            cachedBoundsShape = glyphVector.getLogicalBounds();
            cachedText = text;
            cachedFont = font;
        }

        Rectangle2D textBounds = cachedTextShape.getBounds2D();
        double anchorOffsetY = textBounds.getHeight() * 0.37;

        delegateTransform.translate(anchorOffsetX,anchorOffsetY);

        Shape transformedText = delegateTransform.createTransformedShape(cachedTextShape);
        Shape bounds = delegateTransform.createTransformedShape(cachedBoundsShape);

        DrawUtilities.paintOutlinedText(g, bounds, transformedText, null, fill);
    }

    /**
     * Renders multiple lines of text stacked vertically, anchored at the current world position.
     * The first line uses {@code headerFont} and subsequent lines use {@code detailFont}.
     * All lines share the same outlined-text rendering style.
     *
     * @param lines      array of text lines to render; null/empty entries are skipped.
     * @param headerFont font for the first line (header).
     * @param detailFont font for subsequent lines (details).
     * @param fill       fill color for all text.
     */
    public void paintMultiLineText(Graphics2D g, AffineTransform worldToScreen,
                                   String[] lines, Font headerFont, Font detailFont, Color fill) {
        if (lines == null || lines.length == 0) return;

        double anchorOffsetX = 25;
        if (constantScreenShape) {
            delegateTransform.setToIdentity();
            Point2D screenPosition = worldToScreen.transform(worldPosition, null);
            delegateTransform.translate(screenPosition.getX(), screenPosition.getY());
            anchorOffsetX += (StaticController.getZoomLevel() * 0.25);
        } else {
            double textScale = 0.025;

            delegateTransform.setTransform(worldToScreen);
            delegateTransform.translate(worldPosition.getX(), worldPosition.getY());
            delegateTransform.scale(textScale, textScale);
            delegateTransform.rotate(StaticController.getRotationRadians());
        }

        double lineSpacing = 4;
        double cumulativeY = 0;

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            if (line == null || line.isEmpty()) continue;

            Font lineFont = (i == 0) ? headerFont : detailFont;

            GlyphVector glyphVector = lineFont.createGlyphVector(g.getFontRenderContext(), line);
            Shape textShape = glyphVector.getOutline();

            Rectangle2D textBounds = textShape.getBounds2D();

            if (i == 0) {
                cumulativeY = textBounds.getHeight() * 0.37;
            }

            AffineTransform lineTransform = new AffineTransform(delegateTransform);
            lineTransform.translate(anchorOffsetX, cumulativeY);

            Shape transformedText = lineTransform.createTransformedShape(textShape);
            Shape bounds = lineTransform.createTransformedShape(glyphVector.getLogicalBounds());

            DrawUtilities.paintOutlinedText(g, bounds, transformedText, null, fill);

            cumulativeY += textBounds.getHeight() + lineSpacing;
        }
    }

}
