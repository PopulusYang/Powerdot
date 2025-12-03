
// SlideEditorPanel类
// 功能：幻灯片编辑面板，处理元素的绘制和交互
// 编辑ppt的核心
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import javax.swing.*;

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
                } else if (e.getKeyCode() == KeyEvent.VK_BACK_SPACE) {
                    // 在非编辑状态下按退格键删除当前选中元素
                    if (activeTextEditor == null && selectedElement != null) {
                        deleteSelectedElement();
                    }
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

    /** 设置缩放并自动将幻灯片居中，避免在缩放后偏移。 */
    public void setScaleAndCenter(double scale) {
        stopEditingText();
        // 限制缩放范围，避免过小或过大
        this.scaleFactor = Math.max(0.1, Math.min(5.0, scale));
        recenterSlide();
        repaint();
    }

    /** 按百分比设置缩放并居中，例如 100 -> 1.0。 */
    public void setZoomPercent(double percent) {
        setScaleAndCenter(percent / 100.0);
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

    /** 根据当前缩放将幻灯片居中。 */
    private void recenterSlide() {
        if (currentSlide == null) {
            return;
        }
        int panelWidth = Math.max(getWidth(), 1);
        int panelHeight = Math.max(getHeight(), 1);
        int designWidth = currentSlide.getWidth();
        int designHeight = currentSlide.getHeight();
        translateX = (int) ((panelWidth - designWidth * scaleFactor) / 2);
        translateY = (int) ((panelHeight - designHeight * scaleFactor) / 2);
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
            currentPage.renderBackground(g2d, currentSlide.getWidth(), currentSlide.getHeight());
        }

        // 绘制幻灯片边界
        g.setColor(Color.BLACK);
        g.drawRect(0, 0, currentSlide.getWidth(), currentSlide.getHeight());

        if (currentPage != null) {
            // 绘制该页所有元素
            for (SlideElement element : currentPage.getElements()) {
                // 正在编辑的元素由 JTextArea 显示，避免与绘制态重叠
                if (element == editingElement) {
                    continue;
                }
                element.draw(g);
            }
            // 绘制选中元素的边框和控制点
            if (selectedElement != null) {
                java.awt.geom.AffineTransform originalSelectionTransform = g2d.getTransform();

                if (selectedElement.getRotation() != 0&&!(selectedElement instanceof LineElement)) {
                    Rectangle bounds = selectedElement.getBounds();
                    Point center;
                        center = new Point(bounds.x + bounds.width / 2, bounds.y + bounds.height / 2);
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
             if (!(selectedElement instanceof LineElement)) {
                g2d.setColor(Color.BLACK);
                g2d.setStroke(new BasicStroke(1));
                g2d.drawLine(topCenter.x, topCenter.y, handleCenter.x, handleCenter.y);

                g2d.setColor(Color.GREEN);
                g2d.fillOval(handleBounds.x, handleBounds.y, handleBounds.width, handleBounds.height);
                g2d.setColor(Color.BLACK);
                g2d.drawOval(handleBounds.x, handleBounds.y, handleBounds.width, handleBounds.height);
                 }
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
            if(activeTextEditor != null&&editingElement != null) {
                Point logical =panel.toLogical(e.getPoint());
                if(!editingElement.contains(logical)){
                    panel.stopEditingText();
                   panel.requestFocusInWindow();
                }
            }
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
            if (panel.selectedElement != null && !(panel.selectedElement instanceof LineElement)) {
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
                if (elementUnderMouse instanceof TextElement textElement) {
                        Rectangle textArea = getTextContentBounds(textElement, panel.getGraphics());
                        if (textArea.contains(localPoint)) {
                            if ("双击以编辑文本".equals(textElement.getText())) {
                                textElement.setText(""); // 清空占位（按需）
                            }
                            panel.selectedElement = textElement;
                            panel.startEditingText(textElement);
                            return;
                        } else if (textElement.getBounds().contains(localPoint)) {
                            // 点在框内但不在文字上：只选中，允许拖动移动
                            panel.selectedElement = textElement;
                            panel.currentState = State.MOVING;
                            panel.lastMousePoint = logicalPoint;
                            panel.repaint();
                            return;
                        }
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
                if (selectedElement instanceof LineElement) {
                    currentState = State.IDLE;
                    panel.setCursor(Cursor.getDefaultCursor());
                    return;
                }
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
                   if(line.getRotation()==0){
                        if (panel.activeHandle == 0) {
                            
                            line.moveStartPoint(dx, dy);
                        } else if (panel.activeHandle == 1) {
                            line.moveEndPoint(dx, dy);
                        }
                    }
                    else{
                        Point distance=rotateVector(new Point(dx,dy), panel.selectedElement.getRotation());
                        if (panel.activeHandle == 0) {
                            
                            line.moveStartPoint(distance.x, distance.y);
                        } else if (panel.activeHandle == 1) {
                            line.moveEndPoint(distance.x, distance.y);
                        }
                    }
                  
                } else {
                    if(panel.selectedElement.getRotation()==0) {
                          boolean keepAspect = e.isShiftDown() && originalRectBounds != null;
                            resizeRectElement(dx, dy, keepAspect);
                    }
                    else{
                        Point oldCenter = panel.selectedElement.getRotationCenter();
                        boolean keepAspect = e.isShiftDown() && originalRectBounds != null;
                        resizeRectElement(dx, dy, keepAspect);

                        // 缩放后校正位置以保持原旋转中心不变
                        Point newCenter = panel.selectedElement.getRotationCenter();
                        int fixDx =  newCenter.x- oldCenter.x;
                        int fixDy = newCenter.y-oldCenter.y;
                        Point distance=rotateVector(new Point(fixDx,fixDy), panel.selectedElement.getRotation());
                        
                        panel.selectedElement.move(distance.x-fixDx, distance.y-fixDy); // 或 translate 方法
                    }
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

            if (panel.selectedElement != null&&!(panel.selectedElement instanceof LineElement)) {
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
                            double rotation = panel.selectedElement != null ? panel.selectedElement.getRotation() : 0;
                            panel.setCursor(getResizeCursor(i, rotation));
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
        private Cursor getResizeCursor(int handleIndex, double rotationDegrees) {
            // 先获取基础方向向量，再按元素旋转角度旋转向量，映射到最近的8方向光标
            int[][] dirs = {
                    { -1, -1 }, { 0, -1 }, { 1, -1 },
                    { 1, 0 }, { 1, 1 }, { 0, 1 },
                    { -1, 1 }, { -1, 0 }
            };
            int[] vec = dirs[handleIndex % dirs.length];
            double rad = Math.toRadians(rotationDegrees);
            double rx = vec[0] * Math.cos(rad) - vec[1] * Math.sin(rad);
            double ry = vec[0] * Math.sin(rad) + vec[1] * Math.cos(rad);
            double angle = Math.atan2(ry, rx); // -pi..pi
            if (angle < 0) angle += Math.PI * 2;
            int dirIndex = (int) Math.round(angle / (Math.PI / 4)) % 8;
            return switch (dirIndex) {
                case 0 -> Cursor.getPredefinedCursor(Cursor.E_RESIZE_CURSOR);   // 0 deg
                case 1 -> Cursor.getPredefinedCursor(Cursor.SE_RESIZE_CURSOR);  // 45
                case 2 -> Cursor.getPredefinedCursor(Cursor.S_RESIZE_CURSOR);   // 90
                case 3 -> Cursor.getPredefinedCursor(Cursor.SW_RESIZE_CURSOR);  // 135
                case 4 -> Cursor.getPredefinedCursor(Cursor.W_RESIZE_CURSOR);   // 180
                case 5 -> Cursor.getPredefinedCursor(Cursor.NW_RESIZE_CURSOR);  // 225
                case 6 -> Cursor.getPredefinedCursor(Cursor.N_RESIZE_CURSOR);   // 270
                case 7 -> Cursor.getPredefinedCursor(Cursor.NE_RESIZE_CURSOR);  // 315
                default -> Cursor.getDefaultCursor();
            };
        }
        
        // 调整矩形元素大小
        private void resizeRectElement(int dx, int dy, boolean keepAspect) {
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
            if (keepAspect && originalRectBounds != null && bounds.width > 0 && bounds.height > 0) {
                double aspect = originalRectBounds.getHeight() / (double) originalRectBounds.getWidth();
                switch (panel.activeHandle) {
                    case 0 -> { // 左上锚在右下
                        int anchorX = bounds.x + bounds.width;
                        int anchorY = bounds.y + bounds.height;
                        bounds.height = (int) Math.round(bounds.width * aspect);
                        bounds.x = anchorX - bounds.width;
                        bounds.y = anchorY - bounds.height;
                    }
                    case 1 -> { // 上中，锚在下中
                        int anchorX = bounds.x + bounds.width / 2;
                        int anchorY = bounds.y + bounds.height;
                        bounds.width = (int) Math.round(bounds.height / aspect);
                        bounds.x = anchorX - bounds.width / 2;
                        bounds.y = anchorY - bounds.height;
                    }
                    case 2 -> { // 右上，锚在左下
                        int anchorX = bounds.x;
                        int anchorY = bounds.y + bounds.height;
                        bounds.height = (int) Math.round(bounds.width * aspect);
                        bounds.y = anchorY - bounds.height;
                        // x 保持左侧锚不变
                    }
                    case 3 -> { // 右中，锚在左中
                        int anchorY = bounds.y + bounds.height / 2;
                        bounds.height = (int) Math.round(bounds.width * aspect);
                        bounds.y = anchorY - bounds.height / 2;
                    }
                    case 4 -> { // 右下，锚在左上
                        bounds.height = (int) Math.round(bounds.width * aspect);
                        // x,y 保持左上锚不变
                    }
                    case 5 -> { // 下中，锚在上中
                        int anchorX = bounds.x + bounds.width / 2;
                        bounds.width = (int) Math.round(bounds.height / aspect);
                        bounds.x = anchorX - bounds.width / 2;
                    }
                    case 6 -> { // 左下，锚在右上
                        int anchorX = bounds.x + bounds.width;
                        int anchorY = bounds.y;
                        bounds.height = (int) Math.round(bounds.width * aspect);
                        bounds.x = anchorX - bounds.width;
                        // y 保持右上锚不变
                    }
                    case 7 -> { // 左中，锚在右中
                        int anchorY = bounds.y + bounds.height / 2;
                        int anchorX = bounds.x + bounds.width;
                        bounds.height = (int) Math.round(bounds.width * aspect);
                        bounds.x = anchorX - bounds.width;
                        bounds.y = anchorY - bounds.height / 2;
                    }
                }
                if (bounds.width < minSize)
                    bounds.width = minSize;
                if (bounds.height < minSize)
                    bounds.height = minSize;
            }
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
                deleteItem.addActionListener(evt-> panel.deleteSelectedElement());
                contextMenu.add(deleteItem);
                if( !(panel.selectedElement instanceof LineElement)){
                JMenuItem rotateItem = new JMenuItem("旋转...");
                rotateItem.addActionListener(evt -> {
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
            }
                JMenuItem bringToFrontItem = new JMenuItem("置于顶层");
                bringToFrontItem.addActionListener(evt -> {
                    PresentationApp app = (PresentationApp) SwingUtilities.getWindowAncestor(panel);
                    if (app != null) {
                        Command cmd = new BringToFrontCommand(panel.currentPage, panel.selectedElement);
                        app.getUndoManager().executeCommand(cmd);
                        panel.repaint();
                    }
                });
                contextMenu.add(bringToFrontItem);

                JMenuItem sendToBackItem = new JMenuItem("置于底层");
                sendToBackItem.addActionListener(evt -> {
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
                colorItem.addActionListener(evt -> {
                    Color newColor = pickColor(panel.selectedElement);
                    if (newColor != null) {
                        PresentationApp app = (PresentationApp) SwingUtilities.getWindowAncestor(panel);
                        if (app != null) {
                            SlideElement selected = panel.selectedElement;
                            if (selected instanceof TextElement textElem) {
                                Object[] options = { "文字", "边框" };
                                int choice = JOptionPane.showOptionDialog(panel, "修改哪个部分的颜色？", "选择颜色类型",
                                        JOptionPane.YES_NO_OPTION,
                                        JOptionPane.QUESTION_MESSAGE, null, options, options[0]);
                                if (choice == 0) {
                                    Color oldColor = textElem.getColor();
                                    Command cmd = new ChangeElementPropertyCommand(() -> textElem.setColor(newColor),
                                            () -> textElem.setColor(oldColor));
                                    app.getUndoManager().executeCommand(cmd);
                                } else {
                                    Color oldColor = textElem.getBorderColor();
                                    Command cmd = new ChangeElementPropertyCommand(() -> textElem.setBorderColor(newColor),
                                            () -> textElem.setBorderColor(oldColor));
                                    app.getUndoManager().executeCommand(cmd);
                                }
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

                if (panel.selectedElement instanceof ShapeElement || panel.selectedElement instanceof LineElement || panel.selectedElement instanceof TextElement) {
                    JMenuItem thicknessItem = new JMenuItem("修改粗细...");
                    thicknessItem.addActionListener(evt -> {
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
                            } else if (selected instanceof TextElement textElem) {
                                String input = JOptionPane.showInputDialog(panel, "请输入边框粗细:",
                                        textElem.getBorderThickness());
                                if (input != null) {
                                    try {
                                        int newThickness = Integer.parseInt(input);
                                        if (newThickness >= 0) {
                                            int oldThickness = textElem.getBorderThickness();
                                            Command cmd = new ChangeElementPropertyCommand(
                                                    () -> textElem.setBorderThickness(newThickness),
                                                    () -> textElem.setBorderThickness(oldThickness));
                                            app.getUndoManager().executeCommand(cmd);
                                            panel.repaint();
                                        } else {
                                            JOptionPane.showMessageDialog(panel, "边框粗细必须大于等于0。");
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
        double rotation = textElement.getRotation();

        // 计算旋转后在屏幕上的包围盒，使编辑框跟随旋转后的位置
        int x = bounds.x;
        int y = bounds.y;
        int w = bounds.width;
        int h = bounds.height;
        Point center = new Point(x + w / 2, y + h / 2);
        Point[] corners = new Point[] {
                new Point(x, y),
                new Point(x + w, y),
                new Point(x + w, y + h),
                new Point(x, y + h)
        };
        double minX = Double.MAX_VALUE, minY = Double.MAX_VALUE, maxX = -Double.MAX_VALUE, maxY = -Double.MAX_VALUE;
        for (Point p : corners) {
            Point rp = rotation == 0 ? p : textElement.rotatePoint(p, center, rotation);
            double sx = rp.x * scaleFactor + translateX;
            double sy = rp.y * scaleFactor + translateY;
            minX = Math.min(minX, sx);
            minY = Math.min(minY, sy);
            maxX = Math.max(maxX, sx);
            maxY = Math.max(maxY, sy);
        }
        int screenX = (int) Math.round(minX);
        int screenY = (int) Math.round(minY);
        int screenW = (int) Math.round(maxX - minX);
        int screenH = (int) Math.round(maxY - minY);

        activeTextEditor = new JTextArea(textElement.getText());
        Font elemFont = textElement.getFont();
        Font scaledFont = elemFont.deriveFont(elemFont.getSize() * (float) scaleFactor);
        activeTextEditor.setFont(scaledFont);
        activeTextEditor.setForeground(textElement.getColor());
        activeTextEditor.setBounds(screenX, screenY, screenW, screenH);
        // 内边距按缩放计算，匹配 TextElement.draw 中的逻辑坐标 padding
        int topPad = (int) Math.round(4 * scaleFactor);
        int leftPad = (int) Math.round(5 * scaleFactor);
        activeTextEditor.setMargin(new java.awt.Insets(topPad, leftPad, 0, 0));
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
             String rawText = activeTextEditor.getText();
            // 根据空值决定占位文字，newText 不再二次赋值
             String newText = (rawText == null || rawText.isBlank()) ? "双击以编辑文本" : rawText;
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
    public Point rotateVector(Point vector, double angleDegrees) {
        double angleRadians = Math.toRadians(angleDegrees);
        double cos = Math.cos(angleRadians);
        double sin = Math.sin(angleRadians);
        
        int newX = (int) Math.round(vector.x * cos - vector.y * sin);
        int newY = (int) Math.round(vector.x * sin + vector.y * cos);
        
        return new Point(newX, newY);
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
    public void selectElement(SlideElement element) {
    stopEditingText();      // 可选：先结束其他编辑
    this.selectedElement = element;
    this.repaint();
    }
    // 计算文本内容实际占用的矩形（逻辑坐标）
    private Rectangle getTextContentBounds(TextElement t, Graphics g) {
        FontMetrics fm = g.getFontMetrics(t.getFont());
        // 与 TextElement.draw 的换行规则一致
        int maxWidth = t.getBounds().width - 10;
        java.util.List<String> lines = new java.util.ArrayList<>();
        for (String para : t.getText().split("\n", -1)) {
            if (para.isEmpty()) { lines.add(""); continue; }
                StringBuilder line = new StringBuilder();
                    for (char c : para.toCharArray()) {
                         String test = line.toString() + c;
                         if (fm.stringWidth(test) > maxWidth && line.length() > 0) {
                             lines.add(line.toString());
                             line = new StringBuilder().append(c);
                         } else {
                             line.append(c);
                         }
                     }
                    if (line.length() > 0) lines.add(line.toString());
                     }
        int lineHeight = fm.getHeight();
        int contentHeight = lines.size() * lineHeight;
        int x = t.getBounds().x + 5;
        int y = t.getBounds().y + fm.getAscent(); // 顶部对齐
        return new Rectangle(x, y - fm.getAscent(), maxWidth, contentHeight);
    }

    /**
     * 颜色选择器，附带预览，尽量贴近 PPT 的体验
     */
    private Color pickColor(SlideElement target) {
        Color initial = Color.BLACK;
        if (target instanceof TextElement textElem) {
            initial = textElem.getColor();
        } else if (target instanceof LineElement lineElem) {
            initial = lineElem.getColor();
        } else if (target instanceof ShapeElement shape) {
            initial = shape.getBorderColor();
        }
        return NewColorPickerDialog.pickColor(this, initial, "选择颜色");
    }
}
