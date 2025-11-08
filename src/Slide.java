// 文件名：Slide.java
// 功能： 表示幻灯片，包含多个幻灯片页面，并提供页面导航功能。
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Slide implements Serializable {
    private List<SlidePage> pages;
    private int currentPageIndex;
    public Slide() { this.pages = new ArrayList<>(); this.currentPageIndex = -1; }
    public void addPage(SlidePage page) { pages.add(page); currentPageIndex = pages.size() - 1; }
    public SlidePage getCurrentPage() { if (currentPageIndex < 0 || currentPageIndex >= pages.size()) { return null; } return pages.get(currentPageIndex); }
    public List<SlidePage> getAllPages() { return pages; }
    public void setCurrentPageIndex(int index) { if (index >= 0 && index < pages.size()) { this.currentPageIndex = index; } }
    public int getCurrentPageIndex() { return currentPageIndex; }
    public int getTotalPages() { return pages.size(); }
    public boolean nextPage() { if (currentPageIndex < pages.size() - 1) { currentPageIndex++; return true; } return false; }
    public boolean previousPage() { if (currentPageIndex > 0) { currentPageIndex--; return true; } return false; }
}