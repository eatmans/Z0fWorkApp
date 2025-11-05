package com.z0fsec.workhelper.ui;



import com.z0fsec.workhelper.model.Script;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;

public class ScriptDialog extends JDialog {
    private boolean confirmed = false;
    private Script script;
    
    private JTextField nameField;
    private JComboBox<String> typeComboBox;
    private JTextField pathField;
    private JTextField descriptionField;
    private JComboBox<String> categoryComboBox;
    private JComboBox<String> interpreterComboBox;
    private JTextField parametersField;
    private JTextArea contentArea;
    
    public ScriptDialog(JFrame parent, String title, Script existingScript) {
        super(parent, title, true);
        this.script = existingScript;
        initUI();
        pack();
        setLocationRelativeTo(parent);
        setResizable(true);
    }
    
    private void initUI() {
        setLayout(new BorderLayout());
        setMinimumSize(new Dimension(600, 500));
        
        // 主面板
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        // 表单面板
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBorder(BorderFactory.createTitledBorder("脚本信息"));
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        
        // 脚本名称
        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0;
        formPanel.add(new JLabel("脚本名称:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1.0;
        nameField = new JTextField(20);
        formPanel.add(nameField, gbc);
        
        // 脚本类型
        gbc.gridx = 0; gbc.gridy = 1;
        formPanel.add(new JLabel("脚本类型:"), gbc);
        gbc.gridx = 1;
        typeComboBox = new JComboBox<>(new String[]{
            "Python脚本", "Shell脚本", "批处理文件", "PowerShell脚本", "其他"
        });
        typeComboBox.addActionListener(e -> updateInterpreterOptions());
        formPanel.add(typeComboBox, gbc);
        
        // 文件路径
        gbc.gridx = 0; gbc.gridy = 2;
        formPanel.add(new JLabel("文件路径:"), gbc);
        gbc.gridx = 1;
        pathField = new JTextField(20);
        formPanel.add(pathField, gbc);
        gbc.gridx = 2; gbc.weightx = 0;
        JButton browseBtn = new JButton("浏览");
        browseBtn.addActionListener(this::browseFile);
        formPanel.add(browseBtn, gbc);
        
        // 描述
        gbc.gridx = 0; gbc.gridy = 3; gbc.gridwidth = 1;
        formPanel.add(new JLabel("描述:"), gbc);
        gbc.gridx = 1; gbc.gridwidth = 2;
        descriptionField = new JTextField(20);
        formPanel.add(descriptionField, gbc);
        
        // 分类
        gbc.gridx = 0; gbc.gridy = 4; gbc.gridwidth = 1;
        formPanel.add(new JLabel("分类:"), gbc);
        gbc.gridx = 1; gbc.gridwidth = 2;
        categoryComboBox = new JComboBox<>(new String[]{
            "扫描工具", "爬虫工具", "安全工具", "网络工具", "系统工具", "数据分析", "其他"
        });
        formPanel.add(categoryComboBox, gbc);
        
        // 解释器
        gbc.gridx = 0; gbc.gridy = 5; gbc.gridwidth = 1;
        formPanel.add(new JLabel("解释器:"), gbc);
        gbc.gridx = 1; gbc.gridwidth = 2;
        interpreterComboBox = new JComboBox<>();
        updateInterpreterOptions();
        formPanel.add(interpreterComboBox, gbc);
        
        // 参数
        gbc.gridx = 0; gbc.gridy = 6; gbc.gridwidth = 1;
        formPanel.add(new JLabel("参数:"), gbc);
        gbc.gridx = 1; gbc.gridwidth = 2;
        parametersField = new JTextField(20);
        parametersField.setToolTipText("执行脚本时使用的命令行参数");
        formPanel.add(parametersField, gbc);
        
        mainPanel.add(formPanel, BorderLayout.NORTH);
        
        // 内容编辑区域
        JPanel contentPanel = new JPanel(new BorderLayout());
        contentPanel.setBorder(BorderFactory.createTitledBorder("脚本内容"));
        
        contentArea = new JTextArea(15, 50);
        contentArea.setFont(new Font("Consolas", Font.PLAIN, 12));
        JScrollPane contentScroll = new JScrollPane(contentArea);
        
        contentPanel.add(contentScroll, BorderLayout.CENTER);
        mainPanel.add(contentPanel, BorderLayout.CENTER);
        
        // 按钮面板
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton okBtn = new JButton("确定");
        okBtn.addActionListener(e -> confirm());
        
        JButton cancelBtn = new JButton("取消");
        cancelBtn.addActionListener(e -> cancel());
        
        JButton generateBtn = new JButton("生成模板");
        generateBtn.addActionListener(this::generateTemplate);
        
        buttonPanel.add(generateBtn);
        buttonPanel.add(Box.createHorizontalStrut(20));
        buttonPanel.add(okBtn);
        buttonPanel.add(cancelBtn);
        
        add(mainPanel, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);
        
        // 如果是编辑模式，填充现有数据
        if (script != null) {
            nameField.setText(script.getName());
            typeComboBox.setSelectedItem(script.getType());
            pathField.setText(script.getPath());
            descriptionField.setText(script.getDescription());
            if (script.getCategory() != null) {
                categoryComboBox.setSelectedItem(script.getCategory());
            }
            if (script.getInterpreter() != null) {
                interpreterComboBox.setSelectedItem(script.getInterpreter());
            }
            parametersField.setText(script.getParameters());
            if (script.getContent() != null) {
                contentArea.setText(script.getContent());
            }
        } else {
            // 新脚本，生成默认模板
            generateTemplate(null);
        }
        
        // 自动生成路径
        nameField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) { updatePath(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { updatePath(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { updatePath(); }
        });
        
        typeComboBox.addActionListener(e -> updatePath());
    }
    
    private void updateInterpreterOptions() {
        String selectedType = (String) typeComboBox.getSelectedItem();
        interpreterComboBox.removeAllItems();
        
        switch (selectedType) {
            case "Python脚本":
                interpreterComboBox.addItem("python");
                interpreterComboBox.addItem("python3");
                interpreterComboBox.addItem("py");
                break;
            case "Shell脚本":
                interpreterComboBox.addItem("sh");
                interpreterComboBox.addItem("bash");
                break;
            case "批处理文件":
                interpreterComboBox.addItem("cmd");
                break;
            case "PowerShell脚本":
                interpreterComboBox.addItem("powershell");
                interpreterComboBox.addItem("pwsh");
                break;
            default:
                interpreterComboBox.addItem("");
        }
        
        interpreterComboBox.addItem("自定义");
    }
    
    private void updatePath() {
        String name = nameField.getText().trim();
        String type = (String) typeComboBox.getSelectedItem();
        
        if (!name.isEmpty()) {
            String extension = getFileExtension(type);
            if (!name.endsWith(extension)) {
                name += extension;
            }
            pathField.setText("scripts/" + name);
        }
    }
    
    private String getFileExtension(String type) {
        switch (type) {
            case "Python脚本": return ".py";
            case "Shell脚本": return ".sh";
            case "批处理文件": return ".bat";
            case "PowerShell脚本": return ".ps1";
            default: return ".txt";
        }
    }
    
    private void browseFile(ActionEvent e) {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("选择脚本文件");
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
    
    private void generateTemplate(ActionEvent e) {
        String name = nameField.getText().trim();
        if (name.isEmpty()) {
            name = "new_script";
        }
        
        String type = (String) typeComboBox.getSelectedItem();
        String template = generateTemplateContent(name, type);
        contentArea.setText(template);
    }
    
    private String generateTemplateContent(String name, String type) {
        switch (type) {
            case "Python脚本":
                return generatePythonTemplate(name);
            case "Shell脚本":
                return generateShellTemplate(name);
            case "批处理文件":
                return generateBatchTemplate(name);
            case "PowerShell脚本":
                return generatePowerShellTemplate(name);
            default:
                return generateDefaultTemplate(name);
        }
    }
    
    private String generatePythonTemplate(String name) {
        return "# " + name + "\n" +
               "# 自动生成的Python脚本模板\n" +
               "# 创建时间: " + new java.util.Date() + "\n\n" +
               "import sys\nimport os\nimport argparse\n\ndef main():\n" +
               "    print(\"正在执行: " + name + "\")\n" +
               "    print(\"当前工作目录:\", os.getcwd())\n" +
               "    print(\"Python版本:\", sys.version)\n" +
               "    \n" +
               "    # 在这里添加您的代码\n" +
               "    print(\"Hello, World!\")\n\n" +
               "if __name__ == \"__main__\":\n" +
               "    main()";
    }
    
    private String generateShellTemplate(String name) {
        return "#!/bin/bash\n" +
               "# " + name + "\n" +
               "# 自动生成的Shell脚本模板\n" +
               "# 创建时间: " + new java.util.Date() + "\n\n" +
               "echo \"正在执行: " + name + "\"\n" +
               "echo \"当前工作目录: $(pwd)\"\n" +
               "echo \"Shell版本: $BASH_VERSION\"\n" +
               "\n" +
               "# 在这里添加您的代码\n" +
               "echo \"Hello, World!\"\n" +
               "\n" +
               "exit 0";
    }
    
    private String generateBatchTemplate(String name) {
        return "@echo off\n" +
               "REM " + name + "\n" +
               "REM 自动生成的批处理脚本模板\n" +
               "REM 创建时间: " + new java.util.Date() + "\n\n" +
               "echo 正在执行: " + name + "\n" +
               "echo 当前工作目录: %CD%\n" +
               "\n" +
               "REM 在这里添加您的代码\n" +
               "echo Hello, World!\n" +
               "\n" +
               "pause";
    }
    
    private String generatePowerShellTemplate(String name) {
        return "# " + name + "\n" +
               "# 自动生成的PowerShell脚本模板\n" +
               "# 创建时间: " + new java.util.Date() + "\n\n" +
               "Write-Host \"正在执行: " + name + "\"\n" +
               "Write-Host \"当前工作目录: $(Get-Location)\"\n" +
               "Write-Host \"PowerShell版本: $PSVersionTable.PSVersion\"\n" +
               "\n" +
               "# 在这里添加您的代码\n" +
               "Write-Host \"Hello, World!\"\n" +
               "\n" +
               "Read-Host \"按Enter键继续...\"";
    }
    
    private String generateDefaultTemplate(String name) {
        return "# " + name + "\n" +
               "# 自动生成的脚本模板\n" +
               "# 创建时间: " + new java.util.Date() + "\n\n" +
               "// 在这里添加您的代码\n" +
               "// Hello, World!";
    }
    
    private void confirm() {
        if (nameField.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "请输入脚本名称", "错误", JOptionPane.ERROR_MESSAGE);
            nameField.requestFocus();
            return;
        }
        
        if (pathField.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "请输入脚本文件路径", "错误", JOptionPane.ERROR_MESSAGE);
            pathField.requestFocus();
            return;
        }
        
        if (contentArea.getText().trim().isEmpty()) {
            int result = JOptionPane.showConfirmDialog(this, 
                "脚本内容为空，是否继续？", "确认", 
                JOptionPane.YES_NO_OPTION);
            if (result != JOptionPane.YES_OPTION) {
                return;
            }
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
    
    public Script getScript() {
        Script newScript = new Script(
            nameField.getText().trim(),
            (String) typeComboBox.getSelectedItem(),
            pathField.getText().trim(),
            descriptionField.getText().trim(),
            (String) categoryComboBox.getSelectedItem(),
            (String) interpreterComboBox.getSelectedItem(),
            parametersField.getText().trim()
        );
        newScript.setContent(contentArea.getText());
        
        // 如果是编辑模式，保持ID不变
        if (script != null) {
            newScript.setId(script.getId());
        }
        
        return newScript;
    }
}