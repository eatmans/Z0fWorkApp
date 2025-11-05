package com.z0fsec.workhelper.ui;

import com.z0fsec.workhelper.db.DatabaseManager;
import com.z0fsec.workhelper.model.Tool;


import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class ToolManagementPanel extends BasePanel {
    private JPanel toolsCardPanel;
    private JTextField searchField;
    private JComboBox<String> typeFilter;
    private JComboBox<String> statusFilter;
    private List<Tool> toolList;
    private List<Tool> filteredToolList;
    private DatabaseManager dbManager;
    private static final int CARDS_PER_ROW = 3; // 每行显示3个卡片

    public ToolManagementPanel(Consumer<String> logConsumer) {
        super(logConsumer);


        this.dbManager = DatabaseManager.getInstance();
        this.toolList = new ArrayList<>();
        this.filteredToolList = new ArrayList<>();
        initUI();
        loadToolsFromDatabase();
        refreshToolCards();

        // 确保日志系统已完全初始化
        ensureLogInitialized();
        appendInfo("工具管理面板初始化完成");
    }

    private void initUI() {
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // 创建工具栏
        add(createToolBar(), BorderLayout.NORTH);

        // 创建工具卡片面板
        add(createToolsCardPanel(), BorderLayout.CENTER);

        // 创建状态栏
        add(createStatusBar(), BorderLayout.SOUTH);
    }

    private JPanel createToolBar() {
        JPanel toolbar = new JPanel(new BorderLayout());

        // 左侧操作按钮
        JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));

        JButton addBtn = new JButton("添加工具");
        addBtn.setToolTipText("添加新工具到管理列表");
        addBtn.addActionListener(this::addTool);

//        JButton editBtn = new JButton("编辑工具");
//        editBtn.setToolTipText("编辑选中的工具信息");
//        editBtn.addActionListener(this::editTool);
//
//        JButton removeBtn = new JButton("移除工具");
//        removeBtn.setToolTipText("从列表中移除选中的工具");
//        removeBtn.addActionListener(this::removeTool);

        JButton refreshBtn = new JButton("刷新");
        refreshBtn.setToolTipText("刷新工具列表");
        refreshBtn.addActionListener(e -> refreshTools());

        JButton importBtn = new JButton("批量导入");
        importBtn.setToolTipText("批量导入工具文件");
        importBtn.addActionListener(this::importTools);

        JButton exportBtn = new JButton("导出列表");
        exportBtn.setToolTipText("导出工具列表到文件");
        exportBtn.addActionListener(this::exportTools);

        leftPanel.add(addBtn);
//        leftPanel.add(editBtn);
//        leftPanel.add(removeBtn);
        leftPanel.add(refreshBtn);
        leftPanel.add(importBtn);
        leftPanel.add(exportBtn);

        // 右侧搜索和过滤
        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));

        searchField = new JTextField(15);
        searchField.setToolTipText("搜索工具名称或描述");

        JButton searchBtn = new JButton("搜索");
        searchBtn.addActionListener(e -> filterTools());

        typeFilter = new JComboBox<>(new String[]{"全部类型", "JAR程序", "EXE程序", "Python脚本", "批处理文件", "Shell脚本", "PowerShell脚本", "其他"});
        typeFilter.setToolTipText("按工具类型过滤");
        typeFilter.addActionListener(e -> filterTools());

        statusFilter = new JComboBox<>(new String[]{"全部状态", "就绪", "运行中", "已禁用", "错误"});
        statusFilter.setToolTipText("按工具状态过滤");
        statusFilter.addActionListener(e -> filterTools());

        JButton clearFilterBtn = new JButton("清除过滤");
        clearFilterBtn.addActionListener(e -> clearFilters());

        rightPanel.add(new JLabel("类型:"));
        rightPanel.add(typeFilter);
        rightPanel.add(new JLabel("状态:"));
        rightPanel.add(statusFilter);
        rightPanel.add(searchField);
        rightPanel.add(searchBtn);
        rightPanel.add(clearFilterBtn);

        toolbar.add(leftPanel, BorderLayout.WEST);
        toolbar.add(rightPanel, BorderLayout.EAST);

        return toolbar;
    }

    private JPanel createToolsCardPanel() {
        JPanel panel = new JPanel(new BorderLayout());

        toolsCardPanel = new JPanel();
        // 使用 GridLayout 替代 WrapLayout，确保每行固定数量
        toolsCardPanel.setLayout(new GridLayout(0, CARDS_PER_ROW, 15, 15)); // 0行表示自动扩展，3列
        toolsCardPanel.setBackground(new Color(240, 240, 240));

        JScrollPane scrollPane = new JScrollPane(toolsCardPanel);
        scrollPane.setBorder(BorderFactory.createTitledBorder("工具列表"));
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);

        panel.add(scrollPane, BorderLayout.CENTER);
        return panel;
    }


    private JPanel createStatusBar() {
        JPanel statusBar = new JPanel(new BorderLayout());
        statusBar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, Color.GRAY),
                BorderFactory.createEmptyBorder(5, 10, 5, 10)
        ));

        JLabel statusLabel = new JLabel("就绪 | 共 0 个工具");
        statusLabel.setName("statusLabel");

        JLabel filterLabel = new JLabel("显示 0 个工具");
        filterLabel.setName("filterLabel");
        filterLabel.setHorizontalAlignment(SwingConstants.RIGHT);

        statusBar.add(statusLabel, BorderLayout.WEST);
        statusBar.add(filterLabel, BorderLayout.EAST);

        return statusBar;
    }

    private void loadToolsFromDatabase() {
        try {
            toolList.clear();
            toolList.addAll(dbManager.getAllTools());

            if (toolList.isEmpty()) {
                appendInfo("工具列表为空，正在添加示例工具...");
                addSampleTools();
            }

            filteredToolList.clear();
            filteredToolList.addAll(toolList);
            updateStatusBar();

        } catch (Exception e) {
            appendError("从数据库加载工具失败: " + e.getMessage());
        }
    }

    private void addSampleTools() {
        // 添加示例工具到数据库
        List<Tool> sampleTools = List.of(
                new Tool("Nmap", "EXE程序", "C:\\Tools\\nmap.exe", "7.80", "就绪",
                        "网络发现和安全审计工具", "扫描工具"),
                new Tool("Burp Suite", "JAR程序", "C:\\Tools\\burpsuite.jar", "2023.1", "就绪",
                        "用于Web应用程序安全测试的集成平台", "Web安全"),
                new Tool("SQLMap", "Python脚本", "C:\\Tools\\sqlmap.py", "1.6", "就绪",
                        "自动SQL注入和数据库接管工具", "数据库安全"),
                new Tool("Metasploit", "EXE程序", "C:\\Tools\\msfconsole.exe", "6.0", "就绪",
                        "渗透测试框架，提供漏洞利用和Payload生成", "渗透测试")
        );

        for (Tool tool : sampleTools) {
            try {
                dbManager.addTool(tool);
                toolList.add(tool);
            } catch (Exception e) {
                appendError("添加示例工具失败: " + tool.getName() + " - " + e.getMessage());
            }
        }

        if (!sampleTools.isEmpty()) {
            appendSuccess("已添加 " + sampleTools.size() + " 个示例工具");
        }
    }

    private void refreshToolCards() {
        toolsCardPanel.removeAll();

        // 如果工具数量不是3的倍数，添加空面板来保持布局整齐
        int totalCards = filteredToolList.size();
        int cardsToAdd = totalCards;

        // 计算需要添加的空面板数量
        int remainder = totalCards % CARDS_PER_ROW;
        if (remainder != 0) {
            cardsToAdd += CARDS_PER_ROW - remainder;
        }

        for (int i = 0; i < cardsToAdd; i++) {
            if (i < totalCards) {
                Tool tool = filteredToolList.get(i);
                JPanel card = createToolCard(tool);
                toolsCardPanel.add(card);
            } else {
                // 添加空面板保持布局
                JPanel emptyCard = createEmptyCard();
                toolsCardPanel.add(emptyCard);
            }
        }

        toolsCardPanel.revalidate();
        toolsCardPanel.repaint();
        updateStatusBar();
    }

    private JPanel createEmptyCard() {
        JPanel emptyCard = new JPanel();
        emptyCard.setBackground(new Color(240, 240, 240));
        emptyCard.setPreferredSize(new Dimension(280, 180));
        emptyCard.setBorder(BorderFactory.createEmptyBorder());
        return emptyCard;
    }

    private JPanel createToolCard(Tool tool) {
        JPanel card = new JPanel(new BorderLayout());
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(200, 200, 200), 1),
                BorderFactory.createEmptyBorder(12, 12, 12, 12)
        ));
        card.setBackground(Color.WHITE);
        // 设置固定大小，确保每行卡片对齐
        card.setPreferredSize(new Dimension(280, 180));
        card.setMinimumSize(new Dimension(280, 180));
        card.setMaximumSize(new Dimension(280, 180));

        // 添加鼠标悬停效果
        card.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                card.setBackground(new Color(245, 245, 255));
                card.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(new Color(100, 149, 237), 2),
                        BorderFactory.createEmptyBorder(11, 11, 11, 11)
                ));
            }

            @Override
            public void mouseExited(MouseEvent e) {
                card.setBackground(Color.WHITE);
                card.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(new Color(200, 200, 200), 1),
                        BorderFactory.createEmptyBorder(12, 12, 12, 12)
                ));
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    runTool(tool);
                }
            }
        });

        // 顶部：图标和基本信息
        JPanel topPanel = new JPanel(new BorderLayout());

        // 左侧图标和名称
        JPanel leftPanel = new JPanel(new BorderLayout());
        leftPanel.setOpaque(false);

        JLabel iconLabel = new JLabel(createToolIcon(tool.getType()));
        JLabel nameLabel = new JLabel(tool.getName());
        nameLabel.setFont(new Font("Microsoft YaHei", Font.BOLD, 14));

        leftPanel.add(iconLabel, BorderLayout.WEST);
        leftPanel.add(nameLabel, BorderLayout.CENTER);

        // 右侧状态和版本
        JPanel rightPanel = new JPanel(new GridLayout(2, 1));
        rightPanel.setOpaque(false);

        JLabel versionLabel = new JLabel("v" + tool.getVersion());
        versionLabel.setFont(new Font("Microsoft YaHei", Font.PLAIN, 10));
        versionLabel.setForeground(Color.GRAY);
        versionLabel.setHorizontalAlignment(SwingConstants.RIGHT);

        JLabel statusLabel = new JLabel(tool.getStatus());
        statusLabel.setFont(new Font("Microsoft YaHei", Font.PLAIN, 10));
        statusLabel.setForeground(getStatusColor(tool.getStatus()));
        statusLabel.setHorizontalAlignment(SwingConstants.RIGHT);

        rightPanel.add(versionLabel);
        rightPanel.add(statusLabel);

        topPanel.add(leftPanel, BorderLayout.WEST);
        topPanel.add(rightPanel, BorderLayout.EAST);

        // 中部：描述信息
        JTextArea descArea = new JTextArea(tool.getDescription());
        descArea.setEditable(false);
        descArea.setLineWrap(true);
        descArea.setWrapStyleWord(true);
        descArea.setBackground(Color.WHITE);
        descArea.setFont(new Font("Microsoft YaHei", Font.PLAIN, 11));
        descArea.setBorder(new EmptyBorder(8, 0, 8, 0));
        descArea.setRows(3);

        // 底部：操作按钮
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 0));
        bottomPanel.setOpaque(false);

        JButton runBtn = new JButton("运行");
        runBtn.setToolTipText("运行此工具");
        runBtn.setPreferredSize(new Dimension(70, 25));
        runBtn.addActionListener(e -> runTool(tool));

        JButton editBtn = new JButton("编辑");
        editBtn.setToolTipText("编辑工具信息");
        editBtn.setPreferredSize(new Dimension(70, 25));
        editBtn.addActionListener(e -> editTool(tool));

        JButton deleteBtn = new JButton("删除");
        deleteBtn.setToolTipText("删除此工具");
        deleteBtn.setPreferredSize(new Dimension(70, 25));
        deleteBtn.addActionListener(e -> removeTool(tool));

        JButton moreBtn = new JButton("更多");
        moreBtn.setToolTipText("其他操作");
        moreBtn.setPreferredSize(new Dimension(70, 25));
        moreBtn.addActionListener(e -> openToolDirectoryWithOptions(tool));

        bottomPanel.add(runBtn);
        bottomPanel.add(editBtn);
        bottomPanel.add(deleteBtn);
        bottomPanel.add(moreBtn);

        // 组装卡片
        card.add(topPanel, BorderLayout.NORTH);
        card.add(descArea, BorderLayout.CENTER);
        card.add(bottomPanel, BorderLayout.SOUTH);

        return card;
    }


    private ImageIcon createToolIcon(String type) {
        // 根据工具类型创建不同的图标
        String iconText = "";
        Color bgColor = Color.WHITE;
        Color textColor = Color.BLACK;

        switch (type) {
            case "JAR程序":
                iconText = "JAR";
                bgColor = new Color(220, 240, 255);
                break;
            case "EXE程序":
                iconText = "EXE";
                bgColor = new Color(220, 255, 220);
                break;
            case "Python脚本":
                iconText = "PY";
                bgColor = new Color(255, 240, 220);
                break;
            case "批处理文件":
                iconText = "BAT";
                bgColor = new Color(255, 220, 220);
                break;
            case "Shell脚本":
                iconText = "SH";
                bgColor = new Color(240, 220, 255);
                break;
            case "PowerShell脚本":
                iconText = "PS1";
                bgColor = new Color(255, 220, 255);
                break;
            default:
                iconText = "APP";
                bgColor = new Color(240, 240, 240);
        }

        // 创建简单的文本图标
        BufferedImage image = new BufferedImage(32, 32, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = image.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // 绘制背景
        g2d.setColor(bgColor);
        g2d.fillRoundRect(0, 0, 32, 32, 8, 8);

        // 绘制边框
        g2d.setColor(new Color(150, 150, 150));
        g2d.drawRoundRect(0, 0, 31, 31, 8, 8);

        // 绘制文本
        g2d.setColor(textColor);
        g2d.setFont(new Font("Microsoft YaHei", Font.BOLD, 10));
        FontMetrics fm = g2d.getFontMetrics();
        int textWidth = fm.stringWidth(iconText);
        int textHeight = fm.getHeight();
        g2d.drawString(iconText, (32 - textWidth) / 2, (32 + textHeight) / 2 - 2);

        g2d.dispose();
        return new ImageIcon(image);
    }

    private Color getStatusColor(String status) {
        switch (status) {
            case "就绪":
                return new Color(0, 128, 0); // 绿色
            case "运行中":
                return new Color(255, 140, 0); // 橙色
            case "已禁用":
                return new Color(128, 128, 128); // 灰色
            case "错误":
                return new Color(220, 20, 60); // 红色
            default:
                return Color.BLACK;
        }
    }

    private void addTool(ActionEvent e) {
        ToolDialog dialog = new ToolDialog((JFrame) SwingUtilities.getWindowAncestor(this), "添加工具", null);
        dialog.setVisible(true);

        if (dialog.isConfirmed()) {
            try {
                Tool newTool = dialog.getToolInfo();
                dbManager.addTool(newTool);
                toolList.add(newTool);
                filterTools();
                appendSuccess("已添加工具: " + newTool.getName());
            } catch (Exception ex) {
                appendError("添加工具失败: " + ex.getMessage());
            }
        }
    }

    private void editTool(ActionEvent e) {
        if (filteredToolList.isEmpty()) {
            appendWarning("没有可编辑的工具");
            return;
        }
        appendInfo("请双击工具卡片或点击卡片上的编辑按钮进行编辑");
    }

    private void editTool(Tool tool) {
        ToolDialog dialog = new ToolDialog((JFrame) SwingUtilities.getWindowAncestor(this), "编辑工具", tool);
        dialog.setVisible(true);

        if (dialog.isConfirmed()) {
            try {
                Tool updatedTool = dialog.getToolInfo();
                updatedTool.setId(tool.getId()); // 保持相同的ID
                dbManager.updateTool(updatedTool);

                // 更新本地列表
                tool.setName(updatedTool.getName());
                tool.setType(updatedTool.getType());
                tool.setPath(updatedTool.getPath());
                tool.setVersion(updatedTool.getVersion());
                tool.setStatus(updatedTool.getStatus());
                tool.setDescription(updatedTool.getDescription());
                tool.setCategory(updatedTool.getCategory());

                filterTools();
                appendSuccess("已更新工具: " + tool.getName());
            } catch (Exception ex) {
                appendError("更新工具失败: " + ex.getMessage());
            }
        }
    }

    private void removeTool(ActionEvent e) {
        if (filteredToolList.isEmpty()) {
            appendWarning("没有可删除的工具");
            return;
        }
        appendInfo("请点击工具卡片上的删除按钮进行删除");
    }

    private void removeTool(Tool tool) {
        int result = JOptionPane.showConfirmDialog(this,
                "确定要删除工具 '" + tool.getName() + "' 吗？",
                "确认删除",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);

        if (result == JOptionPane.YES_OPTION) {
            try {
                dbManager.deleteTool(tool.getId());
                toolList.remove(tool);
                filterTools();
                appendSuccess("已删除工具: " + tool.getName());
            } catch (Exception ex) {
                appendError("删除工具失败: " + ex.getMessage());
            }
        }
    }

    private void runTool(Tool tool) {
        // 确保日志系统已初始化
        ensureLogInitialized();
        appendInfo("正在启动工具: " + tool.getName());

        // 检查工具文件是否存在
        File toolFile = new File(tool.getPath());
        if (!toolFile.exists()) {
            appendError("工具文件不存在: " + tool.getPath());
            updateToolStatus(tool, "错误", "工具文件不存在");
            return;
        }

        if (!toolFile.canExecute()) {
            appendWarning("工具文件可能没有执行权限: " + tool.getPath());
        }

        try {
            // 更新状态到数据库
            updateToolStatus(tool, "运行中", null);

            // 根据工具类型执行不同的启动逻辑
            new Thread(() -> {
                try {
                    Process process = startToolProcess(tool);
                    if (process != null) {
                        monitorToolProcess(tool, process);
                    }
                } catch (Exception ex) {
                    SwingUtilities.invokeLater(() -> {
                        appendError("启动工具失败: " + ex.getMessage());
                        updateToolStatus(tool, "错误", "启动失败: " + ex.getMessage());
                    });
                }
            }).start();

        } catch (Exception ex) {
            appendError("更新工具状态失败: " + ex.getMessage());
            updateToolStatus(tool, "错误", "状态更新失败");
        }
    }

    private Process startToolProcess(Tool tool) throws IOException {
        String toolPath = tool.getPath();
        ProcessBuilder processBuilder = new ProcessBuilder();

        // 获取工具所在的目录路径
        File toolFile = new File(toolPath);
        File toolDir = toolFile.getParentFile();

        System.out.println("工具目录: " + (toolDir != null ? toolDir.getAbsolutePath() : "null"));

        switch (tool.getType()) {
            case "EXE程序":
                // 对于命令行工具，打开cmd窗口并切换到工具目录
                if (isCommandLineTool(tool)) {
                    // 使用 start 命令打开新的cmd窗口并切换到工具目录
                    String cmdCommand = String.format("cmd /c start \"%s\" /D \"%s\" cmd",
                            tool.getName(), toolDir.getAbsolutePath());
                    processBuilder.command("cmd", "/c", cmdCommand);
                    appendInfo("正在打开命令行窗口到工具目录: " + toolDir.getAbsolutePath());
                } else {
                    // 对于GUI工具，直接执行
                    processBuilder.command(toolPath);
                }
                break;

            case "JAR程序":
                processBuilder.command("java", "-jar", toolPath);
                break;

            case "Python脚本":
                processBuilder.command("python", toolPath);
                break;

            case "批处理文件":
                // 对于批处理文件，也打开新的cmd窗口执行
                String batCommand = String.format("cmd /c start \"%s\" /D \"%s\" \"%s\"",
                        tool.getName(), toolDir.getAbsolutePath(), toolPath);
                processBuilder.command("cmd", "/c", batCommand);
                break;

            case "Shell脚本":
                processBuilder.command("sh", toolPath);
                break;

            case "PowerShell脚本":
                // 对于PowerShell，打开新的PowerShell窗口
                String psCommand = String.format("powershell -NoExit -Command \"cd '%s'\"",
                        toolDir.getAbsolutePath());
                processBuilder.command("cmd", "/c", "start", "PowerShell", psCommand);
                break;

            default:
                // 对于其他类型，打开cmd窗口到工具目录
                String defaultCommand = String.format("cmd /c start \"%s\" /D \"%s\" cmd",
                        tool.getName(), toolDir.getAbsolutePath());
                processBuilder.command("cmd", "/c", defaultCommand);
        }

        // 设置工作目录
        if (toolDir != null && toolDir.exists()) {
            processBuilder.directory(toolDir);
        }

        appendInfo("执行命令: " + String.join(" ", processBuilder.command()));
        return processBuilder.start();
    }

    // 判断是否为命令行工具
    private boolean isCommandLineTool(Tool tool) {
        // 根据工具名称或描述判断是否为命令行工具
        String toolName = tool.getName().toLowerCase();
        String description = tool.getDescription() != null ? tool.getDescription().toLowerCase() : "";

        // 常见的命令行工具关键词
        String[] cliKeywords = {
                "nmap", "sqlmap", "metasploit", "john", "hashcat", "aircrack",
                "wireshark", "tcpdump", "netcat", "nc", "curl", "wget",
                "扫描", "注入", "破解", "分析", "命令行", "终端", "cmd", "console","SQLMap"
        };

        for (String keyword : cliKeywords) {
            if (toolName.contains(keyword) || description.contains(keyword)) {
                return true;
            }
        }

        return false;
    }

    // 或者提供一个让用户选择的方式
    private void runToolWithOptions(Tool tool) {
        if ("EXE程序".equals(tool.getType())) {
            // 让用户选择执行方式
            String[] options = {"直接执行", "打开命令行窗口"};
            int choice = JOptionPane.showOptionDialog(
                    this,
                    "请选择执行方式:\n" +
                            "• 直接执行: 在后台运行工具\n" +
                            "• 打开命令行窗口: 打开cmd窗口并切换到工具目录",
                    "选择执行方式 - " + tool.getName(),
                    JOptionPane.DEFAULT_OPTION,
                    JOptionPane.QUESTION_MESSAGE,
                    null,
                    options,
                    options[0]
            );

            if (choice == 1) {
                // 用户选择打开命令行窗口
                openCommandPromptInToolDirectory(tool);
                return;
            }
        }

        // 默认执行方式
        runTool(tool);
    }

    // 专门用于打开命令行窗口的方法
    private void openCommandPromptInToolDirectory(Tool tool) {
        appendInfo("正在为 " + tool.getName() + " 打开命令行窗口...");

        File toolDir = new File(tool.getPath()).getParentFile();
        if (toolDir == null || !toolDir.exists()) {
            appendError("无法找到工具目录: " + tool.getPath());
            return;
        }

        try {
            ProcessBuilder processBuilder = new ProcessBuilder();

            // 使用 start 命令打开新的cmd窗口并切换到工具目录
            // /D 参数指定起始目录
            // /K 参数保持窗口打开（执行后不关闭）
            String command = String.format("cmd /c start \"%s\" /D \"%s\" cmd /K echo 已切换到工具目录: %s",
                    tool.getName(),
                    toolDir.getAbsolutePath(),
                    toolDir.getAbsolutePath());

            processBuilder.command("cmd", "/c", command);
            processBuilder.start();

            appendSuccess("已打开命令行窗口到: " + toolDir.getAbsolutePath());

        } catch (IOException ex) {
            appendError("打开命令行窗口失败: " + ex.getMessage());
        }
    }

    // 增强版的工具目录打开方法，支持更多选项
    private void openToolDirectoryWithOptions(Tool tool) {
        File toolDir = new File(tool.getPath()).getParentFile();
        if (toolDir == null || !toolDir.exists()) {
            appendError("无法找到工具目录: " + tool.getPath());
            return;
        }

        String[] options = {
                "打开命令行(cmd)",
                "打开PowerShell",
                "打开文件管理器",
                "直接执行工具"
        };

        int choice = JOptionPane.showOptionDialog(
                this,
                "请选择操作:\n" +
                        "工具: " + tool.getName() + "\n" +
                        "目录: " + toolDir.getAbsolutePath(),
                "工具操作 - " + tool.getName(),
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null,
                options,
                options[0]
        );

        switch (choice) {
            case 0: // 打开命令行
                openCommandPrompt(tool, toolDir, "cmd");
                break;
            case 1: // 打开PowerShell
                openCommandPrompt(tool, toolDir, "powershell");
                break;
            case 2: // 打开文件管理器
                openFileExplorer(toolDir);
                break;
            case 3: // 直接执行工具
                runTool(tool);
                break;
            default:
                // 取消操作
                break;
        }
    }

    // 通用的命令行打开方法
    private void openCommandPrompt(Tool tool, File toolDir, String shellType) {
        try {
            ProcessBuilder processBuilder = new ProcessBuilder();
            String command;

            if ("cmd".equals(shellType)) {
                command = String.format("cmd /c start \"%s\" /D \"%s\" cmd /K echo %s 工具目录 && dir",
                        tool.getName(),
                        toolDir.getAbsolutePath(),
                        tool.getName());
            } else {
                command = String.format("cmd /c start \"%s\" /D \"%s\" powershell -NoExit -Command \"Write-Host '%s 工具目录'; Get-Location\"",
                        tool.getName(),
                        toolDir.getAbsolutePath(),
                        tool.getName());
            }

            processBuilder.command("cmd", "/c", command);
            processBuilder.start();

            appendSuccess("已打开" + ("cmd".equals(shellType) ? "命令行" : "PowerShell") +
                    "窗口到: " + toolDir.getAbsolutePath());

        } catch (IOException ex) {
            appendError("打开" + ("cmd".equals(shellType) ? "命令行" : "PowerShell") + "失败: " + ex.getMessage());
        }
    }

    // 打开文件管理器
    private void openFileExplorer(File directory) {
        try {
            ProcessBuilder processBuilder = new ProcessBuilder("explorer", directory.getAbsolutePath());
            processBuilder.start();
            appendSuccess("已打开文件管理器: " + directory.getAbsolutePath());
        } catch (IOException ex) {
            appendError("打开文件管理器失败: " + ex.getMessage());
        }
    }

    private void monitorToolProcess(Tool tool, Process process) {
        try {
            // 读取工具输出
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), getSystemEncoding()));

            String line;
            StringBuilder output = new StringBuilder();
            while ((line = reader.readLine()) != null) {
                final String outputLine = line;
                SwingUtilities.invokeLater(() -> {
                    appendInfo("[ " + tool.getName() + " ] " + outputLine);
                });
                output.append(line).append("\n");
            }

            // 等待进程结束
            int exitCode = process.waitFor();

            SwingUtilities.invokeLater(() -> {
                if (exitCode == 0) {
                    appendSuccess("工具 " + tool.getName() + " 执行完成 (退出码: " + exitCode + ")");
                    updateToolStatus(tool, "就绪", "执行成功");
                } else {
                    appendWarning("工具 " + tool.getName() + " 执行完成 (退出码: " + exitCode + ")");
                    updateToolStatus(tool, "就绪", "执行完成，退出码: " + exitCode);
                }
            });

        } catch (IOException | InterruptedException ex) {
            SwingUtilities.invokeLater(() -> {
                appendError("工具执行异常: " + ex.getMessage());
                updateToolStatus(tool, "错误", "执行异常: " + ex.getMessage());
            });
        } finally {
            // 确保进程被销毁
            if (process.isAlive()) {
                process.destroy();
            }
        }
    }

    private void updateToolStatus(Tool tool, String status, String message) {
        try {
            dbManager.updateToolStatus(tool.getId(), status);
            if (status.equals("运行中")) {
                dbManager.updateLastUsed(tool.getId());
            }
            tool.setStatus(status);
            if (status.equals("运行中")) {
                tool.setLastUsed(System.currentTimeMillis());
            }
            refreshToolCards();

            if (message != null) {
                switch (status) {
                    case "运行中":
                        appendInfo(message);
                        break;
                    case "就绪":
                        appendSuccess(message);
                        break;
                    case "错误":
                        appendError(message);
                        break;
                    default:
                        appendInfo(message);
                }
            }
        } catch (Exception ex) {
            appendError("更新工具状态失败: " + ex.getMessage());
        }
    }

    private String getSystemEncoding() {
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("windows")) {
            return "GBK"; // Windows 系统使用 GBK 编码
        } else {
            return "UTF-8"; // Linux/Mac 使用 UTF-8 编码
        }
    }

    // 增强的工具执行方法，带参数支持
    public void runToolWithParameters(Tool tool, String parameters) {
        appendInfo("正在启动工具: " + tool.getName() + " 参数: " + parameters);

        File toolFile = new File(tool.getPath());
        if (!toolFile.exists()) {
            appendError("工具文件不存在: " + tool.getPath());
            updateToolStatus(tool, "错误", "工具文件不存在");
            return;
        }

        try {
            updateToolStatus(tool, "运行中", null);

            new Thread(() -> {
                try {
                    Process process = startToolProcessWithParameters(tool, parameters);
                    if (process != null) {
                        monitorToolProcess(tool, process);
                    }
                } catch (Exception ex) {
                    SwingUtilities.invokeLater(() -> {
                        appendError("启动工具失败: " + ex.getMessage());
                        updateToolStatus(tool, "错误", "启动失败: " + ex.getMessage());
                    });
                }
            }).start();

        } catch (Exception ex) {
            appendError("更新工具状态失败: " + ex.getMessage());
            updateToolStatus(tool, "错误", "状态更新失败");
        }
    }

    private Process startToolProcessWithParameters(Tool tool, String parameters) throws IOException {
        String toolPath = tool.getPath();
        ProcessBuilder processBuilder = new ProcessBuilder();
        String[] params = parameters.split("\\s+");

        switch (tool.getType()) {
            case "EXE程序":
                String[] exeCommand = new String[params.length + 1];
                exeCommand[0] = toolPath;
                System.arraycopy(params, 0, exeCommand, 1, params.length);
                processBuilder.command(exeCommand);
                break;

            case "JAR程序":
                String[] jarCommand = new String[params.length + 2];
                jarCommand[0] = "java";
                jarCommand[1] = "-jar";
                jarCommand[2] = toolPath;
                System.arraycopy(params, 0, jarCommand, 3, params.length);
                processBuilder.command(jarCommand);
                break;

            case "Python脚本":
                String[] pyCommand = new String[params.length + 2];
                pyCommand[0] = "python";
                pyCommand[1] = toolPath;
                System.arraycopy(params, 0, pyCommand, 2, params.length);
                processBuilder.command(pyCommand);
                break;

            default:
                // 其他类型暂不支持参数
                appendWarning("该工具类型暂不支持参数: " + tool.getType());
                return startToolProcess(tool);
        }

        File toolDir = new File(toolPath).getParentFile();
        if (toolDir != null && toolDir.exists()) {
            processBuilder.directory(toolDir);
        }

        processBuilder.redirectErrorStream(true);
        appendInfo("执行命令: " + String.join(" ", processBuilder.command()));
        return processBuilder.start();
    }

    // 添加工具参数输入对话框
    private void runToolWithDialog(Tool tool) {
        // 检查是否需要参数
        if (toolRequiresParameters(tool)) {
            String parameters = JOptionPane.showInputDialog(
                    this,
                    "请输入工具参数:",
                    "工具参数输入 - " + tool.getName(),
                    JOptionPane.QUESTION_MESSAGE
            );

            if (parameters != null) {
                runToolWithParameters(tool, parameters.trim());
            }
        } else {
            runTool(tool);
        }
    }

    private boolean toolRequiresParameters(Tool tool) {
        // 这里可以根据工具名称或类型判断是否需要参数
        // 例如，扫描工具通常需要参数
        String toolName = tool.getName().toLowerCase();
        return toolName.contains("scan") ||
                toolName.contains("nmap") ||
                toolName.contains("sqlmap") ||
                toolName.contains("metasploit");
    }

    private void importTools(ActionEvent e) {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("选择工具文件");
        fileChooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
        fileChooser.setMultiSelectionEnabled(true);

        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File[] selectedFiles = fileChooser.getSelectedFiles();
            int count = 0;

            for (File file : selectedFiles) {
                if (file.isFile() && isSupportedFileType(file.getName())) {
                    String fileName = file.getName();
                    String fileType = getFileType(fileName);
                    String toolName = fileName.replaceFirst("[.][^.]+$", "");

                    Tool newTool = new Tool(
                            toolName, fileType, file.getAbsolutePath(),
                            "1.0", "就绪", "导入的工具", "其他"
                    );

                    try {
                        dbManager.addTool(newTool);
                        toolList.add(newTool);
                        count++;
                    } catch (Exception ex) {
                        appendError("导入工具失败: " + toolName + " - " + ex.getMessage());
                    }
                }
            }

            filterTools();
            appendSuccess("已导入 " + count + " 个工具");
        }
    }

    private void exportTools(ActionEvent e) {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("导出工具列表");
        fileChooser.setSelectedFile(new File("tools_export.json"));

        if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            // 这里可以实现导出逻辑
            appendInfo("导出功能开发中...");
        }
    }

    private boolean isSupportedFileType(String fileName) {
        String lower = fileName.toLowerCase();
        return lower.endsWith(".exe") || lower.endsWith(".jar") ||
                lower.endsWith(".py") || lower.endsWith(".bat") ||
                lower.endsWith(".sh") || lower.endsWith(".ps1");
    }

    private String getFileType(String fileName) {
        if (fileName.toLowerCase().endsWith(".jar")) return "JAR程序";
        if (fileName.toLowerCase().endsWith(".exe")) return "EXE程序";
        if (fileName.toLowerCase().endsWith(".py")) return "Python脚本";
        if (fileName.toLowerCase().endsWith(".bat")) return "批处理文件";
        if (fileName.toLowerCase().endsWith(".sh")) return "Shell脚本";
        if (fileName.toLowerCase().endsWith(".ps1")) return "PowerShell脚本";
        return "其他";
    }

    private void refreshTools() {
        appendInfo("刷新工具列表...");
        loadToolsFromDatabase();
        filterTools();
        appendSuccess("工具列表已刷新");
    }

    private void filterTools() {
        String searchText = searchField.getText().toLowerCase();
        String selectedType = (String) typeFilter.getSelectedItem();
        String selectedStatus = (String) statusFilter.getSelectedItem();

        try {
            // 使用数据库搜索
            filteredToolList.clear();
            filteredToolList.addAll(dbManager.searchTools(
                    searchText.isEmpty() ? null : searchText,
                    "全部类型".equals(selectedType) ? null : selectedType,
                    "全部状态".equals(selectedStatus) ? null : selectedStatus
            ));

            refreshToolCards();

        } catch (Exception e) {
            appendError("搜索工具失败: " + e.getMessage());
            // 回退到本地过滤
            filteredToolList.clear();
            for (Tool tool : toolList) {
                boolean matches = true;

                if (!searchText.isEmpty()) {
                    matches = tool.getName().toLowerCase().contains(searchText) ||
                            tool.getDescription().toLowerCase().contains(searchText) ||
                            tool.getCategory().toLowerCase().contains(searchText);
                }

                if (matches && !"全部类型".equals(selectedType)) {
                    matches = tool.getType().equals(selectedType);
                }

                if (matches && !"全部状态".equals(selectedStatus)) {
                    matches = tool.getStatus().equals(selectedStatus);
                }

                if (matches) {
                    filteredToolList.add(tool);
                }
            }
            refreshToolCards();
        }
    }

    private void clearFilters() {
        searchField.setText("");
        typeFilter.setSelectedIndex(0);
        statusFilter.setSelectedIndex(0);
        filterTools();
    }

    private void updateStatusBar() {
        Component[] components = getComponents();
        if (components.length > 2) {
            JPanel statusBar = (JPanel) components[2];
            JLabel statusLabel = (JLabel) statusBar.getComponent(0);
            JLabel filterLabel = (JLabel) statusBar.getComponent(1);

            statusLabel.setText("就绪 | 共 " + toolList.size() + " 个工具");
            filterLabel.setText("显示 " + filteredToolList.size() + " 个工具");
        }
    }

    // 自动换行布局
    private static class WrapLayout extends FlowLayout {
        public WrapLayout() {
            super();
        }

        public WrapLayout(int align, int hgap, int vgap) {
            super(align, hgap, vgap);
        }

        @Override
        public Dimension preferredLayoutSize(Container target) {
            return layoutSize(target, true);
        }

        @Override
        public Dimension minimumLayoutSize(Container target) {
            return layoutSize(target, false);
        }

        private Dimension layoutSize(Container target, boolean preferred) {
            synchronized (target.getTreeLock()) {
                int targetWidth = target.getSize().width;
                if (targetWidth == 0) targetWidth = Integer.MAX_VALUE;

                int hgap = getHgap();
                int vgap = getVgap();
                Insets insets = target.getInsets();
                int maxWidth = targetWidth - (insets.left + insets.right + hgap * 2);

                Dimension dim = new Dimension(0, 0);
                int rowWidth = 0;
                int rowHeight = 0;

                for (Component comp : target.getComponents()) {
                    if (comp.isVisible()) {
                        Dimension d = preferred ? comp.getPreferredSize() : comp.getMinimumSize();

                        if (rowWidth + d.width > maxWidth) {
                            dim.width = Math.max(dim.width, rowWidth);
                            dim.height += rowHeight + vgap;
                            rowWidth = 0;
                            rowHeight = 0;
                        }

                        if (rowWidth != 0) {
                            rowWidth += hgap;
                        }

                        rowWidth += d.width;
                        rowHeight = Math.max(rowHeight, d.height);
                    }
                }

                dim.width = Math.max(dim.width, rowWidth);
                dim.height += rowHeight;

                dim.width += insets.left + insets.right + hgap * 2;
                dim.height += insets.top + insets.bottom + vgap * 2;

                return dim;
            }
        }
    }
}