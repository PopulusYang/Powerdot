import java.util.ArrayList;
import java.util.List;

/**
 * ApplyLayoutCommand - 封装了“应用页面布局”操作的命令。
 * 这个命令会保存页面在应用布局前的所有元素，以便能够撤销。
 */
public class ApplyLayoutCommand implements Command {
    private final SlidePage page;
    private final List<SlideElement> newElements;
    private final List<SlideElement> oldElements;

    public ApplyLayoutCommand(SlidePage page, List<SlideElement> newElements) {
        this.page = page;
        this.newElements = newElements;
        // IMPORTANT: Create a copy of the old elements for undo
        this.oldElements = new ArrayList<>(page.getElements());
    }

    @Override
    public void execute() {
        // Clear the page and add the new layout elements
        page.clearElements();
        for (SlideElement element : newElements) {
            page.addElement(element);
        }
    }

    @Override
    public void undo() {
        // Clear the page and restore the old elements
        page.clearElements();
        for (SlideElement element : oldElements) {
            page.addElement(element);
        }
    }
}