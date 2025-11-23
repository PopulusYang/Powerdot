import javax.swing.*;
import java.awt.*;

public class SplashScreen extends JWindow {
    private int duration;

    public SplashScreen(int duration) {
        this.duration = duration;
    }

    public void showSplash() {
        JPanel content = (JPanel) getContentPane();
        content.setBackground(Color.WHITE);

        int width = 450;
        int height = 300;
        Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
        int x = (screen.width - width) / 2;
        int y = (screen.height - height) / 2;
        setBounds(x, y, width, height);

        JLabel label = new JLabel("PowerDot 3", SwingConstants.CENTER);
        label.setFont(new Font("SansSerif", Font.BOLD, 48));
        label.setForeground(new Color(50, 100, 200));
        content.add(label, BorderLayout.CENTER);

        JLabel copyrt = new JLabel("Loading...", SwingConstants.CENTER);
        copyrt.setFont(new Font("SansSerif", Font.PLAIN, 12));
        content.add(copyrt, BorderLayout.SOUTH);

        Color borderColor = new Color(50, 100, 200);
        content.setBorder(BorderFactory.createLineBorder(borderColor, 2));

        setVisible(true);

        try {
            Thread.sleep(duration);
        } catch (Exception e) {
            e.printStackTrace();
        }

        setVisible(false);
    }
}
