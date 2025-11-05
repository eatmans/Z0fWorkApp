package com.z0fsec.workhelper.ui;

import javax.swing.*;
import java.awt.*;
import java.util.function.Consumer;

public class TaskManagementPanel extends BasePanel {
    public TaskManagementPanel(Consumer<String> logConsumer) {
        super(logConsumer);
        initUI();
    }

    private void initUI() {
        setLayout(new BorderLayout());
        // 实现定时任务管理功能
        add(new JLabel("定时任务管理面板 - 开发中..."), BorderLayout.CENTER);
    }
}