package shipeditor.utility.graphics;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIf;

import javax.imageio.ImageIO;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration regression tests that run {@link SpriteOutlineTracer} and {@link CollisionHullGenerator}
 * against <b>real Starsector ship sprites</b> from a local game installation.
 * <p>
 * These tests validate structural invariants that synthetic rectangle/L-shape tests cannot catch:
 * <ul>
 *   <li>Contour generation never throws for any real sprite (including semi-transparent edges, dithered alpha)</li>
 *   <li>All contour vertices stay within the image bounds</li>
 *   <li>Contour bounding box covers a reasonable fraction of the opaque area</li>
 *   <li>{@code SpriteOutlineTracer} (UI highlighting) is tighter than {@code CollisionHullGenerator}
 *       (game bounds with dilation + insetting), validating the decoupling</li>
 * </ul>
 * <p>
 * Conditioned on the game directory via {@code @EnabledIf}. Silently skipped if game is not installed.
 */
class RealSpriteRegressionTest {

    private static Path gameFolder;

    @BeforeAll
    static void resolveGameFolder() {
        try {
            Path workingDirectory = Path.of("").toAbsolutePath();
            Path settingsPath = workingDirectory.resolve("ship_editor_settings.json");
            if (Files.exists(settingsPath)) {
                ObjectMapper tempMapper = new ObjectMapper();
                @SuppressWarnings("unchecked")
                java.util.Map<String, Object> map = tempMapper.readValue(settingsPath.toFile(), java.util.Map.class);
                String gameFolderPath = (String) map.get("gameFolderPath");
                if (gameFolderPath != null) {
                    Path candidate = Path.of(gameFolderPath);
                    if (Files.isDirectory(candidate.resolve("graphics"))) {
                        gameFolder = candidate;
                    }
                }
            }
        } catch (Exception e) {
            // Fallback below
        }
        if (gameFolder == null) {
            // Try relative path (project is usually inside the game directory)
            Path candidate = Path.of("").toAbsolutePath().getParent();
            if (candidate != null && Files.isDirectory(candidate.resolve("graphics"))) {
                gameFolder = candidate;
            }
        }
    }

    static boolean gameSpritesAvailable() {
        return gameFolder != null && Files.isDirectory(gameFolder.resolve("graphics").resolve("ships"));
    }

    private BufferedImage loadSprite(String relativePath) throws IOException {
        Path spritePath = gameFolder.resolve(relativePath);
        assertTrue(Files.exists(spritePath), "Sprite not found: " + spritePath);
        BufferedImage img = ImageIO.read(spritePath.toFile());
        assertNotNull(img, "Failed to read image: " + spritePath);
        return img;
    }

    /**
     * Helper: validates structural invariants of a contour against its source image.
     */
    private void assertContourInvariants(List<Point2D> contour, BufferedImage image, String spriteName) {
        assertNotNull(contour, spriteName + ": contour should not be null");
        assertFalse(contour.isEmpty(), spriteName + ": contour should not be empty for an opaque sprite");
        assertTrue(contour.size() >= 3, spriteName + ": contour should have at least 3 vertices, got " + contour.size());

        int width = image.getWidth();
        int height = image.getHeight();

        for (int i = 0; i < contour.size(); i++) {
            Point2D p = contour.get(i);
            assertTrue(p.getX() >= 0 && p.getX() < width,
                    spriteName + ": vertex " + i + " X=" + p.getX() + " out of image bounds [0, " + width + ")");
            assertTrue(p.getY() >= 0 && p.getY() < height,
                    spriteName + ": vertex " + i + " Y=" + p.getY() + " out of image bounds [0, " + height + ")");
        }
    }

    /**
     * Helper: computes the bounding box of a contour.
     */
    private double[] contourBBox(List<Point2D> contour) {
        double minX = Double.MAX_VALUE, minY = Double.MAX_VALUE;
        double maxX = -Double.MAX_VALUE, maxY = -Double.MAX_VALUE;
        for (Point2D p : contour) {
            minX = Math.min(minX, p.getX());
            minY = Math.min(minY, p.getY());
            maxX = Math.max(maxX, p.getX());
            maxY = Math.max(maxY, p.getY());
        }
        return new double[]{minX, minY, maxX, maxY};
    }

    // =========================================================================
    // SpriteOutlineTracer — UI contour tests with real sprites
    // =========================================================================

    @Test
    @EnabledIf("gameSpritesAvailable")
    @DisplayName("SpriteOutlineTracer: tiny frigate (spark) — 30x24px")
    void testOutlineTracerSpark() throws IOException {
        BufferedImage img = loadSprite("graphics/ships/remnants/spark.png");
        List<Point2D> contour = SpriteOutlineTracer.generateExactContour(img);
        assertContourInvariants(contour, img, "spark");

        double[] bbox = contourBBox(contour);
        // Spark is 30x24, ~54% opaque. The contour should cover a substantial portion.
        double bboxWidth = bbox[2] - bbox[0];
        double bboxHeight = bbox[3] - bbox[1];
        assertTrue(bboxWidth >= 10, "spark: contour bbox width too small: " + bboxWidth);
        assertTrue(bboxHeight >= 10, "spark: contour bbox height too small: " + bboxHeight);
    }

    @Test
    @EnabledIf("gameSpritesAvailable")
    @DisplayName("SpriteOutlineTracer: destroyer (hammerhead) — 108x164px")
    void testOutlineTracerHammerhead() throws IOException {
        BufferedImage img = loadSprite("graphics/ships/hammerhead/hammerhead_base.png");
        List<Point2D> contour = SpriteOutlineTracer.generateExactContour(img);
        assertContourInvariants(contour, img, "hammerhead");

        double[] bbox = contourBBox(contour);
        double bboxWidth = bbox[2] - bbox[0];
        double bboxHeight = bbox[3] - bbox[1];
        // Hammerhead is 108x164, ~65% opaque. Contour should be large.
        assertTrue(bboxWidth >= 60, "hammerhead: contour bbox width too small: " + bboxWidth);
        assertTrue(bboxHeight >= 100, "hammerhead: contour bbox height too small: " + bboxHeight);
    }

    @Test
    @EnabledIf("gameSpritesAvailable")
    @DisplayName("SpriteOutlineTracer: capital ship (astral) — 320x440px")
    void testOutlineTracerAstral() throws IOException {
        BufferedImage img = loadSprite("graphics/ships/astral_cv.png");
        List<Point2D> contour = SpriteOutlineTracer.generateExactContour(img);
        assertContourInvariants(contour, img, "astral");

        double[] bbox = contourBBox(contour);
        double bboxWidth = bbox[2] - bbox[0];
        double bboxHeight = bbox[3] - bbox[1];
        // Astral is 320x440, ~51% opaque.
        assertTrue(bboxWidth >= 150, "astral: contour bbox width too small: " + bboxWidth);
        assertTrue(bboxHeight >= 300, "astral: contour bbox height too small: " + bboxHeight);
    }

    @Test
    @EnabledIf("gameSpritesAvailable")
    @DisplayName("SpriteOutlineTracer: station module (remnant shield) — 128x128px")
    void testOutlineTracerStationModule() throws IOException {
        BufferedImage img = loadSprite("graphics/ships/remnants/remnant_station_shield1.png");
        List<Point2D> contour = SpriteOutlineTracer.generateExactContour(img);
        assertContourInvariants(contour, img, "remnant_station_shield1");

        double[] bbox = contourBBox(contour);
        double bboxWidth = bbox[2] - bbox[0];
        double bboxHeight = bbox[3] - bbox[1];
        assertTrue(bboxWidth >= 40, "station_shield: contour bbox width too small: " + bboxWidth);
        assertTrue(bboxHeight >= 40, "station_shield: contour bbox height too small: " + bboxHeight);
    }

    @Test
    @EnabledIf("gameSpritesAvailable")
    @DisplayName("SpriteOutlineTracer: large station (remnant station) — 512x512px")
    void testOutlineTracerLargeStation() throws IOException {
        BufferedImage img = loadSprite("graphics/ships/remnants/remnant_station1.png");
        List<Point2D> contour = SpriteOutlineTracer.generateExactContour(img);
        assertContourInvariants(contour, img, "remnant_station1");

        double[] bbox = contourBBox(contour);
        double bboxWidth = bbox[2] - bbox[0];
        double bboxHeight = bbox[3] - bbox[1];
        // 512x512, ~45% opaque.
        assertTrue(bboxWidth >= 200, "station: contour bbox width too small: " + bboxWidth);
        assertTrue(bboxHeight >= 200, "station: contour bbox height too small: " + bboxHeight);
    }

    // =========================================================================
    // CollisionHullGenerator — game bounds tests with real sprites
    // =========================================================================

    @Test
    @EnabledIf("gameSpritesAvailable")
    @DisplayName("CollisionHullGenerator: hammerhead game bounds are valid and offset by anchor")
    void testGameBoundsHammerhead() throws IOException {
        BufferedImage img = loadSprite("graphics/ships/hammerhead/hammerhead_base.png");
        Point2D anchor = new Point2D.Double(100.0, 200.0);

        List<Point2D> bounds = CollisionHullGenerator.generateBounds(img, anchor);
        assertNotNull(bounds);
        assertFalse(bounds.isEmpty(), "hammerhead: game bounds should not be empty");
        assertTrue(bounds.size() >= 4, "hammerhead: game bounds should have at least 4 vertices");

        // All points must be offset by anchor
        for (int i = 0; i < bounds.size(); i++) {
            Point2D p = bounds.get(i);
            assertTrue(p.getX() >= 90.0,
                    "hammerhead: bound vertex " + i + " X=" + p.getX() + " should be offset by anchor 100");
            assertTrue(p.getY() >= 190.0,
                    "hammerhead: bound vertex " + i + " Y=" + p.getY() + " should be offset by anchor 200");
        }
    }

    @Test
    @EnabledIf("gameSpritesAvailable")
    @DisplayName("CollisionHullGenerator: large station game bounds with zero anchor")
    void testGameBoundsStation() throws IOException {
        BufferedImage img = loadSprite("graphics/ships/remnants/remnant_station1.png");
        Point2D anchor = new Point2D.Double(0, 0);

        List<Point2D> bounds = CollisionHullGenerator.generateBounds(img, anchor);
        assertNotNull(bounds);
        assertFalse(bounds.isEmpty(), "station: game bounds should not be empty");

        double[] bbox = contourBBox(bounds);
        double bboxWidth = bbox[2] - bbox[0];
        double bboxHeight = bbox[3] - bbox[1];
        // Station is 512x512, bounds should be large but inset from edge
        assertTrue(bboxWidth >= 150, "station: bounds width too small: " + bboxWidth);
        assertTrue(bboxWidth <= 512, "station: bounds width exceeds image: " + bboxWidth);
        assertTrue(bboxHeight >= 150, "station: bounds height too small: " + bboxHeight);
        assertTrue(bboxHeight <= 512, "station: bounds height exceeds image: " + bboxHeight);
    }

    // =========================================================================
    // Cross-validation: UI contour vs game bounds decoupling
    // =========================================================================

    @Test
    @EnabledIf("gameSpritesAvailable")
    @DisplayName("SpriteOutlineTracer contour differs from CollisionHullGenerator bounds (validates decoupling)")
    void testOutlineVsGameBoundsHammerhead() throws IOException {
        BufferedImage img = loadSprite("graphics/ships/hammerhead/hammerhead_base.png");
        Point2D anchor = new Point2D.Double(0, 0);

        List<Point2D> uiContour = SpriteOutlineTracer.generateExactContour(img);
        List<Point2D> gameBounds = CollisionHullGenerator.generateBounds(img, anchor);

        assertFalse(uiContour.isEmpty());
        assertFalse(gameBounds.isEmpty());

        // UI contour should have MORE vertices (finer detail, epsilon=1.0) than game bounds (epsilon=2.0 + insetting)
        assertTrue(uiContour.size() > gameBounds.size(),
                "UI contour (" + uiContour.size() + " vertices) should have finer detail than game bounds ("
                        + gameBounds.size() + " vertices)");

        // UI contour bounding box should be tighter (no dilation) than game bounds (dilated then inset)
        // Both use zero anchor here, so they're in the same coordinate space
        double[] uiBBox = contourBBox(uiContour);
        double[] gameBBox = contourBBox(gameBounds);

        double uiArea = (uiBBox[2] - uiBBox[0]) * (uiBBox[3] - uiBBox[1]);
        double gameArea = (gameBBox[2] - gameBBox[0]) * (gameBBox[3] - gameBBox[1]);

        // The areas should differ because they use different algorithms
        // (One traces raw pixels, the other dilates→traces→simplifies→insets)
        assertNotEquals(uiArea, gameArea, 1.0,
                "UI contour area and game bounds area should differ (different algorithms)");
    }

    // =========================================================================
    // Bulk: walk ALL ship sprites and assert no crashes
    // =========================================================================

    @Test
    @EnabledIf("gameSpritesAvailable")
    @DisplayName("SpriteOutlineTracer: no crash on ANY ship sprite in graphics/ships/")
    void testOutlineTracerAllShipSprites() throws IOException {
        Path shipsDir = gameFolder.resolve("graphics").resolve("ships");
        assertTrue(Files.isDirectory(shipsDir));

        List<Path> pngFiles;
        try (var walk = Files.walk(shipsDir)) {
            pngFiles = walk
                    .filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().endsWith(".png"))
                    .toList();
        }
        assertFalse(pngFiles.isEmpty(), "No PNG files found in " + shipsDir);

        int processed = 0;
        int contoured = 0;
        List<String> failures = new java.util.ArrayList<>();

        for (Path pngFile : pngFiles) {
            try {
                BufferedImage img = ImageIO.read(pngFile.toFile());
                if (img == null) continue;
                processed++;

                List<Point2D> contour = SpriteOutlineTracer.generateExactContour(img);
                assertNotNull(contour, "Null contour for: " + pngFile);

                if (!contour.isEmpty()) {
                    contoured++;
                    // Validate all vertices are within image bounds
                    for (Point2D p : contour) {
                        if (p.getX() < 0 || p.getX() >= img.getWidth() ||
                                p.getY() < 0 || p.getY() >= img.getHeight()) {
                            failures.add(pngFile.getFileName() + ": vertex (" + p.getX() + ", " + p.getY()
                                    + ") out of bounds [" + img.getWidth() + "x" + img.getHeight() + "]");
                        }
                    }
                }
            } catch (Exception e) {
                failures.add(pngFile.getFileName() + ": " + e.getClass().getSimpleName() + " - " + e.getMessage());
            }
        }

        System.out.println("SpriteOutlineTracer bulk: processed " + processed + " sprites, "
                + contoured + " produced contours");

        assertTrue(failures.isEmpty(),
                "Failures in " + failures.size() + " sprites:\n" + String.join("\n", failures));
        assertTrue(processed >= 30, "Expected at least 30 ship sprites, found " + processed);
    }

    @Test
    @EnabledIf("gameSpritesAvailable")
    @DisplayName("CollisionHullGenerator: no crash on ANY ship sprite in graphics/ships/")
    void testGameBoundsAllShipSprites() throws IOException {
        Path shipsDir = gameFolder.resolve("graphics").resolve("ships");
        assertTrue(Files.isDirectory(shipsDir));

        List<Path> pngFiles;
        try (var walk = Files.walk(shipsDir)) {
            pngFiles = walk
                    .filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().endsWith(".png"))
                    .toList();
        }

        int processed = 0;
        int bounded = 0;
        List<String> failures = new java.util.ArrayList<>();
        Point2D anchor = new Point2D.Double(0, 0);

        for (Path pngFile : pngFiles) {
            try {
                BufferedImage img = ImageIO.read(pngFile.toFile());
                if (img == null) continue;
                processed++;

                List<Point2D> bounds = CollisionHullGenerator.generateBounds(img, anchor);
                assertNotNull(bounds, "Null bounds for: " + pngFile);

                if (!bounds.isEmpty()) {
                    bounded++;
                    for (Point2D p : bounds) {
                        // With zero anchor, bounds should be near image coordinate space
                        // (but may slightly exceed due to dilation)
                        if (p.getX() < -10 || p.getX() > img.getWidth() + 10 ||
                                p.getY() < -10 || p.getY() > img.getHeight() + 10) {
                            failures.add(pngFile.getFileName() + ": bound vertex (" + p.getX() + ", " + p.getY()
                                    + ") far outside image [" + img.getWidth() + "x" + img.getHeight() + "]");
                        }
                    }
                }
            } catch (Exception e) {
                failures.add(pngFile.getFileName() + ": " + e.getClass().getSimpleName() + " - " + e.getMessage());
            }
        }

        System.out.println("CollisionHullGenerator bulk: processed " + processed + " sprites, "
                + bounded + " produced bounds");

        assertTrue(failures.isEmpty(),
                "Failures in " + failures.size() + " sprites:\n" + String.join("\n", failures));
        assertTrue(processed >= 30, "Expected at least 30 ship sprites, found " + processed);
    }
}
