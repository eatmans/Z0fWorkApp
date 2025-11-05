// NetworkConfigurator.java
package com.z0fsec.workhelper.util;

import com.z0fsec.workhelper.model.NetworkTemplate;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class NetworkConfigurator {
    
    private static final boolean IS_WINDOWS = System.getProperty("os.name").toLowerCase().contains("windows");
    
    /**
     * 配置网络接口
     */
    public static boolean configureNetworkInterface(NetworkTemplate template) {
        try {
            if (IS_WINDOWS) {
                return configureWindowsNetwork(template);
            } else {
                return configureLinuxNetwork(template);
            }
        } catch (Exception e) {
            System.err.println("配置网络接口失败: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Windows 系统网络配置
     */
    private static boolean configureWindowsNetwork(NetworkTemplate template) throws Exception {
        String interfaceName = template.getInterfaceName();
        List<String> commands = new ArrayList<>();
        
        // 设置IP地址、子网掩码和网关
        if (template.getGateway() != null && !template.getGateway().isEmpty()) {
            commands.add(String.format("netsh interface ip set address \"%s\" static %s %s %s",
                    interfaceName, template.getIpAddress(), template.getSubnetMask(), template.getGateway()));
        } else {
            commands.add(String.format("netsh interface ip set address \"%s\" static %s %s",
                    interfaceName, template.getIpAddress(), template.getSubnetMask()));
        }
        
        // 设置主DNS
        if (template.getDns1() != null && !template.getDns1().isEmpty()) {
            commands.add(String.format("netsh interface ip set dns \"%s\" static %s primary",
                    interfaceName, template.getDns1()));
        }
        
        // 设置备用DNS
        if (template.getDns2() != null && !template.getDns2().isEmpty()) {
            commands.add(String.format("netsh interface ip add dns \"%s\" %s index=2",
                    interfaceName, template.getDns2()));
        }
        
        return executeCommandsWithAdmin(commands);
    }
    
    /**
     * Linux 系统网络配置 (Ubuntu/CentOS)
     */
    private static boolean configureLinuxNetwork(NetworkTemplate template) throws Exception {
        String interfaceName = getLinuxInterfaceName(template.getInterfaceName());
        List<String> commands = new ArrayList<>();
        
        // 临时配置（立即生效）
        // 设置IP地址和子网掩码
        String ipWithMask = template.getIpAddress() + "/" + convertSubnetToCidr(template.getSubnetMask());
        commands.add(String.format("ip addr add %s dev %s", ipWithMask, interfaceName));
        
        // 设置网关
        if (template.getGateway() != null && !template.getGateway().isEmpty()) {
            commands.add(String.format("ip route add default via %s dev %s", template.getGateway(), interfaceName));
        }
        
        // 配置DNS（需要修改resolv.conf）
        if (template.getDns1() != null && !template.getDns1().isEmpty()) {
            commands.add("echo 'nameserver " + template.getDns1() + "' > /etc/resolv.conf");
            if (template.getDns2() != null && !template.getDns2().isEmpty()) {
                commands.add("echo 'nameserver " + template.getDns2() + "' >> /etc/resolv.conf");
            }
        }
        
        // 启用接口
        commands.add(String.format("ip link set %s up", interfaceName));
        
        return executeCommandsWithSudo(commands);
    }
    
    /**
     * 重置网络接口为DHCP
     */
    public static boolean resetToDhcp(String interfaceName) {
        try {
            if (IS_WINDOWS) {
                return resetWindowsToDhcp(interfaceName);
            } else {
                return resetLinuxToDhcp(interfaceName);
            }
        } catch (Exception e) {
            System.err.println("重置网络接口失败: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Windows DHCP重置
     */
    private static boolean resetWindowsToDhcp(String interfaceName) throws Exception {
        List<String> commands = new ArrayList<>();
        commands.add(String.format("netsh interface ip set address \"%s\" dhcp", interfaceName));
        commands.add(String.format("netsh interface ip set dns \"%s\" dhcp", interfaceName));
        
        return executeCommandsWithAdmin(commands);
    }
    
    /**
     * Linux DHCP重置
     */
    private static boolean resetLinuxToDhcp(String interfaceName) throws Exception {
        String linuxInterface = getLinuxInterfaceName(interfaceName);
        List<String> commands = new ArrayList<>();
        
        // 使用dhclient获取DHCP
        commands.add(String.format("dhclient -r %s", linuxInterface)); // 释放
        commands.add(String.format("dhclient %s", linuxInterface));    // 重新获取
        
        return executeCommandsWithSudo(commands);
    }
    
    /**
     * 启用网络接口
     */
    public static boolean enableInterface(String interfaceName) {
        try {
            if (IS_WINDOWS) {
                return executeCommandWithAdmin(String.format("netsh interface set interface \"%s\" enable", interfaceName));
            } else {
                String linuxInterface = getLinuxInterfaceName(interfaceName);
                return executeCommandWithSudo(String.format("ip link set %s up", linuxInterface));
            }
        } catch (Exception e) {
            System.err.println("启用网络接口失败: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * 禁用网络接口
     */
    public static boolean disableInterface(String interfaceName) {
        try {
            if (IS_WINDOWS) {
                return executeCommandWithAdmin(String.format("netsh interface set interface \"%s\" disable", interfaceName));
            } else {
                String linuxInterface = getLinuxInterfaceName(interfaceName);
                return executeCommandWithSudo(String.format("ip link set %s down", linuxInterface));
            }
        } catch (Exception e) {
            System.err.println("禁用网络接口失败: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * 获取网络接口状态
     */
    public static String getInterfaceStatus(String interfaceName) {
        try {
            if (IS_WINDOWS) {
                Process process = Runtime.getRuntime().exec(
                    String.format("netsh interface show interface \"%s\"", interfaceName));
                
                BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.contains("状态")) {
                        return line.contains("已连接") ? "UP" : "DOWN";
                    }
                }
            } else {
                String linuxInterface = getLinuxInterfaceName(interfaceName);
                Process process = Runtime.getRuntime().exec(
                    String.format("cat /sys/class/net/%s/operstate", linuxInterface));
                
                BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                String status = reader.readLine();
                return "up".equals(status) ? "UP" : "DOWN";
            }
        } catch (Exception e) {
            System.err.println("获取接口状态失败: " + e.getMessage());
        }
        return "UNKNOWN";
    }
    
    /**
     * 验证IP地址配置
     */
    public static boolean verifyNetworkConfiguration(NetworkTemplate template) {
        try {
            // 简单的IP地址格式验证
            if (!isValidIP(template.getIpAddress())) {
                throw new IllegalArgumentException("无效的IP地址: " + template.getIpAddress());
            }
            
            if (!isValidSubnet(template.getSubnetMask())) {
                throw new IllegalArgumentException("无效的子网掩码: " + template.getSubnetMask());
            }
            
            if (template.getGateway() != null && !template.getGateway().isEmpty() && 
                !isValidIP(template.getGateway())) {
                throw new IllegalArgumentException("无效的网关地址: " + template.getGateway());
            }
            
            if (template.getDns1() != null && !template.getDns1().isEmpty() && 
                !isValidIP(template.getDns1())) {
                throw new IllegalArgumentException("无效的DNS地址: " + template.getDns1());
            }
            
            if (template.getDns2() != null && !template.getDns2().isEmpty() && 
                !isValidIP(template.getDns2())) {
                throw new IllegalArgumentException("无效的DNS地址: " + template.getDns2());
            }
            
            return true;
            
        } catch (Exception e) {
            System.err.println("网络配置验证失败: " + e.getMessage());
            return false;
        }
    }
    
    // ========== 辅助方法 ==========
    
    private static boolean executeCommandsWithAdmin(List<String> commands) throws Exception {
        for (String command : commands) {
            if (!executeCommandWithAdmin(command)) {
                return false;
            }
            // 命令间短暂延迟
            Thread.sleep(500);
        }
        return true;
    }
    
    private static boolean executeCommandWithAdmin(String command) throws Exception {
        if (IS_WINDOWS) {
            // Windows下以管理员权限运行
            String[] cmd = {"cmd.exe", "/c", command};
            Process process = Runtime.getRuntime().exec(cmd);
            return process.waitFor(10, TimeUnit.SECONDS) && process.exitValue() == 0;
        } else {
            return executeCommandWithSudo(command);
        }
    }
    
    private static boolean executeCommandsWithSudo(List<String> commands) throws Exception {
        for (String command : commands) {
            if (!executeCommandWithSudo(command)) {
                return false;
            }
        }
        return true;
    }
    
    private static boolean executeCommandWithSudo(String command) throws Exception {
        String[] cmd = {"sudo", "sh", "-c", command};
        Process process = Runtime.getRuntime().exec(cmd);
        return process.waitFor(10, TimeUnit.SECONDS) && process.exitValue() == 0;
    }
    
    private static String getLinuxInterfaceName(String displayName) {
        // 将Windows显示名称转换为Linux接口名称
        // 例如: "以太网" -> "eth0", "WLAN" -> "wlan0"
        if (displayName.toLowerCase().contains("wireless") || 
            displayName.toLowerCase().contains("wlan") ||
            displayName.toLowerCase().contains("wi-fi")) {
            return "wlan0";
        } else {
            return "eth0";
        }
    }
    
    private static String convertSubnetToCidr(String subnetMask) {
        // 将点分十进制子网掩码转换为CIDR表示法
        String[] parts = subnetMask.split("\\.");
        int cidr = 0;
        for (String part : parts) {
            int value = Integer.parseInt(part);
            cidr += Integer.bitCount(value);
        }
        return String.valueOf(cidr);
    }
    
    private static boolean isValidIP(String ip) {
        if (ip == null || ip.isEmpty()) return false;
        String pattern = "^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$";
        return ip.matches(pattern);
    }
    
    private static boolean isValidSubnet(String subnet) {
        if (subnet == null || subnet.isEmpty()) return false;
        
        // 常见的有效子网掩码
        String[] validSubnets = {
            "255.255.255.255", "255.255.255.254", "255.255.255.252", "255.255.255.248",
            "255.255.255.240", "255.255.255.224", "255.255.255.192", "255.255.255.128",
            "255.255.255.0", "255.255.254.0", "255.255.252.0", "255.255.248.0",
            "255.255.240.0", "255.255.224.0", "255.255.192.0", "255.255.128.0",
            "255.255.0.0", "255.254.0.0", "255.252.0.0", "255.248.0.0",
            "255.240.0.0", "255.224.0.0", "255.192.0.0", "255.128.0.0",
            "255.0.0.0", "254.0.0.0", "252.0.0.0", "248.0.0.0",
            "240.0.0.0", "224.0.0.0", "192.0.0.0", "128.0.0.0", "0.0.0.0"
        };
        
        for (String valid : validSubnets) {
            if (valid.equals(subnet)) {
                return true;
            }
        }
        return false;
    }
}