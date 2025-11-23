import java.awt.*;

public abstract class ShapeElement extends SlideElement {
    protected Color fillColor;
    protected Color borderColor;
    protected int width, height;

    // NEW: Fields for border style
    protected int borderThickness;
    protected float[] dashArray; // null for solid, an array for dashed/dotted

    public ShapeElement(int x, int y, int width, int height, Color borderColor, Color fillColor, int borderThickness) {
        super(x, y);
        this.width = width;
        this.height = height;
        this.borderColor = borderColor;
        this.fillColor = fillColor;
        this.borderThickness = borderThickness;
        this.dashArray = null; // Default to solid line
    }

    @Override
    public boolean contains(Point p) {
        if (rotation == 0) {
            return getBounds().contains(p);
        }
        Point center = new Point(x + width / 2, y + height / 2);
        Point rotatedP = rotatePoint(p, center, -rotation);
        return getBounds().contains(rotatedP);
    }

    @Override
    public Rectangle getBounds() {
        return new Rectangle(x, y, width, height);
    }

    public void setFillColor(Color fillColor) {
        this.fillColor = fillColor;
    }

    public void setBorderColor(Color borderColor) {
        this.borderColor = borderColor;
    }

    public Color getFillColor() {
        return fillColor;
    }

    public Color getBorderColor() {
        return borderColor;
    }

    // NEW: Methods to control border style
    public void setBorderStyle(float[] dashArray) {
        this.dashArray = dashArray;
    }

    public float[] getBorderStyle() {
        return this.dashArray;
    }

    @Override
    public void setBounds(Rectangle bounds) {
        this.x = bounds.x;
        this.y = bounds.y;
        this.width = bounds.width;
        this.height = bounds.height;
    }

    public void setBorderThickness(int thickness) {
        this.borderThickness = thickness;
    }

    public int getBorderThickness() {
        return borderThickness;
    }
}