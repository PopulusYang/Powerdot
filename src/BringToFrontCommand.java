import java.util.List;

public class BringToFrontCommand implements Command {
    private final SlidePage page;
    private final SlideElement element;
    private int oldIndex;

    public BringToFrontCommand(SlidePage page, SlideElement element) {
        this.page = page;
        this.element = element;
    }

    @Override
    public void execute() {
        List<SlideElement> elements = page.getElements();
        oldIndex = elements.indexOf(element);
        if (oldIndex != -1 && oldIndex < elements.size() - 1) {
            elements.remove(oldIndex);
            elements.add(element);
        }
    }

    @Override
    public void undo() {
        List<SlideElement> elements = page.getElements();
        if (elements.remove(element)) {
            elements.add(oldIndex, element);
        }
    }
}
