
// SlideEditorPanel类
// 功能：幻灯片编辑面板，处理元素的绘制和交互
// 编辑ppt的核心
import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

public class SlideEditorPanel extends JPanel {
    private SlidePage currentPage;// 当前编辑的幻灯片页面
    private Slide currentSlide; // 当前幻灯片对象，用于获取尺寸
    private SlideElement selectedElement = null;// 当前选中的幻灯片元素
    private Point lastMousePoint;// 上一次鼠标位置

    private enum State {
        IDLE, MOVING, RESIZING, PANNING, ROTATING
    } // 三种编辑状态

    private State currentState = State.IDLE; // 状态标志位
    // 此处handle应该是指调整大小的控制点
    private static final int HANDLE_SIZE = 8;// 控制点位边长为4的正方形
    private static final int ROTATION_HANDLE_OFFSET = 30;
    private static final int ROTATION_HANDLE_SIZE = 8;

    private Rectangle[] resizeHandles = new Rectangle[8];// 八个控制点，八个正方形
    private int activeHandle = -1;// 当前活动的控制点索引
    private double scaleFactor = 1.0; // 缩放比例
    private int translateX = 0; // X轴平移量
    private int translateY = 0; // Y轴平移量
    private boolean isSpacePressed = false; // Space键状态

    private JTextArea activeTextEditor = null;
    private TextElement editingElement = null;

    public SlideEditorPanel(Slide slide)// 构造函数
    {
        this.currentSlide = slide;
        this.currentPage = slide.getCurrentPage();// 设置当前页面
        InteractionHandler handler = new InteractionHandler(this);// 创建交互处理器
        // 注册鼠标事件监听器
        addMouseListener(handler);
        addMouseMotionListener(handler);
        addMouseWheelListener(handler); // 添加滚轮监听器支持缩放

        // 添加键盘监听器支持Space键平移
        setFocusable(true);
        addKeyListener(new java.awt.event.KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_SPACE) {
                    isSpacePressed = true;
                    setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
                } else if (e.getKeyCode() == KeyEvent.VK_DELETE) {
                    deleteSelectedElement();
                }
            }

            @Override
            public void keyReleased(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_SPACE) {
                    isSpacePressed = false;
                    if (currentState != State.PANNING) {
                        setCursor(Cursor.getDefaultCursor());
                    }
                }
            }
        });

        // 初始化控制点矩形
        for (int i = 0; i < 8; i++) {
            resizeHandles[i] = new Rectangle();
        }
        setLayout(null);
    }

    public void setScaleFactor(double scale) {
        stopEditingText();
        this.scaleFactor = scale;
        repaint();
    }

    public double getScaleFactor() {
        return scaleFactor;
    }

    public void setTranslate(int x, int y) {
        stopEditingText();
        this.translateX = x;
        this.translateY = y;
        repaint();
    }

    public Point getTranslate() {
        return new Point(translateX, translateY);
    }

    // 将屏幕坐标转换为逻辑坐标
    public Point toLogical(Point p) {
        return new Point((int) ((p.x - translateX) / scaleFactor), (int) ((p.y - translateY) / scaleFactor));
    }

    // 获取适应缩放的控制点矩形
    public Rectangle getHandleForPoint(Point p) {
        int currentHandleSize = (int) (HANDLE_SIZE / scaleFactor);
        int halfHandle = currentHandleSize / 2;
        return new Rectangle(p.x - halfHandle, p.y - halfHandle, currentHandleSize, currentHandleSize);
    }

    public SlidePage getCurrentPage() {
        return currentPage;
    }

    public SlideElement getSelectedElement() {
        return selectedElement;
    }

    // 设置当前编辑的幻灯片页面
    public void setSlidePage(SlidePage newPage) {
        this.currentPage = newPage;
        this.selectedElement = null;
        this.currentState = State.IDLE;
        repaint();
    }

    public void setSlide(Slide slide) {
        this.currentSlide = slide;
        this.currentPage = slide.getCurrentPage();
        this.selectedElement = null;
        this.currentState = State.IDLE;
        repaint();
    }

    // 重写paintComponent方法进行绘制
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);// 调用父类方法清除背景

        Graphics2D g2d = (Graphics2D) g;
        // 保存当前的变换
        java.awt.geom.AffineTransform originalTransform = g2d.getTransform();

        g2d.translate(translateX, translateY);
        g2d.scale(scaleFactor, scaleFactor);

        // 绘制幻灯片背景
        if (currentPage != null) {
            g.setColor(currentPage.getBackgroundColor());
            g.fillRect(0, 0, currentSlide.getWidth(), currentSlide.getHeight());
            if (currentPage.getBackgroundImage() != null) {
                g.drawImage(currentPage.getBackgroundImage(), 0, 0, currentSlide.getWidth(), currentSlide.getHeight(),
                        null);
            }
        }

        // 绘制幻灯片边界
        g.setColor(Color.BLACK);
        g.drawRect(0, 0, currentSlide.getWidth(), currentSlide.getHeight());

        if (currentPage != null) {
            // 绘制该页所有元素
            for (SlideElement element : currentPage.getElements()) {
                if (element == editingElement)
                    continue;
                element.draw(g);
            }
            // 绘制选中元素的边框和控制点
            if (selectedElement != null) {
                java.awt.geom.AffineTransform originalSelectionTransform = g2d.getTransform();

                if (selectedElement.getRotation() != 0) {
                    Rectangle bounds = selectedElement.getBounds();
                    Point center;
                    if (selectedElement instanceof LineElement) {
                        LineElement line = (LineElement) selectedElement;
                        Point start = line.getStartPoint();
                        Point end = line.getEndPoint();
                        center = new Point((start.x + end.x) / 2, (start.y + end.y) / 2);
                    } else {
                        center = new Point(bounds.x + bounds.width / 2, bounds.y + bounds.height / 2);
                    }
                    g2d.rotate(Math.toRadians(selectedElement.getRotation()), center.x, center.y);
                }

                // 对于直线元素，绘制起点和终点的控制点
                if (selectedElement instanceof LineElement) {
                    LineElement line = (LineElement) selectedElement;
                    Rectangle startHandle = getHandleForPoint(line.getStartPoint());
                    Rectangle endHandle = getHandleForPoint(line.getEndPoint());
                    g2d.setColor(Color.ORANGE);// 端点颜色
                    // 绘制控制点
                    g2d.fillRect(startHandle.x, startHandle.y, startHandle.width, startHandle.height);
                    g2d.fillRect(endHandle.x, endHandle.y, endHandle.width, endHandle.height);

                    g2d.setColor(Color.BLACK);// 控制点边框颜色
                    g2d.drawRect(startHandle.x, startHandle.y, startHandle.width, startHandle.height);
                    g2d.drawRect(endHandle.x, endHandle.y, endHandle.width, endHandle.height);
                } else // 其他元素，绘制边框和八个控制点
                {
                    Rectangle bounds = selectedElement.getBounds();// 获取元素边界，四边形控制边界
                    g2d.setColor(Color.BLUE);
                    // 线宽为1，末端方形截断，线条交会处切角连接，miterlimit不生效，虚线段9像素，间隔9像素，偏移0
                    g2d.setStroke(new BasicStroke(4f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 10f,
                            new float[] { 9f, 9f }, 0f));
                    // 绘制边框
                    g2d.drawRect(bounds.x - 1, bounds.y - 1, bounds.width + 2, bounds.height + 2);

                    // 更新控制点位置
                    updateResizeHandlesForRect();

                    // 绘制控制点
                    g2d.setColor(Color.WHITE);
                    g2d.setStroke(new BasicStroke(1));
                    for (Rectangle handle : resizeHandles) {
                        g2d.fillRect(handle.x, handle.y, handle.width, handle.height);
                        g2d.setColor(Color.BLACK);// 控制点边框（黑色）
                        g2d.drawRect(handle.x, handle.y, handle.width, handle.height);
                        g2d.setColor(Color.WHITE);
                    }
                }

                // Draw rotation handle
                Point handleCenter = getRotationHandleCenter(selectedElement);
                Rectangle handleBounds = getRotationHandleBounds(selectedElement);

                // Calculate connector point (top center of the element)
                Rectangle bounds = selectedElement.getBounds();
                Point center = new Point(bounds.x + bounds.width / 2, bounds.y + bounds.height / 2);
                if (selectedElement instanceof LineElement) {
                    LineElement line = (LineElement) selectedElement;
                    Point start = line.getStartPoint();
                    Point end = line.getEndPoint();
                    center = new Point((start.x + end.x) / 2, (start.y + end.y) / 2);
                }

                Point topCenter = new Point(center.x, bounds.y);

                g2d.setColor(Color.BLACK);
                g2d.setStroke(new BasicStroke(1));
                g2d.drawLine(topCenter.x, topCenter.y, handleCenter.x, handleCenter.y);

                g2d.setColor(Color.GREEN);
                g2d.fillOval(handleBounds.x, handleBounds.y, handleBounds.width, handleBounds.height);
                g2d.setColor(Color.BLACK);
                g2d.drawOval(handleBounds.x, handleBounds.y, handleBounds.width, handleBounds.height);

                g2d.setTransform(originalSelectionTransform);
            }
        }
        // 恢复变换，以免影响其他可能的绘制（虽然这里是最后一步）
        g2d.setTransform(originalTransform);
    }

    // 更新矩形元素的八个控制点位置
    private void updateResizeHandlesForRect() {
        if (selectedElement == null)
            return;
        Rectangle bounds = selectedElement.getBounds();// 获取元素边界
        int currentHandleSize = (int) (HANDLE_SIZE / scaleFactor);
        int halfHandle = currentHandleSize / 2;

        // 设置八个控制点的位置
        resizeHandles[0].setBounds(bounds.x - halfHandle, bounds.y - halfHandle, currentHandleSize, currentHandleSize);
        resizeHandles[1].setBounds(bounds.x + bounds.width / 2 - halfHandle, bounds.y - halfHandle, currentHandleSize,
                currentHandleSize);
        resizeHandles[2].setBounds(bounds.x + bounds.width - halfHandle, bounds.y - halfHandle, currentHandleSize,
                currentHandleSize);
        resizeHandles[3].setBounds(bounds.x + bounds.width - halfHandle, bounds.y + bounds.height / 2 - halfHandle,
                currentHandleSize, currentHandleSize);
        resizeHandles[4].setBounds(bounds.x + bounds.width - halfHandle, bounds.y + bounds.height - halfHandle,
                currentHandleSize, currentHandleSize);
        resizeHandles[5].setBounds(bounds.x + bounds.width / 2 - halfHandle, bounds.y + bounds.height - halfHandle,
                currentHandleSize, currentHandleSize);
        resizeHandles[6].setBounds(bounds.x - halfHandle, bounds.y + bounds.height - halfHandle, currentHandleSize,
                currentHandleSize);
        resizeHandles[7].setBounds(bounds.x - halfHandle, bounds.y + bounds.height / 2 - halfHandle, currentHandleSize,
                currentHandleSize);
    }

    /**
     * 自动缩放并居中显示幻灯片
     */
    public void zoomToFit() {
        stopEditingText();
        int panelWidth = getWidth();
        int panelHeight = getHeight();

        if (panelWidth == 0 || panelHeight == 0)
            return;

        final int designWidth = currentSlide.getWidth();
        final int designHeight = currentSlide.getHeight();
        final int margin = 40; // 边距

        double scaleX = (double) (panelWidth - 2 * margin) / designWidth;
        double scaleY = (double) (panelHeight - 2 * margin) / designHeight;

        // 选择较小的缩放比例以适应屏幕
        this.scaleFactor = Math.min(scaleX, scaleY);

        // 限制最小缩放比例，防止过小
        if (this.scaleFactor < 0.1)
            this.scaleFactor = 0.1;

        // 计算居中位置
        // 目标是让 (designWidth * scale, designHeight * scale) 在 panel 中居中
        // translateX = (panelWidth - designWidth * scale) / 2
        this.translateX = (int) ((panelWidth - designWidth * scaleFactor) / 2);
        this.translateY = (int) ((panelHeight - designHeight * scaleFactor) / 2);

        repaint();
    }

    // 内部类：处理鼠标交互
    private class InteractionHandler extends MouseAdapter {
        private final SlideEditorPanel panel;
        /**** 存储原始元素内容与状态 ****/
        private Rectangle originalRectBounds;// 用于存储调整大小前的边界
        // 记录直线调整前的起点和终点
        private Point originalLineStart;
        private Point originalLineEnd;
        private double originalRotation;

        public InteractionHandler(SlideEditorPanel panel) {
            this.panel = panel;// 保存对编辑面板的引用
        }

        // 鼠标按下事件处理
        @Override
        public void mousePressed(MouseEvent e) {
            if (e.isPopupTrigger()) {
                handlePopup(e);
                return;
            }

            // 检查是否是中键点击或者按住Space键，如果是则进入平移模式
            // 注意：BUTTON1_DOWN_MASK 是左键
            if (SwingUtilities.isMiddleMouseButton(e)
                    || (SwingUtilities.isLeftMouseButton(e) && panel.isSpacePressed)) {
                panel.currentState = State.PANNING;
                panel.lastMousePoint = e.getPoint(); // 记录屏幕坐标
                panel.setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
                return;
            }

            Point logicalPoint = panel.toLogical(e.getPoint());
            Point localPoint = getLocalPoint(logicalPoint);
            panel.lastMousePoint = logicalPoint;// 获取鼠标位置

            // Check rotation handle
            if (panel.selectedElement != null) {
                Rectangle rotationHandle = panel.getRotationHandleBounds(panel.selectedElement);
                if (rotationHandle.contains(localPoint)) {
                    panel.currentState = State.ROTATING;
                    originalRotation = panel.selectedElement.getRotation();
                    return;
                }
            }

            SlideElement elementUnderMouse = findElementAt(logicalPoint);// 获取当前鼠标位置下的元素
            /* 选中 */
            if (panel.selectedElement != null) // 确保当前有选中元素
            {
                boolean clickedOnHandle = false; // 标志位，表示是否点击在控制点上
                // 两种可能，点在控制点上和点到元素但没点在控制点上
                // 直线处理
                if (panel.selectedElement instanceof LineElement) {
                    LineElement line = (LineElement) panel.selectedElement;
                    Rectangle startHandle = panel.getHandleForPoint(line.getStartPoint());
                    Rectangle endHandle = panel.getHandleForPoint(line.getEndPoint());
                    if (startHandle.contains(localPoint)
                            || endHandle.contains(localPoint)) {
                        clickedOnHandle = true;
                        // 存储直线的起点和终点
                        originalLineStart = line.getStartPoint();
                        originalLineEnd = line.getEndPoint();
                    }
                } else// 非直线，矩形处理
                {
                    panel.updateResizeHandlesForRect();
                    for (Rectangle handle : resizeHandles) {
                        if (handle.contains(localPoint)) {
                            clickedOnHandle = true;
                            break;
                        }
                    }
                    if (clickedOnHandle) {
                        // 存储变化前矩形的边界
                        originalRectBounds = new Rectangle(panel.selectedElement.getBounds());
                    }
                }
                // 没有点击在控制点上，且点击在选中元素上，存储原始状态
                if (!clickedOnHandle && elementUnderMouse == panel.selectedElement) {
                    if (elementUnderMouse instanceof LineElement) {
                        originalLineStart = ((LineElement) elementUnderMouse).getStartPoint();
                        originalLineEnd = ((LineElement) elementUnderMouse).getEndPoint();
                    } else {
                        originalRectBounds = new Rectangle(elementUnderMouse.getBounds());
                    }
                }
            }
            /* 缩放 */
            if (panel.selectedElement != null) {
                // 直线
                if (panel.selectedElement instanceof LineElement line) {
                    Rectangle startHandle = panel.getHandleForPoint(line.getStartPoint());
                    Rectangle endHandle = panel.getHandleForPoint(line.getEndPoint());
                    // 判断鼠标是否在两个控制点上
                    if (startHandle.contains(localPoint)) {
                        panel.currentState = State.RESIZING;// 切换状态为resizing
                        panel.activeHandle = 0;
                        return;
                    }
                    if (endHandle.contains(localPoint)) {
                        panel.currentState = State.RESIZING;
                        panel.activeHandle = 1;
                        return;
                    }
                }
                // 其他元素
                else {
                    panel.updateResizeHandlesForRect(); // 更新控制点位置
                    // 逐个扫描
                    for (int i = 0; i < 8; i++) {
                        if (panel.resizeHandles[i].contains(localPoint)) {
                            panel.currentState = State.RESIZING;
                            panel.activeHandle = i; // 调整活动控制点索引
                            return;// 检测到直接返回，结束循环
                        }
                    }
                }
            }
            /* 移动或编辑文本 */
            if (elementUnderMouse != null) {
                // 双击文本元素
                if (e.getClickCount() == 2 && elementUnderMouse instanceof TextElement textElement) {
                    panel.startEditingText(textElement);
                    return;
                }
                // ClickCount != 2 || 非文本元素，移动处理
                else {
                    // 再次点击的元素需与当前选中元素相同
                    if (panel.selectedElement != elementUnderMouse) {
                        panel.selectedElement = elementUnderMouse;
                        // 存储原始状态
                        if (selectedElement instanceof LineElement) {
                            originalLineStart = ((LineElement) selectedElement).getStartPoint();
                            originalLineEnd = ((LineElement) selectedElement).getEndPoint();
                        } else {
                            originalRectBounds = new Rectangle(selectedElement.getBounds());
                        }
                    }
                    // 切换状态为移动
                    panel.currentState = State.MOVING;
                }
            }
            // 瞎jb点，取消选中
            else {
                panel.selectedElement = null;
                panel.currentState = State.IDLE;
            }
            panel.repaint();
        }

        // 鼠标释放事件处理
        @Override
        public void mouseReleased(MouseEvent e) {
            if (e.isPopupTrigger()) {
                handlePopup(e);
            }

            if (currentState == State.PANNING) {
                currentState = State.IDLE;
                panel.setCursor(Cursor.getDefaultCursor());
                return;
            }

            if (currentState == State.ROTATING && selectedElement != null) {
                double finalRotation = selectedElement.getRotation();
                if (finalRotation != originalRotation) {
                    double oldRot = originalRotation;
                    Command cmd = new ChangeElementPropertyCommand(
                            () -> selectedElement.setRotation(finalRotation),
                            () -> selectedElement.setRotation(oldRot));
                    getUndoManager().executeCommand(cmd);
                }
                currentState = State.IDLE;
                panel.setCursor(Cursor.getDefaultCursor());
                return;
            }

            // 完成移动或调整大小后，记录命令以支持撤销/重做
            if ((currentState == State.MOVING || currentState == State.RESIZING) && selectedElement != null) {
                if (selectedElement instanceof LineElement) {
                    LineElement line = (LineElement) selectedElement;
                    if (originalLineStart != null && originalLineEnd != null) {
                        Point finalLineStart = line.getStartPoint();
                        Point finalLineEnd = line.getEndPoint();
                        if (!originalLineStart.equals(finalLineStart) || !originalLineEnd.equals(finalLineEnd)) {
                            Command cmd = new ChangeLineEndpointsCommand(line, originalLineStart, originalLineEnd,
                                    finalLineStart, finalLineEnd);
                            getUndoManager().executeCommand(cmd);
                        }
                    }
                } else {
                    if (originalRectBounds != null) {
                        Rectangle finalBounds = selectedElement.getBounds();
                        if (!originalRectBounds.equals(finalBounds)) {
                            Command cmd = new ChangeBoundsCommand(selectedElement, originalRectBounds, finalBounds);
                            getUndoManager().executeCommand(cmd);
                        }
                    }
                }
            }

            currentState = State.IDLE;
            activeHandle = -1;
            panel.setCursor(Cursor.getDefaultCursor());
            originalRectBounds = null;
            originalLineStart = null;
            originalLineEnd = null;
        }

        // 鼠标拖动事件处理
        @Override
        public void mouseDragged(MouseEvent e) {
            if (panel.lastMousePoint == null)
                return;

            if (panel.currentState == State.PANNING) {
                int dx = e.getX() - panel.lastMousePoint.x;
                int dy = e.getY() - panel.lastMousePoint.y;
                panel.translateX += dx;
                panel.translateY += dy;
                panel.lastMousePoint = e.getPoint();
                panel.repaint();
                return;
            }

            Point logicalPoint = panel.toLogical(e.getPoint());

            if (panel.currentState == State.ROTATING && panel.selectedElement != null) {
                Rectangle bounds = panel.selectedElement.getBounds();
                Point center = new Point(bounds.x + bounds.width / 2, bounds.y + bounds.height / 2);
                if (panel.selectedElement instanceof LineElement) {
                    LineElement line = (LineElement) panel.selectedElement;
                    Point start = line.getStartPoint();
                    Point end = line.getEndPoint();
                    center = new Point((start.x + end.x) / 2, (start.y + end.y) / 2);
                }

                double angle = Math.toDegrees(Math.atan2(logicalPoint.y - center.y, logicalPoint.x - center.x));
                panel.selectedElement.setRotation(angle + 90);
                panel.repaint();
                return;
            }

            if (panel.currentState == State.RESIZING && panel.selectedElement != null) {
                Point localPoint = getLocalPoint(logicalPoint);
                Point lastLocalPoint = getLocalPoint(panel.lastMousePoint);
                int dx = localPoint.x - lastLocalPoint.x;
                int dy = localPoint.y - lastLocalPoint.y;

                if (panel.selectedElement instanceof LineElement) {
                    LineElement line = (LineElement) panel.selectedElement;
                    if (panel.activeHandle == 0) {
                        line.moveStartPoint(dx, dy);
                    } else if (panel.activeHandle == 1) {
                        line.moveEndPoint(dx, dy);
                    }
                } else {
                    resizeRectElement(dx, dy);
                }
            } else if (panel.currentState == State.MOVING && panel.selectedElement != null) {
                int dx = logicalPoint.x - panel.lastMousePoint.x;
                int dy = logicalPoint.y - panel.lastMousePoint.y;
                panel.selectedElement.move(dx, dy);
            }
            panel.lastMousePoint = logicalPoint;
            panel.repaint();
        }

        // 鼠标移动事件处理
        @Override
        public void mouseMoved(MouseEvent e) {
            Point logicalPoint = panel.toLogical(e.getPoint());
            Point localPoint = getLocalPoint(logicalPoint);

            if (panel.selectedElement != null) {
                Rectangle rotationHandle = panel.getRotationHandleBounds(panel.selectedElement);
                if (rotationHandle.contains(localPoint)) {
                    panel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                    return;
                }

                if (panel.selectedElement instanceof LineElement) {
                    LineElement line = (LineElement) panel.selectedElement;
                    Rectangle startHandle = panel.getHandleForPoint(line.getStartPoint());
                    Rectangle endHandle = panel.getHandleForPoint(line.getEndPoint());
                    if (startHandle.contains(localPoint) || endHandle.contains(localPoint)) {
                        panel.setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
                        return;
                    }
                } else {
                    panel.updateResizeHandlesForRect();
                    for (int i = 0; i < 8; i++) {
                        if (panel.resizeHandles[i].contains(localPoint)) {
                            panel.setCursor(getResizeCursor(i));
                            return;
                        }
                    }
                }
            }
            if (findElementAt(logicalPoint) != null) {
                panel.setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
            } else {
                panel.setCursor(Cursor.getDefaultCursor());
            }
        }

        private UndoManager getUndoManager() {
            return ((PresentationApp) SwingUtilities.getWindowAncestor(panel)).getUndoManager();
        }

        // 根据鼠标位置查找元素，返回当前鼠标所指的元素
        private SlideElement findElementAt(Point p) {
            List<SlideElement> elements = panel.currentPage.getElements();
            // 逐个扫描
            for (int i = elements.size() - 1; i >= 0; i--) {
                if (elements.get(i).contains(p)) {
                    return elements.get(i);
                }
            }
            return null;
        }

        // 根据控制点索引获取对应的调整大小光标
        private Cursor getResizeCursor(int handleIndex) {
            return switch (handleIndex) {
                case 0 -> Cursor.getPredefinedCursor(Cursor.NW_RESIZE_CURSOR);
                case 1 -> Cursor.getPredefinedCursor(Cursor.N_RESIZE_CURSOR);
                case 2 -> Cursor.getPredefinedCursor(Cursor.NE_RESIZE_CURSOR);
                case 3 -> Cursor.getPredefinedCursor(Cursor.E_RESIZE_CURSOR);
                case 4 -> Cursor.getPredefinedCursor(Cursor.SE_RESIZE_CURSOR);
                case 5 -> Cursor.getPredefinedCursor(Cursor.S_RESIZE_CURSOR);
                case 6 -> Cursor.getPredefinedCursor(Cursor.SW_RESIZE_CURSOR);
                case 7 -> Cursor.getPredefinedCursor(Cursor.W_RESIZE_CURSOR);
                default -> Cursor.getDefaultCursor();
            };
        }

        // 调整矩形元素大小
        private void resizeRectElement(int dx, int dy) {
            Rectangle bounds = panel.selectedElement.getBounds();
            int minSize = 20;
            switch (panel.activeHandle) {
                case 0:
                    bounds.x += dx;
                    bounds.y += dy;
                    bounds.width -= dx;
                    bounds.height -= dy;
                    break;
                case 1:
                    bounds.y += dy;
                    bounds.height -= dy;
                    break;
                case 2:
                    bounds.width += dx;
                    bounds.y += dy;
                    bounds.height -= dy;
                    break;
                case 3:
                    bounds.width += dx;
                    break;
                case 4:
                    bounds.width += dx;
                    bounds.height += dy;
                    break;
                case 5:
                    bounds.height += dy;
                    break;
                case 6:
                    bounds.x += dx;
                    bounds.width -= dx;
                    bounds.height += dy;
                    break;
                case 7:
                    bounds.x += dx;
                    bounds.width -= dx;
                    break;
            }
            if (bounds.width < minSize)
                bounds.width = minSize;
            if (bounds.height < minSize)
                bounds.height = minSize;
            panel.selectedElement.setBounds(bounds);
        }

        @Override
        public void mouseWheelMoved(java.awt.event.MouseWheelEvent e) {
            if (e.isControlDown()) {
                double currentScale = panel.getScaleFactor();
                if (e.getWheelRotation() < 0) {
                    currentScale += 0.1;
                } else {
                    currentScale -= 0.1;
                }
                // 限制缩放范围
                if (currentScale < 0.1)
                    currentScale = 0.1;
                if (currentScale > 5.0)
                    currentScale = 5.0;
                panel.setScaleFactor(currentScale);
            }
        }

        private void showContextMenu(MouseEvent e) {
            if (panel.selectedElement != null) {
                JPopupMenu contextMenu = new JPopupMenu();

                JMenuItem deleteItem = new JMenuItem("删除");
                deleteItem.addActionListener(_ -> panel.deleteSelectedElement());
                contextMenu.add(deleteItem);

                JMenuItem rotateItem = new JMenuItem("旋转...");
                rotateItem.addActionListener(_ -> {
                    String input = JOptionPane.showInputDialog(panel, "请输入旋转角度 (度):",
                            panel.selectedElement.getRotation());
                    if (input != null) {
                        try {
                            double newRotation = Double.parseDouble(input);
                            double oldRotation = panel.selectedElement.getRotation();
                            PresentationApp app = (PresentationApp) SwingUtilities.getWindowAncestor(panel);
                            if (app != null) {
                                Command cmd = new ChangeElementPropertyCommand(
                                        () -> panel.selectedElement.setRotation(newRotation),
                                        () -> panel.selectedElement.setRotation(oldRotation));
                                app.getUndoManager().executeCommand(cmd);
                                panel.repaint();
                            }
                        } catch (NumberFormatException ex) {
                            JOptionPane.showMessageDialog(panel, "请输入有效的数字。");
                        }
                    }
                });
                contextMenu.add(rotateItem);

                contextMenu.addSeparator();

                JMenuItem bringToFrontItem = new JMenuItem("置于顶层");
                bringToFrontItem.addActionListener(_ -> {
                    PresentationApp app = (PresentationApp) SwingUtilities.getWindowAncestor(panel);
                    if (app != null) {
                        Command cmd = new BringToFrontCommand(panel.currentPage, panel.selectedElement);
                        app.getUndoManager().executeCommand(cmd);
                        panel.repaint();
                    }
                });
                contextMenu.add(bringToFrontItem);

                JMenuItem sendToBackItem = new JMenuItem("置于底层");
                sendToBackItem.addActionListener(_ -> {
                    PresentationApp app = (PresentationApp) SwingUtilities.getWindowAncestor(panel);
                    if (app != null) {
                        Command cmd = new SendToBackCommand(panel.currentPage, panel.selectedElement);
                        app.getUndoManager().executeCommand(cmd);
                        panel.repaint();
                    }
                });
                contextMenu.add(sendToBackItem);

                contextMenu.addSeparator();

                JMenuItem colorItem = new JMenuItem("修改颜色...");
                colorItem.addActionListener(_ -> {
                    Color newColor = JColorChooser.showDialog(panel, "选择颜色", Color.BLACK);
                    if (newColor != null) {
                        PresentationApp app = (PresentationApp) SwingUtilities.getWindowAncestor(panel);
                        if (app != null) {
                            SlideElement selected = panel.selectedElement;
                            if (selected instanceof TextElement textElem) {
                                Color oldColor = textElem.getColor();
                                Command cmd = new ChangeElementPropertyCommand(() -> textElem.setColor(newColor),
                                        () -> textElem.setColor(oldColor));
                                app.getUndoManager().executeCommand(cmd);
                            } else if (selected instanceof LineElement lineElem) {
                                Color oldColor = lineElem.getColor();
                                Command cmd = new ChangeElementPropertyCommand(() -> lineElem.setColor(newColor),
                                        () -> lineElem.setColor(oldColor));
                                app.getUndoManager().executeCommand(cmd);
                            } else if (selected instanceof ShapeElement shape) {
                                Object[] options = { "边框", "填充" };
                                int choice = JOptionPane.showOptionDialog(panel, "修改哪个部分的颜色？", "选择颜色类型",
                                        JOptionPane.YES_NO_OPTION,
                                        JOptionPane.QUESTION_MESSAGE, null, options, options[0]);
                                if (choice == 0) {
                                    Color oldColor = shape.getBorderColor();
                                    Command cmd = new ChangeElementPropertyCommand(() -> shape.setBorderColor(newColor),
                                            () -> shape.setBorderColor(oldColor));
                                    app.getUndoManager().executeCommand(cmd);
                                } else {
                                    Color oldColor = shape.getFillColor();
                                    Command cmd = new ChangeElementPropertyCommand(() -> shape.setFillColor(newColor),
                                            () -> shape.setFillColor(oldColor));
                                    app.getUndoManager().executeCommand(cmd);
                                }
                            }
                            panel.repaint();
                        }
                    }
                });
                contextMenu.add(colorItem);

                if (panel.selectedElement instanceof ShapeElement || panel.selectedElement instanceof LineElement) {
                    JMenuItem thicknessItem = new JMenuItem("修改粗细...");
                    thicknessItem.addActionListener(_ -> {
                        PresentationApp app = (PresentationApp) SwingUtilities.getWindowAncestor(panel);
                        if (app != null) {
                            SlideElement selected = panel.selectedElement;
                            if (selected instanceof ShapeElement shape) {
                                String input = JOptionPane.showInputDialog(panel, "请输入边框粗细:",
                                        shape.getBorderThickness());
                                if (input != null) {
                                    try {
                                        int newThickness = Integer.parseInt(input);
                                        if (newThickness >= 0) {
                                            int oldThickness = shape.getBorderThickness();
                                            Command cmd = new ChangeElementPropertyCommand(
                                                    () -> shape.setBorderThickness(newThickness),
                                                    () -> shape.setBorderThickness(oldThickness));
                                            app.getUndoManager().executeCommand(cmd);
                                            panel.repaint();
                                        } else {
                                            JOptionPane.showMessageDialog(panel, "边框粗细必须大于等于0。");
                                        }
                                    } catch (NumberFormatException ex) {
                                        JOptionPane.showMessageDialog(panel, "请输入有效的整数。");
                                    }
                                }
                            } else if (selected instanceof LineElement line) {
                                String input = JOptionPane.showInputDialog(panel, "请输入线条粗细:", line.getThickness());
                                if (input != null) {
                                    try {
                                        int newThickness = Integer.parseInt(input);
                                        if (newThickness > 0) {
                                            int oldThickness = line.getThickness();
                                            Command cmd = new ChangeElementPropertyCommand(
                                                    () -> line.setThickness(newThickness),
                                                    () -> line.setThickness(oldThickness));
                                            app.getUndoManager().executeCommand(cmd);
                                            panel.repaint();
                                        } else {
                                            JOptionPane.showMessageDialog(panel, "线条粗细必须大于0。");
                                        }
                                    } catch (NumberFormatException ex) {
                                        JOptionPane.showMessageDialog(panel, "请输入有效的整数。");
                                    }
                                }
                            }
                        }
                    });
                    contextMenu.add(thicknessItem);
                }

                contextMenu.show(panel, e.getX(), e.getY());
            }
        }

        private void handlePopup(MouseEvent e) {
            Point logicalPoint = panel.toLogical(e.getPoint());
            SlideElement elementUnderMouse = findElementAt(logicalPoint);

            if (elementUnderMouse != null) {
                if (panel.selectedElement != elementUnderMouse) {
                    panel.selectedElement = elementUnderMouse;
                    panel.repaint();
                }
                showContextMenu(e);
            }
        }

        private Point getLocalPoint(Point logicalPoint) {
            if (panel.selectedElement == null || panel.selectedElement.getRotation() == 0) {
                return logicalPoint;
            }
            Rectangle bounds = panel.selectedElement.getBounds();
            Point center = new Point(bounds.x + bounds.width / 2, bounds.y + bounds.height / 2);
            if (panel.selectedElement instanceof LineElement) {
                LineElement line = (LineElement) panel.selectedElement;
                Point start = line.getStartPoint();
                Point end = line.getEndPoint();
                center = new Point((start.x + end.x) / 2, (start.y + end.y) / 2);
            }
            return panel.selectedElement.rotatePoint(logicalPoint, center, -panel.selectedElement.getRotation());
        }
    }

    public void startEditingText(TextElement textElement) {
        if (activeTextEditor != null) {
            stopEditingText();
        }
        editingElement = textElement;

        Rectangle bounds = textElement.getBounds();
        int screenX = (int) (bounds.x * scaleFactor) + translateX;
        int screenY = (int) (bounds.y * scaleFactor) + translateY;
        int screenW = (int) (bounds.width * scaleFactor);
        int screenH = (int) (bounds.height * scaleFactor);

        activeTextEditor = new JTextArea(textElement.getText());
        Font elemFont = textElement.getFont();
        Font scaledFont = elemFont.deriveFont(elemFont.getSize() * (float) scaleFactor);
        activeTextEditor.setFont(scaledFont);
        activeTextEditor.setForeground(textElement.getColor());
        activeTextEditor.setBounds(screenX, screenY, screenW, screenH);
        activeTextEditor.setOpaque(false);
        activeTextEditor.setLineWrap(true);
        activeTextEditor.setWrapStyleWord(true);

        // 简单的垂直居中模拟（如果需要）
        // 但由于JTextArea不支持垂直对齐，这里暂且保持默认顶部对齐
        // 或者可以通过设置EmptyBorder来模拟，但计算复杂。
        // 鉴于用户要求“直接对文本框操作”，位置的一致性很重要，但完全一致比较难。
        // 暂时接受顶部对齐，或者修改TextElement为顶部对齐。
        // 为了体验更好，我们让JTextArea背景透明，这样可以看到原来的位置（虽然原来的文字被隐藏了）

        activeTextEditor.addFocusListener(new java.awt.event.FocusAdapter() {
            @Override
            public void focusLost(java.awt.event.FocusEvent e) {
                stopEditingText();
            }
        });

        this.add(activeTextEditor);
        this.validate();
        this.repaint();
        activeTextEditor.requestFocusInWindow();
    }

    public void stopEditingText() {
        if (activeTextEditor != null && editingElement != null) {
            String newText = activeTextEditor.getText();
            String oldText = editingElement.getText();
            if (!newText.equals(oldText)) {
                PresentationApp app = (PresentationApp) SwingUtilities.getWindowAncestor(this);
                if (app != null) {
                    Command cmd = new ChangeElementPropertyCommand(
                            () -> editingElement.setText(newText),
                            () -> editingElement.setText(oldText));
                    app.getUndoManager().executeCommand(cmd);
                } else {
                    editingElement.setText(newText);
                }
            }
            this.remove(activeTextEditor);
            activeTextEditor = null;
            editingElement = null;
            this.repaint();
        }
    }

    public void deleteSelectedElement() {
        if (selectedElement != null) {
            // 如果正在编辑文本，先停止编辑
            stopEditingText();

            PresentationApp app = (PresentationApp) SwingUtilities.getWindowAncestor(this);
            if (app != null) {
                Command cmd = new RemoveElementCommand(currentPage, selectedElement);
                app.getUndoManager().executeCommand(cmd);
                selectedElement = null;
                repaint();
            }
        }
    }

    private Point getRotationHandleCenter(SlideElement element) {
        Rectangle bounds = element.getBounds();
        Point center = new Point(bounds.x + bounds.width / 2, bounds.y + bounds.height / 2);

        if (element instanceof LineElement) {
            LineElement line = (LineElement) element;
            Point start = line.getStartPoint();
            Point end = line.getEndPoint();
            center = new Point((start.x + end.x) / 2, (start.y + end.y) / 2);
        }

        // Calculate unrotated handle position (above the element)
        return new Point(center.x, bounds.y - ROTATION_HANDLE_OFFSET);
    }

    private Rectangle getRotationHandleBounds(SlideElement element) {
        Point p = getRotationHandleCenter(element);
        int size = (int) (ROTATION_HANDLE_SIZE / scaleFactor);
        return new Rectangle(p.x - size / 2, p.y - size / 2, size, size);
    }
}