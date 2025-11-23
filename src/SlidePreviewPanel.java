import javax.swing.*;
import java.awt.*;
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
            imagePanel.setPreferredSize(new Dimension(160, 106)); // 1200x800 的比例缩小
            imagePanel.setBorder(BorderFactory.createLineBorder(Color.GRAY));

            add(indexLabel, BorderLayout.WEST);
            add(imagePanel, BorderLayout.CENTER);
        }

        @Override
        public Component getListCellRendererComponent(JList<? extends SlidePage> list, SlidePage value, int index,
                boolean isSelected, boolean cellHasFocus) {
            indexLabel.setText(String.valueOf(index + 1));
            imagePanel.setPage(value);

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

    private static class ImagePanel extends JPanel {
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

            // 填充白色背景
            g2d.setColor(Color.WHITE);
            g2d.fillRect(0, 0, width, height);

            // 计算缩放比例
            double scaleX = (double) width / 1200;
            double scaleY = (double) height / 800;
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
