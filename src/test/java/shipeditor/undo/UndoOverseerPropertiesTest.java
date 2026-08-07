package shipeditor.undo;

import net.jqwik.api.*;
import net.jqwik.api.constraints.*;
import shipeditor.utility.UtilityEnums.EditCategory;

import org.junit.jupiter.api.BeforeEach;

import static org.junit.jupiter.api.Assertions.*;

class UndoOverseerPropertiesTest {

    static class TestEdit extends AbstractEdit {
        private final String name;
        private boolean done = true;

        TestEdit(String name) {
            this.name = name;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public void undo() {
            done = false;
        }

        @Override
        public void redo() {
            done = true;
        }

        @Override
        public EditCategory getCategory() {
            return EditCategory.NONE;
        }

        public boolean isDone() {
            return done;
        }
    }

    @BeforeEach
    void clearStacks() {
        UndoOverseer.setPaused(false);
        UndoOverseer.getUndoStack().clear();
        UndoOverseer.getRedoStack().clear();
    }

    @Property
    void testPostEditIncreasesUndoStack(@ForAll @Size(min = 1, max = 50) java.util.List<String> editNames) {
        clearStacks();
        for (String name : editNames) {
            UndoOverseer.post(new TestEdit(name));
        }

        int expectedSize = Math.min(editNames.size(), 200);
        assertEquals(expectedSize, UndoOverseer.getUndoStack().size());
        assertTrue(UndoOverseer.getRedoStack().isEmpty());
    }

    @Property
    void testPostCappedAt200(@ForAll @Size(min = 205, max = 250) java.util.List<String> editNames) {
        clearStacks();
        for (String name : editNames) {
            UndoOverseer.post(new TestEdit(name));
        }

        assertEquals(200, UndoOverseer.getUndoStack().size());
    }

    @Property
    void testUndoRedoSequence(@ForAll @Size(min = 1, max = 20) java.util.List<String> editNames) {
        clearStacks();
        for (String name : editNames) {
            UndoOverseer.post(new TestEdit(name));
        }

        // Perform undo on all
        int initialSize = UndoOverseer.getUndoStack().size();
        for (int i = 0; i < initialSize; i++) {
            UndoOverseer.getUndoAction().actionPerformed(null);
        }

        assertTrue(UndoOverseer.getUndoStack().isEmpty());
        assertEquals(initialSize, UndoOverseer.getRedoStack().size());

        // Perform redo on all
        for (int i = 0; i < initialSize; i++) {
            UndoOverseer.getRedoAction().actionPerformed(null);
        }

        assertEquals(initialSize, UndoOverseer.getUndoStack().size());
        assertTrue(UndoOverseer.getRedoStack().isEmpty());
    }

    @Property
    void testPausePreventsPost(@ForAll String name) {
        clearStacks();
        UndoOverseer.setPaused(true);
        UndoOverseer.post(new TestEdit(name));

        assertTrue(UndoOverseer.getUndoStack().isEmpty());
        UndoOverseer.setPaused(false);
    }
}
