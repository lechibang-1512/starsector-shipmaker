package shipeditor.utility.components.widgets;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import lombok.Getter;
import lombok.Setter;

import javax.swing.JPanel;
import javax.swing.JSpinner;
import java.awt.GridBagLayout;

/** * Container for holding and accessing spinners. Callers are responsible for setting references.
 * Uses GridBagLayout by default.*/
@Getter @Setter
@SuppressFBWarnings({"EI_EXPOSE_REP", "EI_EXPOSE_REP2", "MS_EXPOSE_REP"})
public class TwinSpinnerPanel extends JPanel {

    private JSpinner firstSpinner;

    private JSpinner secondSpinner;

    TwinSpinnerPanel() {
        this.setLayout(new GridBagLayout());
    }

    public void clear() {
        if (firstSpinner != null) firstSpinner.setValue(0.0d);
        if (secondSpinner != null) secondSpinner.setValue(0.0d);
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        if (firstSpinner != null) firstSpinner.setEnabled(enabled);
        if (secondSpinner != null) secondSpinner.setEnabled(enabled);
    }

}
