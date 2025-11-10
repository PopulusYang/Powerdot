// Commend接口
// 定义了命令的执行和撤销方法
public interface Command {
    void execute();
    void undo();
}