import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;

public class SlidePreviewPanel extends JPanel {
    private final PresentationApp app;
    private final JList<SlidePage> previewList;
    private final DefaultListModel<SlidePage> listModel;

    public SlidePreviewPanel(PresentationApp app) {
        this.app = app;
        this.listModel = new DefaultListModel<>();
        this.previewList = new JList<>(listModel);

        setLayout(new BorderLayout());
        setPreferredSize(new Dimension(200, 0));
        setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, Color.LIGHT_GRAY));

        previewList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        previewList.setCellRenderer(new SlideThumbnailRenderer());
        previewList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                int index = previewList.getSelectedIndex();
                if (index != -1) {
                    app.jumpToPage(index);
                }
            }
        });

        JScrollPane scrollPane = new JScrollPane(previewList);
        scrollPane.setBorder(null);
        add(scrollPane, BorderLayout.CENTER);

        // 添加右键菜单
        previewList.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (SwingUtilities.isRightMouseButton(e)) {
                    int index = previewList.locationToIndex(e.getPoint());
                    if (index != -1) {
                        previewList.setSelectedIndex(index);
                    }
                    showPopupMenu(e.getPoint());
                }
            }
        });
    }

    private void showPopupMenu(Point p) {
        JPopupMenu popup = new JPopupMenu();
        JMenuItem addPageItem = new JMenuItem("添加新页面");
        addPageItem.addActionListener(e -> addNewPage());

        JMenuItem changeColorItem = new JMenuItem("改变主题颜色");
        changeColorItem.addActionListener(e -> changeThemeColor());

        JMenuItem changeBgImageItem = new JMenuItem("更改背景图片");
        changeBgImageItem.addActionListener(e -> changeBackgroundImage());

        popup.add(addPageItem);
        popup.add(changeColorItem);
        popup.add(changeBgImageItem);
        popup.show(previewList, p.x, p.y);
    }

    private void addNewPage() {
        SlidePage newPage = new SlidePage();
        app.getSlide().addPage(newPage);
        updateSlideList(app.getSlide().getAllPages());
        int newIndex = app.getSlide().getTotalPages() - 1;
        setSelectedPage(newIndex);
        app.jumpToPage(newIndex);
    }

    private void changeThemeColor() {
        int index = previewList.getSelectedIndex();
        if (index != -1) {
            SlidePage page = listModel.getElementAt(index);
            Color newColor = JColorChooser.showDialog(this, "选择主题颜色", page.getBackgroundColor());
            if (newColor != null) {
                page.setBackgroundColor(newColor);
                refreshPreviews();
                app.repaint();
            }
        }
    }

    private void changeBackgroundImage() {
        int index = previewList.getSelectedIndex();
        if (index != -1) {
            SlidePage page = listModel.getElementAt(index);
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setDialogTitle("选择背景图片");
            fileChooser.setFileFilter(new FileNameExtensionFilter("图片文件 (*.png, *.jpg, *.jpeg)", "png", "jpg", "jpeg"));
            if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                File file = fileChooser.getSelectedFile();
                try {
                    BufferedImage image = ImageIO.read(file);
                    if (image != null) {
                        page.setBackgroundImage(image);
                        // 当通过预览面板直接更改背景图片时，自动切换为拉伸模式以立即可见
                        page.setBackgroundMode(SlidePage.BackgroundMode.IMAGE_STRETCH);
                        refreshPreviews();
                        app.repaint();
                    } else {
                        JOptionPane.showMessageDialog(this, "无法加载图片。", "错误", JOptionPane.ERROR_MESSAGE);
                    }
                } catch (IOException ex) {
                    ex.printStackTrace();
                    JOptionPane.showMessageDialog(this, "加载图片失败: " + ex.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
                }
            }
        }
    }

    public void updateSlideList(java.util.List<SlidePage> pages) {
        listModel.clear();
        for (SlidePage page : pages) {
            listModel.addElement(page);
        }
    }

    public void setSelectedPage(int index) {
        previewList.setSelectedIndex(index);
        previewList.ensureIndexIsVisible(index);
    }

    public void refreshPreviews() {
        previewList.repaint();
    }

    private class SlideThumbnailRenderer extends JPanel implements ListCellRenderer<SlidePage> {
        private final JLabel indexLabel;
        private final ImagePanel imagePanel;

        public SlideThumbnailRenderer() {
            setLayout(new BorderLayout(5, 5));
            setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
            setOpaque(true);

            indexLabel = new JLabel();
            indexLabel.setVerticalAlignment(SwingConstants.TOP);

            imagePanel = new ImagePanel();
            imagePanel.setPreferredSize(new Dimension(160, 90)); // 1280x720 的比例缩小
            imagePanel.setBorder(BorderFactory.createLineBorder(Color.GRAY));

            add(indexLabel, BorderLayout.WEST);
            add(imagePanel, BorderLayout.CENTER);
        }

        @Override
        public Component getListCellRendererComponent(JList<? extends SlidePage> list, SlidePage value, int index,
                boolean isSelected, boolean cellHasFocus) {
            indexLabel.setText(String.valueOf(index + 1));
            imagePanel.setPage(value);

            // Update preferred size based on current slide dimensions
            int slideWidth = app.getSlide().getWidth();
            int slideHeight = app.getSlide().getHeight();
            int thumbWidth = 160;
            int thumbHeight = (int) ((double) slideHeight / slideWidth * thumbWidth);
            imagePanel.setPreferredSize(new Dimension(thumbWidth, thumbHeight));

            if (isSelected) {
                setBackground(new Color(230, 240, 255));
                imagePanel.setBorder(BorderFactory.createLineBorder(new Color(50, 100, 200), 2));
            } else {
                setBackground(list.getBackground());
                imagePanel.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));
            }

            return this;
        }
    }

    private class ImagePanel extends JPanel {
        private SlidePage page;

        public void setPage(SlidePage page) {
            this.page = page;
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (page != null) {
                BufferedImage thumbnail = renderPageToThumbnail(page, getWidth(), getHeight());
                g.drawImage(thumbnail, 0, 0, null);
            }
        }

        private BufferedImage renderPageToThumbnail(SlidePage page, int width, int height) {
            BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2d = image.createGraphics();

            // 开启抗锯齿
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            // 填充背景（支持纯色/渐变/图片）
            page.renderBackground(g2d, width, height);

            // 计算缩放比例
            int slideWidth = app.getSlide().getWidth();
            int slideHeight = app.getSlide().getHeight();

            double scaleX = (double) width / slideWidth;
            double scaleY = (double) height / slideHeight;
            double scale = Math.min(scaleX, scaleY);

            g2d.scale(scale, scale);

            for (SlideElement element : page.getElements()) {
                element.draw(g2d);
            }

            g2d.dispose();
            return image;
        }
    }
}
