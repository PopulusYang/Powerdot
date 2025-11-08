public class AddElementCommand implements Command {
    private final SlidePage page;
    private final SlideElement element;
    public AddElementCommand(SlidePage page, SlideElement element) { this.page = page; this.element = element; }
    @Override public void execute() { page.addElement(element); }
    @Override public void undo() { page.removeElement(element); }
}