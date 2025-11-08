// SlideEditorPanel类
// 功能：幻灯片编辑面板，处理元素的绘制和交互
import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.util.List;

public class SlideEditorPanel extends JPanel {
    private SlidePage currentPage;// 当前编辑的幻灯片页面
    private SlideElement selectedElement = null;// 当前选中的幻灯片元素
    private Point lastMousePoint;// 上一次鼠标位置
    private enum State { IDLE, MOVING, RESIZING } // 三种编辑状态
    private State currentState = State.IDLE; // 状态标志位
    /*
    * @***********@***********@
    * *                       *
    * *                       *
    * @         八个点         @
    * *                       *
    * *                       *
    * @***********@***********@
    */
    // 此处handle应该是指调整大小的控制点
    private static final int HANDLE_SIZE = 8;//控制点位边长为4的正方形

    private Rectangle[] resizeHandles = new Rectangle[8];// 八个控制点，八个正方形
    private int activeHandle = -1;// 当前活动的控制点索引

    public SlideEditorPanel(SlidePage page)//构造函数
    {
        this.currentPage = page;// 设置当前页面
        InteractionHandler handler = new InteractionHandler(this);// 创建交互处理器
        // 注册鼠标事件监听器
        addMouseListener(handler);
        addMouseMotionListener(handler);
        // 初始化控制点矩形
        for (int i = 0; i < 8; i++) {
            resizeHandles[i] = new Rectangle();
        }
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


    // 重写paintComponent方法进行绘制
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);// 调用父类方法清除背景
        if (currentPage != null) {
            // 绘制该页所有元素
            for (SlideElement element : currentPage.getElements()) {
                element.draw(g);
            }
            // 绘制选中元素的边框和控制点
            if (selectedElement != null) {
                Graphics2D g2d = (Graphics2D) g.create();//将Graphics对象转换为Graphics2D
                //对于直线元素，绘制起点和终点的控制点
                if (selectedElement instanceof LineElement) {
                    LineElement line = (LineElement) selectedElement;
                    Rectangle startHandle = line.getStartHandle();
                    Rectangle endHandle = line.getEndHandle();
                    g2d.setColor(Color.ORANGE);// 端点颜色
                    // 绘制控制点
                    g2d.fillRect(startHandle.x, startHandle.y, startHandle.width, startHandle.height);
                    g2d.fillRect(endHandle.x, endHandle.y, endHandle.width, endHandle.height);

                    g2d.setColor(Color.BLACK);// 控制点边框颜色
                    g2d.drawRect(startHandle.x, startHandle.y, startHandle.width, startHandle.height);
                    g2d.drawRect(endHandle.x, endHandle.y, endHandle.width, endHandle.height);
                } else //其他元素，绘制边框和八个控制点
                {
                    Rectangle bounds = selectedElement.getBounds();// 获取元素边界，四边形控制边界
                    g2d.setColor(Color.BLUE);
                    //线宽为1，末端方形截断，线条交会处切角连接，miterlimit不生效，虚线段9像素，间隔9像素，偏移0
                    g2d.setStroke(new BasicStroke(4f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 10f, new float[]{9f, 9f}, 0f));
                    // 绘制边框
                    g2d.drawRect(bounds.x - 1, bounds.y - 1, bounds.width + 2, bounds.height + 2);

                    // 更新控制点位置
                    updateResizeHandlesForRect();

                    // 绘制控制点
                    g2d.setColor(Color.WHITE);
                    g2d.setStroke(new BasicStroke(1));
                    for (Rectangle handle : resizeHandles) {
                        g2d.fillRect(handle.x, handle.y, handle.width, handle.height);
                        g2d.setColor(Color.BLACK);//控制点边框（黑色）
                        g2d.drawRect(handle.x, handle.y, handle.width, handle.height);
                        g2d.setColor(Color.WHITE);
                    }
                }
                g2d.dispose();// 释放Graphics2D对象
            }
        }
    }

    // 更新矩形元素的八个控制点位置
    private void updateResizeHandlesForRect()
    {
        if (selectedElement == null) return;
        Rectangle bounds = selectedElement.getBounds();// 获取元素边界
        int halfHandle = HANDLE_SIZE / 2;
        resizeHandles[0].setBounds(bounds.x - halfHandle, bounds.y - halfHandle, HANDLE_SIZE, HANDLE_SIZE);
        resizeHandles[1].setBounds(bounds.x + bounds.width / 2 - halfHandle, bounds.y - halfHandle, HANDLE_SIZE, HANDLE_SIZE);
        resizeHandles[2].setBounds(bounds.x + bounds.width - halfHandle, bounds.y - halfHandle, HANDLE_SIZE, HANDLE_SIZE);
        resizeHandles[3].setBounds(bounds.x + bounds.width - halfHandle, bounds.y + bounds.height / 2 - halfHandle, HANDLE_SIZE, HANDLE_SIZE);
        resizeHandles[4].setBounds(bounds.x + bounds.width - halfHandle, bounds.y + bounds.height - halfHandle, HANDLE_SIZE, HANDLE_SIZE);
        resizeHandles[5].setBounds(bounds.x + bounds.width / 2 - halfHandle, bounds.y + bounds.height - halfHandle, HANDLE_SIZE, HANDLE_SIZE);
        resizeHandles[6].setBounds(bounds.x - halfHandle, bounds.y + bounds.height - halfHandle, HANDLE_SIZE, HANDLE_SIZE);
        resizeHandles[7].setBounds(bounds.x - halfHandle, bounds.y + bounds.height / 2 - halfHandle, HANDLE_SIZE, HANDLE_SIZE);
    }

    // 内部类：处理鼠标交互
    private class InteractionHandler extends MouseAdapter implements MouseMotionListener {
        private final SlideEditorPanel panel;
        private String originalText;
        private Rectangle originalRectBounds;
        private Point originalLineStart;
        private Point originalLineEnd;

        public InteractionHandler(SlideEditorPanel panel) {
            this.panel = panel;
        }

        @Override
        public void mousePressed(MouseEvent e) {
            panel.lastMousePoint = e.getPoint();
            SlideElement elementUnderMouse = findElementAt(e.getPoint());

            if (panel.selectedElement != null) {
                boolean clickedOnHandle = false;
                if (panel.selectedElement instanceof LineElement) {
                    LineElement line = (LineElement) panel.selectedElement;
                    if (line.getStartHandle().contains(panel.lastMousePoint) || line.getEndHandle().contains(panel.lastMousePoint)) {
                        clickedOnHandle = true;
                        originalLineStart = line.getStartPoint();
                        originalLineEnd = line.getEndPoint();
                    }
                } else {
                    panel.updateResizeHandlesForRect();
                    for (Rectangle handle : resizeHandles) {
                        if (handle.contains(panel.lastMousePoint)) {
                            clickedOnHandle = true;
                            break;
                        }
                    }
                    if (clickedOnHandle) {
                        originalRectBounds = new Rectangle(panel.selectedElement.getBounds());
                    }
                }

                if (!clickedOnHandle && elementUnderMouse == panel.selectedElement) {
                    if (elementUnderMouse instanceof LineElement) {
                        originalLineStart = ((LineElement) elementUnderMouse).getStartPoint();
                        originalLineEnd = ((LineElement) elementUnderMouse).getEndPoint();
                    } else {
                        originalRectBounds = new Rectangle(elementUnderMouse.getBounds());
                    }
                }
            }

            if (panel.selectedElement != null) {
                if (panel.selectedElement instanceof LineElement) {
                    LineElement line = (LineElement) panel.selectedElement;
                    if (line.getStartHandle().contains(panel.lastMousePoint)) {
                        panel.currentState = State.RESIZING;
                        panel.activeHandle = 0;
                        return;
                    }
                    if (line.getEndHandle().contains(panel.lastMousePoint)) {
                        panel.currentState = State.RESIZING;
                        panel.activeHandle = 1;
                        return;
                    }
                } else {
                    panel.updateResizeHandlesForRect();
                    for (int i = 0; i < 8; i++) {
                        if (panel.resizeHandles[i].contains(panel.lastMousePoint)) {
                            panel.currentState = State.RESIZING;
                            panel.activeHandle = i;
                            return;
                        }
                    }
                }
            }

            if (elementUnderMouse != null) {
                if (e.getClickCount() == 2 && elementUnderMouse instanceof TextElement) {
                    TextElement textElement = (TextElement) elementUnderMouse;
                    originalText = textElement.getText();
                    String newText = JOptionPane.showInputDialog(panel, "编辑文本:", originalText);
                    if (newText != null && !newText.equals(originalText)) {
                        Command cmd = new ChangeElementPropertyCommand(() -> textElement.setText(newText), () -> textElement.setText(originalText));
                        getUndoManager().executeCommand(cmd);
                    }
                } else {
                    if (panel.selectedElement != elementUnderMouse) {
                        panel.selectedElement = elementUnderMouse;
                        if (selectedElement instanceof LineElement) {
                            originalLineStart = ((LineElement) selectedElement).getStartPoint();
                            originalLineEnd = ((LineElement) selectedElement).getEndPoint();
                        } else {
                            originalRectBounds = new Rectangle(selectedElement.getBounds());
                        }
                    }
                    panel.currentState = State.MOVING;
                }
            } else {
                panel.selectedElement = null;
                panel.currentState = State.IDLE;
            }
            panel.repaint();
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            if ((currentState == State.MOVING || currentState == State.RESIZING) && selectedElement != null) {
                if (selectedElement instanceof LineElement) {
                    LineElement line = (LineElement) selectedElement;
                    if (originalLineStart != null && originalLineEnd != null) {
                        Point finalLineStart = line.getStartPoint();
                        Point finalLineEnd = line.getEndPoint();
                        if (!originalLineStart.equals(finalLineStart) || !originalLineEnd.equals(finalLineEnd)) {
                            Command cmd = new ChangeLineEndpointsCommand(line, originalLineStart, originalLineEnd, finalLineStart, finalLineEnd);
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

        @Override
        public void mouseDragged(MouseEvent e) {
            if (panel.lastMousePoint == null) return;
            int dx = e.getX() - panel.lastMousePoint.x;
            int dy = e.getY() - panel.lastMousePoint.y;
            if (panel.currentState == State.MOVING && panel.selectedElement != null) {
                panel.selectedElement.move(dx, dy);
            } else if (panel.currentState == State.RESIZING && panel.selectedElement != null) {
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
            }
            panel.lastMousePoint = e.getPoint();
            panel.repaint();
        }

        @Override
        public void mouseMoved(MouseEvent e) {
            if (panel.selectedElement != null) {
                if (panel.selectedElement instanceof LineElement) {
                    LineElement line = (LineElement) panel.selectedElement;
                    if (line.getStartHandle().contains(e.getPoint()) || line.getEndHandle().contains(e.getPoint())) {
                        panel.setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
                        return;
                    }
                } else {
                    panel.updateResizeHandlesForRect();
                    for (int i = 0; i < 8; i++) {
                        if (panel.resizeHandles[i].contains(e.getPoint())) {
                            panel.setCursor(getResizeCursor(i));
                            return;
                        }
                    }
                }
            }
            if (findElementAt(e.getPoint()) != null) {
                panel.setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
            } else {
                panel.setCursor(Cursor.getDefaultCursor());
            }
        }

        private UndoManager getUndoManager() {
            return ((PresentationApp) SwingUtilities.getWindowAncestor(panel)).getUndoManager();
        }

        private SlideElement findElementAt(Point p) {
            List<SlideElement> elements = panel.currentPage.getElements();
            for (int i = elements.size() - 1; i >= 0; i--) {
                if (elements.get(i).contains(p)) {
                    return elements.get(i);
                }
            }
            return null;
        }

        private Cursor getResizeCursor(int handleIndex) {
            switch (handleIndex) {
                case 0: return Cursor.getPredefinedCursor(Cursor.NW_RESIZE_CURSOR);
                case 1: return Cursor.getPredefinedCursor(Cursor.N_RESIZE_CURSOR);
                case 2: return Cursor.getPredefinedCursor(Cursor.NE_RESIZE_CURSOR);
                case 3: return Cursor.getPredefinedCursor(Cursor.E_RESIZE_CURSOR);
                case 4: return Cursor.getPredefinedCursor(Cursor.SE_RESIZE_CURSOR);
                case 5: return Cursor.getPredefinedCursor(Cursor.S_RESIZE_CURSOR);
                case 6: return Cursor.getPredefinedCursor(Cursor.SW_RESIZE_CURSOR);
                case 7: return Cursor.getPredefinedCursor(Cursor.W_RESIZE_CURSOR);
                default: return Cursor.getDefaultCursor();
            }
        }

        private void resizeRectElement(int dx, int dy) {
            Rectangle bounds = panel.selectedElement.getBounds();
            int minSize = 20;
            switch (panel.activeHandle) {
                case 0: bounds.x += dx; bounds.y += dy; bounds.width -= dx; bounds.height -= dy; break;
                case 1: bounds.y += dy; bounds.height -= dy; break;
                case 2: bounds.width += dx; bounds.y += dy; bounds.height -= dy; break;
                case 3: bounds.width += dx; break;
                case 4: bounds.width += dx; bounds.height += dy; break;
                case 5: bounds.height += dy; break;
                case 6: bounds.x += dx; bounds.width -= dx; bounds.height += dy; break;
                case 7: bounds.x += dx; bounds.width -= dx; break;
            }
            if (bounds.width < minSize) bounds.width = minSize;
            if (bounds.height < minSize) bounds.height = minSize;
            panel.selectedElement.setBounds(bounds);
        }
    }
}