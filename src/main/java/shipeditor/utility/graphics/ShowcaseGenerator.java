package shipeditor.utility.graphics;

import shipeditor.parsing.loading.FileLoading;
import shipeditor.persistence.database.DatabaseQueryService;
import shipeditor.persistence.database.IndexedFile;
import shipeditor.representation.GameDataRepository;
import shipeditor.representation.ship.HullSpecFile;
import shipeditor.representation.weapon.ProjectileSpecFile;
import shipeditor.representation.weapon.WeaponSpecFile;
import shipeditor.utility.text.StringConstants;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class ShowcaseGenerator {

    private static final int MIN_FONT_SIZE = 10;
    private static final int MAX_FONT_SIZE = 28;

    private static class ShowcaseWeapon {
        final WeaponSpecFile spec;
        final String name;
        ShowcaseWeapon(WeaponSpecFile spec, String name) {
            this.spec = spec;
            this.name = name;
        }
    }

    private static class ShowcaseHull {
        final HullSpecFile spec;
        final String name;
        ShowcaseHull(HullSpecFile spec, String name) {
            this.spec = spec;
            this.name = name;
        }
    }

    public static void generate(File dest, String type, String modId, int cellSize, Color bgColor, int limit, boolean renderMissiles) throws java.io.IOException {
        if ("Weapons".equals(type)) {
            generateWeapons(dest, modId, cellSize, bgColor, limit, renderMissiles);
        } else {
            generateHulls(dest, type, modId, cellSize, bgColor, limit);
        }
    }
    
    private static void generateWeapons(File dest, String modId, int cellSize, Color bgColor, int limit, boolean renderMissiles) throws java.io.IOException {
        List<IndexedFile> weaponFiles = DatabaseQueryService.getFilesByType(StringConstants.WEAPON_TYPE);
        List<ShowcaseWeapon> specs = new ArrayList<>();
        for (IndexedFile f : weaponFiles) {
            if (modId != null && !"All".equals(modId) && !modId.equals(f.getModId())) {
                continue;
            }
            if (specs.size() >= limit) break;
            WeaponSpecFile spec = FileLoading.loadWeaponFile(f.getFilePath().toFile());
            if (spec != null) {
                String name = f.getEntityName() != null ? f.getEntityName() : spec.getId();
                specs.add(new ShowcaseWeapon(spec, name));
            }
        }
        
        int count = specs.size();
        if (count == 0) return;
        
        int cols = (int) Math.ceil(Math.sqrt(count));
        int rows = (int) Math.ceil((double) count / cols);
        int width = cols * cellSize;
        int height = rows * cellSize;
        
        BufferedImage finalImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = finalImage.createGraphics();
        
        g2d.setColor(bgColor);
        g2d.fillRect(0, 0, width, height);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setFont(computeFont(cellSize));
        
        int idx = 0;
        for (ShowcaseWeapon item : specs) {
            WeaponSpecFile spec = item.spec;
            BufferedImage cell = new BufferedImage(cellSize, cellSize, BufferedImage.TYPE_INT_ARGB);
            Graphics2D cg2d = cell.createGraphics();
            
            int cx = cellSize / 2;
            int cy = cellSize / 2;
            
            BufferedImage under = loadSprite(spec.getTurretUnderSprite());
            if (under == null) under = loadSprite(spec.getHardpointUnderSprite());
            
            BufferedImage gun = loadSprite(spec.getTurretGunSprite());
            if (gun == null) gun = loadSprite(spec.getHardpointGunSprite());
            
            BufferedImage base = loadSprite(spec.getTurretSprite());
            if (base == null) base = loadSprite(spec.getHardpointSprite());
            
            BufferedImage glow = loadSprite(spec.getTurretGlowSprite());
            if (glow == null) glow = loadSprite(spec.getHardpointGlowSprite());
            
            boolean barrelBelow = spec.getRenderHints() != null && spec.getRenderHints().contains(StringConstants.RENDER_BARREL_BELOW);
            
            if (under != null) pasteCentered(cg2d, under, cx, cy);
            if (barrelBelow && gun != null) pasteCentered(cg2d, gun, cx, cy);
            if (base != null) pasteCentered(cg2d, base, cx, cy);
            if (!barrelBelow && gun != null) pasteCentered(cg2d, gun, cx, cy);
            if (glow != null) pasteCentered(cg2d, glow, cx, cy);
            
            if (renderMissiles && spec.getProjectileSpecId() != null) {
                ProjectileSpecFile projSpec = GameDataRepository.getProjectileByID(spec.getProjectileSpecId());
                if (projSpec != null) {
                    BufferedImage proj = loadSprite(projSpec.getSprite());
                    Point2D[] offsets = spec.getTurretOffsets();
                    if (offsets == null || offsets.length == 0) {
                        offsets = spec.getHardpointOffsets();
                    }
                    if (proj != null && offsets != null) {
                        int pw = proj.getWidth();
                        int ph = proj.getHeight();
                        for (Point2D offset : offsets) {
                            double drawX = cx - offset.getY() - (pw / 2.0);
                            double drawY = cy - offset.getX() - (ph / 2.0);
                            cg2d.drawImage(proj, (int) Math.round(drawX), (int) Math.round(drawY), null);
                        }
                    }
                }
            }
            
            cg2d.dispose();
            drawCellToGrid(g2d, cell, item.name, idx, cols, cellSize);
            idx++;
        }
        
        g2d.dispose();
        ImageIO.write(finalImage, "PNG", dest);
    }

    private static void generateHulls(File dest, String type, String modId, int cellSize, Color bgColor, int limit) throws java.io.IOException {
        boolean onlyFighters = "Fighters".equals(type);
        
        List<IndexedFile> shipFiles = DatabaseQueryService.getFilesByType(StringConstants.SHIP_TYPE);
        List<ShowcaseHull> specs = new ArrayList<>();
        for (IndexedFile f : shipFiles) {
            if (modId != null && !"All".equals(modId) && !modId.equals(f.getModId())) {
                continue;
            }
            if (specs.size() >= limit) break;
            HullSpecFile spec = shipeditor.parsing.loading.JsonSpecLoader.loadHullFile(f.getFilePath().toFile());
            if (spec != null) {
                boolean isFighter = "FIGHTER".equalsIgnoreCase(spec.getHullSize());
                if ((onlyFighters && isFighter) || (!onlyFighters && !isFighter)) {
                    String name = f.getEntityName() != null ? f.getEntityName() : spec.getHullId();
                    specs.add(new ShowcaseHull(spec, name));
                }
            }
        }
        
        int count = specs.size();
        if (count == 0) return;
        
        int cols = (int) Math.ceil(Math.sqrt(count));
        int rows = (int) Math.ceil((double) count / cols);
        int width = cols * cellSize;
        int height = rows * cellSize;
        
        BufferedImage finalImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = finalImage.createGraphics();
        
        g2d.setColor(bgColor);
        g2d.fillRect(0, 0, width, height);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setFont(computeFont(cellSize));
        
        int idx = 0;
        for (ShowcaseHull item : specs) {
            HullSpecFile spec = item.spec;
            BufferedImage cell = new BufferedImage(cellSize, cellSize, BufferedImage.TYPE_INT_ARGB);
            Graphics2D cg2d = cell.createGraphics();
            
            int cx = cellSize / 2;
            int cy = cellSize / 2;
            
            BufferedImage base = loadSprite(spec.getSpriteName());
            if (base != null) {
                pasteCentered(cg2d, base, cx, cy);
            }
            
            cg2d.dispose();
            drawCellToGrid(g2d, cell, item.name, idx, cols, cellSize);
            idx++;
        }
        
        g2d.dispose();
        ImageIO.write(finalImage, "PNG", dest);
    }

    private static Font computeFont(int cellSize) {
        int fontSize = Math.max(MIN_FONT_SIZE, Math.min(MAX_FONT_SIZE, cellSize / 10));
        return new Font("SansSerif", Font.BOLD, fontSize);
    }
    
    private static void drawCellToGrid(Graphics2D g2d, BufferedImage cell, String label, int idx, int cols, int cellSize) {
        int col = idx % cols;
        int row = idx / cols;
        int xOff = col * cellSize;
        int yOff = row * cellSize;
        
        g2d.drawImage(cell, xOff, yOff, null);

        String text = label != null ? label : "unknown";
        FontMetrics fm = g2d.getFontMetrics();

        // Truncate label with ellipsis if it exceeds cell width minus padding.
        int maxTextWidth = cellSize - 10;
        if (fm.stringWidth(text) > maxTextWidth) {
            while (text.length() > 1 && fm.stringWidth(text + "…") > maxTextWidth) {
                text = text.substring(0, text.length() - 1);
            }
            text = text + "…";
        }

        int textX = xOff + 5;
        int textY = yOff + cellSize - fm.getDescent() - 4;

        // Draw black outline for readability on any background.
        g2d.setColor(Color.BLACK);
        g2d.drawString(text, textX - 1, textY);
        g2d.drawString(text, textX + 1, textY);
        g2d.drawString(text, textX, textY - 1);
        g2d.drawString(text, textX, textY + 1);

        g2d.setColor(Color.WHITE);
        g2d.drawString(text, textX, textY);
    }

    private static BufferedImage loadSprite(String path) {
        if (path == null || path.isEmpty()) return null;
        try {
            Path file = Path.of(path);
            File resolved = FileLoading.fetchDataFile(file, null); 
            if (resolved != null && resolved.exists()) {
                return ImageIO.read(resolved);
            }
        } catch (Exception e) {
            // Ignore
        }
        return null;
    }
    
    private static void pasteCentered(Graphics2D g, BufferedImage img, int cx, int cy) {
        int x = cx - (img.getWidth() / 2);
        int y = cy - (img.getHeight() / 2);
        g.drawImage(img, x, y, null);
    }
}
