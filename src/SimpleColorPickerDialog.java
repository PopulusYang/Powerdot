import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import javax.swing.BorderFactory;
import javax.swing.JColorChooser;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

/**
 * 简化版颜色选择器：沿用系统 JColorChooser，缩小窗口，只保留两个小预览方块（当前/新增）。
 */
public class SimpleColorPickerDialog {

    public static Color pickColor(Component parent, Color initial, String title) {
        Color base = initial != null ? initial : Color.WHITE;
        JColorChooser chooser = new JColorChooser(base);
        chooser.setPreviewPanel(new TwoSquarePreview(base));

        java.awt.Window win = SwingUtilities.getWindowAncestor(parent);
        Frame owner = (win instanceof Frame) ? (Frame) win : null;
        final Color[] result = new Color[1];

        JDialog dialog = JColorChooser.createDialog(
                owner,
                title != null ? title : "选择颜色",
                true,
                chooser,
                e -> result[0] = chooser.getColor(),
                null);

        if (dialog != null) {
            dialog.setPreferredSize(new Dimension(480, 520)); // 缩小的界面
            dialog.pack();
            dialog.setLocationRelativeTo(parent);
            dialog.setVisible(true);
        }
        return result[0];
    }

    /**
     * 预览面板：两个小正方形，左“当前”右“新增”。
     */
    private static class TwoSquarePreview extends JPanel {
        private final Color original;

        TwoSquarePreview(Color original) {
            this.original = original != null ? original : Color.WHITE;
            setPreferredSize(new Dimension(120, 60));
            setLayout(new FlowLayout(FlowLayout.CENTER, 12, 10));
            add(makeColumn("当前", this.original));
            add(makeColumn("新增", this.original));
        }

        private JPanel makeColumn(String text, Color color) {
            JPanel col = new JPanel(new BorderLayout(0, 4));
            JLabel box = new JLabel();
            box.setOpaque(true);
            box.setBackground(color);
            box.setPreferredSize(new Dimension(24, 24));
            box.setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY));
            col.add(box, BorderLayout.CENTER);
            JLabel lbl = new JLabel(text, javax.swing.SwingConstants.CENTER);
            col.add(lbl, BorderLayout.SOUTH);
            return col;
        }

        @Override
        protected void paintComponent(java.awt.Graphics g) {
            super.paintComponent(g);
            // 更新“新增”方块颜色
            if (getComponentCount() >= 2 && getComponent(1) instanceof JPanel col) {
                java.awt.Component box = col.getComponent(0);
                if (box instanceof JLabel lbl && getParent() instanceof JColorChooser chooser) {
                    lbl.setBackground(chooser.getColor());
                }
            }
            // “当前”保持 original
            if (getComponentCount() >= 1 && getComponent(0) instanceof JPanel col) {
                java.awt.Component box = col.getComponent(0);
                if (box instanceof JLabel lbl) {
                    lbl.setBackground(original);
                }
            }
        }
    }
}
