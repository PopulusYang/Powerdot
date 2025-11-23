
//文件名： SlidePage.java
//功能： 表示幻灯片中的单个页面，包含多个幻灯片元素
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class SlidePage implements Serializable {
    private List<SlideElement> elements;

    public SlidePage() {
        this.elements = new ArrayList<>();
    }

    public void addElement(SlideElement element) {
        elements.add(element);
    }

    public void addElement(int index, SlideElement element) {
        if (index >= 0 && index <= elements.size()) {
            elements.add(index, element);
        } else {
            elements.add(element);
        }
    }

    public void removeElement(SlideElement element) {
        elements.remove(element);
    }

    public List<SlideElement> getElements() {
        return elements;
    }

    /**
     * NEW: Clears all elements from the page.
     */
    public void clearElements() {
        elements.clear();
    }
}