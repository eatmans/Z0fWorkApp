package com.z0fsec.workhelper.ui;

import com.z0fsec.workhelper.db.DatabaseManager;
import com.z0fsec.workhelper.model.NetworkTemplate;
import com.z0fsec.workhelper.util.NetworkConfigurator;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.function.Consumer;

public class NetworkManagementPanel extends BasePanel {
    private JPanel networkCardsPanel;
    private JTable ipTemplateTable;
    private DefaultTableModel templateTableModel;
    private List<NetworkInterface> networkInterfaces;
    private JComboBox<String> interfaceComboBox;
    private JTextField templateNameField;
    private JTextField ipAddressField;
    private JTextField subnetField;
    private JTextField gatewayField;
    private JTextField dns1Field;
    private JTextField dns2Field;

    // 需要过滤的接口名称关键词
    private static final String[] FILTERED_INTERFACE_KEYWORDS = {
            "WAN", "Wi-Fi Direct", "Filter-0000", "Bluetooth", "VirtualBox", "VMware", "Teredo"
    };

    private List<NetworkTemplate> networkTemplates;
    private DatabaseManager dbManager;

    public NetworkManagementPanel(Consumer<String> logConsumer) {
        super(logConsumer);
        this.dbManager = DatabaseManager.getInstance();
        initUI();
        loadNetworkInterfaces();
        loadNetworkTemplates(); // 新增：从数据库加载模板
    }


    private void initUI() {
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // 创建主分割面板
        JSplitPane mainSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        mainSplitPane.setTopComponent(createNetworkInterfacePanel());
        mainSplitPane.setBottomComponent(createTemplateManagementPanel());
        mainSplitPane.setDividerLocation(500); // 增加分割线位置

        add(mainSplitPane, BorderLayout.CENTER);
        add(createToolBar(), BorderLayout.NORTH);
    }

    private JPanel createToolBar() {
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT));

        JButton refreshBtn = new JButton("刷新网络信息");
        refreshBtn.addActionListener(e -> refreshNetworkInfo());

        JButton disableAllBtn = new JButton("禁用所有网卡");
        disableAllBtn.addActionListener(e -> disableAllInterfaces());

        JButton enableAllBtn = new JButton("启用所有网卡");
        enableAllBtn.addActionListener(e -> enableAllInterfaces());

        toolbar.add(refreshBtn);
        toolbar.add(disableAllBtn);
        toolbar.add(enableAllBtn);

        return toolbar;
    }

    private JPanel createNetworkInterfacePanel() {
        JPanel panel = new JPanel(new BorderLayout());

        networkCardsPanel = new JPanel();
        // 使用GridBagLayout替代BoxLayout，避免内容压缩
        networkCardsPanel.setLayout(new GridBagLayout());

        JScrollPane scrollPane = new JScrollPane(networkCardsPanel);
        scrollPane.setBorder(BorderFactory.createTitledBorder("网络接口列表"));
        scrollPane.setPreferredSize(new Dimension(800, 400)); // 增加高度

        panel.add(scrollPane, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createTemplateManagementPanel() {
        JPanel panel = new JPanel(new BorderLayout());

        JSplitPane templateSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        templateSplitPane.setLeftComponent(createTemplateFormPanel());
        templateSplitPane.setRightComponent(createTemplateTablePanel());
        templateSplitPane.setDividerLocation(300);

        panel.add(templateSplitPane, BorderLayout.CENTER);
        panel.setBorder(BorderFactory.createTitledBorder("IP模板管理"));

        return panel;
    }

    private JPanel createTemplateFormPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // 模板名称
        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0;
        panel.add(new JLabel("模板名称:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1.0;
        templateNameField = new JTextField(15);
        panel.add(templateNameField, gbc);

        // 选择网卡
        gbc.gridx = 0; gbc.gridy = 1;
        panel.add(new JLabel("选择网卡:"), gbc);
        gbc.gridx = 1;
        interfaceComboBox = new JComboBox<>();
        panel.add(interfaceComboBox, gbc);

        // IP地址
        gbc.gridx = 0; gbc.gridy = 2;
        panel.add(new JLabel("IP地址:"), gbc);
        gbc.gridx = 1;
        ipAddressField = new JTextField(15);
        panel.add(ipAddressField, gbc);

        // 子网掩码
        gbc.gridx = 0; gbc.gridy = 3;
        panel.add(new JLabel("子网掩码:"), gbc);
        gbc.gridx = 1;
        subnetField = new JTextField(15);
        panel.add(subnetField, gbc);

        // 网关
        gbc.gridx = 0; gbc.gridy = 4;
        panel.add(new JLabel("网关:"), gbc);
        gbc.gridx = 1;
        gatewayField = new JTextField(15);
        panel.add(gatewayField, gbc);

        // DNS 1
        gbc.gridx = 0; gbc.gridy = 5;
        panel.add(new JLabel("DNS 1:"), gbc);
        gbc.gridx = 1;
        dns1Field = new JTextField(15);
        panel.add(dns1Field, gbc);

        // DNS 2
        gbc.gridx = 0; gbc.gridy = 6;
        panel.add(new JLabel("DNS 2:"), gbc);
        gbc.gridx = 1;
        dns2Field = new JTextField(15);
        panel.add(dns2Field, gbc);

        // 按钮面板
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JButton saveTemplateBtn = new JButton("保存模板");
        saveTemplateBtn.addActionListener(this::saveTemplate);

        JButton applyTemplateBtn = new JButton("应用模板");
        applyTemplateBtn.addActionListener(this::applyTemplate);

        JButton clearTemplateBtn = new JButton("清空表单");
        clearTemplateBtn.addActionListener(e -> clearTemplateForm());

        buttonPanel.add(saveTemplateBtn);
        buttonPanel.add(applyTemplateBtn);
        buttonPanel.add(clearTemplateBtn);

        gbc.gridx = 0; gbc.gridy = 7; gbc.gridwidth = 2;
        panel.add(buttonPanel, gbc);

        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        return panel;
    }

    private JPanel createTemplateTablePanel() {
        JPanel panel = new JPanel(new BorderLayout());

        // 创建表格模型
        String[] columns = {"模板名称", "网卡", "IP地址", "子网掩码", "网关", "操作"};
        templateTableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 5; // 只有操作列可编辑
            }
        };

        ipTemplateTable = new JTable(templateTableModel);
        ipTemplateTable.setRowHeight(30); // 增加行高
        ipTemplateTable.getColumnModel().getColumn(5).setCellRenderer(new ButtonRenderer());
        ipTemplateTable.getColumnModel().getColumn(5).setCellEditor(new ButtonEditor(new JCheckBox()));

        JScrollPane scrollPane = new JScrollPane(ipTemplateTable);
        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }

    private void loadNetworkInterfaces() {
        networkInterfaces = new ArrayList<>();
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            List<NetworkInterface> allInterfaces = Collections.list(interfaces);

            // 过滤接口
            for (NetworkInterface ni : allInterfaces) {
                if (shouldDisplayInterface(ni)) {
                    networkInterfaces.add(ni);
                    System.out.println("显示接口: " + ni.getDisplayName());
                } else {
                    System.out.println("过滤接口: " + ni.getDisplayName());
                }
            }

            refreshNetworkCards();
            refreshInterfaceComboBox();

        } catch (SocketException e) {
            appendError("获取网络接口失败: " + e.getMessage());
        }
    }

    /**
     * 判断是否应该显示该网络接口
     */
    private boolean shouldDisplayInterface(NetworkInterface ni) {
        try {
            // 排除回环接口
            if (ni.isLoopback()) {
                return false;
            }

            String displayName = ni.getDisplayName().toLowerCase();

            // 排除不需要的接口类型
            for (String keyword : FILTERED_INTERFACE_KEYWORDS) {
                if (displayName.toLowerCase().contains(keyword.toLowerCase())) {
                    return false;
                }
            }

            // 排除虚拟接口和特定模式的接口
            if (ni.isVirtual() ||
                    displayName.contains("virtual") ||
                    displayName.contains("pseudo") ||
                    displayName.contains("filter") ||
                    displayName.matches(".*[0-9]{4}.*")) { // 排除类似Filter-0000的接口
                return false;
            }

            return true;

        } catch (SocketException e) {
            return false;
        }
    }

    private void refreshNetworkCards() {
        networkCardsPanel.removeAll();

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;

        int row = 0;
        for (NetworkInterface ni : networkInterfaces) {
            gbc.gridx = 0;
            gbc.gridy = row++;
            gbc.gridwidth = 1;

            JPanel card = createNetworkCard(ni);
            networkCardsPanel.add(card, gbc);
        }

        // 添加填充面板，确保布局正确
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        JPanel filler = new JPanel();
        filler.setOpaque(false);
        networkCardsPanel.add(filler, gbc);

        networkCardsPanel.revalidate();
        networkCardsPanel.repaint();
    }

    private void refreshInterfaceComboBox() {
        interfaceComboBox.removeAllItems();
        for (NetworkInterface ni : networkInterfaces) {
            interfaceComboBox.addItem(ni.getDisplayName());
        }
    }

    private JPanel createNetworkCard(NetworkInterface ni) {
        JPanel card = new JPanel(new BorderLayout());
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.GRAY, 1),
                BorderFactory.createEmptyBorder(15, 15, 15, 15) // 增加内边距
        ));
        card.setBackground(Color.WHITE);
        // 移除最大大小限制，让卡片自然扩展
        card.setPreferredSize(new Dimension(800, 180)); // 增加卡片高度

        // 主信息面板 - 使用GridBagLayout避免压缩
        JPanel infoPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(3, 5, 3, 5);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        int gridY = 0;
        try {
            String status = ni.isUp() ? "已启用" : "已禁用";
            String macAddress = getMacAddress(ni);
            String mtu = String.valueOf(ni.getMTU());

            // 接口名称
            gbc.gridx = 0; gbc.gridy = gridY; gbc.weightx = 0;
            infoPanel.add(new JLabel("接口名称:"), gbc);
            gbc.gridx = 1; gbc.weightx = 1.0;
            JLabel nameLabel = new JLabel(ni.getDisplayName());
            nameLabel.setFont(nameLabel.getFont().deriveFont(Font.BOLD));
            infoPanel.add(nameLabel, gbc);

            gridY++;

            // 状态
            gbc.gridx = 0; gbc.gridy = gridY; gbc.weightx = 0;
            infoPanel.add(new JLabel("状态:"), gbc);
            gbc.gridx = 1; gbc.weightx = 1.0;
            infoPanel.add(new JLabel(status), gbc);

            gridY++;

            // MAC地址
            gbc.gridx = 0; gbc.gridy = gridY; gbc.weightx = 0;
            infoPanel.add(new JLabel("MAC地址:"), gbc);
            gbc.gridx = 1; gbc.weightx = 1.0;
            infoPanel.add(new JLabel(macAddress), gbc);

            gridY++;

            // MTU
            gbc.gridx = 0; gbc.gridy = gridY; gbc.weightx = 0;
            infoPanel.add(new JLabel("MTU:"), gbc);
            gbc.gridx = 1; gbc.weightx = 1.0;
            infoPanel.add(new JLabel(mtu), gbc);

            gridY++;

            // 支持多播
            gbc.gridx = 0; gbc.gridy = gridY; gbc.weightx = 0;
            infoPanel.add(new JLabel("支持多播:"), gbc);
            gbc.gridx = 1; gbc.weightx = 1.0;
            infoPanel.add(new JLabel(ni.supportsMulticast() ? "是" : "否"), gbc);

        } catch (SocketException e) {
            gbc.gridx = 0; gbc.gridy = gridY; gbc.gridwidth = 2;
            infoPanel.add(new JLabel("错误: " + e.getMessage()), gbc);
        }

        // 按钮面板
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));

        JButton toggleBtn = new JButton();
        try {
            toggleBtn.setText(ni.isUp() ? "禁用" : "启用");
            toggleBtn.setPreferredSize(new Dimension(80, 30)); // 增大按钮
            toggleBtn.addActionListener(e -> toggleInterface(ni));
        } catch (SocketException e) {
            toggleBtn.setEnabled(false);
            toggleBtn.setText("错误");
        }

        JButton detailsBtn = new JButton("详细信息");
        detailsBtn.setPreferredSize(new Dimension(100, 30)); // 增大按钮
        detailsBtn.addActionListener(e -> showInterfaceDetails(ni));

        buttonPanel.add(toggleBtn);
        buttonPanel.add(detailsBtn);

        card.add(infoPanel, BorderLayout.CENTER);
        card.add(buttonPanel, BorderLayout.SOUTH);

        return card;
    }

    private String getMacAddress(NetworkInterface ni) {
        try {
            byte[] mac = ni.getHardwareAddress();
            if (mac != null) {
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < mac.length; i++) {
                    sb.append(String.format("%02X%s", mac[i], (i < mac.length - 1) ? ":" : ""));
                }
                return sb.toString();
            }
        } catch (SocketException e) {
            // 忽略错误
        }
        return "未知";
    }

    private void showInterfaceDetails(NetworkInterface ni) {
        try {
            StringBuilder details = new StringBuilder();
            details.append("接口详细信息:\n\n");
            details.append("名称: ").append(ni.getDisplayName()).append("\n");
            details.append("状态: ").append(ni.isUp() ? "已启用" : "已禁用").append("\n");
            details.append("MAC地址: ").append(getMacAddress(ni)).append("\n");
            details.append("MTU: ").append(ni.getMTU()).append("\n");
            details.append("回环接口: ").append(ni.isLoopback() ? "是" : "否").append("\n");
            details.append("虚拟接口: ").append(ni.isVirtual() ? "是" : "否").append("\n");
            details.append("支持多播: ").append(ni.supportsMulticast() ? "是" : "否").append("\n");

            // 添加更多详细信息
            details.append("\n附加信息:\n");
            details.append("接口索引: ").append(ni.getIndex()).append("\n");
            try {
                details.append("最大传输单元: ").append(ni.getMTU()).append("\n");
            } catch (SocketException e) {
                details.append("最大传输单元: 未知\n");
            }

            JTextArea textArea = new JTextArea(details.toString());
            textArea.setEditable(false);
            textArea.setFont(new Font("Consolas", Font.PLAIN, 12));
            JScrollPane scrollPane = new JScrollPane(textArea);
            scrollPane.setPreferredSize(new Dimension(500, 400)); // 增大对话框

            JOptionPane.showMessageDialog(this, scrollPane, "网络接口详情 - " + ni.getDisplayName(),
                    JOptionPane.INFORMATION_MESSAGE);

        } catch (SocketException e) {
            appendError("获取接口详情失败: " + e.getMessage());
        }
    }

    private void refreshNetworkInfo() {
        appendInfo("刷新网络接口信息...");
        loadNetworkInterfaces();
        appendSuccess("网络接口信息已刷新");
    }

    private void disableAllInterfaces() {
        int result = JOptionPane.showConfirmDialog(this,
                "确定要禁用所有网络接口吗？这可能会断开网络连接！",
                "确认操作", JOptionPane.YES_NO_OPTION);

        if (result == JOptionPane.YES_OPTION) {
            appendWarning("正在禁用所有网络接口...");
            // 实际应用中这里需要调用系统命令
            // 模拟操作
            new Thread(() -> {
                try {
                    Thread.sleep(2000);
                    appendWarning("所有网络接口已禁用（模拟操作）");
                    // 注意：实际禁用所有网卡会导致程序无法正常使用，这里只是演示
                } catch (InterruptedException e) {
                    appendError("操作被中断: " + e.getMessage());
                }
            }).start();
        }
    }

    private void enableAllInterfaces() {
        appendInfo("正在启用所有网络接口...");
        // 实际应用中这里需要调用系统命令
        // 模拟操作
        new Thread(() -> {
            try {
                Thread.sleep(2000);
                appendSuccess("所有网络接口已启用（模拟操作）");
            } catch (InterruptedException e) {
                appendError("操作被中断: " + e.getMessage());
            }
        }).start();
    }


    private void addSampleTemplates() {
        templateTableModel.addRow(new Object[]{"办公网络", "以太网", "192.168.1.100", "255.255.255.0", "192.168.1.1", "应用|删除"});
        templateTableModel.addRow(new Object[]{"测试环境", "WLAN", "10.0.0.50", "255.255.255.0", "10.0.0.1", "应用|删除"});
        templateTableModel.addRow(new Object[]{"开发网络", "以太网2", "172.16.1.100", "255.255.0.0", "172.16.1.1", "应用|删除"});
    }

    // 自定义按钮渲染器
    private class ButtonRenderer extends JPanel implements javax.swing.table.TableCellRenderer {
        private JButton applyBtn;
        private JButton deleteBtn;

        public ButtonRenderer() {
            setLayout(new FlowLayout(FlowLayout.CENTER, 5, 0));
            applyBtn = new JButton("应用");
            deleteBtn = new JButton("删除");

            applyBtn.setPreferredSize(new Dimension(80, 30)); // 增大按钮
            deleteBtn.setPreferredSize(new Dimension(80, 30)); // 增大按钮

            add(applyBtn);
            add(deleteBtn);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus, int row, int column) {
            return this;
        }
    }

    // 自定义按钮编辑器
    private class ButtonEditor extends DefaultCellEditor {
        private JPanel panel;
        private JButton applyBtn;
        private JButton deleteBtn;
        private int currentRow;

        public ButtonEditor(JCheckBox checkBox) {
            super(checkBox);
            panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 0));

            applyBtn = new JButton("应用");
            deleteBtn = new JButton("删除");

            applyBtn.setPreferredSize(new Dimension(80, 30)); // 增大按钮
            deleteBtn.setPreferredSize(new Dimension(80, 30)); // 增大按钮

            applyBtn.addActionListener(e -> {
                fireEditingStopped();
                applyTemplateFromTable(currentRow);
            });

            deleteBtn.addActionListener(e -> {
                fireEditingStopped();
                deleteTemplate(currentRow);
            });

            panel.add(applyBtn);
            panel.add(deleteBtn);
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value,
                                                     boolean isSelected, int row, int column) {
            currentRow = row;
            return panel;
        }

        @Override
        public Object getCellEditorValue() {
            return "应用|删除";
        }
    }

    // 修改保存模板方法
    private void saveTemplate(ActionEvent e) {
        String templateName = templateNameField.getText().trim();
        String selectedInterface = (String) interfaceComboBox.getSelectedItem();
        String ipAddress = ipAddressField.getText().trim();

        if (templateName.isEmpty() || selectedInterface == null || ipAddress.isEmpty()) {
            appendError("请填写完整的模板信息");
            return;
        }

        // 检查模板名称是否已存在
        if (isTemplateNameExists(templateName)) {
            appendError("模板名称已存在，请使用其他名称");
            return;
        }

        try {
            // 创建网络模板对象
            NetworkTemplate template = new NetworkTemplate(
                    templateName,
                    selectedInterface,
                    ipAddress,
                    subnetField.getText().trim(),
                    gatewayField.getText().trim(),
                    dns1Field.getText().trim(),
                    dns2Field.getText().trim()
            );

            // 保存到数据库
            dbManager.addNetworkTemplate(template);

            // 重新加载模板列表
            loadNetworkTemplates();

            appendSuccess("已保存IP模板: " + templateName);
            clearTemplateForm();

        } catch (Exception ex) {
            appendError("保存模板失败: " + ex.getMessage());
        }
    }
    // 在 NetworkManagementPanel.java 中修改以下方法

    private void toggleInterface(NetworkInterface ni) {
        try {
            String interfaceName = ni.getDisplayName();
            boolean currentStatus = ni.isUp();

            if (currentStatus) {
                appendWarning("正在禁用网卡: " + interfaceName);
                boolean success = NetworkConfigurator.disableInterface(interfaceName);
                if (success) {
                    appendSuccess("已禁用网卡: " + interfaceName);
                } else {
                    appendError("禁用网卡失败: " + interfaceName);
                }
            } else {
                appendInfo("正在启用网卡: " + interfaceName);
                boolean success = NetworkConfigurator.enableInterface(interfaceName);
                if (success) {
                    appendSuccess("已启用网卡: " + interfaceName);
                } else {
                    appendError("启用网卡失败: " + interfaceName);
                }
            }

            // 刷新网络信息
            Thread.sleep(1000);
            refreshNetworkInfo();

        } catch (Exception e) {
            appendError("操作网卡失败: " + e.getMessage());
        }
    }

    private void applyTemplate(ActionEvent e) {
        String templateName = templateNameField.getText().trim();
        if (templateName.isEmpty()) {
            appendError("请先选择或创建模板");
            return;
        }

        // 从数据库获取模板详情
        NetworkTemplate template = dbManager.getNetworkTemplateByName(templateName);
        if (template == null) {
            appendError("找不到模板: " + templateName);
            return;
        }

        // 验证网络配置
        if (!NetworkConfigurator.verifyNetworkConfiguration(template)) {
            appendError("网络配置参数无效，请检查IP地址、子网掩码等格式");
            return;
        }

        appendInfo("正在应用IP模板: " + templateName);

        // 实际网络配置
        new Thread(() -> {
            try {
                boolean success = NetworkConfigurator.configureNetworkInterface(template);

                if (success) {
                    appendSuccess("IP模板应用完成: " + templateName);

                    // 验证配置是否生效
                    Thread.sleep(2000);
                    String status = NetworkConfigurator.getInterfaceStatus(template.getInterfaceName());
                    appendInfo("接口状态: " + status);

                } else {
                    appendError("应用IP模板失败: " + templateName);
                }

                // 记录应用日志
                logTemplateApplication(template, success);

            } catch (Exception ex) {
                appendError("应用模板时发生错误: " + ex.getMessage());
            }
        }).start();
    }

    private void applyTemplateFromTable(int row) {
        if (row < 0 || row >= networkTemplates.size()) {
            return;
        }

        NetworkTemplate template = networkTemplates.get(row);
        String templateName = template.getName();

        appendInfo("正在应用模板 '" + templateName + "' 到接口 '" + template.getInterfaceName() + "'");

        // 填充表单
        templateNameField.setText(templateName);
        interfaceComboBox.setSelectedItem(template.getInterfaceName());
        ipAddressField.setText(template.getIpAddress());
        subnetField.setText(template.getSubnetMask());
        gatewayField.setText(template.getGateway());
        dns1Field.setText(template.getDns1());
        dns2Field.setText(template.getDns2());

        // 实际应用配置
        new Thread(() -> {
            try {
                // 验证配置
                if (!NetworkConfigurator.verifyNetworkConfiguration(template)) {
                    appendError("网络配置参数无效");
                    return;
                }

                boolean success = NetworkConfigurator.configureNetworkInterface(template);

                if (success) {
                    appendSuccess("模板 '" + templateName + "' 应用完成");

                    // 验证状态
                    Thread.sleep(2000);
                    String status = NetworkConfigurator.getInterfaceStatus(template.getInterfaceName());
                    appendInfo("接口 '" + template.getInterfaceName() + "' 状态: " + status);

                } else {
                    appendError("应用模板 '" + templateName + "' 失败");
                }

                // 记录日志
                logTemplateApplication(template, success);

            } catch (Exception e) {
                appendError("应用模板被中断: " + e.getMessage());
            }
        }).start();
    }

    // 修改日志记录方法
    private void logTemplateApplication(NetworkTemplate template, boolean success) {
        String status = success ? "成功" : "失败";
        String logMessage = String.format(
                "%s应用网络模板: %s [接口: %s, IP: %s, 子网: %s, 网关: %s]",
                status, template.getName(), template.getInterfaceName(),
                template.getIpAddress(), template.getSubnetMask(), template.getGateway()
        );

        if (success) {
            appendSuccess(logMessage);
        } else {
            appendError(logMessage);
        }
    }

    // 添加重置为DHCP的功能
    private void addResetToDhcpButton() {
        // 在工具栏添加重置按钮
        JButton resetDhcpBtn = new JButton("重置为DHCP");
        resetDhcpBtn.addActionListener(e -> resetToDhcp());

        // 在createToolBar方法中添加这个按钮
        // toolbar.add(resetDhcpBtn);
    }

    private void resetToDhcp() {
        String selectedInterface = (String) interfaceComboBox.getSelectedItem();
        if (selectedInterface == null || selectedInterface.isEmpty()) {
            appendError("请先选择要重置的网络接口");
            return;
        }

        int result = JOptionPane.showConfirmDialog(this,
                "确定要将接口 '" + selectedInterface + "' 重置为DHCP吗？",
                "确认重置", JOptionPane.YES_NO_OPTION);

        if (result == JOptionPane.YES_OPTION) {
            appendInfo("正在重置接口 '" + selectedInterface + "' 为DHCP...");

            new Thread(() -> {
                try {
                    boolean success = NetworkConfigurator.resetToDhcp(selectedInterface);

                    if (success) {
                        appendSuccess("接口 '" + selectedInterface + "' 已重置为DHCP");

                        // 刷新状态
                        Thread.sleep(2000);
                        refreshNetworkInfo();

                    } else {
                        appendError("重置接口为DHCP失败");
                    }

                } catch (Exception e) {
                    appendError("重置操作失败: " + e.getMessage());
                }
            }).start();
        }
    }

    // 新增：从数据库加载模板
    private void loadNetworkTemplates() {
        try {
            networkTemplates = dbManager.getAllNetworkTemplates();
            refreshTemplateTable();
            refreshInterfaceComboBox();
        } catch (Exception e) {
            appendError("加载网络模板失败: " + e.getMessage());
            networkTemplates = new ArrayList<>();
        }
    }

    // 修改：刷新模板表格
    private void refreshTemplateTable() {
        templateTableModel.setRowCount(0); // 清空表格

        for (NetworkTemplate template : networkTemplates) {
            templateTableModel.addRow(new Object[]{
                    template.getName(),
                    template.getInterfaceName(),
                    template.getIpAddress(),
                    template.getSubnetMask(),
                    template.getGateway(),
                    "应用|删除"
            });
        }
    }


    // 修改：删除模板
    private void deleteTemplate(int row) {
        if (row < 0 || row >= networkTemplates.size()) {
            return;
        }

        NetworkTemplate template = networkTemplates.get(row);
        String templateName = template.getName();

        int result = JOptionPane.showConfirmDialog(this,
                "确定要删除模板 '" + templateName + "' 吗？",
                "确认删除", JOptionPane.YES_NO_OPTION);

        if (result == JOptionPane.YES_OPTION) {
            try {
                dbManager.deleteNetworkTemplate(template.getId());
                loadNetworkTemplates(); // 重新加载模板列表
                appendSuccess("已删除模板: " + templateName);
            } catch (Exception e) {
                appendError("删除模板失败: " + e.getMessage());
            }
        }
    }

    // 新增：检查模板名称是否存在
    private boolean isTemplateNameExists(String name) {
        for (NetworkTemplate template : networkTemplates) {
            if (template.getName().equals(name)) {
                return true;
            }
        }
        return false;
    }

    // 新增：记录模板应用日志
    private void logTemplateApplication(NetworkTemplate template) {
        String logMessage = String.format(
                "应用网络模板: %s [接口: %s, IP: %s, 子网: %s, 网关: %s]",
                template.getName(),
                template.getInterfaceName(),
                template.getIpAddress(),
                template.getSubnetMask(),
                template.getGateway()
        );
        appendInfo(logMessage);
    }

    // 修改 clearTemplateForm 方法，保留接口选择
    private void clearTemplateForm() {
        templateNameField.setText("");
        ipAddressField.setText("");
        subnetField.setText("");
        gatewayField.setText("");
        dns1Field.setText("");
        dns2Field.setText("");
        // 不清除接口选择，因为用户可能想在同一接口上创建多个模板
    }
}
