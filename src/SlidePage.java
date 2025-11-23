
//文件名： SlidePage.java
//功能： 表示幻灯片中的单个页面，包含多个幻灯片元素
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

public class SlidePage implements Serializable {
    private List<SlideElement> elements;
    private java.awt.Color backgroundColor = java.awt.Color.WHITE;
    private transient BufferedImage backgroundImage;

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