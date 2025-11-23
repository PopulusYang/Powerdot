
// 文件名： SlideElement.java
// 功能： 抽象类，表示幻灯片中的一个元素
import java.awt.*;
import java.io.Serializable;

public abstract class SlideElement implements Serializable {
    protected int x, y;
    protected double rotation = 0; // Rotation in degrees

    public SlideElement(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public abstract void draw(Graphics g);// 绘制元素

    public abstract boolean contains(Point p);// 判断点是否在元素内

    public void move(int dx, int dy) {
        this.x += dx;
        this.y += dy;
    }

    public abstract Rectangle getBounds();// 获取元素边界

    public abstract void setBounds(Rectangle bounds);// 设置元素边界

    public double getRotation() {
        return rotation;
    }

    public void setRotation(double rotation) {
        this.rotation = rotation;
    }

    protected Point rotatePoint(Point p, Point center, double angleDegrees) {
        double angleRadians = Math.toRadians(angleDegrees);
        double cos = Math.cos(angleRadians);
        double sin = Math.sin(angleRadians);
        int dx = p.x - center.x;
        int dy = p.y - center.y;
        int newX = center.x + (int) (dx * cos - dy * sin);
        int newY = center.y + (int) (dx * sin + dy * cos);
        return new Point(newX, newY);
    }
}