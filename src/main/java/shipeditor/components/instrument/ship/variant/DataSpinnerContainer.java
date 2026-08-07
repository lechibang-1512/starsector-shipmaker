package shipeditor.components.instrument.ship.variant;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import shipeditor.components.viewer.layers.ship.ShipLayer;

import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import java.util.function.Consumer;

@Getter
@Setter
@RequiredArgsConstructor
class DataSpinnerContainer<T extends Number> {

    private final SpinnerNumberModel model;

    private final JSpinner spinner;

    private final javax.swing.JLabel maxLabel = new javax.swing.JLabel();

    private Consumer<T> setter;

    Consumer<T> getCurrentSetter() {
        return setter;
    }

    void disableSpinner() {
        setter = null;
        model.setMaximum(0);
        model.setValue(0);
        spinner.setEnabled(false);
        maxLabel.setText("");
    }

    void enableSpinner(ShipLayer shipLayer, T newCurrent, T newMaximum, Consumer<T> newSetter) {
        Comparable<?> comparable = (Comparable<?>) newMaximum;
        setter = null;
        model.setMaximum(comparable);
        model.setValue(newCurrent);
        spinner.setEnabled(true);
        maxLabel.setText(" / " + newMaximum);
        setter = newSetter;
    }

    @SuppressWarnings("unchecked")
    T getMaxValue() {
        Comparable<?> maxComparable = model.getMaximum();
        return (T) maxComparable;
    }

    @SuppressWarnings("unchecked")
    T getMinValue() {
        Comparable<?> minComparable = model.getMinimum();
        return (T) minComparable;
    }

}
