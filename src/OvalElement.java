
// 椭圆
import java.awt.*;

public class OvalElement extends ShapeElement {
    // MODIFIED: Constructor signature updated
    public OvalElement(int x, int y, int width, int height, Color borderColor, Color fillColor, int borderThickness) {
        super(x, y, width, height, borderColor, fillColor, borderThickness);
    }

    @Override
    public void draw(Graphics g) {
        Graphics2D g2d = (Graphics2D) g.create();

        if (fillColor != null) {
            g2d.setColor(fillColor);
            g2d.fillOval(x, y, width, height);
        }

        if (borderColor != null && borderThickness > 0) {
            // MODIFIED: Set the stroke before drawing the border
            g2d.setStroke(new BasicStroke(borderThickness, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f,
                    dashArray, 0.0f));
            g2d.setColor(borderColor);
            g2d.drawOval(x, y, width, height);
        }
        g2d.dispose();
    }
}