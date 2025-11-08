import java.awt.Point;

/**
 * ChangeLineEndpointsCommand - 专门用于处理直线端点变化的命令。
 */
public class ChangeLineEndpointsCommand implements Command {
    private final LineElement line;
    private final Point originalStart;
    private final Point originalEnd;
    private final Point finalStart;
    private final Point finalEnd;

    public ChangeLineEndpointsCommand(LineElement line, Point originalStart, Point originalEnd, Point finalStart, Point finalEnd) {
        this.line = line;
        this.originalStart = originalStart;
        this.originalEnd = originalEnd;
        this.finalStart = finalStart;
        this.finalEnd = finalEnd;
    }

    @Override
    public void execute() {
        line.setEndpoints(finalStart, finalEnd);
    }

    @Override
    public void undo() {
        line.setEndpoints(originalStart, originalEnd);
    }
}