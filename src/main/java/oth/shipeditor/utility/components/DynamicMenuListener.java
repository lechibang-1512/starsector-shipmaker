package oth.shipeditor.utility.components;

import javax.swing.*;
import java.util.function.Supplier;

public class DynamicMenuListener extends MouseoverLabelListener {

    private final Supplier<JPopupMenu> menuSupplier;

    public DynamicMenuListener(Supplier<JPopupMenu> menuGetter, JComponent inputComponent) {
        super(null, inputComponent);
        this.menuSupplier = menuGetter;
    }

    @Override
    public JPopupMenu getPopupMenu() {
        return menuSupplier.get();
    }

}
