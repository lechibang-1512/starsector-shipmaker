package shipeditor.undo;
import shipeditor.utility.UtilityEnums.EditCategory;


/** * Entity interface for the Undo/Redo functionality; this is essentially Command Pattern.*/
public interface Edit {

    void setFinished(boolean state);

    void add(Edit edit);

    void undo();

    void redo();

    String getName();

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    boolean isFinished();

    default EditCategory getCategory() {
        return EditCategory.HULL;
    }


}
