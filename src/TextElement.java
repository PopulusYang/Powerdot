// 文本框元素类
import java.awt.*;
import java.awt.image.BufferedImage;

public class TextElement extends SlideElement {
    private String text; // 字符串
    private Font font; // 字体
    private Color color; // 文字颜色
    // 以四边形编辑大小
    private int width;
    private int height;
    // 默认16号宋体
    private static final Font DEFAULT_FONT = new Font("宋体", Font.PLAIN, 16);
    public TextElement(String text, int x, int y, int width, int height)
    {
        super(x, y);
        this.text = text;
        this.font = DEFAULT_FONT;
        this.color = Color.BLACK;
        this.width = width;
        this.height = height;
    }
    @Override
    public void draw(Graphics g)
    {
        Graphics2D g2d = (Graphics2D) g.create();
        // 灰色边框
        g2d.setColor(Color.LIGHT_GRAY);
        g2d.drawRect(x, y, width, height);
        g2d.setFont(font);
        //以设定颜色绘制文字
        g2d.setColor(color);
        FontMetrics fm = g2d.getFontMetrics(font);
        // 计算文字垂直居中的位置
        int textY = y + fm.getAscent() + (height - (fm.getAscent() + fm.getDescent())) / 2;
        g2d.clipRect(x, y, width, height);
        g2d.drawString(text, x + 5, textY);
        g2d.dispose();
    }
    @Override
    public boolean contains(Point p)
    {
        return new Rectangle(x, y, width, height).contains(p);
    }
    @Override
    public Rectangle getBounds()
    {
        return new Rectangle(x, y, width, height);
    }
    public void setText(String text) { this.text = text; }
    public void setFont(Font font) { this.font = font; }
    public void setColor(Color color) { this.color = color; }
    public Font getFont() { return font; }
    public String getText() { return text; }
    public Color getColor() { return color; }
    @Override
    public void setBounds(Rectangle bounds)
    {
        this.x = bounds.x;
        this.y = bounds.y;
        this.width = bounds.width;
        this.height = bounds.height;
        this.font = findOptimalFont(this.font.getName(), this.font.getStyle(), this.height);
    }
    // 根据文本框大小寻找最合适的字体大小
    private Font findOptimalFont(String fontName, int style, int boxHeight)
    {
        BufferedImage tempImage = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        // 创建Graphics2D对象以测量字体
        Graphics2D g2d = tempImage.createGraphics();
        int bestSize = 1;
        // 从2号字体开始尝试，直到字体高度超过文本框高度减去一些边距
        for (int size = 2; size < 200; size++)
        {
            Font testFont = new Font(fontName, style, size);
            FontMetrics fm = g2d.getFontMetrics(testFont);
            if (fm.getHeight() > boxHeight - 4)
                break;
            bestSize = size;
        }
        g2d.dispose();
        return new Font(fontName, style, bestSize);
    }
}