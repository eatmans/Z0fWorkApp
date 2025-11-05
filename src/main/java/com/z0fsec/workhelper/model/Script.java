package com.z0fsec.workhelper.model;

import java.sql.Timestamp;

public class Script {
    private int id;
    private String name;
    private String type;
    private String path;
    private String description;
    private String category;
    private String interpreter;
    private String parameters;
    private String status;
    private String content;
    private Timestamp createdTime;
    private Timestamp updatedTime;
    private Timestamp lastRunTime;

    public Script() {}

    public Script(String name, String type, String path, String description, 
                  String category, String interpreter, String parameters) {
        this.name = name;
        this.type = type;
        this.path = path;
        this.description = description;
        this.category = category;
        this.interpreter = interpreter;
        this.parameters = parameters;
        this.status = "就绪";
    }

    // Getter and Setter methods
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getPath() { return path; }
    public void setPath(String path) { this.path = path; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getInterpreter() { return interpreter; }
    public void setInterpreter(String interpreter) { this.interpreter = interpreter; }

    public String getParameters() { return parameters; }
    public void setParameters(String parameters) { this.parameters = parameters; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public Timestamp getCreatedTime() { return createdTime; }
    public void setCreatedTime(Timestamp createdTime) { this.createdTime = createdTime; }

    public Timestamp getUpdatedTime() { return updatedTime; }
    public void setUpdatedTime(Timestamp updatedTime) { this.updatedTime = updatedTime; }

    public Timestamp getLastRunTime() { return lastRunTime; }
    public void setLastRunTime(Timestamp lastRunTime) { this.lastRunTime = lastRunTime; }
}