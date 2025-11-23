
// 文本框元素类
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

public class TextElement extends SlideElement {
    private String text; // 字符串
    private Font font; // 字体
    private Color color; // 文字颜色
    // 以四边形编辑大小
    private int width;
    private int height;
    // 默认16号宋体
    private static final Font DEFAULT_FONT = new Font("宋体", Font.PLAIN, 16);

    public TextElement(String text, int x, int y, int width, int height) {
        super(x, y);
        this.text = text;
        this.font = DEFAULT_FONT;
        this.color = Color.BLACK;
        this.width = width;
        this.height = height;
    }

    @Override
    public void draw(Graphics g) {
        Graphics2D g2d = (Graphics2D) g.create();
        // 移除默认的灰色边框
        // g2d.setColor(Color.LIGHT_GRAY);
        // g2d.drawRect(x, y, width, height);

        g2d.setFont(font);
        // 以设定颜色绘制文字
        g2d.setColor(color);
        FontMetrics fm = g2d.getFontMetrics(font);

        List<String> lines = wrapText(text, fm, width - 10);
        int lineHeight = fm.getHeight();

        // 改为顶部对齐，以便与编辑时的JTextArea行为一致
        int startY = y + fm.getAscent();

        g2d.clipRect(x, y, width, height);

        for (int i = 0; i < lines.size(); i++) {
            g2d.drawString(lines.get(i), x + 5, startY + i * lineHeight);
        }
        g2d.dispose();
    }

    private List<String> wrapText(String text, FontMetrics fm, int maxWidth) {
        List<String> lines = new ArrayList<>();
        String[] paragraphs = text.split("\n", -1);

        for (String paragraph : paragraphs) {
            if (paragraph.isEmpty()) {
                lines.add("");
                continue;
            }

            StringBuilder currentLine = new StringBuilder();
            for (int i = 0; i < paragraph.length(); i++) {
                char c = paragraph.charAt(i);
                String testLine = currentLine.toString() + c;
                if (fm.stringWidth(testLine) > maxWidth && currentLine.length() > 0) {
                    lines.add(currentLine.toString());
                    currentLine = new StringBuilder();
                    currentLine.append(c);
                } else {
                    currentLine.append(c);
                }
            }
            if (currentLine.length() > 0) {
                lines.add(currentLine.toString());
            }
        }
        return lines;
    }

    @Override
    public boolean contains(Point p) {
        return new Rectangle(x, y, width, height).contains(p);
    }

    @Override
    public Rectangle getBounds() {
        return new Rectangle(x, y, width, height);
    }

    public void setText(String text) {
        this.text = text;
    }

    public void setFont(Font font) {
        this.font = font;
    }

    public void setColor(Color color) {
        this.color = color;
    }

    public Font getFont() {
        return font;
    }

    public String getText() {
        return text;
    }

    public Color getColor() {
        return color;
    }

    @Override
    public void setBounds(Rectangle bounds) {
        this.x = bounds.x;
        this.y = bounds.y;
        this.width = bounds.width;
        this.height = bounds.height;
    }

    public void setFontSize(int size) {
        this.font = this.font.deriveFont((float) size);
        // Adjust height to fit the new font size
        BufferedImage tempImage = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = tempImage.createGraphics();
        FontMetrics fm = g2d.getFontMetrics(this.font);
        this.height = fm.getHeight() + 4;
        g2d.dispose();
    }
}