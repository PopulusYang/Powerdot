import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.ChangeListener;

/**
 * ==========================================================================
 *   全新最终版  —— 完整 PowerPoint 风格颜色选择器
 *   包含：标准色盘、自定义色盘、HSV 色轮、亮度条、RGB/HSV/HSL/CMYK、
 *        HEX、预览（当前/新增）等。
 * ==========================================================================
 */

public class NewColorPickerDialog {

    /** 全局颜色更新监听器 */
    public interface ColorUpdateListener {
        void onColorUpdate(Color c);
    }

    /** 外部入口 */
    public static Color pickColor(Component parent, Color initial, String title) {

        Window owner = SwingUtilities.getWindowAncestor(parent);

        JDialog dialog;
        if (owner instanceof Frame)
            dialog = new JDialog((Frame) owner, title, true);
        else
            dialog = new JDialog((Frame) null, title, true);

        ColorChooserPanel panel = new ColorChooserPanel(initial);
        dialog.setContentPane(panel);

        dialog.pack();
        dialog.setResizable(false);
        dialog.setLocationRelativeTo(parent);
        dialog.setVisible(true);

        return panel.getFinalColor();
    }


    // ==========================================================================
    // 主选择面板
        // ==========================================================================
    static class UnderlineTabButton extends JButton {

        private boolean selected = false;

        public UnderlineTabButton(String text, Runnable action) {
            super(text);
            addActionListener(e -> action.run());
            setFocusPainted(false);
            setBorderPainted(false);
            setContentAreaFilled(false);
            setOpaque(false);
            setForeground(Color.BLACK);
            setFont(new Font("微软雅黑", Font.PLAIN, 11));
        }

        public void setSelectedTab(boolean b) {
            this.selected = b;

            if (b) {
                setForeground(new Color(0, 102, 204));       // 微软蓝
                setFont(new Font("微软雅黑", Font.PLAIN, 12));
            } else {
                setForeground(Color.BLACK);
                setFont(new Font("微软雅黑", Font.PLAIN, 12));
            }

            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);

            if (selected) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(new Color(0, 102, 204));
                g2.fillRect(0, getHeight() - 3, getWidth(), 3); // 下划线
                g2.dispose();
            }
        }
    }
        /** 简单可复用的圆角边框（只画边框，不改变背景） */
    static class RoundBorder extends LineBorder {

        private int radius;

        public RoundBorder(Color color, int thickness, int radius) {
            super(color, thickness, true);
            this.radius = radius;
        }

        @Override
        public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            g2.setColor(lineColor);
            g2.setStroke(new BasicStroke(thickness));

            g2.drawRoundRect(
                    x + thickness/2,
                    y + thickness/2,
                    width - thickness,
                    height - thickness,
                    radius,
                    radius
            );

            g2.dispose();
        }
    }
    static class RoundButton extends JButton {

        private Color bgColor;
        private Color textColor;
        private int arc = 18;

        public RoundButton(String text, Color bgColor, Color textColor) {
            super(text);
            this.bgColor = bgColor;
            this.textColor = textColor;

            setFocusPainted(false);
            setBorderPainted(false);
            setContentAreaFilled(false);
            setOpaque(false);
            setForeground(textColor);
            setFont(new Font("微软雅黑", Font.PLAIN, 14));
        }

        @Override
        protected void paintComponent(Graphics g) {

            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // 背景圆角
            g2.setColor(bgColor);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), arc, arc);

            // 如果是取消按钮（白底）加灰边框
            if (bgColor.equals(Color.WHITE)) {
                g2.setColor(new Color(180, 180, 180));
                g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, arc, arc);
            }

            g2.dispose();

            // 再绘制文字
            super.paintComponent(g);
        }
    }

    static class TopRoundedColorPanel extends JPanel {
        private Color color;

        public TopRoundedColorPanel(Color c) {
            this.color = c;
            setOpaque(true);
        }

        public void setColor(Color c) {
            this.color = c;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            int arc = 20;

            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            g2.setColor(color);

            // 先画一个完整圆角矩形
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), arc, arc);

            // 覆盖下半部分，让底部变成直角
            g2.fillRect(0, getHeight()/2, getWidth(), getHeight()/2);

            g2.dispose();
        }
    }


    static class BottomRoundedColorPanel extends JPanel {
        private Color color;

        public BottomRoundedColorPanel(Color c) {
            this.color = c;
            setOpaque(true);
        }

        public void setColor(Color c) {
            this.color = c;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            int arc = 20;

            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            g2.setColor(color);

            // 先画完整圆角矩形
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), arc, arc);

            // 覆盖上半部分，让顶部变成直角
            g2.fillRect(0, 0, getWidth(), getHeight()/2);

            g2.dispose();
        }
    }


    private static class ColorChooserPanel extends JPanel {

        private Color originalColor;
        private Color finalColor;

        private PreviewPanel previewPanel;

        private int alpha = 255;
        private StandardPanel standardPage;
        private CustomPanel customPage;

        private UnderlineTabButton btnStandard;
        private UnderlineTabButton btnCustom;
        private JPanel contentPanel;   // 切换内容的容器

        private void switchToStandard() {
        btnStandard.setSelectedTab(true);
        btnCustom.setSelectedTab(false);

        contentPanel.removeAll();
        contentPanel.add(standardPage);
        contentPanel.revalidate();
        contentPanel.repaint();
        }

        // 切换到“自定义”页
        private void switchToCustom() {
            btnStandard.setSelectedTab(false);
            btnCustom.setSelectedTab(true);

            contentPanel.removeAll();
            contentPanel.add(customPage);
            contentPanel.revalidate();
            contentPanel.repaint();
        }
        ColorChooserPanel(Color initial) {
            if (initial == null || initial.getAlpha() == 0) {
                initial = Color.BLUE;
            }

            setLayout(null);
            setBorder(null);

            setLayout(null);

            btnStandard = new UnderlineTabButton("标准", this::switchToStandard);
            btnCustom   = new UnderlineTabButton("自定义", this::switchToCustom);

            
            btnStandard.setBounds(5, 0, 60, 30);
            btnCustom.setBounds(70, 0, 70, 30);
            btnStandard.setFont(new Font("微软雅黑", Font.PLAIN, 12));
            btnCustom.setFont(new Font("微软雅黑", Font.PLAIN, 12));
            add(btnStandard);
            add(btnCustom);
            
            // 内容区
            contentPanel = new RoundPanel();
            contentPanel.setOpaque(false);
            contentPanel.setBounds(20, 40, 280, 350);
            add(contentPanel);

            // 创建两个页面
            standardPage = new StandardPanel(this::onColorUpdate, initial);
            customPage   = new CustomPanel(this::onColorUpdate, initial);

            // 初始显示“标准”
            contentPanel.removeAll();
            contentPanel.add(standardPage);
            standardPage.setBounds(20, 40, 280, 340);
            standardPage.setOpaque(false);
            contentPanel.repaint();
            btnStandard.setSelectedTab(true);
            // ============================================================
            // 3) 右侧预览区
            // ============================================================
            previewPanel = new PreviewPanel(initial);
            previewPanel.setBounds(300, 40, 70, 400);
            add(previewPanel);

            // 透明度控制
            // ============================================================
            JLabel alphaLabel = new JLabel("透明度(T)");
            alphaLabel.setBounds(20, 395, 100, 25);
            add(alphaLabel);

            // Slider 0~100
            JSlider alphaSlider = new JSlider(0, 100, 100);
            alphaSlider.setBounds(15, 415, 220, 25);
            alphaSlider.setPaintTicks(false);
            alphaSlider.setPaintLabels(false);
            add(alphaSlider);

            // 数字输入框
            JSpinner alphaSpinner = new JSpinner(
                    new SpinnerNumberModel(100, 0, 100, 1));
            alphaSpinner.setBounds(240, 410, 40, 30);
            add(alphaSpinner);

            // 百分号
            JLabel percent = new JLabel("%");
            percent.setFont(percent.getFont().deriveFont(14f));
            percent.setBounds(285, 410, 30, 30);
            add(percent);


            // ============================================================
            // 双向联动：Slider <-> Spinner
            // ============================================================
            alphaSlider.addChangeListener(e -> {
                int v = alphaSlider.getValue();
                alphaSpinner.setValue(v);
                alpha = (int)(v * 2.55);  // 0~100 对应 0~255

                // 更新 previewPanel 的颜色透明度
                Color c = previewPanel.getNewColor();
                Color newC = new Color(c.getRed(), c.getGreen(), c.getBlue(), alpha);
                previewPanel.setNewColor(newC);
            });

            alphaSpinner.addChangeListener(e -> {
                int v = (int)alphaSpinner.getValue();
                alphaSlider.setValue(v);
                alpha = (int)(v * 2.55);

                Color c = previewPanel.getNewColor();
                Color newC = new Color(c.getRed(), c.getGreen(), c.getBlue(), alpha);
                previewPanel.setNewColor(newC);
            });

            // ============================================================
            // 4) 底部 OK / Cancel 按钮
            // ============================================================
            RoundButton ok = new RoundButton("确定", new Color(58,122,254), Color.WHITE);
            RoundButton cancel = new RoundButton("取消", Color.WHITE, Color.BLACK);

            
            

            ok.setBounds(200, 445, 70, 30);
            cancel.setBounds(280, 445, 70, 30);

            ok.addActionListener(e -> {
                finalColor = previewPanel.getNewColor();
                SwingUtilities.getWindowAncestor(this).dispose();
            });

            cancel.addActionListener(e -> {
                finalColor = null;
                SwingUtilities.getWindowAncestor(this).dispose();
            });

            add(ok);
            add(cancel);

            setPreferredSize(new Dimension(370, 490));
        }
        private void onColorUpdate(Color c) {
            previewPanel.setNewColor(c);
        }
        public Color getFinalColor() {
            return finalColor;
        }
    }
    // ==========================================================================
    // ★★ PowerPoint 风格预览面板（新增 / 当前）
    // ==========================================================================
    
    // 右侧预览面板：绝对布局 + 两个色块（当前 / 新增）
    private static class PreviewPanel extends JPanel {

        private Color oldColor;
        private Color newColor;

         private TopRoundedColorPanel newColorPanel;   // 新增颜色块（上圆角）
        private BottomRoundedColorPanel oldColorPanel; // 当前颜色块（下圆角）

        PreviewPanel(Color initial) {

            this.oldColor = initial; // 现在确保 oldColor 不会是透明色或 null
            this.newColor = this.oldColor;
            // 绝对布局
             setLayout(null);
            setPreferredSize(new Dimension(70, 400));

            // 新增颜色块
            JLabel newLabel = new JLabel("新增");
            newLabel.setBounds(15, 290, 40, 40);
            add(newLabel);
            newColorPanel = new TopRoundedColorPanel(Color.BLUE);
            oldColorPanel = new BottomRoundedColorPanel(Color.BLUE);
            newColorPanel.setColor(Color.BLUE);
            oldColorPanel.setColor(Color.BLUE);
            oldColorPanel.setBounds(5, 345, 50, 25);
            newColorPanel.setBounds(5, 321, 50, 25);

            add(oldColorPanel);
            add(newColorPanel);


            // 当前颜色块
            JLabel oldLabel = new JLabel("当前");
            oldLabel.setBounds(15, 360, 40, 40);
            add(oldLabel);
        }

        /** 供外部更新“新增颜色”用（ColorChooserPanel 调 this.previewPanel.setNewColor） */
        public void setNewColor(Color c) {
            if (c == null || c.getAlpha() == 0) c = Color.BLUE;
            this.newColor = c;
            newColorPanel.setColor(c);
            repaint();
        }

        /** 如果你以后想在某处恢复“当前颜色”，可以调用这个 */
        public void setOldColor(Color c) {
            if (c == null) c = Color.WHITE;
            this.oldColor = c;
            oldColorPanel.setColor(c);
            repaint();
        }

        /** OK 按钮会用这个取最终颜色 */
        public Color getNewColor() {
            return newColor;
        }
    }

    // ==========================================================================
    // 标准色页（你的截图风格，完整、已调美观）
    // ==========================================================================

    private static class StandardPanel extends JPanel {

        private final ColorUpdateListener listener;

        StandardPanel(ColorUpdateListener listener, Color initial) {
            this.listener = listener;

            setLayout(null);                        // ★ 使用绝对布局
            setBackground(Color.WHITE);
            setPreferredSize(new Dimension(280, 340)); // 必须设置这个！
            JLabel color1 = new JLabel("颜色(C)");
            color1.setFont(new Font("微软雅黑", Font.PLAIN, 13));
            color1.setBounds(10, 5, 50, 20);
            add(color1);
            // ====== 颜色调色板 ======
            JPanel palette = createColorPalette();
            palette.setBounds(5, 20, 270, 270);  // 调整到合适位置
            add(palette);

            // ====== 最近使用 ======
            JPanel recent = createRecentColors();
            recent.setBounds(5, 290, 270, 50);
            add(recent);
        }

        private JPanel createColorPalette() {
            JPanel panel = new JPanel(new GridLayout(20, 20, 1, 1));
            panel.setBackground(Color.WHITE);

            Color[] colors = buildOfficeColors();
            for (Color c : colors) panel.add(createCell(c));

            return panel;
        }

        private JPanel createRecentColors() {
    
            // 1. 设置布局为 2 行 10 列，以显示 20 个单元格
            JPanel p = new JPanel(new GridLayout(2, 20, 1, 1)); 

            // 2. 移除边框线，但保留标题文字 "最近使用"
            Border emptyBorder = new EmptyBorder(5, 0, 0, 0); // 在顶部留出一些空间给标题
            TitledBorder titledBorder = BorderFactory.createTitledBorder(
                emptyBorder, 
                "最近使用"   
             );
            titledBorder.setTitleJustification(TitledBorder.LEFT); 
            p.setBorder(titledBorder); 
            
            p.setBackground(Color.WHITE);

            // 默认颜色数据（为了填充 20 个格子，这里需要更多的颜色，或者循环使用）
            Color[] defaultRecent = {
                new Color(255,200,0), new Color(0,200,80),
                new Color(0,140,255), new Color(150,50,255),
            };
            
            // 循环添加 20 个单元格
            for (int i = 0; i < 20; i++) {
                // 使用默认颜色，如果数组不够则填充白色
                Color color = (i < defaultRecent.length) ? defaultRecent[i] : Color.WHITE;
                p.add(createCell(color));
            }
            
            return p;
        }

        private JLabel createCell(Color c) {
            JLabel lab = new JLabel();
            lab.setOpaque(true);
            lab.setBackground(c);
            lab.setPreferredSize(new Dimension(16,16));
            lab.setBorder(new LineBorder(Color.GRAY,1));

            lab.addMouseListener(new MouseAdapter() {
                @Override public void mouseClicked(MouseEvent e){
                    listener.onColorUpdate(c);
                }
            });
            return lab;
        }

        private Color[] buildOfficeColors() {
    int size = 20;
    Color[] colors = new Color[size * size];

    float centerX = (size - 1) / 2f; // 9.5
    float centerY = (size - 1) / 2f; // 9.5
    
    // 关键修改：将最大半径设为到角落的距离，确保所有格子都参与着色计算
    // 从中心 (9.5, 9.5) 到 角落 (0, 0) 的距离：sqrt(9.5^2 + 9.5^2) ≈ 13.43
    float maxRadius = (float) Math.hypot(centerX, centerY); 

    int idx = 0;
    for (int y = 0; y < size; y++) {
        for (int x = 0; x < size; x++) {
            float dx = x - centerX;
            float dy = y - centerY;
            float distance = (float) Math.hypot(dx, dy);

            // 归一化距离：现在 norm 的最大值将接近 1.0
            float norm = distance / maxRadius;

            // 色相（保持不变）
            float hue = (float) (Math.toDegrees(Math.atan2(dy, dx)) + 360) % 360;

            float saturation;
            float brightness;

            // --- 核心渐变逻辑（采用窄白边风格） ---
            
            float blackToColorEnd = 0.4f;   // 黑色到纯色过渡结束点
            float colorToWhiteStart = 0.3f; // 纯色到白色过渡开始点 (设置为 0.90，保持最窄的白边)

            if (norm <= blackToColorEnd) {
                // 【内圈】从圆心的黑色 -> 纯色
                // 亮度 (V) 从 0.0 渐变到 1.0，饱和度 (S) 保持 1.0
                float t = norm / blackToColorEnd;
                saturation = 1f;
                brightness = t;
            } else if (norm > blackToColorEnd && norm <= colorToWhiteStart) {
                // 【中间】宽广的纯色区域
                saturation = 1f;
                brightness = 1f;
            } else { 
                // 【外圈】从纯色 -> 边缘的白色 (0.90 -> 1.0)
                // 饱和度 (S) 从 1.0 渐变到 0.0，亮度 (V) 保持 1.0
                float range = 1f - colorToWhiteStart;
                // 防止 range 为 0 导致除以 0
                if (range < 0.001f) range = 0.001f; 
                
                float t = (norm - colorToWhiteStart) / range;
                saturation = 1f - t;
                brightness = 1f;
            }

            // 边界检查：确保饱和度不会低于 0
            saturation = Math.max(0f, Math.min(1f, saturation));
            
            colors[idx++] = Color.getHSBColor(hue / 360f, saturation, brightness);
        }
    }
    return colors;
}
    }
        // ==========================================================================
    
    private static class RoundPanel extends JPanel {

        private int arc = 20;

        public RoundPanel() {
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // 画白色背景
            g2.setColor(Color.WHITE);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), arc, arc);

            // 画浅灰色边框
            g2.setColor(new Color(200, 200, 200));
            g2.drawRoundRect(0, 0, getWidth(), getHeight(), arc, arc);

            g2.dispose();
            super.paintComponent(g);
        }
    }
    // =========================
//   自定义圆角 ComboBox UI
// =========================
    static class RoundComboBoxUI extends javax.swing.plaf.basic.BasicComboBoxUI {

        @Override
        protected JButton createArrowButton() {
            JButton arrow = new JButton() {
                @Override
                public void paintComponent(Graphics g) {
                    super.paintComponent(g);
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                    // 画下拉小三角
                    int w = getWidth();
                    int h = getHeight();
                    int size = 8;

                    int x = w/2 - size/2;
                    int y = h/2 - size/4;

                    g2.setColor(new Color(120,120,120));
                    int[] xs = {x, x+size, x+size/2};
                    int[] ys = {y, y, y+size};
                    g2.fillPolygon(xs, ys, 3);

                    g2.dispose();
                }
            };

            arrow.setBorder(BorderFactory.createEmptyBorder());
            arrow.setContentAreaFilled(false);
            arrow.setOpaque(false);
            return arrow;
        }

        @Override
        public void paintCurrentValueBackground(Graphics g, Rectangle bounds, boolean hasFocus) {
            // 不画默认背景（否则就是灰色块）
        }
    }

    private static class CustomPanel extends JPanel {

    private final ColorUpdateListener listener;
    private Color currentColor;

    private JComboBox<String> modeBox;

    private JLabel label1, label2, label3, label4;
    private JSpinner sp1, sp2, sp3, sp4;
    private JTextField hexField;
    private JLabel hexLabel;
    private HSVWheel wheel;
    private HSVValueBar valueBar;
    private boolean updateLock = false;
    // 简单方案：只修改编辑器为圆角
    // 修正方案：创建圆角 Spinner 并保持数字左对齐
    // 修正方案：完全自定义圆角 Spinner
    private void updateFromInputs() {
        if (updateLock) return;
        updateLock = true;

        String mode = (String) modeBox.getSelectedItem();
        Color c;

        try {
            if (mode.equals("RGB")) {
                int r = (int) sp1.getValue();
                int g = (int) sp2.getValue();
                int b = (int) sp3.getValue();

                r = Math.max(0, Math.min(255, r));
                g = Math.max(0, Math.min(255, g));
                b = Math.max(0, Math.min(255, b));

                c = new Color(r, g, b);
            }
            else if (mode.equals("HSV")) {
                float h = (int) sp1.getValue();
                float s = (int) sp2.getValue() / 100f;
                float v = (int) sp3.getValue() / 100f;
                c = HSVtoRGB(h, s, v);
            }
            else if (mode.equals("HSL")) {
                float h = (int) sp1.getValue();
                float s = (int) sp2.getValue() / 100f;
                float l = (int) sp3.getValue() / 100f;
                c = HSLtoRGB(h, s, l);
            }
            else { // CMYK
                float C = (int) sp1.getValue() / 100f;
                float M = (int) sp2.getValue() / 100f;
                float Y = (int) sp3.getValue() / 100f;
                float K = (int) sp4.getValue() / 100f;
                c = CMYKtoRGB(C, M, Y, K);
            }

            currentColor = c;
            listener.onColorUpdate(c);

            float[] hsv = RGBtoHSV(c);
            wheel.setHS(hsv[0], hsv[1]);
            wheel.setBrightness(hsv[2]);
            valueBar.setHS(hsv[0], hsv[1]);
            valueBar.regenerate();
            valueBar.repaint();

            hexField.setText(String.format("%02X%02X%02X",
                    c.getRed(), c.getGreen(), c.getBlue()));

        } finally {
            updateLock = false;
        }
    }


    private JSpinner createRoundSpinner(SpinnerNumberModel model) {
        JSpinner spinner = new JSpinner(model) {
            @Override
            protected void paintComponent(Graphics g) {
                // 不调用父类的 paintComponent，完全自己绘制
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                // 绘制白色圆角背景
                g2.setColor(Color.WHITE);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 20, 20);
                
                // 绘制圆角边框
                g2.setColor(new Color(180, 180, 180));
                g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 20, 20);
                
                g2.dispose();
            }
            
            @Override
            public boolean isOpaque() {
                return false; // 让 spinner 透明，我们自己绘制背景
            }
        };
        
        // 移除 spinner 的默认边框
        spinner.setBorder(BorderFactory.createEmptyBorder());
        
        // 获取编辑器文本框
        JSpinner.DefaultEditor editor = (JSpinner.DefaultEditor) spinner.getEditor();
        JTextField textField = editor.getTextField();
        
        // 设置文本框
        textField.setHorizontalAlignment(JTextField.LEFT);
        textField.setFont(new Font("微软雅黑", Font.PLAIN, 13));
        textField.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8)); // 只设置内边距，不要边框

        textField.setOpaque(true); // 文本框透明
        textField.setBackground(Color.WHITE);
        // 自定义箭头按钮样式
        Component[] comps = spinner.getComponents();
        for (Component comp : comps) {
            if (comp instanceof JButton) {
                JButton button = (JButton) comp;
                button.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
                button.setContentAreaFilled(false);
                button.setOpaque(false);
                button.setFocusPainted(false);
            }
        }
        
        return spinner;
    }
    CustomPanel(ColorUpdateListener listener, Color initial) {
        this.listener = listener;
        this.currentColor = initial;

        // ------ 整个 Custom 区域 absolute layout ------
        setLayout(null);
        setPreferredSize(new Dimension(280, 340));
        setOpaque(false);                 // ★ 关键
        setBackground(Color.WHITE);      // ★ 关键
        // 标题：颜色(C)
        JLabel title = new JLabel("颜色(C)");
        title.setFont(new Font("微软雅黑", Font.PLAIN, 13));
        title.setBounds(10, 5, 50, 20);
        add(title);
         
        // 亮度条
        valueBar = new HSVValueBar(initial, this::updateValueBar);
        valueBar.setBounds(245, 35, 25, 100);
        add(valueBar);

        // 色盘
        wheel = new HSVWheel(initial, this::updateWheel,valueBar);
        wheel.setBounds(10, 30, 230, 110);
        add(wheel);
        valueBar.setBrightnessListener(() -> {
            float v = valueBar.getValue();    // brightness 0~1
            wheel.setBrightness(v);           // 改亮度
            wheel.setMarkerFromBrightness(v); // ★★ 让 marker 跟随亮度移动
        });

        // 颜色模式
        JLabel modeLabel = new JLabel("颜色模式(D)");
        modeLabel.setFont(new Font("微软雅黑", Font.PLAIN, 13));
        modeLabel.setBounds(10, 155, 80, 20);
        add(modeLabel);

        modeBox = new JComboBox<>(new String[]{"RGB", "HSV", "HSL", "CMYK"});
        modeBox.setBounds(85, 150, 180, 30);

        // 1) Renderer（最先设置）
        modeBox.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(
                    JList<?> list, Object value, int index,
                    boolean isSelected, boolean cellHasFocus) {

                JLabel lb = (JLabel) super.getListCellRendererComponent(
                        list, value, index, isSelected, cellHasFocus);

                lb.setFont(new Font("微软雅黑", Font.PLAIN, 13));

                if (isSelected) {
                    lb.setBackground(new Color(220, 235, 252)); // 微软淡蓝
                } else {
                    lb.setBackground(Color.WHITE);
                }
                lb.setOpaque(true);
                return lb;
            }
        });

        // 2) 自定义 UI
        modeBox.setUI(new RoundComboBoxUI());

        // 3) 圆角边框
        modeBox.setBorder(new RoundBorder(new Color(180, 180, 180), 1, 12));
        modeBox.setFont(new Font("微软雅黑", Font.PLAIN, 13));

        // ⚠⚠⚠ 关键：必须在最后强制背景为白色 ＆ 不透明
        modeBox.setOpaque(true);
        modeBox.setBackground(Color.WHITE);

        // 再 repaint 强刷一次
        SwingUtilities.invokeLater(modeBox::repaint);

        add(modeBox);


        
        modeBox.addActionListener(e -> {
            rebuildInputs();
            syncInputs(); // 重要：切换模式时同步当前颜色到输入控件
        });

        // ===== Labels =====
        label1 = new JLabel("红色(R)");
        label2 = new JLabel("绿色(G)");
        label3 = new JLabel("蓝色(B)");
        label4 = new JLabel("黑色(K)");
        label1.setFont(new Font("微软雅黑", Font.PLAIN, 13));
        label2.setFont(new Font("微软雅黑", Font.PLAIN, 13));
        label3.setFont(new Font("微软雅黑", Font.PLAIN, 13));
        label4.setFont(new Font("微软雅黑", Font.PLAIN, 13));
        label1.setBounds(10, 194, 80, 20);
        label2.setBounds(10, 233, 80, 20);
        label3.setBounds(10, 272, 80, 20);
        label4.setBounds(10, 311, 80, 20);
        add(label1);
        add(label2);
        add(label3);
        add(label4);
        sp1 = createRoundSpinner(new SpinnerNumberModel(0, 0, 255, 1));
        sp2 = createRoundSpinner(new SpinnerNumberModel(0, 0, 255, 1));
        sp3 = createRoundSpinner(new SpinnerNumberModel(0, 0, 255, 1));
        sp4 = createRoundSpinner(new SpinnerNumberModel(0, 0, 100, 1));
        
        sp1.setBorder(new RoundBorder(new Color(180,180,180), 1, 10));
        sp2.setBorder(new RoundBorder(new Color(180,180,180), 1, 10));
        sp3.setBorder(new RoundBorder(new Color(180,180,180), 1, 10));
        sp4.setBorder(new RoundBorder(new Color(180,180,180), 1, 10));
        
        ChangeListener spinnerListener = e -> updateFromInputs();

        sp1.addChangeListener(spinnerListener);
        sp2.addChangeListener(spinnerListener);
        sp3.addChangeListener(spinnerListener);
        sp4.addChangeListener(spinnerListener);
        sp1.setBounds(85, 189,100, 30);
        sp2.setBounds(85, 228, 100, 30);
        sp3.setBounds(85, 267, 100, 30);
        sp4.setBounds(85, 306, 100, 30);
        sp1.setFont(new Font("微软雅黑", Font.PLAIN, 13));
        sp2.setFont(new Font("微软雅黑", Font.PLAIN, 13));
        sp3.setFont(new Font("微软雅黑", Font.PLAIN, 13));
        sp4.setFont(new Font("微软雅黑", Font.PLAIN, 13));
        add(sp1);
        add(sp2);
        add(sp3);
        add(sp4);

        sp4.setVisible(false);

        // HEX
        hexLabel = new JLabel("HEX");
        hexLabel.setBounds(10, 310, 80, 20);
        hexLabel.setFont(new Font("微软雅黑", Font.PLAIN, 13));
        add(hexLabel);

        hexField = new JTextField("000000");
        hexField.setFont(new Font("微软雅黑", Font.PLAIN, 13));
        hexField.setBounds(85, 306, 180, 30);
        hexField.setBorder(new RoundBorder(new Color(180,180,180), 1, 8));
        add(hexField);

        rebuildInputs();
        SwingUtilities.invokeLater(() -> syncInputs());
    }

    private void updateWheel(Color c) {
        currentColor = c;
        listener.onColorUpdate(c);
        syncInputs();
    }

    private void updateValueBar(Color c) {
        currentColor = c;
        listener.onColorUpdate(c);
        syncInputs();
    }

    private void rebuildInputs() {
        String mode = (String) modeBox.getSelectedItem();

        if (mode.equals("RGB")) {
            label1.setText("红色(R)");
            label2.setText("绿色(G)");
            label3.setText("蓝色(B)");
            label4.setVisible(false);
            sp4.setVisible(false);
            hexLabel.setVisible(true);
            hexField.setVisible(true);

        } else if (mode.equals("HSV")) {
            label1.setText("色相(H)");
            label2.setText("饱和度(S)");
            label3.setText("明度(V)");
            label4.setVisible(false);
            sp4.setVisible(false);
            hexLabel.setVisible(true);
            hexField.setVisible(true);

        } else if (mode.equals("HSL")) {
            label1.setText("色相(H)");
            label2.setText("饱和度(S)");
            label3.setText("亮度(L)");
            label4.setVisible(false);
            sp4.setVisible(false);
            hexLabel.setVisible(true);
            hexField.setVisible(true);

        } else { // CMYK
            label1.setText("青(C)");
            label2.setText("品(M)");
            label3.setText("黄(Y)");
            label4.setText("黑(K)");
            label4.setVisible(true);
            sp4.setVisible(true);

            // 关键：恢复 HEX 显示状态
            hexLabel.setVisible(false);
            hexField.setVisible(false);
        }

        // ⚠ 必须强制刷新 JComboBox 的背景，否则会回到灰色！
        modeBox.repaint();
    }

    private void syncInputs() {
    if (updateLock) return;
    updateLock = true;

    try {
        float[] hsl = RGBtoHSL(currentColor);
        sp1.setValue((int) hsl[0]);
        sp2.setValue((int) (hsl[1] * 100));
        sp3.setValue((int) (hsl[2] * 100));

        hexField.setText(String.format("%02X%02X%02X",
                currentColor.getRed(),
                currentColor.getGreen(),
                currentColor.getBlue()));
    }
    finally {
        updateLock = false;
    }
}

}




    // ==========================================================================
    // HSV 色盘与亮度条 —— 最终版
    // ==========================================================================

    private static class HSVWheel extends JPanel {

        private static final int W = 230, H = 190;
        private BufferedImage image;

        private float hue, sat, val;
        private Color currentColor;
        private final ColorUpdateListener listener;
        private HSVValueBar valueBar;

        HSVWheel(Color initial, ColorUpdateListener listener, HSVValueBar valueBar) {
            this.listener = listener;
            this.valueBar = valueBar;
            float[] hsv = RGBtoHSV(initial);
            hue = hsv[0];
            sat = hsv[1];
            val = hsv[2];
            currentColor = initial;

            setPreferredSize(new Dimension(W, H));
            generateImage();

            addMouseListener(mouseHandler);
            addMouseMotionListener(mouseHandler);
        }

        private MouseAdapter mouseHandler = new MouseAdapter() {

            @Override
            public void mousePressed(MouseEvent e) { updateFromMouse(e.getX(), e.getY()); }

            @Override
            public void mouseDragged(MouseEvent e) { updateFromMouse(e.getX(), e.getY()); }
        };

        public void setBrightness(float v) {
            this.val = v;
            this.currentColor = HSVtoRGB(hue, sat, val);

            if (listener != null) {
                listener.onColorUpdate(currentColor);
            }

            repaint();
        }

        private void updateFromMouse(int x, int y) {
            x = Math.max(0, Math.min(W - 1, x));
            y = Math.max(0, Math.min(H - 1, y));

            hue = (x / (float)W) * 360;
            sat = 1 - (y / (float)H);

            currentColor = HSVtoRGB(hue, sat, val);
            listener.onColorUpdate(currentColor);

            repaint();
            valueBar.setHS(hue, sat);
        }
        public float getBrightness() {
            return val;
        }
        private void generateImage() {
            image = new BufferedImage(W, H, BufferedImage.TYPE_INT_RGB);
            for (int x=0;x<W;x++) {
                for (int y=0;y<H;y++) {
                    float h = x/(float)W * 360f;
                    float s = 1 - (y/(float)H);
                    Color c = HSVtoRGB(h,s,val);
                    image.setRGB(x, y, c.getRGB());
                }
            }
        }

        @Override
        protected void paintComponent(Graphics g) {
            g.drawImage(image, 0, 0, null);

            // ------- 绘制十字准星 (marker) -------
            int mx = (int)(hue / 360f * W);
            int my = (int)((1 - sat) * H);

            Graphics2D g2 = (Graphics2D) g.create();
            g2.setStroke(new BasicStroke(2f));
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // 黑色外圈
            g2.setColor(Color.BLACK);
            g2.drawOval(mx - 5, my - 5, 10, 10);

            // 白色内圈（确保亮色背景也能看见）
            g2.setColor(Color.WHITE);
            g2.drawOval(mx - 4, my - 4, 8, 8);

            g2.dispose();
        }
        public void setMarkerFromBrightness(float v) {
            // 将亮度转换为 sat，让 marker 上下移动
            this.sat = 1-v;     // 你可以改成 1 - v，看你想 marker 上升还是下降

            currentColor = HSVtoRGB(hue, sat, val);
            repaint();
        }
        public void setHS(float h, float s) {
            this.hue = h;
            this.sat = s;

            // 更新当前颜色（保持亮度不变）
            this.currentColor = HSVtoRGB(hue, sat, val);

            // 让监听器回调，触发 previewPanel 更新颜色
            if (listener != null) {
                listener.onColorUpdate(currentColor);
            }

            // 重新绘制面板
            repaint();
        }

    }



    private static class HSVValueBar extends JPanel {

        private static final int BAR_W = 10;   // 亮度条实际宽度
        private static final int H = 100;

        private BufferedImage img;

        private float hue, sat, val;
        private final ColorUpdateListener listener;
        private Runnable onBrightnessChanged;

        public void setBrightnessListener(Runnable r) {
            this.onBrightnessChanged = r;
        }
        HSVValueBar(Color initial, ColorUpdateListener listener) {
            this.listener = listener;

            float[] hsv = RGBtoHSV(initial);
            hue = hsv[0];
            sat = hsv[1];
            val = 0.5f;

            setOpaque(false);

            // ⚠ 组件宽度要预留 20px，亮度条依然是 10px！！
            setPreferredSize(new Dimension(BAR_W + 10, H));

            regen();

            addMouseListener(mh);
            addMouseMotionListener(mh);
        }

        private MouseAdapter mh = new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e) { update(e.getY()); }
            @Override public void mouseDragged(MouseEvent e) { update(e.getY()); }
        };

        private void update(int y) {
            y = Math.max(0, Math.min(H - 1, y));
            val = 1 - (y / (float)H);

            listener.onColorUpdate(HSVtoRGB(hue, sat, val));
            regenerate();
            if (onBrightnessChanged != null) {
                onBrightnessChanged.run();
            }
        }

        public void setHS(float h, float s) {
            hue = h;
            sat = s;
            regenerate();
            repaint();
        }
        public float getValue() {
            return val;
        }
        private void regenerate() {
            regen();
            repaint();
        }
        
         private Color mix(Color a, Color b, float t) {
            int r = (int)(a.getRed()   * (1 - t) + b.getRed()   * t);
            int g = (int)(a.getGreen() * (1 - t) + b.getGreen() * t);
            int bl = (int)(a.getBlue()  * (1 - t) + b.getBlue()  * t);
            return new Color(r, g, bl);
        }

         private void regen() {
            img = new BufferedImage(BAR_W, H, BufferedImage.TYPE_INT_RGB);

            Color pure = HSVtoRGB(hue, sat, 1f);  // sat=当前选中的饱和度

            for (int y = 0; y < H; y++) {

                float t = y / (float)H;
                Color c;

                if (t < 0.5f) {
                    // 上半段：白 → 纯色
                    float k = t / 0.5f;
                    c = mix(Color.WHITE, pure, k);
                } else {
                    // 下半段：纯色 → 黑
                    float k = (t - 0.5f) / 0.5f;
                    c = mix(pure, Color.BLACK, k);
                }

                for (int x = 0; x < BAR_W; x++)
                    img.setRGB(x, y, c.getRGB());
            }
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);

            // 亮度条绘制在左边 5px 位置
            g.drawImage(img, 5, 0, null);

            // 三角形 marker 的 Y 坐标
            int my = (int)((1 - val) * H);

            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            g2.setColor(Color.BLACK);

            // 三角形绘制在亮度条右侧！！！（完全在组件内部）
            int baseX = 5 + BAR_W + 3;  // 亮度条右边 + 3px

            int[] xs = { baseX, baseX + 9, baseX + 9 };
            int[] ys = { my, my - 6, my + 6 };

            g2.fillPolygon(xs, ys, 3);
            g2.dispose();
        }
    }

    // ==========================================================================
    // 颜色模型转换（完整）
    // ==========================================================================

    public static float[] RGBtoHSV(Color c) {
        float r = c.getRed()/255f;
        float g = c.getGreen()/255f;
        float b = c.getBlue()/255f;

        float max=Math.max(r,Math.max(g,b));
        float min=Math.min(r,Math.min(g,b));
        float d=max-min;

        float h=0;
        if(d!=0){
            if(max==r) h=60*((g-b)/d%6);
            else if(max==g) h=60*((b-r)/d+2);
            else h=60*((r-g)/d+4);
        }
        if(h<0) h+=360;
        float s=max==0?0:d/max;
        float v=max;

        return new float[]{h,s,v};
    }

    public static Color HSVtoRGB(float h,float s,float v){
        float C=v*s;
        float X=C*(1-Math.abs(h/60%2-1));
        float m=v-C;

        float r,g,b;
        if(h<60){ r=C; g=X; b=0; }
        else if(h<120){ r=X; g=C; b=0; }
        else if(h<180){ r=0; g=C; b=X; }
        else if(h<240){ r=0; g=X; b=C; }
        else if(h<300){ r=X; g=0; b=C; }
        else{ r=C; g=0; b=X; }

        return new Color((int)((r+m)*255),(int)((g+m)*255),(int)((b+m)*255));
    }

    public static float[] RGBtoHSL(Color c){
        float r=c.getRed()/255f;
        float g=c.getGreen()/255f;
        float b=c.getBlue()/255f;

        float max = Math.max(r,Math.max(g,b));
        float min = Math.min(r,Math.min(g,b));
        float l=(max+min)/2;
        float d=max-min;

        float h=0,s=0;
        if(d!=0){
            s=d/(1-Math.abs(2*l-1));
            if(max==r) h=60*((g-b)/d%6);
            else if(max==g) h=60*((b-r)/d+2);
            else h=60*((r-g)/d+4);
        }

        if(h<0)h+=360;
        return new float[]{h,s,l};
    }

    public static Color HSLtoRGB(float h,float s,float l){
        float C=(1-Math.abs(2*l-1))*s;
        float X=C*(1-Math.abs(h/60%2-1));
        float m=l-C/2;

        float r,g,b;
        if(h<60){ r=C; g=X; b=0; }
        else if(h<120){ r=X; g=C; b=0; }
        else if(h<180){ r=0; g=C; b=X; }
        else if(h<240){ r=0; g=X; b=C; }
        else if(h<300){ r=X; g=0; b=C; }
        else{ r=C; g=0; b=X; }

        return new Color((int)((r+m)*255),(int)((g+m)*255),(int)((b+m)*255));
    }

    public static float[] RGBtoCMYK(Color c){
        float r=c.getRed()/255f;
        float g=c.getGreen()/255f;
        float b=c.getBlue()/255f;

        float k=1-Math.max(r,Math.max(g,b));
        if(k==1)return new float[]{0,0,0,1};

        float C=(1-r-k)/(1-k);
        float M=(1-g-k)/(1-k);
        float Y=(1-b-k)/(1-k);

        return new float[]{C,M,Y,k};
    }

    public static Color CMYKtoRGB(float C,float M,float Y,float K){
        int r=(int)(255*(1-C)*(1-K));
        int g=(int)(255*(1-M)*(1-K));
        int b=(int)(255*(1-Y)*(1-K));
        return new Color(r,g,b);
    }
   
}
