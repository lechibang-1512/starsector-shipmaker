package shipeditor.components.instrument.weapon;

import com.formdev.flatlaf.ui.FlatLineBorder;
import shipeditor.communication.EventBus;
import shipeditor.communication.events.components.LayerTabUpdated;
import shipeditor.components.viewer.layers.LayerPainter;
import shipeditor.components.viewer.layers.weapon.WeaponLayer;
import shipeditor.representation.weapon.WeaponSpecFile;
import shipeditor.utility.components.ComponentUtilities;
import shipeditor.utility.components.MouseoverLabelListener;
import shipeditor.utility.graphics.ColorUtilities;
import shipeditor.utility.overseers.StaticController;
import shipeditor.utility.text.StringValues;
import shipeditor.utility.themes.Themes;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;
import java.awt.Color;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class WeaponVisualsPanel extends AbstractWeaponPropertiesPanel {

    private WeaponLayer cachedLayer;
    private boolean readyForInput;

    private JTextField turretSpriteEditor;
    private JTextField turretUnderSpriteEditor;
    private JTextField turretGunSpriteEditor;
    private JTextField turretGlowSpriteEditor;

    private JTextField hardpointSpriteEditor;
    private JTextField hardpointUnderSpriteEditor;
    private JTextField hardpointGunSpriteEditor;
    private JTextField hardpointGlowSpriteEditor;

    private JCheckBox renderBelowWeaponsCheckbox;
    private JCheckBox renderAboveWeaponsCheckbox;
    private JCheckBox renderAdditiveCheckbox;

    private JLabel fringeColorValue;
    private JLabel coreColorValue;
    private JLabel glowColorValue;

    private JTextField visualRecoilEditor;
    private JCheckBox separateRecoilCheckbox;
    private JTextField numFramesEditor;
    private JTextField frameRateEditor;
    private JCheckBox alwaysAnimateCheckbox;

    private JTextField renderHintsEditor;
    private JTextField displayArcRadiusEditor;

    public WeaponVisualsPanel() {
        super();
    }

    @Override
    protected void populateContent() {
        this.setLayout(new GridBagLayout());
        ComponentUtilities.outfitPanelWithTitle(this, new Insets(1, 0, 0, 0), "Visuals & Animation");

        int row = 0;
        
        row = addSpriteFields(row);
        row = addRenderCheckboxes(row);
        row = addColorPickers(row);
        row = addAnimationFields(row);
        row = addMiscFields(row);



        clearData();
    }

    private int addSpriteFields(int row) {
        turretSpriteEditor = createTextField(value -> {
            if (cachedLayer != null && cachedLayer.getSpecFile() != null) {
                cachedLayer.getSpecFile().setTurretSprite(value);
            }
        });
        ComponentUtilities.addLabelAndComponent(this, new JLabel("Turret Sprite:"), turretSpriteEditor, row++);

        turretUnderSpriteEditor = createTextField(value -> {
            if (cachedLayer != null && cachedLayer.getSpecFile() != null) {
                cachedLayer.getSpecFile().setTurretUnderSprite(value);
            }
        });
        ComponentUtilities.addLabelAndComponent(this, new JLabel("Turret Under Sprite:"), turretUnderSpriteEditor, row++);

        turretGunSpriteEditor = createTextField(value -> {
            if (cachedLayer != null && cachedLayer.getSpecFile() != null) {
                cachedLayer.getSpecFile().setTurretGunSprite(value);
            }
        });
        ComponentUtilities.addLabelAndComponent(this, new JLabel("Turret Gun Sprite:"), turretGunSpriteEditor, row++);

        turretGlowSpriteEditor = createTextField(value -> {
            if (cachedLayer != null && cachedLayer.getSpecFile() != null) {
                cachedLayer.getSpecFile().setTurretGlowSprite(value);
            }
        });
        ComponentUtilities.addLabelAndComponent(this, new JLabel("Turret Glow Sprite:"), turretGlowSpriteEditor, row++);

        hardpointSpriteEditor = createTextField(value -> {
            if (cachedLayer != null && cachedLayer.getSpecFile() != null) {
                cachedLayer.getSpecFile().setHardpointSprite(value);
            }
        });
        ComponentUtilities.addLabelAndComponent(this, new JLabel("Hardpoint Sprite:"), hardpointSpriteEditor, row++);

        hardpointUnderSpriteEditor = createTextField(value -> {
            if (cachedLayer != null && cachedLayer.getSpecFile() != null) {
                cachedLayer.getSpecFile().setHardpointUnderSprite(value);
            }
        });
        ComponentUtilities.addLabelAndComponent(this, new JLabel("Hardpoint Under Sprite:"), hardpointUnderSpriteEditor, row++);

        hardpointGunSpriteEditor = createTextField(value -> {
            if (cachedLayer != null && cachedLayer.getSpecFile() != null) {
                cachedLayer.getSpecFile().setHardpointGunSprite(value);
            }
        });
        ComponentUtilities.addLabelAndComponent(this, new JLabel("Hardpoint Gun Sprite:"), hardpointGunSpriteEditor, row++);

        hardpointGlowSpriteEditor = createTextField(value -> {
            if (cachedLayer != null && cachedLayer.getSpecFile() != null) {
                cachedLayer.getSpecFile().setHardpointGlowSprite(value);
            }
        });
        ComponentUtilities.addLabelAndComponent(this, new JLabel("Hardpoint Glow Sprite:"), hardpointGlowSpriteEditor, row++);

        return row;
    }

    private int addRenderCheckboxes(int row) {
        renderBelowWeaponsCheckbox = createCheckBox("Render Below All Weapons", value -> {
            if (cachedLayer != null && cachedLayer.getSpecFile() != null) {
                cachedLayer.getSpecFile().setRenderBelowAllWeapons(value);
            }
        });
        ComponentUtilities.addLabelAndComponent(this, new JLabel(), renderBelowWeaponsCheckbox, row++);

        renderAboveWeaponsCheckbox = createCheckBox("Render Above All Weapons", value -> {
            if (cachedLayer != null && cachedLayer.getSpecFile() != null) {
                cachedLayer.getSpecFile().setRenderAboveAllWeapons(value);
            }
        });
        ComponentUtilities.addLabelAndComponent(this, new JLabel(), renderAboveWeaponsCheckbox, row++);

        renderAdditiveCheckbox = createCheckBox("Render Additive", value -> {
            if (cachedLayer != null && cachedLayer.getSpecFile() != null) {
                cachedLayer.getSpecFile().setRenderAdditive(value);
            }
        });
        ComponentUtilities.addLabelAndComponent(this, new JLabel(), renderAdditiveCheckbox, row++);

        return row;
    }

    private int addColorPickers(int row) {
        fringeColorValue = new JLabel();
        JLabel fringeColorLabel = createColorLabel("Fringe Color:", fringeColorValue, 
                () -> cachedLayer != null ? cachedLayer.getSpecFile().getFringeColor() : null,
                color -> {
                    if (cachedLayer != null && cachedLayer.getSpecFile() != null) {
                        cachedLayer.getSpecFile().setFringeColor(color);
                        processChange();
                    }
                });
        ComponentUtilities.addLabelAndComponent(this, fringeColorLabel, fringeColorValue, row++);

        coreColorValue = new JLabel();
        JLabel coreColorLabel = createColorLabel("Core Color:", coreColorValue, 
                () -> cachedLayer != null ? cachedLayer.getSpecFile().getCoreColor() : null,
                color -> {
                    if (cachedLayer != null && cachedLayer.getSpecFile() != null) {
                        cachedLayer.getSpecFile().setCoreColor(color);
                        processChange();
                    }
                });
        ComponentUtilities.addLabelAndComponent(this, coreColorLabel, coreColorValue, row++);

        glowColorValue = new JLabel();
        JLabel glowColorLabel = createColorLabel("Glow Color:", glowColorValue, 
                () -> cachedLayer != null ? cachedLayer.getSpecFile().getGlowColor() : null,
                color -> {
                    if (cachedLayer != null && cachedLayer.getSpecFile() != null) {
                        cachedLayer.getSpecFile().setGlowColor(color);
                        processChange();
                    }
                });
        ComponentUtilities.addLabelAndComponent(this, glowColorLabel, glowColorValue, row++);

        return row;
    }

    private int addAnimationFields(int row) {
        visualRecoilEditor = createDoubleField(value -> {
            if (cachedLayer != null && cachedLayer.getSpecFile() != null) {
                cachedLayer.getSpecFile().setVisualRecoil(value);
            }
        });
        ComponentUtilities.addLabelAndComponent(this, new JLabel("Visual Recoil:"), visualRecoilEditor, row++);

        separateRecoilCheckbox = createCheckBox("Separate Recoil For Linked Barrels", value -> {
            if (cachedLayer != null && cachedLayer.getSpecFile() != null) {
                cachedLayer.getSpecFile().setSeparateRecoilForLinkedBarrels(value);
            }
        });
        ComponentUtilities.addLabelAndComponent(this, new JLabel(), separateRecoilCheckbox, row++);

        numFramesEditor = createIntField(value -> {
            if (cachedLayer != null && cachedLayer.getSpecFile() != null) {
                cachedLayer.getSpecFile().setNumFrames(value);
            }
        });
        ComponentUtilities.addLabelAndComponent(this, new JLabel("Num Frames:"), numFramesEditor, row++);

        frameRateEditor = createIntField(value -> {
            if (cachedLayer != null && cachedLayer.getSpecFile() != null) {
                cachedLayer.getSpecFile().setFrameRate(value);
            }
        });
        ComponentUtilities.addLabelAndComponent(this, new JLabel("Frame Rate:"), frameRateEditor, row++);

        alwaysAnimateCheckbox = createCheckBox("Always Animate", value -> {
            if (cachedLayer != null && cachedLayer.getSpecFile() != null) {
                cachedLayer.getSpecFile().setAlwaysAnimate(value);
            }
        });
        ComponentUtilities.addLabelAndComponent(this, new JLabel(), alwaysAnimateCheckbox, row++);

        return row;
    }

    private int addMiscFields(int row) {
        renderHintsEditor = createListField(value -> {
            if (cachedLayer != null && cachedLayer.getSpecFile() != null) {
                cachedLayer.getSpecFile().setRenderHints(value);
            }
        });
        ComponentUtilities.addLabelAndComponent(this, new JLabel("Render Hints (comma-separated):"), renderHintsEditor, row++);

        displayArcRadiusEditor = createDoubleField(value -> {
            if (cachedLayer != null && cachedLayer.getSpecFile() != null) {
                cachedLayer.getSpecFile().setDisplayArcRadius(value);
            }
        });
        ComponentUtilities.addLabelAndComponent(this, new JLabel("Display Arc Radius:"), displayArcRadiusEditor, row++);

        return row;
    }

    private JTextField createTextField(Consumer<String> setter) {
        JTextField textField = new JTextField();
        textField.setColumns(10);
        textField.addActionListener(e -> {
            if (readyForInput) {
                setter.accept(textField.getText());
                processChange();
            }
        });
        return textField;
    }

    private JTextField createDoubleField(Consumer<Double> setter) {
        JTextField textField = new JTextField();
        textField.setColumns(10);
        textField.addActionListener(e -> {
            if (readyForInput) {
                try {
                    setter.accept(Double.parseDouble(textField.getText()));
                    processChange();
                } catch (NumberFormatException ex) {
                    // Ignore invalid input
                }
            }
        });
        return textField;
    }

    private JTextField createIntField(Consumer<Integer> setter) {
        JTextField textField = new JTextField();
        textField.setColumns(10);
        textField.addActionListener(e -> {
            if (readyForInput) {
                try {
                    setter.accept(Integer.parseInt(textField.getText()));
                    processChange();
                } catch (NumberFormatException ex) {
                    // Ignore invalid input
                }
            }
        });
        return textField;
    }

    private JTextField createListField(Consumer<List<String>> setter) {
        JTextField textField = new JTextField();
        textField.setColumns(10);
        textField.addActionListener(e -> {
            if (readyForInput) {
                String text = textField.getText();
                if (text.isEmpty()) {
                    setter.accept(null);
                } else {
                    setter.accept(Arrays.asList(text.split("\\s*,\\s*")));
                }
                processChange();
            }
        });
        return textField;
    }

    private JCheckBox createCheckBox(String text, Consumer<Boolean> setter) {
        JCheckBox checkBox = new JCheckBox(text);
        checkBox.addActionListener(e -> {
            if (readyForInput) {
                setter.accept(checkBox.isSelected());
                processChange();
            }
        });
        return checkBox;
    }

    private JLabel createColorLabel(String labelText, JLabel valueLabel, Supplier<Color> getter, Consumer<Color> setter) {
        JLabel label = new JLabel(labelText);
        label.setToolTipText(StringValues.RIGHT_CLICK_TO_CHANGE_COLOR);

        JPopupMenu colorChooserMenu = new JPopupMenu();
        JMenuItem adjustColor = new JMenuItem(StringValues.ADJUST_VALUE);
        adjustColor.addActionListener(event -> {
            Color current = getter.get();
            Color chosen = current != null ? ColorUtilities.showColorChooser(current) : ColorUtilities.showColorChooser();
            if (chosen != null) {
                setter.accept(chosen);
            }
        });
        colorChooserMenu.add(adjustColor);

        JMenuItem removeColor = new JMenuItem("Clear value");
        removeColor.addActionListener(event -> setter.accept(null));
        colorChooserMenu.add(removeColor);

        label.addMouseListener(new MouseoverLabelListener(colorChooserMenu, label));
        Insets insets = ComponentUtilities.createLabelInsets();
        insets.top = 1;
        label.setBorder(ComponentUtilities.createLabelSimpleBorder(insets));

        return label;
    }

    private void updateColorLabel(JLabel valueLabel, Color color) {
        if (color != null) {
            valueLabel.setIcon(ComponentUtilities.createIconFromColor(color, 10, 10));
            valueLabel.setOpaque(true);
            valueLabel.setBorder(new FlatLineBorder(new Insets(2, 2, 2, 2), Color.GRAY));
            valueLabel.setBackground(Color.LIGHT_GRAY);
            valueLabel.setToolTipText(ColorUtilities.getColorBreakdown(color));
            valueLabel.setText(null);
        } else {
            valueLabel.setIcon(null);
            valueLabel.setOpaque(false);
            valueLabel.setBorder(new EmptyBorder(0, 2, 0, 2));
            valueLabel.setBackground(null);
            valueLabel.setToolTipText(null);
            valueLabel.setText("Not defined");
        }
        valueLabel.setForeground(Themes.getTextColor());
    }

    @Override
    protected void processChange() {
        if (cachedLayer != null) {
            EventBus.publish(new LayerTabUpdated(cachedLayer));
            StaticController.getScheduler().queueViewerRepaint();
        }
    }

    @Override
    public void refreshContent(LayerPainter layerPainter) {
        if (layerPainter == null || !(layerPainter.getParentLayer() instanceof WeaponLayer weaponLayer)) {
            clearData();
            return;
        }
        cachedLayer = weaponLayer;
        WeaponSpecFile spec = cachedLayer.getSpecFile();
        if (spec == null) {
            clearData();
            return;
        }

        readyForInput = false;

        turretSpriteEditor.setText(spec.getTurretSprite() != null ? spec.getTurretSprite() : "");
        turretUnderSpriteEditor.setText(spec.getTurretUnderSprite() != null ? spec.getTurretUnderSprite() : "");
        turretGunSpriteEditor.setText(spec.getTurretGunSprite() != null ? spec.getTurretGunSprite() : "");
        turretGlowSpriteEditor.setText(spec.getTurretGlowSprite() != null ? spec.getTurretGlowSprite() : "");

        hardpointSpriteEditor.setText(spec.getHardpointSprite() != null ? spec.getHardpointSprite() : "");
        hardpointUnderSpriteEditor.setText(spec.getHardpointUnderSprite() != null ? spec.getHardpointUnderSprite() : "");
        hardpointGunSpriteEditor.setText(spec.getHardpointGunSprite() != null ? spec.getHardpointGunSprite() : "");
        hardpointGlowSpriteEditor.setText(spec.getHardpointGlowSprite() != null ? spec.getHardpointGlowSprite() : "");

        renderBelowWeaponsCheckbox.setSelected(spec.isRenderBelowAllWeapons());
        renderAboveWeaponsCheckbox.setSelected(spec.isRenderAboveAllWeapons());
        renderAdditiveCheckbox.setSelected(spec.isRenderAdditive());

        updateColorLabel(fringeColorValue, spec.getFringeColor());
        updateColorLabel(coreColorValue, spec.getCoreColor());
        updateColorLabel(glowColorValue, spec.getGlowColor());

        visualRecoilEditor.setText(String.valueOf(spec.getVisualRecoil()));
        separateRecoilCheckbox.setSelected(spec.isSeparateRecoilForLinkedBarrels());
        numFramesEditor.setText(String.valueOf(spec.getNumFrames()));
        frameRateEditor.setText(String.valueOf(spec.getFrameRate()));
        alwaysAnimateCheckbox.setSelected(spec.isAlwaysAnimate());

        renderHintsEditor.setText(spec.getRenderHints() != null ? String.join(", ", spec.getRenderHints()) : "");
        displayArcRadiusEditor.setText(String.valueOf(spec.getDisplayArcRadius()));


        readyForInput = true;
    }

    private void clearData() {
        readyForInput = false;

        turretSpriteEditor.setText("");
        turretUnderSpriteEditor.setText("");
        turretGunSpriteEditor.setText("");
        turretGlowSpriteEditor.setText("");

        hardpointSpriteEditor.setText("");
        hardpointUnderSpriteEditor.setText("");
        hardpointGunSpriteEditor.setText("");
        hardpointGlowSpriteEditor.setText("");

        renderBelowWeaponsCheckbox.setSelected(false);
        renderAboveWeaponsCheckbox.setSelected(false);
        renderAdditiveCheckbox.setSelected(false);

        updateColorLabel(fringeColorValue, null);
        updateColorLabel(coreColorValue, null);
        updateColorLabel(glowColorValue, null);

        visualRecoilEditor.setText("");
        separateRecoilCheckbox.setSelected(false);
        numFramesEditor.setText("");
        frameRateEditor.setText("");
        alwaysAnimateCheckbox.setSelected(false);

        renderHintsEditor.setText("");
        displayArcRadiusEditor.setText("");


        cachedLayer = null;
    }
}
