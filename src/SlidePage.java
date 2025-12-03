
//文件名： SlidePage.java
//功能： 表示幻灯片中的单个页面，包含多个幻灯片元素
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.awt.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

public class SlidePage implements Serializable {
    private List<SlideElement> elements;
    private java.awt.Color backgroundColor = java.awt.Color.WHITE;
    private transient BufferedImage backgroundImage;
    public enum BackgroundMode {
        SOLID, GRADIENT, IMAGE_STRETCH, IMAGE_TILE
    }

    private BackgroundMode backgroundMode = BackgroundMode.SOLID;
    private java.awt.Color gradientStart = java.awt.Color.WHITE;
    private java.awt.Color gradientEnd = java.awt.Color.WHITE;

    public SlidePage() {
        this.elements = new ArrayList<>();
    }

    public void addElement(SlideElement element) {
        elements.add(element);
    }

    public void addElement(int index, SlideElement element) {
        if (index >= 0 && index <= elements.size()) {
            elements.add(index, element);
        } else {
            elements.add(element);
        }
    }

    public void removeElement(SlideElement element) {
        elements.remove(element);
    }

    public List<SlideElement> getElements() {
        return elements;
    }

    /**
     * NEW: Clears all elements from the page.
     */
    public void clearElements() {
        elements.clear();
    }

    public java.awt.Color getBackgroundColor() {
        if (backgroundColor == null) {
            return java.awt.Color.WHITE;
        }
        return backgroundColor;
    }

    public void setBackgroundColor(java.awt.Color backgroundColor) {
        this.backgroundColor = backgroundColor;
    }

    public BufferedImage getBackgroundImage() {
        return backgroundImage;
    }

    public void setBackgroundImage(BufferedImage backgroundImage) {
        this.backgroundImage = backgroundImage;
    }

    public BackgroundMode getBackgroundMode() {
        return backgroundMode == null ? BackgroundMode.SOLID : backgroundMode;
    }

    public void setBackgroundMode(BackgroundMode mode) {
        this.backgroundMode = mode;
    }

    public java.awt.Color getGradientStart() {
        return gradientStart == null ? getBackgroundColor() : gradientStart;
    }

    public void setGradientStart(java.awt.Color gradientStart) {
        this.gradientStart = gradientStart;
    }

    public java.awt.Color getGradientEnd() {
        return gradientEnd == null ? getBackgroundColor() : gradientEnd;
    }

    public void setGradientEnd(java.awt.Color gradientEnd) {
        this.gradientEnd = gradientEnd;
    }

    /**
     * 绘制页面背景，支持纯色、渐变、图片拉伸/平铺。
     */
    public void renderBackground(Graphics2D g2d, int width, int height) {
        BackgroundMode mode = getBackgroundMode();
        switch (mode) {
            case GRADIENT: {
                Color start = getGradientStart();
                Color end = getGradientEnd();
                GradientPaint gp = new GradientPaint(0, 0, start, 0, height, end);
                Paint old = g2d.getPaint();
                g2d.setPaint(gp);
                g2d.fillRect(0, 0, width, height);
                g2d.setPaint(old);
                break;
            }
            case IMAGE_STRETCH: {
                if (backgroundImage != null) {
                    g2d.drawImage(backgroundImage, 0, 0, width, height, null);
                } else {
                    g2d.setColor(getBackgroundColor());
                    g2d.fillRect(0, 0, width, height);
                }
                break;
            }
            case IMAGE_TILE: {
                if (backgroundImage != null) {
                    int imgW = backgroundImage.getWidth();
                    int imgH = backgroundImage.getHeight();
                    for (int x = 0; x < width; x += imgW) {
                        for (int y = 0; y < height; y += imgH) {
                            g2d.drawImage(backgroundImage, x, y, null);
                        }
                    }
                } else {
                    g2d.setColor(getBackgroundColor());
                    g2d.fillRect(0, 0, width, height);
                }
                break;
            }
            case SOLID:
            default: {
                g2d.setColor(getBackgroundColor());
                g2d.fillRect(0, 0, width, height);
                break;
            }
        }
    }

    private void writeObject(ObjectOutputStream out) throws IOException {
        out.defaultWriteObject();
        if (backgroundImage != null) {
            out.writeBoolean(true);
            ImageIO.write(backgroundImage, "png", out);
        } else {
            out.writeBoolean(false);
        }
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        boolean hasImage = in.readBoolean();
        if (hasImage) {
            try {
                backgroundImage = ImageIO.read(in);
            } catch (IOException e) {
                e.printStackTrace();
                backgroundImage = null;
            }
        } else {
            backgroundImage = null;
        }
    }
}
