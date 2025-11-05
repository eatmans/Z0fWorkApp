// NetworkTemplate.java
package com.z0fsec.workhelper.model;

import java.sql.Timestamp;

public class NetworkTemplate {
    private Integer id;
    private String name;
    private String interfaceName;
    private String ipAddress;
    private String subnetMask;
    private String gateway;
    private String dns1;
    private String dns2;
    private String description;
    private Timestamp createdTime;
    private Timestamp updatedTime;

    // 构造函数
    public NetworkTemplate() {}

    public NetworkTemplate(String name, String interfaceName, String ipAddress, 
                          String subnetMask, String gateway, String dns1, String dns2) {
        this.name = name;
        this.interfaceName = interfaceName;
        this.ipAddress = ipAddress;
        this.subnetMask = subnetMask;
        this.gateway = gateway;
        this.dns1 = dns1;
        this.dns2 = dns2;
    }

    // Getter 和 Setter 方法
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getInterfaceName() { return interfaceName; }
    public void setInterfaceName(String interfaceName) { this.interfaceName = interfaceName; }

    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }

    public String getSubnetMask() { return subnetMask; }
    public void setSubnetMask(String subnetMask) { this.subnetMask = subnetMask; }

    public String getGateway() { return gateway; }
    public void setGateway(String gateway) { this.gateway = gateway; }

    public String getDns1() { return dns1; }
    public void setDns1(String dns1) { this.dns1 = dns1; }

    public String getDns2() { return dns2; }
    public void setDns2(String dns2) { this.dns2 = dns2; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Timestamp getCreatedTime() { return createdTime; }
    public void setCreatedTime(Timestamp createdTime) { this.createdTime = createdTime; }

    public Timestamp getUpdatedTime() { return updatedTime; }
    public void setUpdatedTime(Timestamp updatedTime) { this.updatedTime = updatedTime; }

    @Override
    public String toString() {
        return name;
    }
}