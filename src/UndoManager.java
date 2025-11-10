// 文件名： UndoManager.java
// 功能： 管理命令的执行、撤销和重做操作
import java.util.ArrayDeque;
import java.util.Deque;

public class UndoManager {
    // 栈用于存储已执行的命令以支持撤销操作
    private final Deque<Command> undoStack = new ArrayDeque<>();
    private final Deque<Command> redoStack = new ArrayDeque<>();

    // 执行命令并将其添加到撤销栈
    public void executeCommand(Command command)
    {
        command.execute(); // 执行
        undoStack.push(command); // 添加到撤销栈
        redoStack.clear(); // 清空重做栈
    }
    // 撤销
    public void undo()
    {
        if (!undoStack.isEmpty())
        {
            Command command = undoStack.pop(); // 从撤销栈中弹出命令
            command.undo();// 执行撤销
            redoStack.push(command); // 添加到重做栈
        }
    }
    // 重做
    public void redo()
    {
        if (!redoStack.isEmpty())
        {
            Command command = redoStack.pop(); // 从重做栈中弹出命令
            command.execute();// 重新执行命令
            undoStack.push(command); // 添加回撤销栈
        }
    }
    // 清空撤销和重做栈
    public void clear()
    {
        undoStack.clear();
        redoStack.clear();
    }
}