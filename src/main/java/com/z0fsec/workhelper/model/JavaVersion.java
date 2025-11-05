package com.z0fsec.workhelper.model;

// 内部类
    public class JavaVersion {
        public String name;
        public String path;
        public String version;
        public boolean isDefault;

        public JavaVersion(String name, String path, String version, boolean isDefault) {
            this.name = name;
            this.path = path;
            this.version = version;
            this.isDefault = isDefault;
        }
    }
