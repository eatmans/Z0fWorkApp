package com.z0fsec.workhelper.model;

import java.sql.Timestamp;

public class Tool {
    private int id;
    private String name;
    private String type;
    private String path;
    private String version;
    private String status;
    private String description;
    private String category;
    private long lastUsed;
    private Timestamp createdTime;
    private Timestamp updatedTime;

    // 构造函数
    public Tool() {}

    public Tool(String name, String type, String path, String version, 
                String status, String description, String category) {
        this.name = name;
        this.type = type;
        this.path = path;
        this.version = version;
        this.status = status;
        this.description = description;
        this.category = category;
        this.lastUsed = 0;
    }

    // Getter 和 Setter 方法
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getPath() { return path; }
    public void setPath(String path) { this.path = path; }

    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public long getLastUsed() { return lastUsed; }
    public void setLastUsed(long lastUsed) { this.lastUsed = lastUsed; }

    public Timestamp getCreatedTime() { return createdTime; }
    public void setCreatedTime(Timestamp createdTime) { this.createdTime = createdTime; }

    public Timestamp getUpdatedTime() { return updatedTime; }
    public void setUpdatedTime(Timestamp updatedTime) { this.updatedTime = updatedTime; }

    @Override
    public String toString() {
        return "Tool{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", type='" + type + '\'' +
                ", status='" + status + '\'' +
                '}';
    }
}