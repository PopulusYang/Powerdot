
// 文件名: SlideshowPlayer.java
// 功能: 实现幻灯片放映功能，包括页面切换和动画效果
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;


// 该播放器以JDialog为基础，实现全屏幻灯片放映功能
public class SlideshowPlayer extends JDialog {
    // 底层幻灯片数据
    private final Slide slide;
    private int currentIndex;
    private final PlayerPanel playerPanel;

    private Timer animationTimer;// 动画计时器

    public enum Transition {
        FADE, SLIDE, ZOOM, NONE
    } // 动画枚举

    private final Transition transitionEffect;// 当前动画效果
    private boolean isAnimating = false; // 是否正在动画中

    private BufferedImage previousPageImage; // 上一页图像
    private BufferedImage currentPageImage;// 当前页图像
    private long animationStartTime; // 动画开始时间
    private final int ANIMATION_DURATION = 500; // 动画持续时间（毫秒）

    private enum Direction {
        FORWARD, BACKWARD
    } // 动画方向枚举

    private Direction animationDirection;// 当前动画方向

    public SlideshowPlayer(JFrame owner, Slide slide, int startIndex, Transition transition) {
        super(owner, "幻灯片放映", true);
        this.slide = slide;
        this.currentIndex = startIndex;
        this.transitionEffect = transition;

        setUndecorated(true);
        setResizable(false);

        playerPanel = new PlayerPanel();
        setContentPane(playerPanel);

        setupInputHandling();

        // 改为手动全屏，避免 GraphicsDevice.setFullScreenWindow 可能导致的 NPE
        setSize(Toolkit.getDefaultToolkit().getScreenSize());
        setLocation(0, 0);
    }

    // 设置输入处理，包括鼠标点击和键盘按键
    private void setupInputHandling() {
        // 匿名派生类处理鼠标点击事件
        playerPanel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (SwingUtilities.isLeftMouseButton(e)) {
                    nextPage();// 点击鼠标左键切换到下一页
                }
            }
        });

        int mapCondition = JComponent.WHEN_IN_FOCUSED_WINDOW;// 输入映射条件，全局映射
        var inputMap = playerPanel.getInputMap(mapCondition);
        var actionMap = playerPanel.getActionMap();

        // 定义键盘快捷键
        // 下一页
        KeyStroke[] nextKeys = {
                KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, 0),
                KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0),
                KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0),
                KeyStroke.getKeyStroke(KeyEvent.VK_PAGE_DOWN, 0)
        };
        // 上一页
        KeyStroke[] prevKeys = {
                KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, 0),
                KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0),
                KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SPACE, 0),
                KeyStroke.getKeyStroke(KeyEvent.VK_PAGE_UP, 0)
        };
        // 退出放映
        KeyStroke exitKey = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0);

        actionMap.put("nextPage", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                nextPage();
            }
        });
        actionMap.put("prevPage", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                previousPage();
            }
        });
        actionMap.put("exit", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                exitSlideshow();
            }
        });

        for (KeyStroke key : nextKeys) {
            inputMap.put(key, "nextPage");
        }
        for (KeyStroke key : prevKeys) {
            inputMap.put(key, "prevPage");
        }
        inputMap.put(exitKey, "exit");
    }

    private void nextPage() {
        if (isAnimating || currentIndex >= slide.getAllPages().size() - 1) {
            if (!isAnimating)
                exitSlideshow();
            return;
        }
        startAnimation(currentIndex + 1, Direction.FORWARD);
    }

    private void previousPage() {
        if (isAnimating || currentIndex <= 0) {
            return;
        }
        startAnimation(currentIndex - 1, Direction.BACKWARD);
    }

    private void startAnimation(int nextIndex, Direction direction) {
        if (isAnimating)
            return;

        if (transitionEffect == Transition.NONE) {
            currentIndex = nextIndex;
            repaint();
            return;
        }

        isAnimating = true;
        this.animationDirection = direction;

        previousPageImage = renderPageToImage(slide.getAllPages().get(currentIndex));
        currentPageImage = renderPageToImage(slide.getAllPages().get(nextIndex));
        currentIndex = nextIndex;

        animationStartTime = System.currentTimeMillis();
        animationTimer = new Timer(16, e -> {
            long elapsedTime = System.currentTimeMillis() - animationStartTime;
            if (elapsedTime >= ANIMATION_DURATION) {
                stopAnimation();
            } else {
                playerPanel.repaint();
            }
        });
        animationTimer.start();
    }

    private void stopAnimation() {
        if (animationTimer != null) {
            animationTimer.stop();
        }
        isAnimating = false;
        previousPageImage = null;
        playerPanel.repaint();
    }

    private BufferedImage renderPageToImage(SlidePage page) {
        final int designWidth = slide.getWidth();
        final int designHeight = slide.getHeight();
        BufferedImage image = new BufferedImage(designWidth, designHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = image.createGraphics();

        // 绘制背景颜色
        g2d.setColor(page.getBackgroundColor());
        g2d.fillRect(0, 0, designWidth, designHeight);

        // 绘制背景图片
        if (page.getBackgroundImage() != null) {
            g2d.drawImage(page.getBackgroundImage(), 0, 0, designWidth, designHeight, null);
        }

        for (SlideElement element : page.getElements()) {
            element.draw(g2d);
        }
        g2d.dispose();
        return image;
    }

    private void exitSlideshow() {
        stopAnimation();
        dispose();
    }

    private class PlayerPanel extends JPanel {
        public PlayerPanel() {
            setBackground(Color.BLACK);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (slide == null || slide.getAllPages().isEmpty()) {
                return;
            }

            Graphics2D g2d = (Graphics2D) g.create();

            if (isAnimating) {
                long elapsedTime = System.currentTimeMillis() - animationStartTime;
                float progress = Math.min(1.0f, (float) elapsedTime / ANIMATION_DURATION);

                switch (transitionEffect) {
                    case FADE:
                        drawFade(g2d, progress);
                        break;
                    case SLIDE:
                        drawSlide(g2d, progress);
                        break;
                    case ZOOM:
                        drawZoom(g2d, progress);
                        break;
                    case NONE:
                        break;
                }
            } else {
                SlidePage currentPage = slide.getAllPages().get(currentIndex);
                BufferedImage imageToDraw = renderPageToImage(currentPage);
                drawScaledAndCentered(g2d, imageToDraw);
            }
            g2d.dispose();
        }

        private void drawScaledAndCentered(Graphics2D g2d, BufferedImage image) {
            final double designWidth = image.getWidth();
            final double designHeight = image.getHeight();
            double scale = Math.min(getWidth() / designWidth, getHeight() / designHeight);
            int scaledWidth = (int) (designWidth * scale);
            int scaledHeight = (int) (designHeight * scale);
            int xOffset = (getWidth() - scaledWidth) / 2;
            int yOffset = (getHeight() - scaledHeight) / 2;
            g2d.drawImage(image, xOffset, yOffset, scaledWidth, scaledHeight, null);
        }

        private void drawFade(Graphics2D g2d, float progress) {
            if (animationDirection == Direction.FORWARD) {
                g2d.setComposite(AlphaComposite.SrcOver.derive(1.0f - progress));
                drawScaledAndCentered(g2d, previousPageImage);
                g2d.setComposite(AlphaComposite.SrcOver.derive(progress));
                drawScaledAndCentered(g2d, currentPageImage);
            } else {
                drawScaledAndCentered(g2d, currentPageImage);
                g2d.setComposite(AlphaComposite.SrcOver.derive(1.0f - progress));
                drawScaledAndCentered(g2d, previousPageImage);
            }
        }

        private void drawSlide(Graphics2D g2d, float progress) {
            Rectangle bounds = getScaledBounds(previousPageImage);

            if (animationDirection == Direction.FORWARD) {
                int oldPageOffset = (int) (-bounds.width * progress);
                g2d.drawImage(previousPageImage, bounds.x + oldPageOffset, bounds.y, bounds.width, bounds.height, null);
                int newPageOffset = (int) (bounds.width * (1.0f - progress));
                g2d.drawImage(currentPageImage, bounds.x + newPageOffset, bounds.y, bounds.width, bounds.height, null);
            } else {
                int oldPageOffset = (int) (bounds.width * progress);
                g2d.drawImage(previousPageImage, bounds.x + oldPageOffset, bounds.y, bounds.width, bounds.height, null);
                int newPageOffset = (int) (-bounds.width * (1.0f - progress));
                g2d.drawImage(currentPageImage, bounds.x + newPageOffset, bounds.y, bounds.width, bounds.height, null);
            }
        }

        private void drawZoom(Graphics2D g2d, float progress) {
            if (animationDirection == Direction.FORWARD) {
                drawScaledAndCentered(g2d, previousPageImage);
                Rectangle bounds = getScaledBounds(currentPageImage);
                float scale = 0.5f + (0.5f * progress);
                int newWidth = (int) (bounds.width * scale);
                int newHeight = (int) (bounds.height * scale);
                int newX = bounds.x + (bounds.width - newWidth) / 2;
                int newY = bounds.y + (bounds.height - newHeight) / 2;
                g2d.setComposite(AlphaComposite.SrcOver.derive(progress));
                g2d.drawImage(currentPageImage, newX, newY, newWidth, newHeight, null);
            } else {
                drawScaledAndCentered(g2d, currentPageImage);
                Rectangle bounds = getScaledBounds(previousPageImage);
                float scale = 1.0f - (0.5f * progress);
                int newWidth = (int) (bounds.width * scale);
                int newHeight = (int) (bounds.height * scale);
                int newX = bounds.x + (bounds.width - newWidth) / 2;
                int newY = bounds.y + (bounds.height - newHeight) / 2;
                g2d.setComposite(AlphaComposite.SrcOver.derive(1.0f - progress));
                g2d.drawImage(previousPageImage, newX, newY, newWidth, newHeight, null);
            }
        }

        private Rectangle getScaledBounds(BufferedImage image) {
            final double designWidth = image.getWidth();
            final double designHeight = image.getHeight();
            double scale = Math.min(getWidth() / designWidth, getHeight() / designHeight);
            int scaledWidth = (int) (designWidth * scale);
            int scaledHeight = (int) (designHeight * scale);
            int xOffset = (getWidth() - scaledWidth) / 2;
            int yOffset = (getHeight() - scaledHeight) / 2;
            return new Rectangle(xOffset, yOffset, scaledWidth, scaledHeight);
        }
    }
}