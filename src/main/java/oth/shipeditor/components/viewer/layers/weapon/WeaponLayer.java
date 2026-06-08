package oth.shipeditor.components.viewer.layers.weapon;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import lombok.Getter;
import lombok.Setter;
import oth.shipeditor.components.viewer.layers.ViewerLayer;
import oth.shipeditor.persistence.SettingsManager;
import oth.shipeditor.representation.GameDataRepository;
import oth.shipeditor.representation.weapon.WeaponSpecFile;

@SuppressFBWarnings({"EI_EXPOSE_REP", "EI_EXPOSE_REP2", "MS_EXPOSE_REP"})
public class WeaponLayer extends ViewerLayer {

    @Getter @Setter
    private WeaponSpecFile specFile;

    @Override
    public WeaponPainter getPainter() {
        return (WeaponPainter) super.getPainter();
    }

    public String getSpecFileName() {
        return String.valueOf(specFile.getWeaponSpecFilePath().getFileName());
    }

    public String getWeaponName() {
        if (specFile != null) {
            String weaponID = specFile.getId();
            GameDataRepository repository = SettingsManager.getGameData();
            var allWeapons = repository.getAllWeaponEntries();
            if (allWeapons == null) return "";
            var weaponEntry = allWeapons.get(weaponID);
            if (weaponEntry != null) {
                return weaponEntry.toString();
            }
        }
        return "";
    }

}
