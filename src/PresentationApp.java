// PowerDot - A simple presentation software in Java Swing
// 文件名：PresentationApp.java
// 描述：主应用程序类，包含幻灯片编辑器的主要界面和功能实现。

import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JColorChooser;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JToolBar;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.GridLayout;
import javax.swing.filechooser.FileNameExtensionFilter;

public class PresentationApp extends JFrame {
    private Slide slide;// 当前幻灯片
    private final SlideEditorPanel editorPanel; // 编辑幻灯片面板对象
    private final SlidePreviewPanel previewPanel; // 幻灯片预览面板
    private final UndoManager undoManager = new UndoManager(); // 撤销管理器

    private JButton prevPageButton; // 上一页
    private JButton nextPageButton; // 下一页
    private JLabel pageStatusLabel; // 页面状态标签
    private SlideshowPlayer.Transition selectedTransition = SlideshowPlayer.Transition.FADE;// 播放幻灯片时的过渡动画
    private JComboBox<String> fontComboBox; // 字体选择下拉框

    private JMenuBar menuBar;
    private JToolBar toolBar;

    public enum PageLayout // 页面布局枚举
    {
        TITLE_ONLY, // 仅标题
        TITLE_AND_CONTENT, // 标题和内容
        TWO_COLUMNS // 两栏
    }

    public UndoManager getUndoManager() {
        return undoManager;
    }

    // 画界面
    public PresentationApp() {
        setTitle("PowerDot");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); // 关闭操作 EXIT_ON_CLOSE会直接关闭JVM,后续需要更改
        setSize(1200, 800);
        setLocationRelativeTo(null); // 显示在屏幕中间

        // 新建幻灯片
        slide = new Slide();
        slide.addPage(new SlidePage());

        createMenuBar();
        createToolBar();

        // 初始化编辑面板
        editorPanel = new SlideEditorPanel(slide.getCurrentPage());
        editorPanel.setBackground(Color.WHITE);
        add(editorPanel, BorderLayout.CENTER);

        // 初始化预览面板
        previewPanel = new SlidePreviewPanel(this);
        previewPanel.updateSlideList(slide.getAllPages());
        previewPanel.setSelectedPage(0);
        add(previewPanel, BorderLayout.WEST);

        // 注册撤销管理器监听器，当内容发生变化时刷新预览
        undoManager.addListener(() -> {
            previewPanel.refreshPreviews();
            editorPanel.repaint(); // 确保编辑区也重绘
        });

        createStatusBar();
        setVisible(true);
        updatePageStatus();

        // 启动时自动适配屏幕
        SwingUtilities.invokeLater(() -> {
            editorPanel.zoomToFit();
            // 更新工具栏缩放比例显示（如果有的话，这里暂时没有直接访问到那个ComboBox，可以后续优化）
        });
    }

    public void jumpToPage(int index) {
        if (index == slide.getCurrentPageIndex())
            return;
        if (index >= 0 && index < slide.getTotalPages()) {
            slide.setCurrentPageIndex(index);
            editorPanel.setSlidePage(slide.getCurrentPage());
            updatePageStatus();
        }
    }

    // 创建菜单栏
    private void createMenuBar() {
        menuBar = new JMenuBar();

        JMenu fileMenu = new JMenu("文件(F)");
        fileMenu.setMnemonic(KeyEvent.VK_F);// 设置助记键
        JMenuItem newMenuItem = new JMenuItem("新建(N)");
        newMenuItem.setMnemonic(KeyEvent.VK_N);
        newMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N, InputEvent.CTRL_DOWN_MASK));// 设置全局快捷键Ctrl+N
        // lambda表达式简化代码
        newMenuItem.addActionListener(_ -> {
            // 新建幻灯片
            this.slide = new Slide();
            this.slide.addPage(new SlidePage());
            editorPanel.setSlidePage(this.slide.getCurrentPage());
            undoManager.clear();

            // 更新预览列表
            previewPanel.updateSlideList(slide.getAllPages());
            previewPanel.setSelectedPage(0);

            updatePageStatus();
        });
        JMenuItem openMenuItem = new JMenuItem("打开(O)...");
        openMenuItem.setMnemonic(KeyEvent.VK_O);
        openMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, InputEvent.CTRL_DOWN_MASK));
        openMenuItem.addActionListener(_ -> {
            openSlide();
            updatePageStatus();
        });
        JMenuItem saveMenuItem = new JMenuItem("保存(S)...");
        saveMenuItem.setMnemonic(KeyEvent.VK_S);
        saveMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK));
        saveMenuItem.addActionListener(_ -> saveSlide());
        JMenuItem exportImageMenuItem = new JMenuItem("导出为图片(E)...");
        exportImageMenuItem.setMnemonic(KeyEvent.VK_E);
        exportImageMenuItem.addActionListener(_ -> exportCurrentPageAsImage());
        JMenuItem exportPDFMenuItem = new JMenuItem("导出为PDF...");
        fileMenu.add(newMenuItem);
        fileMenu.addSeparator();
        fileMenu.add(openMenuItem);
        fileMenu.add(saveMenuItem);
        fileMenu.addSeparator();
        fileMenu.add(exportImageMenuItem);
        fileMenu.add(exportPDFMenuItem);

        JMenu editMenu = new JMenu("编辑(E)");
        editMenu.setMnemonic(KeyEvent.VK_E);
        JMenuItem undoMenuItem = new JMenuItem("撤销(U)");
        undoMenuItem.setMnemonic(KeyEvent.VK_U);
        undoMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Z, InputEvent.CTRL_DOWN_MASK));
        undoMenuItem.addActionListener(_ -> {
            undoManager.undo();
            editorPanel.repaint();
        });
        JMenuItem redoMenuItem = new JMenuItem("重做(R)");
        redoMenuItem.setMnemonic(KeyEvent.VK_R);
        redoMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Y, InputEvent.CTRL_DOWN_MASK));
        redoMenuItem.addActionListener(_ -> {
            undoManager.redo();
            editorPanel.repaint();
        });
        editMenu.add(undoMenuItem);
        editMenu.add(redoMenuItem);

        JMenuItem deleteMenuItem = new JMenuItem("删除(D)");
        deleteMenuItem.setMnemonic(KeyEvent.VK_D);
        deleteMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0));
        deleteMenuItem.addActionListener(_ -> editorPanel.deleteSelectedElement());
        editMenu.add(deleteMenuItem);

        editMenu.addSeparator();
        JMenuItem newPageMenuItem = new JMenuItem("新建空白页面(P)");
        newPageMenuItem.setMnemonic(KeyEvent.VK_P);
        newPageMenuItem.addActionListener(_ -> {
            SlidePage newPage = new SlidePage();
            this.slide.addPage(newPage);
            this.slide.setCurrentPageIndex(this.slide.getTotalPages() - 1);
            editorPanel.setSlidePage(newPage);

            // 更新预览列表
            previewPanel.updateSlideList(slide.getAllPages());
            previewPanel.setSelectedPage(slide.getCurrentPageIndex());

            updatePageStatus();
        });
        editMenu.add(newPageMenuItem);

        JMenu insertMenu = new JMenu("插入(I)");
        insertMenu.setMnemonic(KeyEvent.VK_I);
        JMenuItem insertTextMenuItem = new JMenuItem("文本框(T)");
        insertTextMenuItem.setMnemonic(KeyEvent.VK_T);
        insertTextMenuItem.addActionListener(_ -> {
            TextElement newText = new TextElement("双击以编辑文本", 100, 100, 200, 40);
            Command cmd = new AddElementCommand(editorPanel.getCurrentPage(), newText);
            undoManager.executeCommand(cmd);
            editorPanel.repaint();
        });
        JMenu insertShapeMenu = new JMenu("基本图形(S)");
        insertShapeMenu.setMnemonic(KeyEvent.VK_S);
        JMenuItem insertLineMenuItem = new JMenuItem("直线");
        insertLineMenuItem.addActionListener(_ -> {
            LineElement newLine = new LineElement(150, 150, 300, 200, Color.BLACK, 2);
            Command cmd = new AddElementCommand(editorPanel.getCurrentPage(), newLine);
            undoManager.executeCommand(cmd);
            editorPanel.repaint();
        });
        JMenuItem insertRectMenuItem = new JMenuItem("矩形");
        insertRectMenuItem.addActionListener(_ -> {
            RectangleElement newRect = new RectangleElement(150, 150, 150, 80, Color.BLACK, Color.LIGHT_GRAY, 1);
            Command cmd = new AddElementCommand(editorPanel.getCurrentPage(), newRect);
            undoManager.executeCommand(cmd);
            editorPanel.repaint();
        });
        JMenuItem insertCircleMenuItem = new JMenuItem("圆");
        insertCircleMenuItem.addActionListener(_ -> {
            CircleElement newCircle = new CircleElement(150, 150, 100, Color.RED, Color.ORANGE, 1);
            Command cmd = new AddElementCommand(editorPanel.getCurrentPage(), newCircle);
            undoManager.executeCommand(cmd);
            editorPanel.repaint();
        });
        JMenuItem insertOvalMenuItem = new JMenuItem("椭圆");
        insertOvalMenuItem.addActionListener(_ -> {
            OvalElement newOval = new OvalElement(150, 150, 150, 80, Color.BLUE, Color.CYAN, 1);
            Command cmd = new AddElementCommand(editorPanel.getCurrentPage(), newOval);
            undoManager.executeCommand(cmd);
            editorPanel.repaint();
        });
        insertShapeMenu.add(insertLineMenuItem);
        insertShapeMenu.add(insertRectMenuItem);
        insertShapeMenu.add(insertCircleMenuItem);
        insertShapeMenu.add(insertOvalMenuItem);
        JMenuItem insertImageMenuItem = new JMenuItem("图片(I)...");
        insertImageMenuItem.setMnemonic(KeyEvent.VK_I);
        insertImageMenuItem.addActionListener(_ -> insertImage());
        insertMenu.add(insertTextMenuItem);
        insertMenu.add(insertShapeMenu);
        insertMenu.add(insertImageMenuItem);

        JMenu formatMenu = new JMenu("格式(O)");
        formatMenu.setMnemonic(KeyEvent.VK_O);
        JMenu borderStyleMenu = new JMenu("边框样式");
        JMenuItem noBorderItem = new JMenuItem("无边框");
        noBorderItem.addActionListener(_ -> setNoBorder());
        JMenuItem solidItem = new JMenuItem("实线");
        solidItem.addActionListener(_ -> setBorderStyle(null));
        JMenuItem dashedItem = new JMenuItem("虚线");
        dashedItem.addActionListener(_ -> setBorderStyle(new float[] { 9.0f, 3.0f }));
        JMenuItem dottedItem = new JMenuItem("点线");
        dottedItem.addActionListener(_ -> setBorderStyle(new float[] { 1.0f, 2.0f }));
        borderStyleMenu.add(noBorderItem);
        borderStyleMenu.add(solidItem);
        borderStyleMenu.add(dashedItem);
        borderStyleMenu.add(dottedItem);
        formatMenu.add(borderStyleMenu);

        JMenu viewMenu = new JMenu("视图(V)");
        viewMenu.setMnemonic(KeyEvent.VK_V);
        JMenuItem themeMenuItem = new JMenuItem("更改主题颜色...");
        themeMenuItem.addActionListener(_ -> {
            ThemeChooserDialog dialog = new ThemeChooserDialog(this);
            dialog.setVisible(true);
            if (dialog.isConfirmed()) {
                applyTheme(dialog.getPrimaryColor(), dialog.getBackgroundColor());
            }
        });
        viewMenu.add(themeMenuItem);

        JMenu layoutMenu = new JMenu("应用页面布局");
        JMenuItem titleOnlyItem = new JMenuItem("仅标题");
        titleOnlyItem.addActionListener(_ -> applyLayout(PageLayout.TITLE_ONLY));
        JMenuItem titleContentItem = new JMenuItem("标题和内容");
        titleContentItem.addActionListener(_ -> applyLayout(PageLayout.TITLE_AND_CONTENT));
        JMenuItem twoColumnsItem = new JMenuItem("两栏");
        twoColumnsItem.addActionListener(_ -> applyLayout(PageLayout.TWO_COLUMNS));
        layoutMenu.add(titleOnlyItem);
        layoutMenu.add(titleContentItem);
        layoutMenu.add(twoColumnsItem);
        viewMenu.add(layoutMenu);

        JMenu slideshowMenu = new JMenu("放映(S)");
        slideshowMenu.setMnemonic(KeyEvent.VK_S);
        JMenuItem playFromStartMenuItem = new JMenuItem("从头开始");
        playFromStartMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0));
        playFromStartMenuItem.addActionListener(_ -> playSlideshow(0));
        slideshowMenu.add(playFromStartMenuItem);

        JMenu transitionMenu = new JMenu("动画(A)");
        transitionMenu.setMnemonic(KeyEvent.VK_A);
        ButtonGroup transitionGroup = new ButtonGroup();

        JRadioButtonMenuItem noneItem = new JRadioButtonMenuItem("无动画");
        noneItem.addActionListener(_ -> selectedTransition = SlideshowPlayer.Transition.NONE);

        JRadioButtonMenuItem fadeItem = new JRadioButtonMenuItem("淡入淡出", true);
        fadeItem.addActionListener(_ -> selectedTransition = SlideshowPlayer.Transition.FADE);
        JRadioButtonMenuItem slideItem = new JRadioButtonMenuItem("滑动");
        slideItem.addActionListener(_ -> selectedTransition = SlideshowPlayer.Transition.SLIDE);
        JRadioButtonMenuItem zoomItem = new JRadioButtonMenuItem("缩放");
        zoomItem.addActionListener(_ -> selectedTransition = SlideshowPlayer.Transition.ZOOM);

        transitionGroup.add(noneItem);
        transitionGroup.add(fadeItem);
        transitionGroup.add(slideItem);
        transitionGroup.add(zoomItem);

        transitionMenu.add(noneItem);
        transitionMenu.add(fadeItem);
        transitionMenu.add(slideItem);
        transitionMenu.add(zoomItem);

        menuBar.add(fileMenu);
        menuBar.add(editMenu);
        menuBar.add(insertMenu);
        menuBar.add(formatMenu);
        menuBar.add(viewMenu);
        menuBar.add(slideshowMenu);
        menuBar.add(transitionMenu);
        setJMenuBar(menuBar);
    }

    // 设置无边框
    private void setNoBorder() {
        SlideElement selected = editorPanel.getSelectedElement();
        if (selected instanceof ShapeElement shape) {
            int oldThickness = shape.getBorderThickness();
            Command cmd = new ChangeElementPropertyCommand(() -> shape.setBorderThickness(0),
                    () -> shape.setBorderThickness(oldThickness));
            undoManager.executeCommand(cmd);
            editorPanel.repaint();
        } else {
            JOptionPane.showMessageDialog(this, "请先选择一个基本图形（矩形、圆、椭圆）。");
        }
    }

    // 设置边框样式
    private void setBorderStyle(float[] dashArray) {
        SlideElement selected = editorPanel.getSelectedElement();
        if (selected instanceof ShapeElement shape) {
            float[] oldDashArray = shape.getBorderStyle();
            int oldThickness = shape.getBorderThickness();

            Command cmd = new ChangeElementPropertyCommand(() -> {
                shape.setBorderStyle(dashArray);
                if (shape.getBorderThickness() == 0) {
                    shape.setBorderThickness(1);
                }
            }, () -> {
                shape.setBorderStyle(oldDashArray);
                shape.setBorderThickness(oldThickness);
            });
            undoManager.executeCommand(cmd);
            editorPanel.repaint();
        } else {
            JOptionPane.showMessageDialog(this, "请先选择一个基本图形（矩形、圆、椭圆）。");
        }
    }

    // 创建工具栏
    private void createToolBar() {
        toolBar = new JToolBar();
        JButton colorButton = new JButton("颜色");
        colorButton.setToolTipText("设置选中元素的颜色");
        colorButton.setFocusPainted(false);
        colorButton.addActionListener(_ -> {
            SlideElement selected = editorPanel.getSelectedElement();
            if (selected == null) {
                JOptionPane.showMessageDialog(this, "请先选择一个元素。");
                return;
            }
            Color newColor = JColorChooser.showDialog(this, "选择颜色", Color.BLACK);
            if (newColor == null) {
                return;
            }

            switch (selected) {
                case TextElement textElem -> {
                    Color oldColor = textElem.getColor();
                    Command cmd = new ChangeElementPropertyCommand(() -> textElem.setColor(newColor),
                            () -> textElem.setColor(oldColor));
                    undoManager.executeCommand(cmd);
                }
                case LineElement lineElem -> {
                    Color oldColor = lineElem.getColor();
                    Command cmd = new ChangeElementPropertyCommand(() -> lineElem.setColor(newColor),
                            () -> lineElem.setColor(oldColor));
                    undoManager.executeCommand(cmd);
                }
                case ShapeElement shape -> {
                    Object[] options = { "边框", "填充" };
                    int choice = JOptionPane.showOptionDialog(this, "修改哪个部分的颜色？", "选择颜色类型", JOptionPane.YES_NO_OPTION,
                            JOptionPane.QUESTION_MESSAGE, null, options, options[0]);
                    if (choice == 0) {
                        Color oldColor = shape.getBorderColor();
                        Command cmd = new ChangeElementPropertyCommand(() -> shape.setBorderColor(newColor),
                                () -> shape.setBorderColor(oldColor));
                        undoManager.executeCommand(cmd);
                    } else {
                        Color oldColor = shape.getFillColor();
                        Command cmd = new ChangeElementPropertyCommand(() -> shape.setFillColor(newColor),
                                () -> shape.setFillColor(oldColor));
                        undoManager.executeCommand(cmd);
                    }
                }
                default -> {
                }
            }
            editorPanel.repaint();
        });
        toolBar.add(colorButton);
        toolBar.addSeparator();

        String[] fontNames = GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames();
        fontComboBox = new JComboBox<>(fontNames);
        fontComboBox.setToolTipText("选择字体");
        fontComboBox.setMaximumSize(new Dimension(150, 30));
        fontComboBox.setFocusable(false);
        fontComboBox.addActionListener(_ -> {
            String selectedFontName = (String) fontComboBox.getSelectedItem();
            if (selectedFontName != null) {
                applyFontChange(selectedFontName, -1, -1);
            }
        });
        toolBar.add(new JLabel(" 字体: "));
        toolBar.add(fontComboBox);
        toolBar.addSeparator();

        // 加粗按钮
        JButton boldButton = new JButton("B");
        boldButton.setFont(new Font("Arial", Font.BOLD, 14));
        boldButton.setToolTipText("加粗");
        boldButton.setFocusPainted(false);
        boldButton.addActionListener(_ -> applyFontChange(null, Font.BOLD, -1));
        toolBar.add(boldButton);

        // 斜体按钮
        JButton italicButton = new JButton("I");
        italicButton.setFont(new Font("Arial", Font.ITALIC, 14));
        italicButton.setToolTipText("斜体");
        italicButton.setFocusPainted(false);
        italicButton.addActionListener(_ -> applyFontChange(null, Font.ITALIC, -1));
        toolBar.add(italicButton);

        // 字号按钮
        JButton fontSizeButton = new JButton("字号");
        fontSizeButton.setToolTipText("设置字号");
        fontSizeButton.setFocusPainted(false);
        fontSizeButton.addActionListener(_ -> {
            SlideElement selected = editorPanel.getSelectedElement();
            if (selected instanceof TextElement textElem) {
                String input = JOptionPane.showInputDialog(this, "请输入字号:", textElem.getFont().getSize());
                if (input != null) {
                    try {
                        int newSize = Integer.parseInt(input);
                        if (newSize > 0) {
                            int oldSize = textElem.getFont().getSize();
                            Command cmd = new ChangeElementPropertyCommand(() -> textElem.setFontSize(newSize),
                                    () -> textElem.setFontSize(oldSize));
                            undoManager.executeCommand(cmd);
                            editorPanel.repaint();
                        } else {
                            JOptionPane.showMessageDialog(this, "字号必须大于0。");
                        }
                    } catch (NumberFormatException ex) {
                        JOptionPane.showMessageDialog(this, "请输入有效的整数。");
                    }
                }
            } else {
                JOptionPane.showMessageDialog(this, "请先选择一个文本框。");
            }
        });
        toolBar.add(fontSizeButton);

        toolBar.addSeparator();
        toolBar.add(new JLabel(" 缩放: "));
        String[] zoomLevels = { "50%", "75%", "100%", "125%", "150%", "200%" };
        JComboBox<String> zoomComboBox = new JComboBox<>(zoomLevels);
        zoomComboBox.setSelectedItem("100%");
        zoomComboBox.setMaximumSize(new Dimension(80, 30));
        zoomComboBox.setFocusable(false);
        zoomComboBox.addActionListener(_ -> {
            String selected = (String) zoomComboBox.getSelectedItem();
            if (selected != null) {
                String value = selected.replace("%", "");
                try {
                    double scale = Double.parseDouble(value) / 100.0;
                    editorPanel.setScaleFactor(scale);
                } catch (NumberFormatException ex) {
                    ex.printStackTrace();
                }
            }
        });
        toolBar.add(zoomComboBox);

        add(toolBar, BorderLayout.NORTH);
    }

    private void applyFontChange(String newName, int styleToToggle, int newSize) {
        SlideElement selected = editorPanel.getSelectedElement();
        if (selected instanceof TextElement textElem) {
            Font oldFont = textElem.getFont();

            String fontName = (newName != null) ? newName : oldFont.getName();
            int style = oldFont.getStyle();
            if (styleToToggle != -1) {
                style = style ^ styleToToggle;
            }
            int size = (newSize != -1) ? newSize : oldFont.getSize();

            Font newFont = new Font(fontName, style, size);

            Command cmd = new ChangeElementPropertyCommand(
                    () -> textElem.setFont(newFont),
                    () -> textElem.setFont(oldFont));
            undoManager.executeCommand(cmd);
            editorPanel.repaint();
        } else {
            if (styleToToggle != -1 || newName != null) {
                JOptionPane.showMessageDialog(this, "请选择一个文本框。");
            }
        }
    }

    private void createStatusBar() {
        JPanel statusBar = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 5));
        statusBar.setBorder(BorderFactory.createEtchedBorder());

        prevPageButton = new JButton("<");
        nextPageButton = new JButton(">");
        pageStatusLabel = new JLabel();
        pageStatusLabel.setHorizontalAlignment(SwingConstants.CENTER);
        pageStatusLabel.setPreferredSize(new Dimension(100, 20));

        prevPageButton.addActionListener(_ -> {
            if (slide.previousPage()) {
                editorPanel.setSlidePage(slide.getCurrentPage());
                updatePageStatus();
            }
        });

        nextPageButton.addActionListener(_ -> {
            if (slide.nextPage()) {
                editorPanel.setSlidePage(slide.getCurrentPage());
                updatePageStatus();
            }
        });

        statusBar.add(prevPageButton);
        statusBar.add(pageStatusLabel);
        statusBar.add(nextPageButton);

        add(statusBar, BorderLayout.SOUTH);
    }

    private void updatePageStatus() {
        if (slide == null || slide.getTotalPages() == 0) {
            pageStatusLabel.setText("第 0 / 0 页");
            prevPageButton.setEnabled(false);
            nextPageButton.setEnabled(false);
        } else {
            int currentPage = slide.getCurrentPageIndex() + 1;
            int totalPages = slide.getTotalPages();
            pageStatusLabel.setText("第 " + currentPage + " / " + totalPages + " 页");
            prevPageButton.setEnabled(currentPage > 1);
            nextPageButton.setEnabled(currentPage < totalPages);

            // 同步预览列表选中状态
            previewPanel.setSelectedPage(slide.getCurrentPageIndex());
        }
    }

    private void insertImage() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("选择要插入的图片");
        fileChooser.setFileFilter(new FileNameExtensionFilter("图片文件", "png", "jpg", "jpeg", "gif"));
        if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                ImageElement newImage = new ImageElement(100, 100, fileChooser.getSelectedFile().getAbsolutePath());
                Command cmd = new AddElementCommand(editorPanel.getCurrentPage(), newImage);
                undoManager.executeCommand(cmd);
                editorPanel.repaint();
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this, "图片加载失败: " + ex.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void saveSlide() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("保存幻灯片");
        fileChooser.setFileFilter(new FileNameExtensionFilter("幻灯片文件 (*.slide)", "slide"));
        if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File fileToSave = fileChooser.getSelectedFile();
            if (!fileToSave.getName().toLowerCase().endsWith(".slide")) {
                fileToSave = new File(fileToSave.getParentFile(), fileToSave.getName() + ".slide");
            }
            try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(fileToSave))) {
                oos.writeObject(slide);
                JOptionPane.showMessageDialog(this, "保存成功！");
            } catch (IOException ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(this, "保存失败: " + ex.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void openSlide() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("打开幻灯片");
        fileChooser.setFileFilter(new FileNameExtensionFilter("幻灯片文件 (*.slide)", "slide"));
        if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File fileToOpen = fileChooser.getSelectedFile();
            try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(fileToOpen))) {
                slide = (Slide) ois.readObject();
                editorPanel.setSlidePage(slide.getCurrentPage());
                undoManager.clear();

                // 更新预览列表
                previewPanel.updateSlideList(slide.getAllPages());
                previewPanel.setSelectedPage(slide.getCurrentPageIndex());

                JOptionPane.showMessageDialog(this, "打开成功！");
            } catch (IOException | ClassNotFoundException ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(this, "打开失败: " + ex.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void exportCurrentPageAsImage() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("导出为PNG图片");
        fileChooser.setFileFilter(new FileNameExtensionFilter("PNG 图片 (*.png)", "png"));
        if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File fileToSave = fileChooser.getSelectedFile();
            if (!fileToSave.getName().toLowerCase().endsWith(".png")) {
                fileToSave = new File(fileToSave.getParentFile(), fileToSave.getName() + ".png");
            }
            try {
                BufferedImage image = new BufferedImage(editorPanel.getWidth(), editorPanel.getHeight(),
                        BufferedImage.TYPE_INT_ARGB);
                Graphics2D g2d = image.createGraphics();
                editorPanel.paint(g2d);
                g2d.dispose();
                ImageIO.write(image, "png", fileToSave);
                JOptionPane.showMessageDialog(this, "导出成功！");
            } catch (IOException ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(this, "导出失败: " + ex.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void playSlideshow(int startIndex) {
        if (slide.getAllPages().isEmpty()) {
            JOptionPane.showMessageDialog(this, "没有可播放的幻灯片页面。");
            return;
        }
        SlideshowPlayer player = new SlideshowPlayer(this, slide, startIndex, selectedTransition);
        player.setVisible(true);
    }

    private void applyTheme(Color primary, Color background) {
        if (primary != null) {
            menuBar.setBackground(primary);
            toolBar.setBackground(primary);
        }
        if (background != null) {
            editorPanel.setBackground(background);
        }
    }

    private void applyLayout(PageLayout layout) {
        SlidePage currentPage = editorPanel.getCurrentPage();
        if (currentPage == null)
            return;

        List<SlideElement> newElements = new ArrayList<>();
        switch (layout) {
            case TITLE_ONLY:
                newElements.add(new TextElement("点击添加标题", 50, 50, 1100, 100));
                break;
            case TITLE_AND_CONTENT:
                newElements.add(new TextElement("点击添加标题", 50, 50, 1100, 100));
                newElements.add(new TextElement("点击添加文本", 50, 180, 1100, 550));
                break;
            case TWO_COLUMNS:
                newElements.add(new TextElement("点击添加标题", 50, 50, 1100, 100));
                newElements.add(new TextElement("点击添加文本", 50, 180, 540, 550));
                newElements.add(new TextElement("点击添加文本", 610, 180, 540, 550));
                break;
        }

        Command cmd = new ApplyLayoutCommand(currentPage, newElements);
        undoManager.executeCommand(cmd);
        editorPanel.repaint();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(PresentationApp::new);
    }
}

class ThemeChooserDialog extends JDialog {
    private Color primaryColor;
    private Color backgroundColor;
    private boolean confirmed = false;

    public ThemeChooserDialog(JFrame owner) {
        super(owner, "选择主题颜色", true);

        primaryColor = owner.getJMenuBar().getBackground();
        backgroundColor = owner.getContentPane().getBackground();

        JPanel panel = new JPanel(new GridLayout(3, 2, 10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JButton primaryButton = new JButton("选择主色调 (菜单/工具栏)");
        JLabel primaryPreview = new JLabel();
        primaryPreview.setOpaque(true);
        primaryPreview.setBackground(primaryColor);
        primaryPreview.setBorder(BorderFactory.createLineBorder(Color.BLACK));

        JButton backgroundButton = new JButton("选择背景色 (编辑区)");
        JLabel backgroundPreview = new JLabel();
        backgroundPreview.setOpaque(true);
        backgroundPreview.setBackground(backgroundColor);
        backgroundPreview.setBorder(BorderFactory.createLineBorder(Color.BLACK));

        primaryButton.addActionListener(_ -> {
            Color chosen = JColorChooser.showDialog(this, "选择主色调", primaryColor);
            if (chosen != null) {
                primaryColor = chosen;
                primaryPreview.setBackground(primaryColor);
            }
        });

        backgroundButton.addActionListener(_ -> {
            Color chosen = JColorChooser.showDialog(this, "选择背景色", backgroundColor);
            if (chosen != null) {
                backgroundColor = chosen;
                backgroundPreview.setBackground(backgroundColor);
            }
        });

        JButton okButton = new JButton("确定");
        okButton.addActionListener(_ -> {
            confirmed = true;
            dispose();
        });

        JButton cancelButton = new JButton("取消");
        cancelButton.addActionListener(_ -> dispose());

        panel.add(primaryButton);
        panel.add(primaryPreview);
        panel.add(backgroundButton);
        panel.add(backgroundPreview);
        panel.add(okButton);
        panel.add(cancelButton);

        setContentPane(panel);
        pack();
        setLocationRelativeTo(owner);
    }

    public boolean isConfirmed() {
        return confirmed;
    }

    public Color getPrimaryColor() {
        return primaryColor;
    }

    public Color getBackgroundColor() {
        return backgroundColor;
    }
}