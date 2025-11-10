// 文件名：Slide.java
// 功能： 表示幻灯片，包含多个幻灯片页面，并提供页面导航功能。
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Slide implements Serializable {
    private List<SlidePage> pages; // 幻灯片页面列表
    private int currentPageIndex; // 当前页面索引
    public Slide()
    {
        // 初始化页面列表和当前页面索引
        this.pages = new ArrayList<>();
        this.currentPageIndex = -1;
    }
    public void addPage(SlidePage page) // 添加页面
    {
        pages.add(page);
        currentPageIndex = pages.size() - 1;
    }
    public SlidePage getCurrentPage() // 获取当前页面
    {
        if (currentPageIndex < 0 || currentPageIndex >= pages.size())
            return null;
        return pages.get(currentPageIndex);
    }
    public List<SlidePage> getAllPages() { return pages; }// 获取所有页面列表
    public void setCurrentPageIndex(int index)// 设置当前页面索引
    {
        if (index >= 0 && index < pages.size())
            this.currentPageIndex = index;
    }
    public int getCurrentPageIndex() { return currentPageIndex; }// 获取页面索引
    public int getTotalPages() { return pages.size(); }// 获取页面总数
    public boolean nextPage()// 判断是否可以跳转到下一页
    {
        if (currentPageIndex < pages.size() - 1)
        {
            currentPageIndex++;
            return true;
        }
        return false;
    }
    public boolean previousPage() // 判断是否可以跳转到上一页
    {
        if (currentPageIndex > 0)
        {
            currentPageIndex--;
            return true;
        }
        return false;
    }
}