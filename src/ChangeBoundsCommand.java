import java.awt.Rectangle;

public class ChangeBoundsCommand implements Command {
    private final SlideElement element;
    private final Rectangle originalBounds;
    private final Rectangle finalBounds;
    public ChangeBoundsCommand(SlideElement element, Rectangle originalBounds, Rectangle finalBounds) { this.element = element; this.originalBounds = originalBounds; this.finalBounds = finalBounds; }
    @Override public void execute() { element.setBounds(finalBounds); }
    @Override public void undo() { element.setBounds(originalBounds); }
}