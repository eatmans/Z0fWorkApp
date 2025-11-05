package com.z0fsec.workhelper.ui;


import com.z0fsec.workhelper.model.Tool;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;

class ToolDialog extends JDialog {
    private boolean confirmed = false;
    private Tool toolInfo;

    private JTextField nameField;
    private JComboBox<String> typeComboBox;
    private JTextField pathField;
    private JTextField versionField;
    private JComboBox<String> statusComboBox;
    private JTextArea descriptionArea;
    private JTextField categoryField;

    public ToolDialog(JFrame parent, String title, Tool existingTool) {
        super(parent, title, true);
        this.toolInfo = existingTool;
        initUI();
        pack();
        setLocationRelativeTo(parent);
    }

    private void initUI() {
        setLayout(new BorderLayout());
        setResizable(false);

        // 表单面板
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // 工具名称
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0;
        formPanel.add(new JLabel("工具名称:"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        nameField = new JTextField(20);
        formPanel.add(nameField, gbc);

        // 工具类型
        gbc.gridx = 0;
        gbc.gridy = 1;
        formPanel.add(new JLabel("工具类型:"), gbc);
        gbc.gridx = 1;
        typeComboBox = new JComboBox<>(new String[]{"JAR程序", "EXE程序", "Python脚本", "批处理文件", "Shell脚本", "PowerShell脚本", "其他"});
        formPanel.add(typeComboBox, gbc);

        // 文件路径
        gbc.gridx = 0;
        gbc.gridy = 2;
        formPanel.add(new JLabel("文件路径:"), gbc);
        gbc.gridx = 1;
        pathField = new JTextField(20);
        formPanel.add(pathField, gbc);
        gbc.gridx = 2;
        gbc.weightx = 0;
        JButton browseBtn = new JButton("浏览");
        browseBtn.addActionListener(this::browseFile);
        formPanel.add(browseBtn, gbc);

        // 版本号
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.weightx = 0;
        formPanel.add(new JLabel("版本号:"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        gbc.gridwidth = 2;
        versionField = new JTextField(20);
        formPanel.add(versionField, gbc);

        // 状态
        gbc.gridx = 0;
        gbc.gridy = 4;
        gbc.gridwidth = 1;
        formPanel.add(new JLabel("状态:"), gbc);
        gbc.gridx = 1;
        gbc.gridwidth = 2;
        statusComboBox = new JComboBox<>(new String[]{"就绪", "运行中", "已禁用", "错误"});
        formPanel.add(statusComboBox, gbc);

        // 分类
        gbc.gridx = 0;
        gbc.gridy = 5;
        gbc.gridwidth = 1;
        formPanel.add(new JLabel("分类:"), gbc);
        gbc.gridx = 1;
        gbc.gridwidth = 2;
        categoryField = new JTextField(20);
        formPanel.add(categoryField, gbc);

        // 描述
        gbc.gridx = 0;
        gbc.gridy = 6;
        formPanel.add(new JLabel("描述:"), gbc);
        gbc.gridx = 1;
        gbc.gridwidth = 2;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        descriptionArea = new JTextArea(4, 20);
        descriptionArea.setLineWrap(true);
        descriptionArea.setWrapStyleWord(true);
        formPanel.add(new JScrollPane(descriptionArea), gbc);

        // 按钮面板
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton okBtn = new JButton("确定");
        okBtn.addActionListener(e -> confirm());

        JButton cancelBtn = new JButton("取消");
        cancelBtn.addActionListener(e -> cancel());

        buttonPanel.add(okBtn);
        buttonPanel.add(cancelBtn);

        add(formPanel, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);

        // 如果是编辑模式，填充现有数据
        if (toolInfo != null) {
            nameField.setText(toolInfo.getName());
            typeComboBox.setSelectedItem(toolInfo.getType());
            pathField.setText(toolInfo.getPath());
            versionField.setText(toolInfo.getVersion());
            statusComboBox.setSelectedItem(toolInfo.getStatus());
            categoryField.setText(toolInfo.getCategory());
            descriptionArea.setText(toolInfo.getDescription());
        }
    }

    private void browseFile(ActionEvent e) {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("选择工具文件");
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);

        if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            pathField.setText(fileChooser.getSelectedFile().getAbsolutePath());
            // 自动填充名称
            if (nameField.getText().isEmpty()) {
                String fileName = fileChooser.getSelectedFile().getName();
                nameField.setText(fileName.replaceFirst("[.][^.]+$", ""));
            }
        }
    }

    private void confirm() {
        if (nameField.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "请输入工具名称", "错误", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (pathField.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "请选择工具文件路径", "错误", JOptionPane.ERROR_MESSAGE);
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

    public Tool getToolInfo() {
        return new Tool(
                nameField.getText().trim(),
                (String) typeComboBox.getSelectedItem(),
                pathField.getText().trim(),
                versionField.getText().trim(),
                (String) statusComboBox.getSelectedItem(),
                descriptionArea.getText().trim(),
                categoryField.getText().trim()
        );
    }
}