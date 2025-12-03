
// 文本框元素类
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JTextPane;
import javax.swing.text.BadLocationException;
import javax.swing.text.StyledDocument;
import javax.swing.text.rtf.RTFEditorKit;

public class TextElement extends SlideElement {
    private String text; // 字符串
    private Font font; // 字体
    private Color color; // 文字颜色
    private String richTextRtf; // 可选的 RTF 富文本
    private Color borderColor = Color.BLACK;
    private int borderThickness = 0;
    private float[] borderStyle = null; 
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
        this.richTextRtf = null;
        this.width = width;
        this.height = height;
        this.borderColor = null;  // null 表示无边框
        this.borderThickness = 0;
        this.borderStyle = null;
    }
    @Override
public Point getRotationCenter() {
    return new Point(x + width / 2, y + height / 2);
}

    @Override
    public void draw(Graphics g) {
        Graphics2D g2d = (Graphics2D) g.create();

        if (rotation != 0) {
            g2d.rotate(Math.toRadians(rotation), x + width / 2.0, y + height / 2.0);
        }

        if (richTextRtf != null) {
            JTextPane pane = new JTextPane();
            pane.setEditable(false);
            pane.setOpaque(false);
            StyledDocument doc = createDocumentFromRtf(richTextRtf);
            if (doc != null) {
                pane.setDocument(doc);
            } else {
                pane.setText(text);
            }
            pane.setFont(font);
            pane.setForeground(color);
            pane.setBounds(x, y, width, height);
            pane.setSize(width, height);
            pane.printAll(g2d);
        } else {
            g2d.setFont(font);
            // 以设定颜色绘制文字
            g2d.setColor(color);
            FontMetrics fm = g2d.getFontMetrics(font);

            List<String> lines = wrapText(text, fm, width - 10);
            int lineHeight = fm.getHeight();

            // 顶部内边距，避免文字紧贴边框
            int topPadding = 4;
            int startY = y + topPadding + fm.getAscent();

            g2d.clipRect(x, y + topPadding, width, height - topPadding);

            for (int i = 0; i < lines.size(); i++) {
                g2d.drawString(lines.get(i), x + 5, startY + i * lineHeight);
            }
        }
        if (borderThickness > 0) {
            g2d.setClip(null); // 取消文字剪裁，避免顶边被截掉
            if (borderStyle != null) {
        g2d.setStroke(new BasicStroke(borderThickness, BasicStroke.CAP_BUTT,
                BasicStroke.JOIN_MITER, 10f, borderStyle, 0f));
    } else {
        g2d.setStroke(new BasicStroke(borderThickness));
    }
    g2d.setColor(borderColor);
    g2d.drawRect(x, y, width, height);
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
        if (rotation == 0) {
            return new Rectangle(x, y, width, height).contains(p);
        }
        Point center = new Point(x + width / 2, y + height / 2);
        Point rotatedP = rotatePoint(p, center, -rotation);
        return new Rectangle(x, y, width, height).contains(rotatedP);
    }

    @Override
    public Rectangle getBounds() {
        return new Rectangle(x, y, width, height);
    }

    public void setText(String text) {
        this.text = text;
        this.richTextRtf = null;
    }
public float[] getBorderStyle() { return borderStyle; }
public void setBorderStyle(float[] dash) { this.borderStyle = dash; }

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

    public String getRichTextRtf() {
        return richTextRtf;
    }

    public void setRichTextRtf(String rtf) {
        this.richTextRtf = rtf;
    }

    public Color getBorderColor() {
        return borderColor;
    }

    public void setBorderColor(Color borderColor) {
        this.borderColor = borderColor;
    }

    public int getBorderThickness() {
        return borderThickness;
    }

    public void setBorderThickness(int borderThickness) {
        this.borderThickness = Math.max(0, borderThickness);
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

    private StyledDocument createDocumentFromRtf(String rtf) {
        try {
            RTFEditorKit kit = new RTFEditorKit();
            StyledDocument doc = (StyledDocument) kit.createDefaultDocument();
            kit.read(new ByteArrayInputStream(rtf.getBytes(StandardCharsets.UTF_8)), doc, 0);
            return doc;
        } catch (Exception ex) {
            return null;
        }
    }

    public String exportDocumentToRtf(StyledDocument doc) {
        try {
            RTFEditorKit kit = new RTFEditorKit();
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            kit.write(out, doc, 0, doc.getLength());
            return out.toString(StandardCharsets.UTF_8);
        } catch (Exception e) {
            return null;
        }
    }

    public StyledDocument createDocumentForEditing() {
        if (richTextRtf != null) {
            StyledDocument doc = createDocumentFromRtf(richTextRtf);
            if (doc != null) return doc;
        }
        try {
            RTFEditorKit kit = new RTFEditorKit();
            StyledDocument doc = (StyledDocument) kit.createDefaultDocument();
            doc.insertString(0, text, null);
            return doc;
        } catch (BadLocationException e) {
            return null;
        }
    }
}
