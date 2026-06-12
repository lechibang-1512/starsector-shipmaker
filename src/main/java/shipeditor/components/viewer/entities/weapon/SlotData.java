package shipeditor.components.viewer.entities.weapon;

import shipeditor.representation.weapon.WeaponEnums.WeaponMount;
import shipeditor.representation.weapon.WeaponEnums.WeaponSize;
import shipeditor.representation.weapon.WeaponEnums.WeaponType;

public interface SlotData {

    String getId();

    void changeSlotID(String newId);

    WeaponType getWeaponType();

    void setWeaponType(WeaponType newType);

    WeaponMount getWeaponMount();

    void setWeaponMount(WeaponMount newMount);

    WeaponSize getWeaponSize();

    void setWeaponSize(WeaponSize newSize);

    double getArc();

    void setArc(double degrees);

    double getAngle();

    void setAngle(double degrees);

    int getRenderOrderMod();

    void setRenderOrderMod(int orderMod);

}
