import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.List;

public class SlideshowPlayer extends JDialog {
    private final Slide slide;
    private int currentIndex;
    private final PlayerPanel playerPanel;

    private Timer animationTimer;
    public enum Transition { FADE, SLIDE, ZOOM }
    private final Transition transitionEffect;
    private boolean isAnimating = false;

    private BufferedImage previousPageImage;
    private BufferedImage currentPageImage;
    private long animationStartTime;
    private final int ANIMATION_DURATION = 500;

    private enum Direction { FORWARD, BACKWARD }
    private Direction animationDirection;

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

        GraphicsDevice gd = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
        if (gd.isFullScreenSupported()) {
            gd.setFullScreenWindow(this);
        } else {
            setSize(owner.getToolkit().getScreenSize());
            setLocation(0, 0);
        }
    }

    private void setupInputHandling() {
        playerPanel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (SwingUtilities.isLeftMouseButton(e)) {
                    nextPage();
                }
            }
        });

        int mapCondition = JComponent.WHEN_IN_FOCUSED_WINDOW;
        var inputMap = playerPanel.getInputMap(mapCondition);
        var actionMap = playerPanel.getActionMap();

        KeyStroke[] nextKeys = { KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, 0), KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0), KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0), KeyStroke.getKeyStroke(KeyEvent.VK_PAGE_DOWN, 0) };
        KeyStroke[] prevKeys = { KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, 0), KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0), KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SPACE, 0), KeyStroke.getKeyStroke(KeyEvent.VK_PAGE_UP, 0) };
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
            if (!isAnimating) exitSlideshow();
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
        if (isAnimating) return;

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
        final int designWidth = 1200;
        final int designHeight = 800;
        BufferedImage image = new BufferedImage(designWidth, designHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = image.createGraphics();

        g2d.setColor(Color.WHITE);
        g2d.fillRect(0, 0, designWidth, designHeight);

        for (SlideElement element : page.getElements()) {
            element.draw(g2d);
        }
        g2d.dispose();
        return image;
    }

    private void exitSlideshow() {
        stopAnimation();
        GraphicsDevice gd = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
        try {
            if (gd.getFullScreenWindow() == this) {
                gd.setFullScreenWindow(null);
            }
        } catch (Exception e) {
            System.err.println("Error while exiting fullscreen mode: " + e.getMessage());
        } finally {
            dispose();
        }
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