public class RemoveElementCommand implements Command {
    private final SlidePage page;
    private final SlideElement element;
    private final int index;

    public RemoveElementCommand(SlidePage page, SlideElement element) {
        this.page = page;
        this.element = element;
        this.index = page.getElements().indexOf(element);
    }

    @Override
    public void execute() {
        page.removeElement(element);
    }

    @Override
    public void undo() {
        page.addElement(index, element);
    }
}
