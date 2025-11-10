// 修改元素属性的命令实现
public class ChangeElementPropertyCommand implements Command {
    private final Runnable executeLogic;
    private final Runnable undoLogic;
    public ChangeElementPropertyCommand(Runnable executeLogic, Runnable undoLogic)
    {
        this.executeLogic = executeLogic;
        this.undoLogic = undoLogic;
    }
    @Override public void execute() { executeLogic.run(); }
    @Override public void undo() { undoLogic.run(); }
}