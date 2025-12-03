
// 文件名： LineElement.java
// 功能： 表示幻灯片中的一条直线元素
import java.awt.*;

public class LineElement extends SlideElement {
    private int x2, y2;
    private Color color;
    private int thickness;// 线条粗细
    private static final int HANDLE_SIZE = 8;// 依旧8个控制点

    // x1, y1 是起点坐标，x2, y2 是终点坐标, color 是线条颜色, thickness 是线条粗细
    // x1, y1 在父类中定义，代表元素位置，这里表示起点
    public LineElement(int x1, int y1, int x2, int y2, Color color, int thickness) {
        super(x1, y1);
        this.x2 = x2;
        this.y2 = y2;
        this.color = color;
        this.thickness = thickness;
    }
    
    @Override
    public void draw(Graphics g) {
        Graphics2D g2d = (Graphics2D) g.create();

        if (rotation != 0) {
            int cx = (x + x2) / 2;
            int cy = (y + y2) / 2;
            g2d.rotate(Math.toRadians(rotation), cx, cy);
        }

        g2d.setColor(color);
        g2d.setStroke(new BasicStroke(thickness));
        g2d.drawLine(x, y, x2, y2);
        g2d.dispose();
    }

    @Override
    public boolean contains(Point p) {
        if (rotation == 0) {
            return getBounds().contains(p);
        }
        Point center = new Point((x + x2) / 2, (y + y2) / 2);
        Point rotatedP = rotatePoint(p, center, -rotation);
        return getBounds().contains(rotatedP);
    }

    @Override
    public void move(int dx, int dy) {
        super.move(dx, dy);
        this.x2 += dx;
        this.y2 += dy;
    }

    @Override
    public Rectangle getBounds() {
        int minX = Math.min(x, x2);
        int minY = Math.min(y, y2);
        int width = Math.abs(x2 - x);
        int height = Math.abs(y2 - y);
        int margin = 5;
        return new Rectangle(minX - margin, minY - margin, width + 2 * margin, height + 2 * margin);
    }

    public void setColor(Color color) {
        this.color = color;
    }

    public Color getColor() {
        return this.color;
    }

    public int getThickness() {
        return thickness;
    }

    public void setThickness(int thickness) {
        this.thickness = thickness;
    }

    /**
     * 注意：对于直线，这个方法的实现是有损的，不应用于精确的撤销/重做。
     * 它主要用于满足抽象类的契约。
     */
    @Override
    public void setBounds(Rectangle bounds) {
        this.x = bounds.x;
        this.y = bounds.y;
        this.x2 = bounds.x + bounds.width;
        this.y2 = bounds.y + bounds.height;
    }

    // 获取起点和终点的控制点矩形区域
    // 起点-handle的一半大小偏移，确保控制点以起点为中心
    public Rectangle getStartHandle() {
        int halfHandle = HANDLE_SIZE / 2;
        return new Rectangle(x - halfHandle, y - halfHandle, HANDLE_SIZE, HANDLE_SIZE);
    }

    public Rectangle getEndHandle() {
        int halfHandle = HANDLE_SIZE / 2;
        return new Rectangle(x2 - halfHandle, y2 - halfHandle, HANDLE_SIZE, HANDLE_SIZE);
    }

    public void moveStartPoint(int dx, int dy) {
        this.x += dx;
        this.y += dy;
    }

    public void moveEndPoint(int dx, int dy) {
        this.x2 += dx;
        this.y2 += dy;
    }

    public Point getStartPoint() {
        return new Point(x, y);
    }

    public Point getEndPoint() {
        return new Point(x2, y2);
    }

    public void setEndpoints(Point start, Point end) {
        this.x = start.x;
        this.y = start.y;
        this.x2 = end.x;
        this.y2 = end.y;
         this.rotationCenter = new Point((x + x2) / 2, (y + y2) / 2);
    }
    @Override
    public Point getRotationCenter() {
    return new Point((x + x2) / 2, (y + y2) / 2);
     // 获取起点坐标
   
}
}