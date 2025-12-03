// PowerDot - A simple presentation software in Java Swing
// 文件名：PresentationApp.java
// 描述：主应用程序类，包含幻灯片编辑器的主要界面和功能实现

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.GridLayout;
import java.awt.RenderingHints;
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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
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
import javax.swing.JRadioButton;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JSpinner;
import javax.swing.JToolBar;
import javax.swing.KeyStroke;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileNameExtensionFilter;

public class PresentationApp extends JFrame {
    private Slide slide;// 当前幻灯片数据对象
    private final SlideEditorPanel editorPanel; // 幻灯片编辑面板，用于编辑当前页
    private final SlidePreviewPanel previewPanel; // 幻灯片预览面板，显示所有页
    private final UndoManager undoManager = new UndoManager(); // 撤销管理器

    private JButton prevPageButton; // 上一页按钮
    private JButton nextPageButton; // 下一页按钮
    private JLabel pageStatusLabel; // 页面状态标签显示当前页
    private SlideshowPlayer.Transition selectedTransition = SlideshowPlayer.Transition.FADE;// 默认切换效果为淡入淡出
    private JComboBox<String> fontComboBox; // 字体选择下拉框

    private JMenuBar menuBar;
    private Set<String> availableFontSet = new HashSet<>(); // 可用字体集合，用于验证用户输入
    private String lastFontName; // 上次有效的字体名称，用于回退
    private JToolBar toolBar;

    private File currentFile = null; // 当前打开的文件路径
    private boolean isModified = false; // 标记文档是否已修改

    public enum PageLayout // 页面布局枚举
    {
        TITLE_ONLY, // 仅标题
        TITLE_AND_CONTENT, // 标题和内容
        TWO_COLUMNS // 两栏
    }

    public UndoManager getUndoManager() {
        return undoManager;
    }

    public Slide getSlide() {
        return slide;
    }

    // 构造函数
    public PresentationApp() {
        setTitle("PowerDot");
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE); // 自定义关闭操作，提示保存
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                exitApp();
            }
        });
        setSize(1280, 720);
        setLocationRelativeTo(null); // 窗口居中显示

        // 初始化幻灯片
        slide = new Slide();
        slide.addPage(new SlidePage());

        createMenuBar();
        createToolBar();

        // 创建中央编辑面板
        editorPanel = new SlideEditorPanel(slide);
        editorPanel.setBackground(Color.WHITE);
        add(editorPanel, BorderLayout.CENTER);

        // 创建左侧预览面板
        previewPanel = new SlidePreviewPanel(this);
        previewPanel.updateSlideList(slide.getAllPages());
        previewPanel.setSelectedPage(0);
        add(previewPanel, BorderLayout.WEST);

        // 监听撤销管理器的状态变化，更新界面
        undoManager.addListener(() -> {
            isModified = true; // 标记为已修改
            previewPanel.refreshPreviews();
            editorPanel.repaint(); // 重绘画布
        });

        createStatusBar();
        setVisible(true);
        updatePageStatus();

        // 延迟执行，确保界面已显示
        SwingUtilities.invokeLater(() -> {
            editorPanel.zoomToFit();
            // 初始化字体选择框，确保在界面显示后设置默认值
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
        fileMenu.setMnemonic(KeyEvent.VK_F);
        JMenuItem newMenuItem = new JMenuItem("新建(N)");
        newMenuItem.setMnemonic(KeyEvent.VK_N);
        newMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N, InputEvent.CTRL_DOWN_MASK));
        newMenuItem.addActionListener(e-> {
            if (!confirmSaveIfNeeded())
                return;

            this.slide = new Slide();
            this.slide.addPage(new SlidePage());
            editorPanel.setSlide(this.slide);
            undoManager.clear();
            isModified = false;
            currentFile = null;

            previewPanel.updateSlideList(slide.getAllPages());
            previewPanel.setSelectedPage(0);

            updatePageStatus();
        });
        JMenuItem openMenuItem = new JMenuItem("打开(O)...");
        openMenuItem.setMnemonic(KeyEvent.VK_O);
        openMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, InputEvent.CTRL_DOWN_MASK));
        openMenuItem.addActionListener(e-> {
            if (!confirmSaveIfNeeded())
                return;
            openSlide();
            updatePageStatus();
        });
        JMenuItem saveMenuItem = new JMenuItem("保存(S)...");
        saveMenuItem.setMnemonic(KeyEvent.VK_S);
        saveMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK));
        saveMenuItem.addActionListener(e-> saveSlide());

        JMenuItem saveAsMenuItem = new JMenuItem("另存为(A)...");
        saveAsMenuItem.setMnemonic(KeyEvent.VK_A);
        saveAsMenuItem.addActionListener(e-> saveSlideAs());

        JMenuItem pageSetupMenuItem = new JMenuItem("页面设置(P)...");
        pageSetupMenuItem.setMnemonic(KeyEvent.VK_P);
        pageSetupMenuItem.addActionListener(e-> showPageSetupDialog());

        JMenuItem exportImageMenuItem = new JMenuItem("导出为图片(E)...");
        exportImageMenuItem.setMnemonic(KeyEvent.VK_E);
        exportImageMenuItem.addActionListener(e-> exportCurrentPageAsImage());
        JMenuItem exportPDFMenuItem = new JMenuItem("导出为PDF...");
        exportPDFMenuItem.addActionListener(e-> exportToPDF());
        fileMenu.add(newMenuItem);
        fileMenu.addSeparator();
        fileMenu.add(openMenuItem);
        fileMenu.add(saveMenuItem);
        fileMenu.add(saveAsMenuItem);
        fileMenu.addSeparator();
        fileMenu.add(pageSetupMenuItem);
        fileMenu.addSeparator();
        fileMenu.add(exportImageMenuItem);
        fileMenu.add(exportPDFMenuItem);

        JMenu editMenu = new JMenu("编辑(E)");
        editMenu.setMnemonic(KeyEvent.VK_E);
        JMenuItem undoMenuItem = new JMenuItem("撤销(U)");
        undoMenuItem.setMnemonic(KeyEvent.VK_U);
        undoMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Z, InputEvent.CTRL_DOWN_MASK));
        undoMenuItem.addActionListener(e-> {
            undoManager.undo();
            editorPanel.repaint();
        });
        JMenuItem redoMenuItem = new JMenuItem("重做(R)");
        redoMenuItem.setMnemonic(KeyEvent.VK_R);
        redoMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Y, InputEvent.CTRL_DOWN_MASK));
        redoMenuItem.addActionListener(e-> {
            undoManager.redo();
            editorPanel.repaint();
        });
        editMenu.add(undoMenuItem);
        editMenu.add(redoMenuItem);

        JMenuItem deleteMenuItem = new JMenuItem("删除(D)");
        deleteMenuItem.setMnemonic(KeyEvent.VK_D);
        deleteMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0));
        deleteMenuItem.addActionListener(e-> editorPanel.deleteSelectedElement());
        editMenu.add(deleteMenuItem);

        editMenu.addSeparator();
        JMenuItem newPageMenuItem = new JMenuItem("新建空白页面(P)");
        newPageMenuItem.setMnemonic(KeyEvent.VK_P);
        newPageMenuItem.addActionListener(e-> {
            SlidePage newPage = new SlidePage();
            this.slide.addPage(newPage);
            this.slide.setCurrentPageIndex(this.slide.getTotalPages() - 1);
            editorPanel.setSlidePage(newPage);

            previewPanel.updateSlideList(slide.getAllPages());
            previewPanel.setSelectedPage(slide.getCurrentPageIndex());

            updatePageStatus();
        });
        editMenu.add(newPageMenuItem);

        JMenuItem backgroundMenuItem = new JMenuItem("页面背景...");
        backgroundMenuItem.addActionListener(e -> showPageBackgroundDialog());
        editMenu.add(backgroundMenuItem);

        JMenu insertMenu = new JMenu("插入(I)");
        insertMenu.setMnemonic(KeyEvent.VK_I);
        JMenuItem insertTextMenuItem = new JMenuItem("文本框(T)");
        insertTextMenuItem.setMnemonic(KeyEvent.VK_T);
        insertTextMenuItem.addActionListener(e-> {
            TextElement newText = new TextElement("", 100, 100, 200, 40);
            Command cmd = new AddElementCommand(editorPanel.getCurrentPage(), newText);
            undoManager.executeCommand(cmd);
            editorPanel.selectElement(newText);
            editorPanel.startEditingText(newText);
            editorPanel.repaint();
        });
        JMenu insertShapeMenu = new JMenu("基本形状(S)");
        insertShapeMenu.setMnemonic(KeyEvent.VK_S);
        JMenuItem insertLineMenuItem = new JMenuItem("直线");
        insertLineMenuItem.addActionListener(e-> {
            LineElement newLine = new LineElement(150, 150, 300, 200, Color.BLACK, 2);
            Command cmd = new AddElementCommand(editorPanel.getCurrentPage(), newLine);
            undoManager.executeCommand(cmd);
            editorPanel.repaint();
        });
        JMenuItem insertRectMenuItem = new JMenuItem("矩形");
        insertRectMenuItem.addActionListener(e-> {
            RectangleElement newRect = new RectangleElement(150, 150, 150, 80, Color.BLACK, Color.LIGHT_GRAY, 1);
            Command cmd = new AddElementCommand(editorPanel.getCurrentPage(), newRect);
            undoManager.executeCommand(cmd);
            editorPanel.repaint();
        });
        JMenuItem insertCircleMenuItem = new JMenuItem("圆形");
        insertCircleMenuItem.addActionListener(e-> {
            CircleElement newCircle = new CircleElement(150, 150, 100, Color.RED, Color.ORANGE, 1);
            Command cmd = new AddElementCommand(editorPanel.getCurrentPage(), newCircle);
            undoManager.executeCommand(cmd);
            editorPanel.repaint();
        });
        JMenuItem insertOvalMenuItem = new JMenuItem("椭圆");
        insertOvalMenuItem.addActionListener(e-> {
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
        insertImageMenuItem.addActionListener(e-> insertImage());
        insertMenu.add(insertTextMenuItem);
        insertMenu.add(insertShapeMenu);
        insertMenu.add(insertImageMenuItem);

        JMenu formatMenu = new JMenu("格式(O)");
        formatMenu.setMnemonic(KeyEvent.VK_O);
        JMenu borderStyleMenu = new JMenu("边框样式");
        JMenuItem noBorderItem = new JMenuItem("无边框");
        noBorderItem.addActionListener(e-> setNoBorder());
        JMenuItem solidItem = new JMenuItem("实线");
        solidItem.addActionListener(e-> setBorderStyle(null));
        JMenuItem dashedItem = new JMenuItem("虚线");
        dashedItem.addActionListener(e-> setBorderStyle(new float[] { 9.0f, 3.0f }));
        JMenuItem dottedItem = new JMenuItem("点线");
        dottedItem.addActionListener(e-> setBorderStyle(new float[] { 1.0f, 2.0f }));
        borderStyleMenu.add(noBorderItem);
        borderStyleMenu.add(solidItem);
        borderStyleMenu.add(dashedItem);
        borderStyleMenu.add(dottedItem);
        formatMenu.add(borderStyleMenu);

        JMenu viewMenu = new JMenu("视图(V)");
        viewMenu.setMnemonic(KeyEvent.VK_V);
        JMenuItem themeMenuItem = new JMenuItem("更改主题颜色...");
        themeMenuItem.addActionListener(e-> {
            ThemeChooserDialog dialog = new ThemeChooserDialog(this);
            dialog.setVisible(true);
            if (dialog.isConfirmed()) {
                applyTheme(dialog.getPrimaryColor(), dialog.getBackgroundColor());
            }
        });
        viewMenu.add(themeMenuItem);

        JMenuItem zoomMenuItem = new JMenuItem("显示比例...");
        zoomMenuItem.addActionListener(e -> showZoomDialog());
        viewMenu.add(zoomMenuItem);

        JMenuItem resetZoomMenuItem = new JMenuItem("重排大小");
        resetZoomMenuItem.addActionListener(e -> resetZoomTo100());
        viewMenu.add(resetZoomMenuItem);

        JMenu layoutMenu = new JMenu("应用页面布局");
        JMenuItem titleOnlyItem = new JMenuItem("仅标题");
        titleOnlyItem.addActionListener(e-> applyLayout(PageLayout.TITLE_ONLY));
        JMenuItem titleContentItem = new JMenuItem("标题和内容");
        titleContentItem.addActionListener(e-> applyLayout(PageLayout.TITLE_AND_CONTENT));
        JMenuItem twoColumnsItem = new JMenuItem("两栏");
        twoColumnsItem.addActionListener(e-> applyLayout(PageLayout.TWO_COLUMNS));
        layoutMenu.add(titleOnlyItem);
        layoutMenu.add(titleContentItem);
        layoutMenu.add(twoColumnsItem);
        viewMenu.add(layoutMenu);

        JMenu slideshowMenu = new JMenu("放映(S)");
        slideshowMenu.setMnemonic(KeyEvent.VK_S);
        JMenuItem playFromStartMenuItem = new JMenuItem("从头开始放映");
        playFromStartMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0));
        playFromStartMenuItem.addActionListener(e-> playSlideshow(0));
        slideshowMenu.add(playFromStartMenuItem);

        JMenu transitionMenu = new JMenu("切换(A)");
        transitionMenu.setMnemonic(KeyEvent.VK_A);
        ButtonGroup transitionGroup = new ButtonGroup();

        JRadioButtonMenuItem noneItem = new JRadioButtonMenuItem("无动画");
        noneItem.addActionListener(e-> selectedTransition = SlideshowPlayer.Transition.NONE);

        JRadioButtonMenuItem fadeItem = new JRadioButtonMenuItem("渐入渐出", true);
        fadeItem.addActionListener(e-> selectedTransition = SlideshowPlayer.Transition.FADE);
        JRadioButtonMenuItem slideItem = new JRadioButtonMenuItem("滑动");
        slideItem.addActionListener(e-> selectedTransition = SlideshowPlayer.Transition.SLIDE);
        JRadioButtonMenuItem zoomItem = new JRadioButtonMenuItem("缩放");
        zoomItem.addActionListener(e-> selectedTransition = SlideshowPlayer.Transition.ZOOM);

        transitionGroup.add(noneItem);
        transitionGroup.add(fadeItem);
        transitionGroup.add(slideItem);
        transitionGroup.add(zoomItem);

        transitionMenu.add(noneItem);
        transitionMenu.add(fadeItem);
        transitionMenu.add(slideItem);
        transitionMenu.add(zoomItem);

        JMenu helpMenu = new JMenu("帮助(H)");
        helpMenu.setMnemonic(KeyEvent.VK_H);

        JMenuItem tutorialMenuItem = new JMenuItem("使用教程(T)");
        tutorialMenuItem.setMnemonic(KeyEvent.VK_T);
        tutorialMenuItem.addActionListener(e-> showTutorial());
        helpMenu.add(tutorialMenuItem);

        JMenuItem aboutMenuItem = new JMenuItem("关于(A)");
        aboutMenuItem.setMnemonic(KeyEvent.VK_A);
        aboutMenuItem.addActionListener(e-> JOptionPane.showMessageDialog(this,
                "Java Swing 开发的轻量级幻燈片工具 PowerDot。\n作者：PowerDot 团队\n版本：2025.11",
                "关于 PowerDot",
                JOptionPane.INFORMATION_MESSAGE));
        helpMenu.add(aboutMenuItem);

        menuBar.add(fileMenu);
        menuBar.add(editMenu);
        menuBar.add(insertMenu);
        menuBar.add(formatMenu);
        menuBar.add(viewMenu);
        menuBar.add(slideshowMenu);
        menuBar.add(transitionMenu);
        menuBar.add(helpMenu);
        setJMenuBar(menuBar);
    }
private void setNoBorder() {
        SlideElement selected = editorPanel.getSelectedElement();
        if (selected instanceof ShapeElement shape) {
            int oldThickness = shape.getBorderThickness();
            Command cmd = new ChangeElementPropertyCommand(() -> shape.setBorderThickness(0),
                    () -> shape.setBorderThickness(oldThickness));
            undoManager.executeCommand(cmd);
            editorPanel.repaint();
        } else {
            JOptionPane.showMessageDialog(this, "请选择一个形状元素来设置边框样式。");
        }
    }

    // 设置边框样式（虚线、点线等）
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
        } else if (selected instanceof TextElement text) {
        float[] oldDash = text.getBorderStyle();
        int oldThick = text.getBorderThickness();
        Command cmd = new ChangeElementPropertyCommand(() -> {
            text.setBorderStyle(dashArray);
            if (text.getBorderThickness() == 0) text.setBorderThickness(1);
        }, () -> {
            text.setBorderStyle(oldDash);
            text.setBorderThickness(oldThick);
        });
        undoManager.executeCommand(cmd);
        editorPanel.repaint();
    } else {
            JOptionPane.showMessageDialog(this, "请选择一个形状元素来设置边框样式。");
        }
    }

    // 创建工具栏
    private void createToolBar() {
        toolBar = new JToolBar();
        JButton colorButton = new JButton("颜色");
        colorButton.setToolTipText("更改选中元素的颜色");
        colorButton.setFocusPainted(false);
        colorButton.addActionListener(e-> {
            SlideElement selected = editorPanel.getSelectedElement();
            if (selected == null) {
                JOptionPane.showMessageDialog(this, "请先选择一个元素。");
                return;
            }

            Color baseColor = Color.BLACK;
            Integer shapeChoice = null;
            if (selected instanceof TextElement textElem) {
                baseColor = textElem.getColor();
            } else if (selected instanceof LineElement lineElem) {
                baseColor = lineElem.getColor();
            } else if (selected instanceof ShapeElement shape) {
                Object[] options = { "边框颜色", "填充颜色" };
                shapeChoice = JOptionPane.showOptionDialog(this, "选择要更改的颜色类型", "颜色选择", JOptionPane.YES_NO_OPTION,
                        JOptionPane.QUESTION_MESSAGE, null, options, options[0]);
                if (shapeChoice == JOptionPane.CLOSED_OPTION) {
                    return;
                }
                baseColor = shapeChoice == 0 ? shape.getBorderColor() : shape.getFillColor();
            }

            Color newColor = pickColor(baseColor, "颜色选择");
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
                    if (shapeChoice != null && shapeChoice == 0) {
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
        // 将可用字体名称存入集合，用于验证用户输入
        availableFontSet.clear();
        for (String name : fontNames) {
            availableFontSet.add(name.toLowerCase());
        }
        fontComboBox = new JComboBox<>(fontNames);
        fontComboBox.setToolTipText("选择字体");
        fontComboBox.setMaximumSize(new Dimension(200, 30));
        fontComboBox.setPreferredSize(new Dimension(200, 30));
        fontComboBox.setFocusable(false);
        fontComboBox.setEditable(true); // 允许用户输入自定义字体，如PPT
        // 如果系统有可用字体，设置默认选中第一个
        if (fontNames.length > 0) {
            fontComboBox.setSelectedItem(fontNames[0]);
            lastFontName = fontNames[0];
        }
        fontComboBox.addActionListener(e-> {
            String selectedFontName = (String) fontComboBox.getSelectedItem();
            if (selectedFontName != null) {
                String trimmed = selectedFontName.trim();
                if (trimmed.isEmpty())
                    return;
                if (availableFontSet.contains(trimmed.toLowerCase())) {
                    lastFontName = trimmed;
                    applyFontChange(trimmed, -1, -1);
                } else {
                    JOptionPane.showMessageDialog(this, "字体 " + trimmed + " 不可用", "字体错误",
                            JOptionPane.WARNING_MESSAGE);
                    if (lastFontName != null) {
                        fontComboBox.setSelectedItem(lastFontName);
                    }
                }
            }
        });
        toolBar.add(new JLabel(" 字体："));
        toolBar.add(fontComboBox);
        toolBar.addSeparator();

        // 粗体按钮
        JButton boldButton = new JButton("B");
        boldButton.setFont(new Font("Arial", Font.BOLD, 14));
        boldButton.setToolTipText("粗体");
        boldButton.setFocusPainted(false);
        boldButton.addActionListener(e-> applyFontChange(null, Font.BOLD, -1));
        toolBar.add(boldButton);

        // 斜体按钮
        JButton italicButton = new JButton("I");
        italicButton.setFont(new Font("Arial", Font.ITALIC, 14));
        italicButton.setToolTipText("斜体");
        italicButton.setFocusPainted(false);
        italicButton.addActionListener(e-> applyFontChange(null, Font.ITALIC, -1));
        toolBar.add(italicButton);

        // 字体大小按钮
        JButton fontSizeButton = new JButton("字体大小");
        fontSizeButton.setToolTipText("更改字体大小");
        fontSizeButton.setFocusPainted(false);
        fontSizeButton.addActionListener(e-> {
            SlideElement selected = editorPanel.getSelectedElement();
            if (selected instanceof TextElement textElem) {
                String input = JOptionPane.showInputDialog(this, "输入字体大小", textElem.getFont().getSize());
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
                            JOptionPane.showMessageDialog(this, "字体大小必须大于0。");
                        }
                    } catch (NumberFormatException ex) {
                        JOptionPane.showMessageDialog(this, "请输入有效的数字。");
                    }
                }
            } else {
                JOptionPane.showMessageDialog(this, "请选择一个文本元素。");
            }
        });
        // toolBar.add(fontSizeButton); // 暂时注释掉，用下拉框代替
        // 创建字体大小下拉框，提供常用字号
        Integer[] commonSizes = new Integer[] { 10, 11, 12, 14, 16, 18, 20, 22, 24, 28, 32, 36, 40, 48, 56, 64, 72 };
        JComboBox<Integer> fontSizeComboBox = new JComboBox<>(commonSizes);
        fontSizeComboBox.setEditable(true);
        fontSizeComboBox.setMaximumSize(new Dimension(80, 30));
        fontSizeComboBox.setPreferredSize(new Dimension(80, 30));
        fontSizeComboBox.setToolTipText("选择或输入字体大小");
        fontSizeComboBox.addActionListener(e-> {
            Object sel = fontSizeComboBox.getSelectedItem();
            if (sel == null)
                return;
            try {
                int newSize = Integer.parseInt(sel.toString().trim());
                if (newSize <= 0) {
                    JOptionPane.showMessageDialog(this, "字体大小必须大于0。");
                    return;
                }
                SlideElement selected = editorPanel.getSelectedElement();
                if (selected instanceof TextElement textElem) {
                    int oldSize = textElem.getFont().getSize();
                    Command cmd = new ChangeElementPropertyCommand(() -> textElem.setFontSize(newSize),
                            () -> textElem.setFontSize(oldSize));
                    undoManager.executeCommand(cmd);
                    editorPanel.repaint();
                } else {
                    JOptionPane.showMessageDialog(this, "请选择一个文本元素。");
                }
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "请输入有效的数字。");
            }
        });
        toolBar.add(new JLabel(" 字号："));
        toolBar.add(fontSizeComboBox);

        // 边框粗细按钮
        JButton borderThicknessButton = new JButton("边框粗细");
        borderThicknessButton.setToolTipText("更改边框或线条粗细");
        borderThicknessButton.setFocusPainted(false);
        borderThicknessButton.addActionListener(e-> {
            SlideElement selected = editorPanel.getSelectedElement();
            if (selected instanceof ShapeElement shape) {
                String input = JOptionPane.showInputDialog(this, "输入边框粗细", shape.getBorderThickness());
                if (input != null) {
                    try {
                        int newThickness = Integer.parseInt(input);
                        if (newThickness >= 0) {
                            int oldThickness = shape.getBorderThickness();
                            Command cmd = new ChangeElementPropertyCommand(() -> shape.setBorderThickness(newThickness),
                                    () -> shape.setBorderThickness(oldThickness));
                            undoManager.executeCommand(cmd);
                            editorPanel.repaint();
                        } else {
                            JOptionPane.showMessageDialog(this, "边框粗细必须大于等于0。");
                        }
                    } catch (NumberFormatException ex) {
                        JOptionPane.showMessageDialog(this, "请输入有效的数字。");
                    }
                }
            } else if (selected instanceof LineElement line) {
                String input = JOptionPane.showInputDialog(this, "输入线条粗细", line.getThickness());
                if (input != null) {
                    try {
                        int newThickness = Integer.parseInt(input);
                        if (newThickness > 0) {
                            int oldThickness = line.getThickness();
                            Command cmd = new ChangeElementPropertyCommand(() -> line.setThickness(newThickness),
                                    () -> line.setThickness(oldThickness));
                            undoManager.executeCommand(cmd);
                            editorPanel.repaint();
                        } else {
                            JOptionPane.showMessageDialog(this, "线条粗细必须大于0。");
                        }
                    } catch (NumberFormatException ex) {
                        JOptionPane.showMessageDialog(this, "请输入有效的数字。");
                    }
                }
            } else {
                JOptionPane.showMessageDialog(this, "请选择一个形状或线条元素。");
            }
        });
        toolBar.add(borderThicknessButton);

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
                JOptionPane.showMessageDialog(this, "请选择一个文本元素来更改字体。");
            }
        }
    }

    private void createStatusBar() {
        JPanel statusBar = new JPanel(new BorderLayout());
        statusBar.setBorder(BorderFactory.createEtchedBorder());

        // Navigation Panel (Center)
        JPanel navPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 5));
        prevPageButton = new JButton("<");
        nextPageButton = new JButton(">");
        pageStatusLabel = new JLabel();
        pageStatusLabel.setHorizontalAlignment(SwingConstants.CENTER);
        pageStatusLabel.setPreferredSize(new Dimension(100, 20));

        prevPageButton.addActionListener(e-> {
            if (slide.previousPage()) {
                editorPanel.setSlidePage(slide.getCurrentPage());
                updatePageStatus();
            }
        });

        nextPageButton.addActionListener(e-> {
            if (slide.nextPage()) {
                editorPanel.setSlidePage(slide.getCurrentPage());
                updatePageStatus();
            }
        });

        navPanel.add(prevPageButton);
        navPanel.add(pageStatusLabel);
        navPanel.add(nextPageButton);
        statusBar.add(navPanel, BorderLayout.CENTER);

        // Zoom Panel (Right)
        JPanel zoomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 5));
        zoomPanel.add(new JLabel("缩放："));
        String[] zoomLevels = { "50%", "75%", "100%", "125%", "150%", "200%" };
        JComboBox<String> zoomComboBox = new JComboBox<>(zoomLevels);
        zoomComboBox.setSelectedItem("100%");
        zoomComboBox.setPreferredSize(new Dimension(80, 25));
        zoomComboBox.setFocusable(false);
        zoomComboBox.addActionListener(e-> {
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
        zoomPanel.add(zoomComboBox);
        statusBar.add(zoomPanel, BorderLayout.EAST);

        add(statusBar, BorderLayout.SOUTH);
    }

    private void updatePageStatus() {
        if (slide == null || slide.getTotalPages() == 0) {
            pageStatusLabel.setText("第0 / 0 页");
            prevPageButton.setEnabled(false);
            nextPageButton.setEnabled(false);
        } else {
            int currentPage = slide.getCurrentPageIndex() + 1;
            int totalPages = slide.getTotalPages();
            pageStatusLabel.setText("第" + currentPage + " / " + totalPages + " 页");
            prevPageButton.setEnabled(currentPage > 1);
            nextPageButton.setEnabled(currentPage < totalPages);

            // 更新预览面板的选中状态
            previewPanel.setSelectedPage(slide.getCurrentPageIndex());
        }
    }

    private void insertImage() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("选择要插入的图片");
        fileChooser.setFileFilter(new FileNameExtensionFilter("图像文件", "png", "jpg", "jpeg", "gif"));
        if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                BufferedImage image = ImageIO.read(fileChooser.getSelectedFile());
                if (image != null) {
                    ImageElement newImage = new ImageElement(100, 100, image);
                    Command cmd = new AddElementCommand(editorPanel.getCurrentPage(), newImage);
                    undoManager.executeCommand(cmd);
                    editorPanel.repaint();
                } else {
                    JOptionPane.showMessageDialog(this, "无法读取图像文件。", "错误", JOptionPane.ERROR_MESSAGE);
                }
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this, "读取图像文件失败: " + ex.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private boolean saveSlide() {
        if (currentFile != null) {
            return saveToFile(currentFile);
        } else {
            return saveSlideAs();
        }
    }

    private boolean saveSlideAs() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("保存幻灯片");
        fileChooser.setFileFilter(new FileNameExtensionFilter("幻灯片文件(*.slide)", "slide"));
        if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File fileToSave = fileChooser.getSelectedFile();
            if (!fileToSave.getName().toLowerCase().endsWith(".slide")) {
                fileToSave = new File(fileToSave.getParentFile(), fileToSave.getName() + ".slide");
            }
            currentFile = fileToSave;
            return saveToFile(currentFile);
        }
        return false;
    }

    private boolean saveToFile(File file) {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(file))) {
            oos.writeObject(slide);
            isModified = false;
            JOptionPane.showMessageDialog(this, "保存成功。");
            return true;
        } catch (IOException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "保存失败: " + ex.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
            return false;
        }
    }

    private void openSlide() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("打开幻灯片文件");
        fileChooser.setFileFilter(new FileNameExtensionFilter("幻灯片文件(*.slide)", "slide"));
        if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File fileToOpen = fileChooser.getSelectedFile();
            try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(fileToOpen))) {
                slide = (Slide) ois.readObject();
                editorPanel.setSlide(slide);
                undoManager.clear();
                isModified = false;

                currentFile = fileToOpen;

                // 更新预览面板
                previewPanel.updateSlideList(slide.getAllPages());
                previewPanel.setSelectedPage(slide.getCurrentPageIndex());

                currentFile = fileToOpen; // 更新当前文件引用

                JOptionPane.showMessageDialog(this, "打开成功。");
            } catch (java.io.InvalidClassException ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(this, "文件版本不兼容，可能由不同版本的PowerDot创建。" + ex.getMessage(), "版本错误",
                        JOptionPane.ERROR_MESSAGE);
            } catch (IOException | ClassNotFoundException ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(this, "打开失败: " + ex.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void exportCurrentPageAsImage() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("导出当前页为PNG图片");
        fileChooser.setFileFilter(new FileNameExtensionFilter("PNG 图片(*.png)", "png"));
        if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File fileToSave = fileChooser.getSelectedFile();
            if (!fileToSave.getName().toLowerCase().endsWith(".png")) {
                fileToSave = new File(fileToSave.getParentFile(), fileToSave.getName() + ".png");
            }
            try {
                int width = slide.getWidth();
                int height = slide.getHeight();
                BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
                Graphics2D g2d = image.createGraphics();

                // 设置抗锯齿
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

                SlidePage currentPage = slide.getCurrentPage();
                if (currentPage != null) {
                    // 绘制页面背景
                    currentPage.renderBackground(g2d, width, height);

                    // 绘制页面元素
                    for (SlideElement element : currentPage.getElements()) {
                        element.draw(g2d);
                    }
                } else {
                    g2d.setColor(Color.WHITE);
                    g2d.fillRect(0, 0, width, height);
                }

                g2d.dispose();
                ImageIO.write(image, "png", fileToSave);
                JOptionPane.showMessageDialog(this, "导出成功。");
            } catch (IOException ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(this, "导出失败: " + ex.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void exportToPDF() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("导出为PDF");
        fileChooser.setFileFilter(new FileNameExtensionFilter("PDF 文档(*.pdf)", "pdf"));
        if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File fileToSave = fileChooser.getSelectedFile();
            if (!fileToSave.getName().toLowerCase().endsWith(".pdf")) {
                fileToSave = new File(fileToSave.getParentFile(), fileToSave.getName() + ".pdf");
            }
            try {
                SimplePdfExporter.export(slide, fileToSave);
                JOptionPane.showMessageDialog(this, "导出成功。");
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

    /** 显示比例对话框，提供常用比例和自定义百分比。 */
    private void showZoomDialog() {
        JDialog dialog = new JDialog(this, "显示比例", true);
        dialog.setLayout(new BorderLayout(10, 10));

        JPanel options = new JPanel();
        options.setLayout(new BoxLayout(options, BoxLayout.Y_AXIS));
        options.setBorder(BorderFactory.createEmptyBorder(10, 12, 0, 12));

        ButtonGroup group = new ButtonGroup();
        JRadioButton best = new JRadioButton("最佳(F)");
        JRadioButton p400 = new JRadioButton("400%");
        JRadioButton p200 = new JRadioButton("200%");
        JRadioButton p100 = new JRadioButton("100%");
        JRadioButton p66 = new JRadioButton("66%");
        JRadioButton p50 = new JRadioButton("50%");
        JRadioButton p33 = new JRadioButton("33%");
        JRadioButton customRadio = new JRadioButton();

        group.add(best);
        group.add(p400);
        group.add(p200);
        group.add(p100);
        group.add(p66);
        group.add(p50);
        group.add(p33);
        group.add(customRadio);

        int currentPercent = (int) Math.round(editorPanel.getScaleFactor() * 100);
        if (Math.abs(currentPercent - 400) < 1) {
            p400.setSelected(true);
        } else if (Math.abs(currentPercent - 200) < 1) {
            p200.setSelected(true);
        } else if (Math.abs(currentPercent - 100) < 1) {
            p100.setSelected(true);
        } else if (Math.abs(currentPercent - 66) < 1) {
            p66.setSelected(true);
        } else if (Math.abs(currentPercent - 50) < 1) {
            p50.setSelected(true);
        } else if (Math.abs(currentPercent - 33) < 1) {
            p33.setSelected(true);
        } else {
            customRadio.setSelected(true);
        }

        JSpinner customSpinner = new JSpinner(new SpinnerNumberModel(currentPercent, 10, 800, 1));
        customSpinner.addChangeListener(e -> customRadio.setSelected(true));

        options.add(wrapRadio(best));
        options.add(wrapRadio(p400));
        options.add(wrapRadio(p200));
        options.add(wrapRadio(p100));
        options.add(wrapRadio(p66));
        options.add(wrapRadio(p50));
        options.add(wrapRadio(p33));

        JPanel customRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        customRow.add(customRadio);
        customRow.add(new JLabel("百分比(P)"));
        customRow.add(customSpinner);
        customRow.add(new JLabel("%"));
        options.add(customRow);

        dialog.add(options, BorderLayout.CENTER);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 8));
        JButton ok = new JButton("确定");
        JButton cancel = new JButton("取消");
        ok.addActionListener(e -> {
            if (best.isSelected()) {
                editorPanel.zoomToFit();
            } else if (p400.isSelected()) {
                applyZoomPercent(400);
            } else if (p200.isSelected()) {
                applyZoomPercent(200);
            } else if (p100.isSelected()) {
                applyZoomPercent(100);
            } else if (p66.isSelected()) {
                applyZoomPercent(66);
            } else if (p50.isSelected()) {
                applyZoomPercent(50);
            } else if (p33.isSelected()) {
                applyZoomPercent(33);
            } else {
                applyZoomPercent((int) customSpinner.getValue());
            }
            dialog.dispose();
        });
        cancel.addActionListener(e -> dialog.dispose());
        actions.add(ok);
        actions.add(cancel);
        dialog.add(actions, BorderLayout.SOUTH);

        dialog.pack();
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    private JPanel wrapRadio(JRadioButton btn) {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        row.add(btn);
        return row;
    }

    private void applyZoomPercent(int percent) {
        editorPanel.setZoomPercent(percent);
        editorPanel.requestFocusInWindow();
    }

    private void resetZoomTo100() {
        editorPanel.zoomToFit();
    }

    private Color pickColor(Color baseColor, String title) {
        Color fallback = baseColor != null ? baseColor : Color.WHITE;
        return NewColorPickerDialog.pickColor(this, fallback, title);
    }

    private void showPageBackgroundDialog() {
        SlidePage currentPage = editorPanel.getCurrentPage();
        if (currentPage == null)
            return;

        PageBackgroundDialog dialog = new PageBackgroundDialog(this, currentPage);
        dialog.setVisible(true);
        if (dialog.isConfirmed()) {
            currentPage.setBackgroundMode(dialog.getMode());
            currentPage.setBackgroundColor(dialog.getSolidColor());
            currentPage.setGradientStart(dialog.getGradientStart());
            currentPage.setGradientEnd(dialog.getGradientEnd());
            currentPage.setBackgroundImage(dialog.getSelectedImage());
            editorPanel.repaint();
            previewPanel.updateSlideList(slide.getAllPages());
            previewPanel.setSelectedPage(slide.getCurrentPageIndex());
        }
    }

    private void applyLayout(PageLayout layout) {
        SlidePage currentPage = editorPanel.getCurrentPage();
        if (currentPage == null)
            return;

        List<SlideElement> newElements = new ArrayList<>();
        switch (layout) {
            case TITLE_ONLY:
                newElements.add(new TextElement("标题文本", 50, 50, 1100, 100));
                break;
            case TITLE_AND_CONTENT:
                newElements.add(new TextElement("标题文本", 50, 50, 1100, 100));
                newElements.add(new TextElement("内容文本", 50, 180, 1100, 550));
                break;
            case TWO_COLUMNS:
                newElements.add(new TextElement("标题文本", 50, 50, 1100, 100));
                newElements.add(new TextElement("内容文本", 50, 180, 540, 550));
                newElements.add(new TextElement("内容文本", 610, 180, 540, 550));
                break;
        }

        Command cmd = new ApplyLayoutCommand(currentPage, newElements);
        undoManager.executeCommand(cmd);
        editorPanel.repaint();
    }

    private void showPageSetupDialog() {
        javax.swing.JTextField widthField = new javax.swing.JTextField(String.valueOf(slide.getWidth()));
        javax.swing.JTextField heightField = new javax.swing.JTextField(String.valueOf(slide.getHeight()));
        Object[] message = {
                "宽度：", widthField,
                "高度：", heightField
        };

        int option = JOptionPane.showConfirmDialog(this, message, "页面设置", JOptionPane.OK_CANCEL_OPTION);
        if (option == JOptionPane.OK_OPTION) {
            try {
                int width = Integer.parseInt(widthField.getText());
                int height = Integer.parseInt(heightField.getText());
                if (width > 0 && height > 0) {
                    slide.setWidth(width);
                    slide.setHeight(height);
                    editorPanel.zoomToFit();
                    previewPanel.refreshPreviews();
                    isModified = true;
                } else {
                    JOptionPane.showMessageDialog(this, "宽度和高度必须大于0。");
                }
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "请输入有效的数字。");
            }
        }
    }

    private boolean confirmSaveIfNeeded() {
        if (!isModified) {
            return true;
        }
        int result = JOptionPane.showConfirmDialog(this, "当前文档已修改，是否保存？", "保存确认", JOptionPane.YES_NO_CANCEL_OPTION);
        if (result == JOptionPane.YES_OPTION) {
            return saveSlide();
        } else if (result == JOptionPane.NO_OPTION) {
            return true; // Proceed without saving
        } else {
            return false; // Cancel
        }
    }

    private void showTutorial() {
        if (!confirmSaveIfNeeded()) {
            return;
        }

        slide = new Slide();

        // Page 1: Welcome
        SlidePage page1 = new SlidePage();
        TextElement title1 = new TextElement("欢迎使用 PowerDot", 400, 200, 500, 80);
        title1.setFont(new Font("微软雅黑", Font.BOLD, 48));
        title1.setColor(Color.BLUE);
        page1.addElement(title1);

        TextElement content1 = new TextElement("一个简单易用的幻灯片制作工具", 450, 300, 400, 40);
        content1.setFont(new Font("宋体", Font.PLAIN, 24));
        page1.addElement(content1);
        slide.addPage(page1);

        // Page 2: Basic Operations
        SlidePage page2 = new SlidePage();
        TextElement title2 = new TextElement("基本操作", 50, 50, 300, 50);
        title2.setFont(new Font("微软雅黑", Font.BOLD, 36));
        page2.addElement(title2);

        TextElement content2 = new TextElement("您可以通过插入菜单添加文本、形状和图片等元素", 50, 120, 600, 40);
        content2.setFont(new Font("宋体", Font.PLAIN, 24));
        page2.addElement(content2);

        RectangleElement rect = new RectangleElement(100, 200, 150, 100, Color.BLACK, Color.YELLOW, 2);
        page2.addElement(rect);

        CircleElement circle = new CircleElement(300, 200, 100, Color.BLACK, Color.GREEN, 2);
        page2.addElement(circle);

        LineElement line = new LineElement(500, 200, 700, 300, Color.RED, 4);
        page2.addElement(line);
        slide.addPage(page2);

        // Page 3: Editing
        SlidePage page3 = new SlidePage();
        TextElement title3 = new TextElement("编辑功能", 50, 50, 300, 50);
        title3.setFont(new Font("微软雅黑", Font.BOLD, 36));
        page3.addElement(title3);

        TextElement content3 = new TextElement("选中元素后，可以使用工具栏更改颜色、字体、大小等属性", 50, 120, 800, 40);
        content3.setFont(new Font("宋体", Font.PLAIN, 24));
        page3.addElement(content3);
        slide.addPage(page3);

        // Page 4: Slideshow
        SlidePage page4 = new SlidePage();
        TextElement title4 = new TextElement("幻灯片放映", 50, 50, 300, 50);
        title4.setFont(new Font("微软雅黑", Font.BOLD, 36));
        page4.addElement(title4);

        TextElement content4 = new TextElement("点击放映菜单中的'从头开始放映'或按F5键开始播放", 50, 120, 600, 40);
        content4.setFont(new Font("宋体", Font.PLAIN, 24));
        page4.addElement(content4);
        slide.addPage(page4);

        // Update UI
        slide.setCurrentPageIndex(0);
        editorPanel.setSlide(slide);
        undoManager.clear();
        isModified = false;
        currentFile = null;

        previewPanel.updateSlideList(slide.getAllPages());
        previewPanel.setSelectedPage(0);
        updatePageStatus();
    }

    private void exitApp() {
        if (confirmSaveIfNeeded()) {
            System.exit(0);
        }
    }

    public static void main(String[] args) {
        SplashScreen splash = new SplashScreen(500);
        splash.showSplash();
        SwingUtilities.invokeLater(PresentationApp::new);
    }
}

class PageBackgroundDialog extends JDialog {
    private SlidePage.BackgroundMode mode;
    private Color solidColor;
    private Color gradientStart;
    private Color gradientEnd;
    private BufferedImage selectedImage;
    private boolean confirmed = false;

    private final JComboBox<SlidePage.BackgroundMode> modeCombo;
    private final JButton solidColorButton;
    private final JButton gradStartButton;
    private final JButton gradEndButton;
    private final JButton imageButton;
    private final JLabel imageLabel;

    public PageBackgroundDialog(JFrame owner, SlidePage page) {
        super(owner, "页面背景", true);
        this.mode = page.getBackgroundMode();
        this.solidColor = page.getBackgroundColor();
        this.gradientStart = page.getGradientStart();
        this.gradientEnd = page.getGradientEnd();
        this.selectedImage = page.getBackgroundImage();
        JPanel root = new JPanel(new BorderLayout(10, 10));
        root.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel form = new JPanel(new GridLayout(5, 2, 8, 8));

        modeCombo = new JComboBox<>(SlidePage.BackgroundMode.values());
        modeCombo.setSelectedItem(mode);
        modeCombo.addActionListener(e -> updateEnabled());

        solidColorButton = new JButton("选择纯色");
        solidColorButton.addActionListener(e -> {
            Color c = NewColorPickerDialog.pickColor(owner, solidColor, "选择纯色背景");
            if (c != null) {
                solidColor = c;
                solidColorButton.setBackground(c);
            }
        });
        solidColorButton.setBackground(solidColor);

        gradStartButton = new JButton("渐变起始颜色");
        gradStartButton.addActionListener(e -> {
            Color c = NewColorPickerDialog.pickColor(owner, gradientStart, "选择渐变起始颜色");
            if (c != null) {
                gradientStart = c;
                gradStartButton.setBackground(c);
            }
        });
        gradStartButton.setBackground(gradientStart);

        gradEndButton = new JButton("渐变结束颜色");
        gradEndButton.addActionListener(e -> {
            Color c = NewColorPickerDialog.pickColor(owner, gradientEnd, "选择渐变结束颜色");
            if (c != null) {
                gradientEnd = c;
                gradEndButton.setBackground(c);
            }
        });
        gradEndButton.setBackground(gradientEnd);

        imageButton = new JButton("选择图片");
        imageLabel = new JLabel(selectedImage != null ? "已选图片" : "未选择");
        imageButton.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser();
            int result = chooser.showOpenDialog(this);
            if (result == JFileChooser.APPROVE_OPTION) {
                File file = chooser.getSelectedFile();
                try {
                    BufferedImage img = ImageIO.read(file);
                    if (img != null) {
                        selectedImage = img;
                        imageLabel.setText(file.getName());
                    } else {
                        JOptionPane.showMessageDialog(this, "无法读取图片文件。");
                    }
                } catch (IOException ex) {
                    JOptionPane.showMessageDialog(this, "读取图片失败: " + ex.getMessage());
                }
            }
        });

        form.add(new JLabel("模式:"));
        form.add(modeCombo);
        form.add(new JLabel("纯色:"));
        form.add(solidColorButton);
        form.add(new JLabel("渐变起始:"));
        form.add(gradStartButton);
        form.add(new JLabel("渐变结束:"));
        form.add(gradEndButton);
        form.add(imageButton);
        form.add(imageLabel);

        JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton ok = new JButton("确定");
        ok.addActionListener(e -> {
            confirmed = true;
            mode = (SlidePage.BackgroundMode) modeCombo.getSelectedItem();
            dispose();
        });
        JButton cancel = new JButton("取消");
        cancel.addActionListener(e -> dispose());
        btns.add(ok);
        btns.add(cancel);

        root.add(form, BorderLayout.CENTER);
        root.add(btns, BorderLayout.SOUTH);
        setContentPane(root);
        pack();
        setLocationRelativeTo(owner);
        updateEnabled();
    }

    private void updateEnabled() {
        SlidePage.BackgroundMode m = (SlidePage.BackgroundMode) modeCombo.getSelectedItem();
        boolean isSolid = m == SlidePage.BackgroundMode.SOLID;
        boolean isGradient = m == SlidePage.BackgroundMode.GRADIENT;
        boolean isImage = m == SlidePage.BackgroundMode.IMAGE_STRETCH || m == SlidePage.BackgroundMode.IMAGE_TILE;

        solidColorButton.setEnabled(isSolid || isGradient);
        gradStartButton.setEnabled(isGradient);
        gradEndButton.setEnabled(isGradient);
        imageButton.setEnabled(isImage);
        imageLabel.setEnabled(isImage);
    }

    public boolean isConfirmed() { return confirmed; }
    public SlidePage.BackgroundMode getMode() { return mode; }
    public Color getSolidColor() { return solidColor; }
    public Color getGradientStart() { return gradientStart; }
    public Color getGradientEnd() { return gradientEnd; }
    public BufferedImage getSelectedImage() { return selectedImage; }
}

class ThemeChooserDialog extends JDialog {
    private Color primaryColor;
    private Color backgroundColor;
    private boolean confirmed = false;

    public ThemeChooserDialog(JFrame owner) {
        super(owner, "选择主题颜色", true);

        primaryColor = owner.getJMenuBar().getBackground();
        backgroundColor = owner.getJMenuBar().getBackground();
        if (primaryColor == null || primaryColor.getAlpha() == 0) {
            primaryColor = Color.LIGHT_GRAY;
        }
        if (backgroundColor == null || backgroundColor.getAlpha() == 0) {
            System.out.println(".()");
            backgroundColor = Color.LIGHT_GRAY;
        }

        JPanel panel = new JPanel(new GridLayout(3, 2, 10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JButton primaryButton = new JButton("选择主色调（菜单栏、工具栏）");
        JLabel primaryPreview = new JLabel();
        primaryPreview.setOpaque(true);
        primaryPreview.setBackground(primaryColor);
        primaryPreview.setBorder(BorderFactory.createLineBorder(Color.BLACK));

        JButton backgroundButton = new JButton("选择背景色（编辑区域）");
        JLabel backgroundPreview = new JLabel();
        backgroundPreview.setOpaque(true);
        backgroundPreview.setBackground(backgroundColor);
        backgroundPreview.setBorder(BorderFactory.createLineBorder(Color.BLACK));

        primaryButton.addActionListener(e -> {
            Color chosen = NewColorPickerDialog.pickColor(owner, primaryColor, "选择主色调");
            if (chosen != null) {
                primaryColor = chosen;
                primaryPreview.setBackground(primaryColor);
            }
        });

        backgroundButton.addActionListener(e -> {
            Color chosen = NewColorPickerDialog.pickColor(owner, backgroundColor, "选择背景色");
            if (chosen != null) {
                backgroundColor = chosen;
                System.err.println(chosen);
                backgroundPreview.setBackground(backgroundColor);
            }
        });

        JButton okButton = new JButton("确定");
        okButton.addActionListener(e -> { confirmed = true; dispose(); });
        JButton cancelButton = new JButton("取消");
        cancelButton.addActionListener(e -> dispose());

        panel.add(primaryButton); panel.add(primaryPreview);
        panel.add(backgroundButton); panel.add(backgroundPreview);
        panel.add(okButton); panel.add(cancelButton);

        setContentPane(panel);
        pack();
        setLocationRelativeTo(owner);
    }

    public boolean isConfirmed() { return confirmed; }
    public Color getPrimaryColor() { return primaryColor; }
    public Color getBackgroundColor() { return backgroundColor; }
}
