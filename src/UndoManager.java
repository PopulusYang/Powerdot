import java.util.ArrayDeque;
import java.util.Deque;

public class UndoManager {
    private final Deque<Command> undoStack = new ArrayDeque<>();
    private final Deque<Command> redoStack = new ArrayDeque<>();
    public void executeCommand(Command command) { command.execute(); undoStack.push(command); redoStack.clear(); }
    public void undo() { if (!undoStack.isEmpty()) { Command command = undoStack.pop(); command.undo(); redoStack.push(command); } }
    public void redo() { if (!redoStack.isEmpty()) { Command command = redoStack.pop(); command.execute(); undoStack.push(command); } }
    public void clear() { undoStack.clear(); redoStack.clear(); }
}