import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import javax.imageio.ImageIO;

public class ImageElement extends SlideElement {
    private transient BufferedImage image;
    private int width;
    private int height;

    public ImageElement(int x, int y, BufferedImage image) {
        super(x, y);
        this.image = image;
        if (this.image != null) {
            this.width = this.image.getWidth();
            this.height = this.image.getHeight();
        }
    }

    @Override
    public void draw(Graphics g) {
        Graphics2D g2d = (Graphics2D) g.create();
        if (rotation != 0) {
            g2d.rotate(Math.toRadians(rotation), x + width / 2.0, y + height / 2.0);
        }

        if (image != null) {
            g2d.drawImage(image, x, y, width, height, null);
        } else {
            g2d.setColor(Color.RED);
            g2d.drawRect(x, y, 100, 100);
            g2d.drawString("图片丢失", x + 10, y + 50);
        }
        g2d.dispose();
    }
    @Override
public Point getRotationCenter() {
    return new Point(x + width / 2, y + height / 2);
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

    @Override
    public void setBounds(Rectangle bounds) {
        this.x = bounds.x;
        this.y = bounds.y;
        this.width = bounds.width;
        this.height = bounds.height;
    }

    private void writeObject(ObjectOutputStream out) throws IOException {
        out.defaultWriteObject();
        if (image != null) {
            ImageIO.write(image, "png", out);
        }
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        try {
            image = ImageIO.read(in);
        } catch (IOException e) {
            e.printStackTrace();
            image = null;
        }
    }
}