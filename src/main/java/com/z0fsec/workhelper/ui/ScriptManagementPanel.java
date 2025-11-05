package com.z0fsec.workhelper.ui;

import com.z0fsec.workhelper.db.DatabaseManager;
import com.z0fsec.workhelper.model.Script;


import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class ScriptManagementPanel extends BasePanel {
    private JTable scriptTable;
    private DefaultTableModel tableModel;
    private JTextArea scriptEditor;
    private JComboBox<String> pythonInterpreter;
    private JComboBox<String> scriptTypeFilter;
    private JTextField searchField;
    private List<Script> scriptList;
    private List<Script> filteredScriptList;
    private DatabaseManager dbManager;
    private Script currentScript;
    private boolean isModified = false;

    public ScriptManagementPanel(Consumer<String> logConsumer) {
        super(logConsumer);
        this.dbManager = DatabaseManager.getInstance();
        this.scriptList = new ArrayList<>();
        this.filteredScriptList = new ArrayList<>();
        initUI();
        loadScriptsFromDatabase();
        refreshScriptTable();

        ensureLogInitialized();
        appendLog("脚本管理面板初始化完成");
    }

    private void initUI() {
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // 创建工具栏和过滤面板
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.add(createToolBar(), BorderLayout.NORTH);
        topPanel.add(createFilterPanel(), BorderLayout.SOUTH);

        // 创建分割面板
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setLeftComponent(createScriptListPanel());
        splitPane.setRightComponent(createEditorPanel());
        splitPane.setDividerLocation(400);

        add(topPanel, BorderLayout.NORTH);
        add(splitPane, BorderLayout.CENTER);
        add(createStatusBar(), BorderLayout.SOUTH);
    }

    private JPanel createToolBar() {
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT));

        JButton newBtn = new JButton("新建脚本");
        newBtn.addActionListener(this::newScript);

        JButton openBtn = new JButton("打开脚本");
        openBtn.addActionListener(this::openScript);

        JButton saveBtn = new JButton("保存脚本");
        saveBtn.addActionListener(this::saveScript);

        JButton saveAsBtn = new JButton("另存为");
        saveAsBtn.addActionListener(this::saveAsScript);

        JButton runBtn = new JButton("运行脚本");
        runBtn.addActionListener(this::runScript);

        JButton stopBtn = new JButton("停止");
        stopBtn.addActionListener(this::stopScript);

        JButton importBtn = new JButton("导入脚本");
        importBtn.addActionListener(this::importScript);

        JButton refreshBtn = new JButton("刷新");
        refreshBtn.addActionListener(e -> refreshScripts());

        // 获取Python解释器设置
        String defaultPython = dbManager.getSetting("default_python_path");
        pythonInterpreter = new JComboBox<>(new String[]{
                "python", "python3", defaultPython != null ? defaultPython : "python", "自定义路径"
        });

        toolbar.add(newBtn);
        toolbar.add(openBtn);
        toolbar.add(saveBtn);
        toolbar.add(saveAsBtn);
        toolbar.add(runBtn);
        toolbar.add(stopBtn);
        toolbar.add(importBtn);
        toolbar.add(refreshBtn);
        toolbar.add(new JLabel("Python解释器:"));
        toolbar.add(pythonInterpreter);

        return toolbar;
    }

    private JPanel createFilterPanel() {
        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));

        searchField = new JTextField(20);
        searchField.setToolTipText("搜索脚本名称或描述");

        scriptTypeFilter = new JComboBox<>(new String[]{"全部类型", "Python脚本", "Shell脚本", "批处理文件", "PowerShell脚本", "其他"});

        JButton searchBtn = new JButton("搜索");
        searchBtn.addActionListener(e -> filterScripts());

        JButton clearBtn = new JButton("清除");
        clearBtn.addActionListener(e -> clearFilters());

        filterPanel.add(new JLabel("搜索:"));
        filterPanel.add(searchField);
        filterPanel.add(new JLabel("类型:"));
        filterPanel.add(scriptTypeFilter);
        filterPanel.add(searchBtn);
        filterPanel.add(clearBtn);

        return filterPanel;
    }

    private JPanel createScriptListPanel() {
        JPanel panel = new JPanel(new BorderLayout());

        // 创建表格模型
        String[] columns = {"脚本名称", "类型", "路径", "最后修改", "状态"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        scriptTable = new JTable(tableModel);
        scriptTable.getSelectionModel().addListSelectionListener(e -> loadSelectedScript());

        // 设置行高
        scriptTable.setRowHeight(25);

        JScrollPane scrollPane = new JScrollPane(scriptTable);
        panel.add(scrollPane, BorderLayout.CENTER);
        panel.setBorder(BorderFactory.createTitledBorder("脚本列表"));
        return panel;
    }

    private JPanel createEditorPanel() {
        JPanel panel = new JPanel(new BorderLayout());

        scriptEditor = new JTextArea();
        scriptEditor.setFont(new Font("Consolas", Font.PLAIN, 12));
        scriptEditor.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) { setModified(true); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { setModified(true); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { setModified(true); }
        });

        JScrollPane editorScroll = new JScrollPane(scriptEditor);
        editorScroll.setBorder(BorderFactory.createTitledBorder("脚本编辑器"));

        panel.add(editorScroll, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createStatusBar() {
        JPanel statusBar = new JPanel(new BorderLayout());
        JLabel statusLabel = new JLabel("就绪");
        statusBar.add(statusLabel, BorderLayout.WEST);
        return statusBar;
    }

    private void setModified(boolean modified) {
        this.isModified = modified;
        // 可以在这里更新状态栏显示修改状态
    }

    private void loadScriptsFromDatabase() {
        try {
            scriptList.clear();
            scriptList.addAll(dbManager.getAllScripts());

            if (scriptList.isEmpty()) {
                appendInfo("脚本列表为空，正在添加示例脚本...");
                addSampleScripts();
            }

            filteredScriptList.clear();
            filteredScriptList.addAll(scriptList);

        } catch (Exception e) {
            appendError("从数据库加载脚本失败: " + e.getMessage());
        }
    }

    private void addSampleScripts() {
        // 创建示例脚本目录
        File scriptDir = new File("scripts");
        if (!scriptDir.exists()) {
            scriptDir.mkdirs();
        }

        // 示例脚本
        List<Script> sampleScripts = List.of(
                createSampleScript("port_scanner.py", "Python脚本",
                        "端口扫描工具", "扫描工具", "python", ""),
                createSampleScript("web_crawler.py", "Python脚本",
                        "网络爬虫", "爬虫工具", "python", ""),
                createSampleScript("vuln_scanner.py", "Python脚本",
                        "漏洞扫描", "安全扫描", "python", "")
        );

        for (Script script : sampleScripts) {
            try {
                // 创建示例脚本文件
                createSampleScriptFile(script);
                dbManager.addScript(script);
                scriptList.add(script);
            } catch (Exception e) {
                appendError("添加示例脚本失败: " + script.getName() + " - " + e.getMessage());
            }
        }

        if (!sampleScripts.isEmpty()) {
            appendSuccess("已添加 " + sampleScripts.size() + " 个示例脚本");
        }
    }

    private Script createSampleScript(String name, String type, String description, String category, String interpreter, String parameters) {
        Script script = new Script(name, type, "scripts/" + name, description, category, interpreter, parameters);
        script.setContent(generateSampleScriptContent(name));
        return script;
    }

    private String generateSampleScriptContent(String scriptName) {
        return "# " + scriptName + "\n" +
                "# 自动生成的示例脚本\n" +
                "# 创建时间: " + new java.util.Date() + "\n\n" +
                "import sys\nimport os\n\ndef main():\n" +
                "    print(\"正在执行: " + scriptName + "\")\n" +
                "    print(\"当前工作目录:\", os.getcwd())\n" +
                "    print(\"Python版本:\", sys.version)\n" +
                "    # 在这里添加您的代码\n\n" +
                "if __name__ == \"__main__\":\n" +
                "    main()";
    }

    private void createSampleScriptFile(Script script) throws IOException {
        File scriptFile = new File(script.getPath());
        try (FileWriter writer = new FileWriter(scriptFile)) {
            writer.write(script.getContent());
        }
    }

    private void refreshScriptTable() {
        tableModel.setRowCount(0);

        for (Script script : filteredScriptList) {
            tableModel.addRow(new Object[]{
                    script.getName(),
                    script.getType(),
                    script.getPath(),
                    script.getUpdatedTime() != null ?
                            script.getUpdatedTime().toString().substring(0, 16) : "未知",
                    script.getStatus()
            });
        }
    }

    // 事件处理方法
    private void newScript(ActionEvent e) {
        ScriptDialog dialog = new ScriptDialog((JFrame) SwingUtilities.getWindowAncestor(this), "新建脚本", null);
        dialog.setVisible(true);

        if (dialog.isConfirmed()) {
            try {
                Script newScript = dialog.getScript();
                // 创建脚本文件
                createScriptFile(newScript);
                dbManager.addScript(newScript);
                scriptList.add(newScript);
                filterScripts();
                appendSuccess("已创建新脚本: " + newScript.getName());
            } catch (Exception ex) {
                appendError("创建脚本失败: " + ex.getMessage());
            }
        }
    }

    private void openScript(ActionEvent e) {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("打开脚本文件");
        fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(
                "脚本文件 (*.py, *.sh, *.bat, *.ps1)", "py", "sh", "bat", "ps1"));

        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            importScriptFile(selectedFile);
        }
    }

    private void saveScript(ActionEvent e) {
        if (currentScript == null) {
            appendWarning("没有选中的脚本");
            return;
        }

        try {
            // 更新脚本内容
            currentScript.setContent(scriptEditor.getText());
            dbManager.updateScript(currentScript);

            // 保存到文件
            saveScriptToFile(currentScript);

            isModified = false;
            appendSuccess("脚本已保存: " + currentScript.getName());
            refreshScripts();
        } catch (Exception ex) {
            appendError("保存脚本失败: " + ex.getMessage());
        }
    }

    private void saveAsScript(ActionEvent e) {
        if (currentScript == null) {
            appendWarning("没有选中的脚本");
            return;
        }

        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("另存为脚本文件");
        fileChooser.setSelectedFile(new File(currentScript.getName()));

        int result = fileChooser.showSaveDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            try {
                // 创建新脚本
                Script newScript = new Script(
                        selectedFile.getName(),
                        currentScript.getType(),
                        selectedFile.getAbsolutePath(),
                        currentScript.getDescription(),
                        currentScript.getCategory(),
                        currentScript.getInterpreter(),
                        currentScript.getParameters()
                );
                newScript.setContent(scriptEditor.getText());

                // 保存文件
                saveScriptToFile(newScript);
                dbManager.addScript(newScript);
                scriptList.add(newScript);
                filterScripts();

                appendSuccess("脚本已另存为: " + newScript.getName());
            } catch (Exception ex) {
                appendError("另存为失败: " + ex.getMessage());
            }
        }
    }

//    private void runScript(ActionEvent e) {
//        if (currentScript == null) {
//            appendWarning("请先选择一个脚本");
//            return;
//        }
//
//        appendInfo("正在运行脚本: " + currentScript.getName());
//
//        try {
//            // 更新状态
//            dbManager.updateScriptStatus(currentScript.getId(), "运行中");
//            dbManager.updateLastRunTime(currentScript.getId());
//            currentScript.setStatus("运行中");
//            refreshScriptTable();
//
//            // 执行脚本
//            new Thread(() -> {
//                try {
//                    Process process = startScriptProcess(currentScript);
//                    if (process != null) {
//                        monitorScriptProcess(currentScript, process);
//                    }
//                } catch (Exception ex) {
//                    SwingUtilities.invokeLater(() -> {
//                        appendError("启动脚本失败: " + ex.getMessage());
//                        updateScriptStatus(currentScript, "错误", "启动失败");
//                    });
//                }
//            }).start();
//
//        } catch (Exception ex) {
//            appendError("更新脚本状态失败: " + ex.getMessage());
//        }
//    }

    private void stopScript(ActionEvent e) {
        // 实现停止脚本的功能
        appendInfo("停止脚本功能开发中...");
    }

    private void importScript(ActionEvent e) {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("导入脚本文件");
        fileChooser.setMultiSelectionEnabled(true);
        fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(
                "脚本文件 (*.py, *.sh, *.bat, *.ps1)", "py", "sh", "bat", "ps1"));

        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File[] selectedFiles = fileChooser.getSelectedFiles();
            for (File file : selectedFiles) {
                importScriptFile(file);
            }
        }
    }

    private void importScriptFile(File file) {
        try {
            // 读取文件内容
            String content = new String(Files.readAllBytes(file.toPath()));

            // 确定脚本类型
            String type = getScriptType(file.getName());

            // 创建脚本对象
            Script script = new Script(
                    file.getName(),
                    type,
                    file.getAbsolutePath(),
                    "导入的脚本",
                    "导入",
                    getDefaultInterpreter(type),
                    ""
            );
            script.setContent(content);

            // 保存到数据库
            dbManager.addScript(script);
            scriptList.add(script);

            appendSuccess("已导入脚本: " + script.getName());
            filterScripts();

        } catch (Exception ex) {
            appendError("导入脚本失败: " + file.getName() + " - " + ex.getMessage());
        }
    }

    private void refreshScripts() {
        appendInfo("刷新脚本列表...");
        loadScriptsFromDatabase();
        filterScripts();
        appendSuccess("脚本列表已刷新");
    }

    private void filterScripts() {
        String searchText = searchField.getText().toLowerCase();
        String selectedType = (String) scriptTypeFilter.getSelectedItem();

        filteredScriptList.clear();

        for (Script script : scriptList) {
            boolean matches = true;

            // 搜索过滤
            if (!searchText.isEmpty()) {
                matches = script.getName().toLowerCase().contains(searchText) ||
                        (script.getDescription() != null &&
                                script.getDescription().toLowerCase().contains(searchText)) ||
                        (script.getCategory() != null &&
                                script.getCategory().toLowerCase().contains(searchText));
            }

            // 类型过滤
            if (matches && !"全部类型".equals(selectedType)) {
                matches = script.getType().equals(selectedType);
            }

            if (matches) {
                filteredScriptList.add(script);
            }
        }

        refreshScriptTable();
    }

    private void clearFilters() {
        searchField.setText("");
        scriptTypeFilter.setSelectedIndex(0);
        filterScripts();
    }

    private void loadSelectedScript() {
        int selectedRow = scriptTable.getSelectedRow();
        if (selectedRow != -1 && selectedRow < filteredScriptList.size()) {
            currentScript = filteredScriptList.get(selectedRow);

            try {
                // 从文件读取内容或使用数据库中的内容
                String content;
                File scriptFile = new File(currentScript.getPath());
                if (scriptFile.exists()) {
                    content = new String(Files.readAllBytes(scriptFile.toPath()));
                } else {
                    content = currentScript.getContent() != null ?
                            currentScript.getContent() : "# 脚本文件不存在\n";
                }

                scriptEditor.setText(content);
                isModified = false;

            } catch (Exception ex) {
                appendError("加载脚本内容失败: " + ex.getMessage());
                scriptEditor.setText("# 加载脚本失败\n# " + ex.getMessage());
            }
        }
    }

    // 脚本执行相关方法 - 修改为打开命令行窗口执行
    private Process startScriptProcess(Script script) throws IOException {
        ProcessBuilder processBuilder = new ProcessBuilder();
        File scriptFile = new File(script.getPath());

        if (!scriptFile.exists()) {
            throw new IOException("脚本文件不存在: " + script.getPath());
        }

        File scriptDir = scriptFile.getParentFile();
        String scriptName = scriptFile.getName();

        // 根据脚本类型构建不同的命令行启动命令
        switch (script.getType()) {
            case "Python脚本":
                // 打开cmd窗口，切换到脚本目录，执行Python脚本
                String pythonCommand = String.format("cmd /c start \"%s\" /D \"%s\" cmd /k \"%s \"%s\" %s\"",
                        script.getName() + " - Python脚本",
                        scriptDir.getAbsolutePath(),
                        getPythonInterpreter(script),
                        scriptFile.getAbsolutePath(),
                        script.getParameters() != null ? script.getParameters() : ""
                );
                processBuilder.command("cmd", "/c", pythonCommand);
                break;

            case "Shell脚本":
                if (System.getProperty("os.name").toLowerCase().contains("windows")) {
                    // Windows系统下，使用Git Bash或WSL
                    String shellCommand = String.format("cmd /c start \"%s\" /D \"%s\" bash -c \"cd '%s'; ./'%s' %s; exec bash\"",
                            script.getName() + " - Shell脚本",
                            scriptDir.getAbsolutePath(),
                            scriptDir.getAbsolutePath(),
                            scriptName,
                            script.getParameters() != null ? script.getParameters() : ""
                    );
                    processBuilder.command("cmd", "/c", shellCommand);
                } else {
                    // Linux/Mac系统
                    String shellCommand = String.format("xterm -title \"%s\" -e \"cd '%s'; sh '%s' %s; echo '按任意键退出...'; read\"",
                            script.getName() + " - Shell脚本",
                            scriptDir.getAbsolutePath(),
                            scriptFile.getAbsolutePath(),
                            script.getParameters() != null ? script.getParameters() : ""
                    );
                    processBuilder.command("sh", "-c", shellCommand);
                }
                break;

            case "批处理文件":
                // 打开cmd窗口执行批处理文件
                String batchCommand = String.format("cmd /c start \"%s\" /D \"%s\" \"%s\" %s",
                        script.getName() + " - 批处理",
                        scriptDir.getAbsolutePath(),
                        scriptFile.getAbsolutePath(),
                        script.getParameters() != null ? script.getParameters() : ""
                );
                processBuilder.command("cmd", "/c", batchCommand);
                break;

            case "PowerShell脚本":
                // 打开PowerShell窗口执行脚本
                String powershellCommand = String.format("cmd /c start \"%s\" /D \"%s\" powershell -NoExit -Command \"& '%s' %s\"",
                        script.getName() + " - PowerShell",
                        scriptDir.getAbsolutePath(),
                        scriptFile.getAbsolutePath(),
                        script.getParameters() != null ? script.getParameters() : ""
                );
                processBuilder.command("cmd", "/c", powershellCommand);
                break;

            default:
                // 对于其他类型，打开cmd窗口到脚本目录
                String defaultCommand = String.format("cmd /c start \"%s\" /D \"%s\" cmd /k echo 脚本目录: %s && dir",
                        script.getName(),
                        scriptDir.getAbsolutePath(),
                        scriptDir.getAbsolutePath()
                );
                processBuilder.command("cmd", "/c", defaultCommand);
        }

        appendInfo("打开命令行执行: " + String.join(" ", processBuilder.command()));
        return processBuilder.start();
    }

    // 获取Python解释器
    private String getPythonInterpreter(Script script) {
        String interpreter = script.getInterpreter();
        if (interpreter != null && !interpreter.isEmpty() && !"自定义".equals(interpreter)) {
            return interpreter;
        }

        // 使用设置中的默认Python解释器
        String defaultPython = dbManager.getSetting("default_python_path");
        return defaultPython != null ? defaultPython : "python";
    }

    // 修改运行按钮的行为，提供执行选项
    private void runScriptWithOptions(Script script) {
        String[] options = {"在命令行中执行", "在后台执行", "编辑脚本", "打开脚本目录"};

        int choice = JOptionPane.showOptionDialog(
                this,
                "请选择执行方式:\n" +
                        "脚本: " + script.getName() + "\n" +
                        "路径: " + script.getPath(),
                "脚本执行选项 - " + script.getName(),
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null,
                options,
                options[0]
        );

        switch (choice) {
            case 0: // 在命令行中执行
                runScriptInTerminal(script);
                break;
            case 1: // 在后台执行
                runScriptInBackground(script);
                break;
            case 2: // 编辑脚本
                editScript(script);
                break;
            case 3: // 打开脚本目录
                openScriptDirectory(script);
                break;
            default:
                // 取消操作
                break;
        }
    }

    // 在命令行中执行脚本
    private void runScriptInTerminal(Script script) {
        appendInfo("正在为脚本 " + script.getName() + " 打开命令行窗口...");

        try {
            // 更新状态
            dbManager.updateScriptStatus(script.getId(), "运行中");
            dbManager.updateLastRunTime(script.getId());
            script.setStatus("运行中");
            refreshScriptTable();

            Process process = startScriptProcess(script);

            // 监控进程状态
            new Thread(() -> {
                try {
                    int exitCode = process.waitFor();
                    SwingUtilities.invokeLater(() -> {
                        if (exitCode == 0) {
                            appendSuccess("脚本 " + script.getName() + " 命令行窗口已关闭 (退出码: " + exitCode + ")");
                            updateScriptStatus(script, "就绪", "执行完成");
                        } else {
                            appendWarning("脚本 " + script.getName() + " 命令行窗口已关闭 (退出码: " + exitCode + ")");
                            updateScriptStatus(script, "就绪", "执行完成，退出码: " + exitCode);
                        }
                    });
                } catch (InterruptedException ex) {
                    SwingUtilities.invokeLater(() -> {
                        appendError("脚本执行监控被中断: " + ex.getMessage());
                        updateScriptStatus(script, "错误", "监控中断");
                    });
                }
            }).start();

        } catch (Exception ex) {
            appendError("打开命令行窗口失败: " + ex.getMessage());
            updateScriptStatus(script, "错误", "启动失败: " + ex.getMessage());
        }
    }

    // 在后台执行脚本（原来的执行方式）
    private void runScriptInBackground(Script script) {
        appendInfo("正在后台执行脚本: " + script.getName());

        try {
            // 更新状态
            dbManager.updateScriptStatus(script.getId(), "运行中");
            dbManager.updateLastRunTime(script.getId());
            script.setStatus("运行中");
            refreshScriptTable();

            // 使用原来的执行逻辑
            Process process = startBackgroundScriptProcess(script);
            if (process != null) {
                monitorScriptProcess(script, process);
            }

        } catch (Exception ex) {
            appendError("执行脚本失败: " + ex.getMessage());
            updateScriptStatus(script, "错误", "执行失败: " + ex.getMessage());
        }
    }

    // 后台执行脚本的原始方法
    private Process startBackgroundScriptProcess(Script script) throws IOException {
        ProcessBuilder processBuilder = new ProcessBuilder();
        File scriptFile = new File(script.getPath());

        if (!scriptFile.exists()) {
            throw new IOException("脚本文件不存在: " + script.getPath());
        }

        String interpreter = script.getInterpreter() != null ?
                script.getInterpreter() : getDefaultInterpreter(script.getType());

        switch (script.getType()) {
            case "Python脚本":
                processBuilder.command(interpreter, scriptFile.getAbsolutePath());
                break;
            case "Shell脚本":
                processBuilder.command("sh", scriptFile.getAbsolutePath());
                break;
            case "批处理文件":
                processBuilder.command("cmd", "/c", scriptFile.getAbsolutePath());
                break;
            case "PowerShell脚本":
                processBuilder.command("powershell", "-File", scriptFile.getAbsolutePath());
                break;
            default:
                processBuilder.command(interpreter, scriptFile.getAbsolutePath());
        }

        // 设置工作目录
        File scriptDir = scriptFile.getParentFile();
        if (scriptDir != null && scriptDir.exists()) {
            processBuilder.directory(scriptDir);
        }

        processBuilder.redirectErrorStream(true);
        appendInfo("后台执行命令: " + String.join(" ", processBuilder.command()));

        return processBuilder.start();
    }

    // 打开脚本目录
    private void openScriptDirectory(Script script) {
        File scriptDir = new File(script.getPath()).getParentFile();
        if (scriptDir == null || !scriptDir.exists()) {
            appendError("无法找到脚本目录: " + script.getPath());
            return;
        }

        try {
            String os = System.getProperty("os.name").toLowerCase();
            ProcessBuilder processBuilder = new ProcessBuilder();

            if (os.contains("windows")) {
                processBuilder.command("explorer", scriptDir.getAbsolutePath());
            } else if (os.contains("mac")) {
                processBuilder.command("open", scriptDir.getAbsolutePath());
            } else {
                processBuilder.command("xdg-open", scriptDir.getAbsolutePath());
            }

            processBuilder.start();
            appendSuccess("已打开脚本目录: " + scriptDir.getAbsolutePath());

        } catch (IOException ex) {
            appendError("打开脚本目录失败: " + ex.getMessage());
        }
    }

    // 编辑脚本
    private void editScript(Script script) {
        // 确保当前脚本被选中
        currentScript = script;
        loadSelectedScript();
        appendInfo("已切换到脚本编辑: " + script.getName());
    }

    // 修改运行按钮的事件处理
    private void runScript(ActionEvent e) {
        if (currentScript == null) {
            appendWarning("请先选择一个脚本");
            return;
        }

        // 使用新的执行选项
        runScriptWithOptions(currentScript);
    }

    private void monitorScriptProcess(Script script, Process process) {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream(), getSystemEncoding()))) {

            String line;
            while ((line = reader.readLine()) != null) {
                final String outputLine = line;
                SwingUtilities.invokeLater(() -> {
                    appendInfo("[ " + script.getName() + " ] " + outputLine);
                });
            }

            int exitCode = process.waitFor();

            SwingUtilities.invokeLater(() -> {
                if (exitCode == 0) {
                    appendSuccess("脚本 " + script.getName() + " 执行完成 (退出码: " + exitCode + ")");
                    updateScriptStatus(script, "就绪", "执行成功");
                } else {
                    appendWarning("脚本 " + script.getName() + " 执行完成 (退出码: " + exitCode + ")");
                    updateScriptStatus(script, "就绪", "执行完成，退出码: " + exitCode);
                }
            });

        } catch (IOException | InterruptedException ex) {
            SwingUtilities.invokeLater(() -> {
                appendError("脚本执行异常: " + ex.getMessage());
                updateScriptStatus(script, "错误", "执行异常");
            });
        }
    }

    private void updateScriptStatus(Script script, String status, String message) {
        try {
            dbManager.updateScriptStatus(script.getId(), status);
            script.setStatus(status);
            refreshScriptTable();

            if (message != null) {
                switch (status) {
                    case "运行中": appendInfo(message); break;
                    case "就绪": appendSuccess(message); break;
                    case "错误": appendError(message); break;
                    default: appendInfo(message);
                }
            }
        } catch (Exception ex) {
            appendError("更新脚本状态失败: " + ex.getMessage());
        }
    }

    // 辅助方法
    private String getScriptType(String fileName) {
        if (fileName.endsWith(".py")) return "Python脚本";
        if (fileName.endsWith(".sh")) return "Shell脚本";
        if (fileName.endsWith(".bat")) return "批处理文件";
        if (fileName.endsWith(".ps1")) return "PowerShell脚本";
        return "其他";
    }

    private String getDefaultInterpreter(String scriptType) {
        switch (scriptType) {
            case "Python脚本": return "python";
            case "Shell脚本": return "sh";
            case "批处理文件": return "cmd";
            case "PowerShell脚本": return "powershell";
            default: return "";
        }
    }

    private String getSystemEncoding() {
        return System.getProperty("os.name").toLowerCase().contains("windows") ? "GBK" : "UTF-8";
    }

    private void createScriptFile(Script script) throws IOException {
        File scriptFile = new File(script.getPath());
        // 确保目录存在
        File parentDir = scriptFile.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            parentDir.mkdirs();
        }

        try (FileWriter writer = new FileWriter(scriptFile)) {
            writer.write(script.getContent() != null ? script.getContent() : "");
        }
    }

    private void saveScriptToFile(Script script) throws IOException {
        createScriptFile(script);
    }
    // 增强的命令行执行方法，支持更多选项
    private void openAdvancedTerminal(Script script) {
        String[] terminalOptions = {
                "CMD命令行",
                "PowerShell",
                "Git Bash (如果已安装)",
                "WSL (如果已安装)",
                "仅打开目录"
        };

        int choice = JOptionPane.showOptionDialog(
                this,
                "选择命令行类型:\n" +
                        "脚本: " + script.getName() + "\n" +
                        "目录: " + new File(script.getPath()).getParentFile().getAbsolutePath(),
                "高级终端选项 - " + script.getName(),
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null,
                terminalOptions,
                terminalOptions[0]
        );

        File scriptDir = new File(script.getPath()).getParentFile();

        try {
            ProcessBuilder processBuilder = new ProcessBuilder();
            String command = "";

            switch (choice) {
                case 0: // CMD
                    command = String.format("cmd /c start \"%s\" /D \"%s\" cmd /k echo 脚本目录: %s && dir",
                            script.getName(), scriptDir.getAbsolutePath(), scriptDir.getAbsolutePath());
                    processBuilder.command("cmd", "/c", command);
                    break;

                case 1: // PowerShell
                    command = String.format("cmd /c start \"%s\" /D \"%s\" powershell -NoExit -Command \"Write-Host '脚本目录: %s'; Get-Location\"",
                            script.getName(), scriptDir.getAbsolutePath(), scriptDir.getAbsolutePath());
                    processBuilder.command("cmd", "/c", command);
                    break;

                case 2: // Git Bash
                    command = String.format("cmd /c start \"%s\" /D \"%s\" \"C:\\Program Files\\Git\\bin\\bash.exe\" --login -i",
                            script.getName() + " - Git Bash", scriptDir.getAbsolutePath());
                    processBuilder.command("cmd", "/c", command);
                    break;

                case 3: // WSL
                    command = String.format("cmd /c start \"%s\" /D \"%s\" wsl",
                            script.getName() + " - WSL", scriptDir.getAbsolutePath());
                    processBuilder.command("cmd", "/c", command);
                    break;

                case 4: // 仅打开目录
                    openScriptDirectory(script);
                    return;

                default:
                    return;
            }

            processBuilder.start();
            appendSuccess("已打开" + terminalOptions[choice] + "到脚本目录");

        } catch (IOException ex) {
            appendError("打开终端失败: " + ex.getMessage());
        }
    }
}