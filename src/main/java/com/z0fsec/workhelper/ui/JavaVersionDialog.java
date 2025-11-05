package com.z0fsec.workhelper.ui;


import com.z0fsec.workhelper.model.JavaVersion;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;

// Java版本对话框
public class JavaVersionDialog extends JDialog {
    private boolean confirmed = false;
    private JavaVersion javaVersion;

    private JTextField nameField;
    private JTextField pathField;
    private JButton browseBtn;

    public JavaVersionDialog(JFrame parent, String title, JavaVersion existingVersion) {
        super(parent, title, true);
        this.javaVersion = existingVersion;
        initUI();
        pack();
        setLocationRelativeTo(parent);
        setResizable(false);
    }

    private void initUI() {
        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // 版本名称
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0;
        add(new JLabel("版本名称:"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        nameField = new JTextField(20);
        add(nameField, gbc);

        // Java路径
        gbc.gridx = 0;
        gbc.gridy = 1;
        add(new JLabel("Java路径:"), gbc);
        gbc.gridx = 1;
        pathField = new JTextField(20);
        add(pathField, gbc);
        gbc.gridx = 2;
        gbc.weightx = 0;
        browseBtn = new JButton("浏览");
        browseBtn.addActionListener(e -> browseJavaPath());
        add(browseBtn, gbc);

        // 按钮面板
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 3;
        gbc.anchor = GridBagConstraints.CENTER;
        JPanel buttonPanel = new JPanel();
        JButton okBtn = new JButton("确定");
        okBtn.addActionListener(e -> confirm());
        JButton cancelBtn = new JButton("取消");
        cancelBtn.addActionListener(e -> cancel());

        buttonPanel.add(okBtn);
        buttonPanel.add(cancelBtn);
        add(buttonPanel, gbc);

        // 如果是编辑模式，填充数据
        if (javaVersion != null) {
            nameField.setText(javaVersion.name);
            pathField.setText(javaVersion.path);
        }
    }

    private void browseJavaPath() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("选择Java可执行文件");
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);

        if (System.getProperty("os.name").toLowerCase().contains("windows")) {
            fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("Java可执行文件", "exe"));
        }

        if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            pathField.setText(fileChooser.getSelectedFile().getAbsolutePath());
        }
    }

    private void confirm() {
        if (nameField.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "请输入版本名称", "错误", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (pathField.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "请输入Java路径", "错误", JOptionPane.ERROR_MESSAGE);
            return;
        }

        confirmed = true;
        dispose();
    }

    private void cancel() {
        confirmed = false;
        dispose();
    }

    public boolean isConfirmed() {
        return confirmed;
    }

    public JavaVersion getJavaVersion() {
        return new JavaVersion(
                nameField.getText().trim(),
                pathField.getText().trim(),
                "未知版本", // 实际应该检测版本
                false
        );
    }
}
