import java.awt.*;
import java.awt.image.BufferedImage;

public class TextElement extends SlideElement {
    private String text; private Font font; private Color color; private int width; private int height;
    private static final Font DEFAULT_FONT = new Font("宋体", Font.PLAIN, 16);
    public TextElement(String text, int x, int y, int width, int height) { super(x, y); this.text = text; this.font = DEFAULT_FONT; this.color = Color.BLACK; this.width = width; this.height = height; }
    @Override public void draw(Graphics g) { Graphics2D g2d = (Graphics2D) g.create(); g2d.setColor(Color.LIGHT_GRAY); g2d.drawRect(x, y, width, height); g2d.setFont(font); g2d.setColor(color); FontMetrics fm = g2d.getFontMetrics(font); int textY = y + fm.getAscent() + (height - (fm.getAscent() + fm.getDescent())) / 2; g2d.clipRect(x, y, width, height); g2d.drawString(text, x + 5, textY); g2d.dispose(); }
    @Override public boolean contains(Point p) { return new Rectangle(x, y, width, height).contains(p); }
    @Override public Rectangle getBounds() { return new Rectangle(x, y, width, height); }
    public void setText(String text) { this.text = text; }
    public void setFont(Font font) { this.font = font; }
    public void setColor(Color color) { this.color = color; }
    public Font getFont() { return font; }
    public String getText() { return text; }
    public Color getColor() { return color; }
    @Override public void setBounds(Rectangle bounds) { this.x = bounds.x; this.y = bounds.y; this.width = bounds.width; this.height = bounds.height; this.font = findOptimalFont(this.font.getName(), this.font.getStyle(), this.width, this.height); }
    private Font findOptimalFont(String fontName, int style, int boxWidth, int boxHeight) { BufferedImage tempImage = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB); Graphics2D g2d = tempImage.createGraphics(); int bestSize = 1; for (int size = 2; size < 200; size++) { Font testFont = new Font(fontName, style, size); FontMetrics fm = g2d.getFontMetrics(testFont); if (fm.getHeight() > boxHeight - 4) { break; } bestSize = size; } g2d.dispose(); return new Font(fontName, style, bestSize); }
}