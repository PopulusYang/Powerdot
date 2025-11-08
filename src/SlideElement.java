// 文件名： SlideElement.java
// 功能： 抽象类，表示幻灯片中的一个元素
import java.awt.*;
import java.io.Serializable;

public abstract class SlideElement implements Serializable {
    protected int x, y;
    public SlideElement(int x, int y) { this.x = x; this.y = y; }
    public abstract void draw(Graphics g);// 绘制元素
    public abstract boolean contains(Point p);// 判断点是否在元素内
    public void move(int dx, int dy) { this.x += dx; this.y += dy; }
    public abstract Rectangle getBounds();// 获取元素边界
    public abstract void setBounds(Rectangle bounds);// 设置元素边界
}