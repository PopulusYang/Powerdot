import javax.imageio.ImageIO;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

public class ImageElement extends SlideElement {
    private transient Image image;
    private String imagePath;
    private int width;
    private int height;
    public ImageElement(int x, int y, String imagePath) throws IOException { super(x, y); this.imagePath = imagePath; loadImage(); if (this.image != null) { this.width = this.image.getWidth(null); this.height = this.image.getHeight(null); } }
    private void loadImage() throws IOException { this.image = ImageIO.read(new File(this.imagePath)); }
    @Override public void draw(Graphics g) { if (image != null) { g.drawImage(image, x, y, width, height, null); } else { g.setColor(Color.RED); g.drawRect(x, y, 100, 100); g.drawString("图片加载失败", x + 10, y + 50); } }
    @Override public boolean contains(Point p) { return getBounds().contains(p); }
    @Override public Rectangle getBounds() { return new Rectangle(x, y, width, height); }
    private void writeObject(ObjectOutputStream out) throws IOException { out.defaultWriteObject(); }
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException { in.defaultReadObject(); loadImage(); }
    @Override public void setBounds(Rectangle bounds) { this.x = bounds.x; this.y = bounds.y; this.width = bounds.width; this.height = bounds.height; }
}