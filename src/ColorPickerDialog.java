import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JColorChooser;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JTabbedPane;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.colorchooser.AbstractColorChooserPanel;

/** 仿截图的小型颜色选择器：标签（标准/自定义/高级）、两个小方块预览（当前/新增）、透明度滑条。 */
public class ColorPickerDialog {

    public static Color pickColor(Component parent, Color initial, String title) {
        Color base = initial != null ? initial : Color.WHITE;
        JColorChooser chooser = new JColorChooser(base);
        chooser.setPreviewPanel(new TwoSquarePreview(base));

        // 分类面板：标准(样本)、自定义(HSB/HSL/RGB)、高级(其余)
        AbstractColorChooserPanel[] panels = chooser.getChooserPanels();
        List<AbstractColorChooserPanel> swatch = new ArrayList<>();
        List<AbstractColorChooserPanel> custom = new ArrayList<>();
        List<AbstractColorChooserPanel> advanced = new ArrayList<>();
        for (AbstractColorChooserPanel p : panels) {
            String name = p.getDisplayName();
            if ("Swatches".equalsIgnoreCase(name) || "样本".equals(name)) {
                swatch.add(p);
            } else if ("HSB".equalsIgnoreCase(name) || "HSL".equalsIgnoreCase(name) || "RGB".equalsIgnoreCase(name)) {
                custom.add(p);
            } else {
                advanced.add(p);
            }
        }

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("标准", wrap(chooser, swatch));
        tabs.addTab("自定义", wrap(chooser, custom.isEmpty() ? swatch : custom));
        tabs.addTab("高级", wrap(chooser, advanced.isEmpty() ? panels : advanced.toArray(new AbstractColorChooserPanel[0])));

        // 透明度滑条
        int alpha = base.getAlpha();
        JSlider alphaSlider = new JSlider(0, 255, alpha);
        alphaSlider.setPaintTicks(true);
        alphaSlider.setMajorTickSpacing(51);
        JLabel alphaLabel = new JLabel("透明度(T)");
        JLabel alphaValue = new JLabel(alpha + " / 255");
        alphaSlider.addChangeListener(e -> alphaValue.setText(alphaSlider.getValue() + " / 255"));

        JPanel alphaRow = new JPanel(new BorderLayout(8, 0));
        alphaRow.add(alphaLabel, BorderLayout.WEST);
        alphaRow.add(alphaSlider, BorderLayout.CENTER);
        alphaRow.add(alphaValue, BorderLayout.EAST);
        alphaRow.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));

        JPanel content = new JPanel(new BorderLayout());
        content.add(tabs, BorderLayout.CENTER);
        content.add(alphaRow, BorderLayout.SOUTH);

        java.awt.Window win = SwingUtilities.getWindowAncestor(parent);
        Frame owner = (win instanceof Frame) ? (Frame) win : null;
        final Color[] result = new Color[1];

        JDialog dialog = new JDialog(owner, title != null ? title : "选择颜色", true);
        dialog.setLayout(new BorderLayout());
        dialog.add(content, BorderLayout.CENTER);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton ok = new JButton("确定");
        JButton cancel = new JButton("取消");
        ok.addActionListener(e -> {
            Color c = chooser.getColor();
            if (c != null) {
                result[0] = new Color(c.getRed(), c.getGreen(), c.getBlue(), alphaSlider.getValue());
            }
            dialog.dispose();
        });
        cancel.addActionListener(e -> {
            result[0] = null;
            dialog.dispose();
        });
        actions.add(ok);
        actions.add(cancel);
        dialog.add(actions, BorderLayout.SOUTH);

        dialog.setPreferredSize(new Dimension(520, 560));
        dialog.pack();
        dialog.setLocationRelativeTo(parent);
        dialog.setVisible(true);
        return result[0];
    }

    private static JPanel wrap(JColorChooser chooser, AbstractColorChooserPanel[] panels) {
        JPanel wrapper = new JPanel();
        wrapper.setLayout(new BoxLayout(wrapper, BoxLayout.Y_AXIS));
        JColorChooser sub = new JColorChooser(chooser.getColor());
        sub.setPreviewPanel(new TwoSquarePreview(chooser.getColor()));
        sub.setChooserPanels(panels);
        sub.getSelectionModel().addChangeListener(e -> chooser.setColor(sub.getColor()));
        wrapper.add(sub);
        wrapper.add(Box.createVerticalStrut(8));
        return wrapper;
    }

    private static JPanel wrap(JColorChooser chooser, List<AbstractColorChooserPanel> list) {
        return wrap(chooser, list.toArray(new AbstractColorChooserPanel[0]));
    }

    /** 两个小正方形预览：左“当前”右“新增” */
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
            box.setHorizontalAlignment(SwingConstants.CENTER);
            col.add(box, BorderLayout.CENTER);
            JLabel lbl = new JLabel(text, SwingConstants.CENTER);
            col.add(lbl, BorderLayout.SOUTH);
            return col;
        }

        @Override
        protected void paintComponent(java.awt.Graphics g) {
            super.paintComponent(g);
            // 更新新增
            if (getComponentCount() >= 2 && getComponent(1) instanceof JPanel col) {
                java.awt.Component box = col.getComponent(0);
                if (box instanceof JLabel lbl && getParent() instanceof JColorChooser chooser) {
                    lbl.setBackground(chooser.getColor());
                }
            }
            // 当前保持 original
            if (getComponentCount() >= 1 && getComponent(0) instanceof JPanel col) {
                java.awt.Component box = col.getComponent(0);
                if (box instanceof JLabel lbl) {
                    lbl.setBackground(original);
                }
            }
        }
    }
}
