package com.z0fsec.workhelper.ui;

import javax.swing.*;
import java.awt.*;
import java.util.function.Consumer;

public class MainPanel extends BasePanel {
    private JTabbedPane tabbedPane;

    // 各个功能面板
    private ToolManagementPanel toolPanel;
    private ScriptManagementPanel scriptPanel;
    private TaskManagementPanel taskPanel;
    private NetworkManagementPanel networkPanel;
    private SettingsPanel settingsPanel;

    public MainPanel(Consumer<String> logConsumer) {
        super(logConsumer);
        initUI();
    }

    private void initUI() {
        setLayout(new BorderLayout());

        // 创建主面板
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        // 创建TabbedPane
        tabbedPane = new JTabbedPane();

        // 初始化各个功能面板
        toolPanel = new ToolManagementPanel(this::appendLog);
        scriptPanel = new ScriptManagementPanel(this::appendLog);
//        taskPanel = new TaskManagementPanel(this::appendLog);
        networkPanel = new NetworkManagementPanel(this::appendLog);
        settingsPanel = new SettingsPanel(this::appendLog);

        // 添加tab页
        tabbedPane.addTab("工具管理", toolPanel);
        tabbedPane.addTab("脚本管理", scriptPanel);
//        tabbedPane.addTab("定时任务", taskPanel);
        tabbedPane.addTab("网络管理", networkPanel);
        tabbedPane.addTab("设置", settingsPanel);

        mainPanel.add(tabbedPane, BorderLayout.CENTER);
        add(mainPanel, BorderLayout.CENTER);

        // 添加底部日志面板
        add(createLogPanelWithScroll(), BorderLayout.SOUTH);
    }

    /**
     * 主界面初始化完成后的回调
     */
    public void onPanelReady() {
        appendSuccess("渗透测试管理平台已就绪");
        appendInfo("提示：请根据需要选择相应的功能面板进行操作");
        appendInfo("功能包括：工具管理、脚本管理、定时任务、网络管理和系统设置");
    }

}