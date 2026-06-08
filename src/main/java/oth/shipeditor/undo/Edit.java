package oth.shipeditor.undo;

/** * Entity interface for the Undo/Redo functionality; this is essentially Command Pattern.*/
public interface Edit {

    void setFinished(boolean state);

    void add(Edit edit);

    void undo();

    void redo();

    String getName();

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    boolean isFinished();

}
