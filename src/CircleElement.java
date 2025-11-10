// 圆，纯凑数用的
import java.awt.Color;

public class CircleElement extends OvalElement {
    // MODIFIED: Constructor signature updated
    public CircleElement(int x, int y, int diameter, Color borderColor, Color fillColor, int borderThickness) {
        super(x, y, diameter, diameter, borderColor, fillColor, borderThickness);
    }
}