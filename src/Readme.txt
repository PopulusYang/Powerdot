简易幻灯片制作与播放软件 (Simple Java Presentation App)

本项目是基于 Java Swing/AWT 实现的一个简易幻灯片制作和播放软件。

架构概述

本项目采用 Model-View-Controller (MVC) 架构思想：

Model (数据模型): Slide (整个文稿), SlidePage (单页), SlideElement (页面元素抽象), TextElement, ShapeElement, ImageElement 等。

View (视图): SlideEditorPanel (用于绘制和交互的自定义 JPanel，位于 PresentationApp.java 内部)，负责元素的渲染和显示。

Controller (控制器): 负责处理用户输入（菜单点击、工具栏操作、鼠标拖拽等），并更新 Model。

核心功能实现骨架

本骨架提供了以下基本功能的数据结构和入口：

新建幻灯片/页面: 通过 Slide 和 SlidePage 类实现。

菜单与工具栏: 在 PresentationApp.java 中初始化。

元素抽象: 通过 SlideElement 及其子类实现页面元素的统一管理。

选取和移动: SlideEditorPanel 将通过鼠标监听器，利用 SlideElement.contains(Point) 方法实现选取，并使用 SlideElement.move(int dx, int dy) 实现移动。

编译与执行方法

1. 源代码编译方法

本项目的源代码完全由标准的 Java 文件组成，不依赖任何第三方库（除了导出 PDF/图片可能需要额外的库，但本骨架中暂未集成）。

打开命令行或终端，进入包含所有 .java 文件的目录，执行以下命令：

# 编译所有 Java 文件
javac *.java


成功后，将生成对应的 .class 字节码文件。

2. 字节码执行方法

在同一目录下，执行以下命令：

# 运行主程序
java PresentationApp


一个包含菜单和空白编辑区的窗口将会出现。

团队分工建议

本项目工作量较大，建议按功能模块进行分工：

数据模型与持久化 (Model & Save/Load): 负责 Slide、SlidePage、所有 Element 类的属性设计，以及实现文件（反）序列化（保存/打开，如 JSON 或 Java Serialization）。

编辑界面与交互 (View & Controller - Drawing): 负责 SlideEditorPanel 的绘制逻辑 (paintComponent 方法)，实现元素的渲染和拖拽移动（要求 8）。

元素添加与属性设置 (Controller - Editing): 负责菜单/工具栏操作的处理，实现添加文本框、图形、插入图片的功能，以及修改颜色、大小、字体等属性（要求 4, 5, 6, 7）。

播放与导出 (Export & Playback): 负责全屏播放功能（选做 4）和导出功能（PNG/JPG/PDF，要求 10）。