package com.z0fsec.workhelper.model;

public class PythonVersion {
    public String name;
    public String path;
    public String version;
    public boolean isDefault;

    public PythonVersion(String name, String path, String version, boolean isDefault) {
        this.name = name;
        this.path = path;
        this.version = version;
        this.isDefault = isDefault;
    }
}
