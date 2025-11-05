package com.z0fsec.workhelper.ui;

import com.z0fsec.workhelper.model.JavaVersion;
import com.z0fsec.workhelper.db.DatabaseManager;
import com.z0fsec.workhelper.model.PythonVersion;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class SettingsPanel extends BasePanel {
    private DatabaseManager dbManager;

    // 数据库设置
    private JTextField databasePathField;
    private JButton databaseBrowseBtn;

    // Java设置
    private JTable javaVersionsTable;
    private DefaultTableModel javaTableModel;
    private JButton addJavaBtn;
    private JButton removeJavaBtn;
    private JButton setDefaultJavaBtn;

    // Python设置
    private JTable pythonVersionsTable;
    private DefaultTableModel pythonTableModel;
    private JButton addPythonBtn;
    private JButton removePythonBtn;
    private JButton setDefaultPythonBtn;

    // 其他设置
    private JCheckBox autoSaveCheckBox;
    private JCheckBox autoBackupCheckBox;
    private JSpinner backupIntervalSpinner;
    private JTextField backupPathField;
    private JButton backupBrowseBtn;

    private List<JavaVersion> javaVersions;
    private List<PythonVersion> pythonVersions;

    public SettingsPanel(Consumer<String> logConsumer) {
        super(logConsumer);
        this.dbManager = DatabaseManager.getInstance();
        this.javaVersions = new ArrayList<>();
        this.pythonVersions = new ArrayList<>();
        initUI();
        loadSettings();

        ensureLogInitialized();
        appendInfo("设置面板初始化完成");
    }

    private void initUI() {
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JTabbedPane tabbedPane = new JTabbedPane();

        // 数据库设置标签页
        tabbedPane.addTab("数据库设置", createDatabasePanel());

        // Java设置标签页
        tabbedPane.addTab("Java设置", createJavaPanel());

        // Python设置标签页
        tabbedPane.addTab("Python设置", createPythonPanel());

        // 其他设置标签页
        tabbedPane.addTab("其他设置", createOtherSettingsPanel());

        add(tabbedPane, BorderLayout.CENTER);
        add(createButtonPanel(), BorderLayout.SOUTH);
    }

    private JPanel createDatabasePanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // 数据库路径
        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0;
        panel.add(new JLabel("数据库路径:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1.0;
        databasePathField = new JTextField(30);
        panel.add(databasePathField, gbc);
        gbc.gridx = 2; gbc.weightx = 0;
        databaseBrowseBtn = new JButton("浏览");
        databaseBrowseBtn.addActionListener(this::browseDatabasePath);
        panel.add(databaseBrowseBtn, gbc);

        // 数据库信息
        gbc.gridx = 0; gbc.gridy = 1; gbc.gridwidth = 3;
        JTextArea infoArea = new JTextArea(3, 40);
        infoArea.setEditable(false);
        infoArea.setBackground(panel.getBackground());
        infoArea.setText("数据库文件存储所有工具和脚本的配置信息。\n" +
                "更改数据库路径后需要重启应用程序生效。");
        panel.add(infoArea, gbc);

        return panel;
    }

    private JPanel createJavaPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // 表格
        String[] columns = {"版本名称", "路径", "版本号", "是否默认"};
        javaTableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }

            @Override
            public Class<?> getColumnClass(int columnIndex) {
                return columnIndex == 3 ? Boolean.class : String.class;
            }
        };

        javaVersionsTable = new JTable(javaTableModel);
        javaVersionsTable.setRowHeight(25);
        javaVersionsTable.getSelectionModel().addListSelectionListener(e -> updateJavaButtons());

        JScrollPane scrollPane = new JScrollPane(javaVersionsTable);

        // 按钮面板
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        addJavaBtn = new JButton("添加Java版本");
        addJavaBtn.addActionListener(this::addJavaVersion);

        removeJavaBtn = new JButton("移除选中");
        removeJavaBtn.addActionListener(this::removeJavaVersion);
        removeJavaBtn.setEnabled(false);

        setDefaultJavaBtn = new JButton("设为默认");
        setDefaultJavaBtn.addActionListener(this::setDefaultJava);
        setDefaultJavaBtn.setEnabled(false);

        buttonPanel.add(addJavaBtn);
        buttonPanel.add(removeJavaBtn);
        buttonPanel.add(setDefaultJavaBtn);

        panel.add(scrollPane, BorderLayout.CENTER);
        panel.add(buttonPanel, BorderLayout.SOUTH);

        return panel;
    }

    private JPanel createPythonPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // 表格
        String[] columns = {"版本名称", "路径", "版本号", "是否默认"};
        pythonTableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }

            @Override
            public Class<?> getColumnClass(int columnIndex) {
                return columnIndex == 3 ? Boolean.class : String.class;
            }
        };

        pythonVersionsTable = new JTable(pythonTableModel);
        pythonVersionsTable.setRowHeight(25);
        pythonVersionsTable.getSelectionModel().addListSelectionListener(e -> updatePythonButtons());

        JScrollPane scrollPane = new JScrollPane(pythonVersionsTable);

        // 按钮面板
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        addPythonBtn = new JButton("添加Python版本");
        addPythonBtn.addActionListener(this::addPythonVersion);

        removePythonBtn = new JButton("移除选中");
        removePythonBtn.addActionListener(this::removePythonVersion);
        removePythonBtn.setEnabled(false);

        setDefaultPythonBtn = new JButton("设为默认");
        setDefaultPythonBtn.addActionListener(this::setDefaultPython);
        setDefaultPythonBtn.setEnabled(false);

        buttonPanel.add(addPythonBtn);
        buttonPanel.add(removePythonBtn);
        buttonPanel.add(setDefaultPythonBtn);

        panel.add(scrollPane, BorderLayout.CENTER);
        panel.add(buttonPanel, BorderLayout.SOUTH);

        return panel;
    }

    private JPanel createOtherSettingsPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        int row = 0;

        // 自动保存
        gbc.gridx = 0; gbc.gridy = row++; gbc.gridwidth = 2;
        autoSaveCheckBox = new JCheckBox("自动保存修改");
        panel.add(autoSaveCheckBox, gbc);

        // 自动备份
        gbc.gridx = 0; gbc.gridy = row++; gbc.gridwidth = 2;
        autoBackupCheckBox = new JCheckBox("启用自动备份");
        autoBackupCheckBox.addActionListener(e -> updateBackupSettings());
        panel.add(autoBackupCheckBox, gbc);

        // 备份间隔
        gbc.gridx = 0; gbc.gridy = row; gbc.gridwidth = 1;
        panel.add(new JLabel("备份间隔(分钟):"), gbc);
        gbc.gridx = 1;
        backupIntervalSpinner = new JSpinner(new SpinnerNumberModel(60, 5, 1440, 5));
        panel.add(backupIntervalSpinner, gbc);

        row++;

        // 备份路径
        gbc.gridx = 0; gbc.gridy = row; gbc.gridwidth = 1;
        panel.add(new JLabel("备份路径:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1.0;
        backupPathField = new JTextField(25);
        panel.add(backupPathField, gbc);
        gbc.gridx = 2; gbc.weightx = 0;
        backupBrowseBtn = new JButton("浏览");
        backupBrowseBtn.addActionListener(this::browseBackupPath);
        panel.add(backupBrowseBtn, gbc);

        return panel;
    }

    private JPanel createButtonPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT));

        JButton saveBtn = new JButton("保存设置");
        saveBtn.addActionListener(this::saveSettings);

        JButton resetBtn = new JButton("恢复默认");
        resetBtn.addActionListener(this::resetSettings);

        JButton applyBtn = new JButton("应用");
        applyBtn.addActionListener(this::applySettings);

        panel.add(resetBtn);
        panel.add(applyBtn);
        panel.add(saveBtn);

        return panel;
    }

    private void loadSettings() {
        // 加载数据库设置
        String dbPath = dbManager.getSetting("database_path");
        if (dbPath != null) {
            databasePathField.setText(dbPath);
        }

        // 加载Java版本
        loadJavaVersions();

        // 加载Python版本
        loadPythonVersions();

        // 加载其他设置
        autoSaveCheckBox.setSelected("true".equals(dbManager.getSetting("auto_save")));
        autoBackupCheckBox.setSelected("true".equals(dbManager.getSetting("auto_backup")));

        String backupInterval = dbManager.getSetting("backup_interval");
        if (backupInterval != null) {
            try {
                backupIntervalSpinner.setValue(Integer.parseInt(backupInterval));
            } catch (NumberFormatException e) {
                // 使用默认值
            }
        }

        String backupPath = dbManager.getSetting("backup_path");
        if (backupPath != null) {
            backupPathField.setText(backupPath);
        }

        updateBackupSettings();
    }

    private void loadJavaVersions() {
        // 从数据库加载Java版本信息
        // 这里简化实现，实际应该从数据库读取
        javaVersions.clear();
        javaTableModel.setRowCount(0);

        // 添加默认Java版本
        String defaultJava = dbManager.getSetting("default_java_path");
        if (defaultJava != null) {
            JavaVersion java = new JavaVersion("默认Java", defaultJava, getJavaVersion(defaultJava), true);
            javaVersions.add(java);
            javaTableModel.addRow(new Object[]{java.name, java.path, java.version, java.isDefault});
        }

        // 可以从数据库加载更多Java版本
        // String javaVersionsStr = dbManager.getSetting("java_versions");
        // 解析并加载...
    }

    private void loadPythonVersions() {
        // 从数据库加载Python版本信息
        pythonVersions.clear();
        pythonTableModel.setRowCount(0);

        // 添加默认Python版本
        String defaultPython = dbManager.getSetting("default_python_path");
        if (defaultPython != null) {
            PythonVersion python = new PythonVersion("默认Python", defaultPython, getPythonVersion(defaultPython), true);
            pythonVersions.add(python);
            pythonTableModel.addRow(new Object[]{python.name, python.path, python.version, python.isDefault});
        }
    }

    // 事件处理方法
    private void browseDatabasePath(ActionEvent e) {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("选择数据库文件");
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        fileChooser.setSelectedFile(new File(databasePathField.getText()));

        if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            databasePathField.setText(fileChooser.getSelectedFile().getAbsolutePath());
        }
    }

    private void browseBackupPath(ActionEvent e) {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("选择备份目录");
        fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        fileChooser.setSelectedFile(new File(backupPathField.getText()));

        if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            backupPathField.setText(fileChooser.getSelectedFile().getAbsolutePath());
        }
    }

    private void addJavaVersion(ActionEvent e) {
        JavaVersionDialog dialog = new JavaVersionDialog((JFrame) SwingUtilities.getWindowAncestor(this), "添加Java版本", null);
        dialog.setVisible(true);

        if (dialog.isConfirmed()) {
            com.z0fsec.workhelper.model.JavaVersion java = dialog.getJavaVersion();
            javaVersions.add(java);
            javaTableModel.addRow(new Object[]{java.name, java.path, java.version, java.isDefault});
            appendSuccess("已添加Java版本: " + java.name);
        }
    }

    private void removeJavaVersion(ActionEvent e) {
        int selectedRow = javaVersionsTable.getSelectedRow();
        if (selectedRow != -1) {
            String name = (String) javaTableModel.getValueAt(selectedRow, 0);
            int result = JOptionPane.showConfirmDialog(this,
                    "确定要移除Java版本 '" + name + "' 吗？",
                    "确认移除",
                    JOptionPane.YES_NO_OPTION);

            if (result == JOptionPane.YES_OPTION) {
                javaVersions.remove(selectedRow);
                javaTableModel.removeRow(selectedRow);
                appendSuccess("已移除Java版本: " + name);
            }
        }
    }

    private void setDefaultJava(ActionEvent e) {
        int selectedRow = javaVersionsTable.getSelectedRow();
        if (selectedRow != -1) {
            // 清除所有默认标记
            for (int i = 0; i < javaTableModel.getRowCount(); i++) {
                javaTableModel.setValueAt(false, i, 3);
            }
            // 设置选中行为默认
            javaTableModel.setValueAt(true, selectedRow, 3);

            // 更新数据
            for (int i = 0; i < javaVersions.size(); i++) {
                javaVersions.get(i).isDefault = (i == selectedRow);
            }

            appendSuccess("已设置默认Java版本: " + javaTableModel.getValueAt(selectedRow, 0));
        }
    }

    private void addPythonVersion(ActionEvent e) {
        PythonVersionDialog dialog = new PythonVersionDialog((JFrame) SwingUtilities.getWindowAncestor(this), "添加Python版本", null);
        dialog.setVisible(true);

        if (dialog.isConfirmed()) {
            PythonVersion python = dialog.getPythonVersion();
            pythonVersions.add(python);
            pythonTableModel.addRow(new Object[]{python.name, python.path, python.version, python.isDefault});
            appendSuccess("已添加Python版本: " + python.name);
        }
    }

    private void removePythonVersion(ActionEvent e) {
        int selectedRow = pythonVersionsTable.getSelectedRow();
        if (selectedRow != -1) {
            String name = (String) pythonTableModel.getValueAt(selectedRow, 0);
            int result = JOptionPane.showConfirmDialog(this,
                    "确定要移除Python版本 '" + name + "' 吗？",
                    "确认移除",
                    JOptionPane.YES_NO_OPTION);

            if (result == JOptionPane.YES_OPTION) {
                pythonVersions.remove(selectedRow);
                pythonTableModel.removeRow(selectedRow);
                appendSuccess("已移除Python版本: " + name);
            }
        }
    }

    private void setDefaultPython(ActionEvent e) {
        int selectedRow = pythonVersionsTable.getSelectedRow();
        if (selectedRow != -1) {
            // 清除所有默认标记
            for (int i = 0; i < pythonTableModel.getRowCount(); i++) {
                pythonTableModel.setValueAt(false, i, 3);
            }
            // 设置选中行为默认
            pythonTableModel.setValueAt(true, selectedRow, 3);

            // 更新数据
            for (int i = 0; i < pythonVersions.size(); i++) {
                pythonVersions.get(i).isDefault = (i == selectedRow);
            }

            appendSuccess("已设置默认Python版本: " + pythonTableModel.getValueAt(selectedRow, 0));
        }
    }

    private void saveSettings(ActionEvent e) {
        applySettings(e);
        appendSuccess("设置已保存");
    }

    private void resetSettings(ActionEvent e) {
        int result = JOptionPane.showConfirmDialog(this,
                "确定要恢复默认设置吗？这将清除所有自定义设置。",
                "确认恢复默认",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);

        if (result == JOptionPane.YES_OPTION) {
            // 重置数据库设置
            dbManager.setSetting("database_path", "workhelper.db");

            // 重新加载设置
            loadSettings();
            appendSuccess("已恢复默认设置");
        }
    }

    private void applySettings(ActionEvent e) {
        try {
            // 保存数据库设置
            dbManager.setSetting("database_path", databasePathField.getText());

            // 保存Java设置
            saveJavaVersions();

            // 保存Python设置
            savePythonVersions();

            // 保存其他设置
            dbManager.setSetting("auto_save", String.valueOf(autoSaveCheckBox.isSelected()));
            dbManager.setSetting("auto_backup", String.valueOf(autoBackupCheckBox.isSelected()));
            dbManager.setSetting("backup_interval", backupIntervalSpinner.getValue().toString());
            dbManager.setSetting("backup_path", backupPathField.getText());

            appendSuccess("设置已应用");

        } catch (Exception ex) {
            appendError("保存设置失败: " + ex.getMessage());
        }
    }

    private void saveJavaVersions() {
        // 找到默认Java版本
        for (JavaVersion java : javaVersions) {
            if (java.isDefault) {
                dbManager.setSetting("default_java_path", java.path);
                break;
            }
        }

        // 可以保存所有Java版本到数据库
        // StringBuilder sb = new StringBuilder();
        // for (JavaVersion java : javaVersions) {
        //     sb.append(java.name).append("|").append(java.path).append("|").append(java.isDefault).append(";");
        // }
        // dbManager.setSetting("java_versions", sb.toString());
    }

    private void savePythonVersions() {
        // 找到默认Python版本
        for (PythonVersion python : pythonVersions) {
            if (python.isDefault) {
                dbManager.setSetting("default_python_path", python.path);
                break;
            }
        }
    }

    private void updateJavaButtons() {
        int selectedRow = javaVersionsTable.getSelectedRow();
        boolean hasSelection = selectedRow != -1;
        removeJavaBtn.setEnabled(hasSelection);
        setDefaultJavaBtn.setEnabled(hasSelection);
    }

    private void updatePythonButtons() {
        int selectedRow = pythonVersionsTable.getSelectedRow();
        boolean hasSelection = selectedRow != -1;
        removePythonBtn.setEnabled(hasSelection);
        setDefaultPythonBtn.setEnabled(hasSelection);
    }

    private void updateBackupSettings() {
        boolean enabled = autoBackupCheckBox.isSelected();
        backupIntervalSpinner.setEnabled(enabled);
        backupPathField.setEnabled(enabled);
        backupBrowseBtn.setEnabled(enabled);
    }

    // 辅助方法
    private String getJavaVersion(String javaPath) {
        try {
            Process process = new ProcessBuilder(javaPath, "-version").start();
            process.waitFor();
            // 这里应该解析版本输出，简化返回
            return "未知版本";
        } catch (Exception e) {
            return "无效路径";
        }
    }

    private String getPythonVersion(String pythonPath) {
        try {
            Process process = new ProcessBuilder(pythonPath, "--version").start();
            process.waitFor();
            // 这里应该解析版本输出，简化返回
            return "未知版本";
        } catch (Exception e) {
            return "无效路径";
        }
    }
}
