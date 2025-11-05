package com.z0fsec.workhelper.db;

import com.z0fsec.workhelper.model.Script;
import com.z0fsec.workhelper.model.Tool;
import com.z0fsec.workhelper.model.NetworkTemplate;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DatabaseManager {
    private static final String DB_URL = "jdbc:sqlite:workhelper.db";
    private static DatabaseManager instance;

    static {
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("SQLite JDBC driver not found", e);
        }
    }

    private DatabaseManager() {
        initializeDatabase();
    }

    public static DatabaseManager getInstance() {
        if (instance == null) {
            instance = new DatabaseManager();
        }
        return instance;
    }

    private void initializeToolsTable() {
        String createToolsTable = """
            CREATE TABLE IF NOT EXISTS tools (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT NOT NULL,
                type TEXT NOT NULL,
                path TEXT NOT NULL,
                version TEXT DEFAULT '1.0',
                status TEXT DEFAULT '就绪',
                description TEXT,
                category TEXT DEFAULT '其他',
                last_used INTEGER DEFAULT 0,
                created_time DATETIME DEFAULT CURRENT_TIMESTAMP,
                updated_time DATETIME DEFAULT CURRENT_TIMESTAMP
            )
            """;

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(createToolsTable);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to initialize database", e);
        }
    }

    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL);
    }

    // 工具相关的数据库操作
    public List<Tool> getAllTools() {
        List<Tool> tools = new ArrayList<>();
        String sql = "SELECT * FROM tools ORDER BY created_time DESC";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                Tool tool = new Tool();
                tool.setId(rs.getInt("id"));
                tool.setName(rs.getString("name"));
                tool.setType(rs.getString("type"));
                tool.setPath(rs.getString("path"));
                tool.setVersion(rs.getString("version"));
                tool.setStatus(rs.getString("status"));
                tool.setDescription(rs.getString("description"));
                tool.setCategory(rs.getString("category"));
                tool.setLastUsed(rs.getLong("last_used"));
                tool.setCreatedTime(rs.getTimestamp("created_time"));
                tool.setUpdatedTime(rs.getTimestamp("updated_time"));
                tools.add(tool);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to get tools", e);
        }
        return tools;
    }

    public void addTool(Tool tool) {
        String sql = """
            INSERT INTO tools (name, type, path, version, status, description, category, last_used)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            """;

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, tool.getName());
            pstmt.setString(2, tool.getType());
            pstmt.setString(3, tool.getPath());
            pstmt.setString(4, tool.getVersion());
            pstmt.setString(5, tool.getStatus());
            pstmt.setString(6, tool.getDescription());
            pstmt.setString(7, tool.getCategory());
            pstmt.setLong(8, tool.getLastUsed());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to add tool", e);
        }
    }

    public void updateTool(Tool tool) {
        String sql = """
            UPDATE tools 
            SET name = ?, type = ?, path = ?, version = ?, status = ?, 
                description = ?, category = ?, last_used = ?, updated_time = CURRENT_TIMESTAMP
            WHERE id = ?
            """;

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, tool.getName());
            pstmt.setString(2, tool.getType());
            pstmt.setString(3, tool.getPath());
            pstmt.setString(4, tool.getVersion());
            pstmt.setString(5, tool.getStatus());
            pstmt.setString(6, tool.getDescription());
            pstmt.setString(7, tool.getCategory());
            pstmt.setLong(8, tool.getLastUsed());
            pstmt.setInt(9, tool.getId());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update tool", e);
        }
    }

    public void deleteTool(int id) {
        String sql = "DELETE FROM tools WHERE id = ?";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, id);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete tool", e);
        }
    }

    public void updateToolStatus(int id, String status) {
        String sql = "UPDATE tools SET status = ?, updated_time = CURRENT_TIMESTAMP WHERE id = ?";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, status);
            pstmt.setInt(2, id);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update tool status", e);
        }
    }

    public void updateLastUsed(int id) {
        String sql = "UPDATE tools SET last_used = ?, updated_time = CURRENT_TIMESTAMP WHERE id = ?";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, System.currentTimeMillis());
            pstmt.setInt(2, id);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update last used time", e);
        }
    }

    // 搜索工具
    public List<Tool> searchTools(String keyword, String type, String status) {
        List<Tool> tools = new ArrayList<>();
        StringBuilder sql = new StringBuilder("SELECT * FROM tools WHERE 1=1");
        List<Object> params = new ArrayList<>();

        if (keyword != null && !keyword.trim().isEmpty()) {
            sql.append(" AND (name LIKE ? OR description LIKE ? OR category LIKE ?)");
            String likeKeyword = "%" + keyword + "%";
            params.add(likeKeyword);
            params.add(likeKeyword);
            params.add(likeKeyword);
        }

        if (type != null && !"全部类型".equals(type)) {
            sql.append(" AND type = ?");
            params.add(type);
        }

        if (status != null && !"全部状态".equals(status)) {
            sql.append(" AND status = ?");
            params.add(status);
        }

        sql.append(" ORDER BY created_time DESC");

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql.toString())) {

            for (int i = 0; i < params.size(); i++) {
                pstmt.setObject(i + 1, params.get(i));
            }

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Tool tool = new Tool();
                    tool.setId(rs.getInt("id"));
                    tool.setName(rs.getString("name"));
                    tool.setType(rs.getString("type"));
                    tool.setPath(rs.getString("path"));
                    tool.setVersion(rs.getString("version"));
                    tool.setStatus(rs.getString("status"));
                    tool.setDescription(rs.getString("description"));
                    tool.setCategory(rs.getString("category"));
                    tool.setLastUsed(rs.getLong("last_used"));
                    tool.setCreatedTime(rs.getTimestamp("created_time"));
                    tool.setUpdatedTime(rs.getTimestamp("updated_time"));
                    tools.add(tool);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to search tools", e);
        }
        return tools;
    }

    // 脚本表创建
    private void initializeScriptsTable() {
        String createScriptsTable = """
        CREATE TABLE IF NOT EXISTS scripts (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            name TEXT NOT NULL,
            type TEXT NOT NULL,
            path TEXT NOT NULL,
            description TEXT,
            category TEXT DEFAULT '其他',
            interpreter TEXT,
            parameters TEXT,
            status TEXT DEFAULT '就绪',
            content TEXT,
            created_time DATETIME DEFAULT CURRENT_TIMESTAMP,
            updated_time DATETIME DEFAULT CURRENT_TIMESTAMP,
            last_run_time DATETIME
        )
        """;

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(createScriptsTable);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to initialize scripts table", e);
        }
    }

    // 环境配置表
    private void initializeSettingsTable() {
        String createSettingsTable = """
        CREATE TABLE IF NOT EXISTS settings (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            key TEXT UNIQUE NOT NULL,
            value TEXT,
            description TEXT,
            updated_time DATETIME DEFAULT CURRENT_TIMESTAMP
        )
        """;

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(createSettingsTable);

            // 插入默认设置
            initializeDefaultSettings();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to initialize settings table", e);
        }
    }

    private void initializeDefaultSettings() {
        String[] defaultSettings = {
                "INSERT OR IGNORE INTO settings (key, value, description) VALUES ('database_path', 'workhelper.db', '数据库文件路径')",
                "INSERT OR IGNORE INTO settings (key, value, description) VALUES ('default_java_path', 'java', '默认Java路径')",
                "INSERT OR IGNORE INTO settings (key, value, description) VALUES ('default_python_path', 'python', '默认Python路径')",
                "INSERT OR IGNORE INTO settings (key, value, description) VALUES ('auto_save', 'true', '自动保存')"
        };

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            for (String sql : defaultSettings) {
                stmt.execute(sql);
            }
        } catch (SQLException e) {
            // 忽略重复插入的错误
        }
    }

    // 在 initializeDatabase() 方法中调用
    private void initializeDatabase() {
        initializeToolsTable();
        initializeScriptsTable();  // 新增
        initializeSettingsTable(); // 新增
        initializeNetworkTemplatesTable(); // 新增网络模板表
    }

    // 脚本相关的数据库操作
    public List<Script> getAllScripts() {
        List<Script> scripts = new ArrayList<>();
        String sql = "SELECT * FROM scripts ORDER BY created_time DESC";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                scripts.add(extractScriptFromResultSet(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to get scripts", e);
        }
        return scripts;
    }

    public void addScript(Script script) {
        String sql = """
        INSERT INTO scripts (name, type, path, description, category, interpreter, parameters, content)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?)
        """;

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            setScriptParameters(pstmt, script);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to add script", e);
        }
    }

    public void updateScript(Script script) {
        String sql = """
        UPDATE scripts 
        SET name = ?, type = ?, path = ?, description = ?, category = ?, 
            interpreter = ?, parameters = ?, content = ?, updated_time = CURRENT_TIMESTAMP
        WHERE id = ?
        """;

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            setScriptParameters(pstmt, script);
            pstmt.setInt(9, script.getId());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update script", e);
        }
    }

    public void deleteScript(int id) {
        String sql = "DELETE FROM scripts WHERE id = ?";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, id);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete script", e);
        }
    }

    public void updateScriptStatus(int id, String status) {
        String sql = "UPDATE scripts SET status = ?, updated_time = CURRENT_TIMESTAMP WHERE id = ?";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, status);
            pstmt.setInt(2, id);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update script status", e);
        }
    }

    public void updateLastRunTime(int id) {
        String sql = "UPDATE scripts SET last_run_time = CURRENT_TIMESTAMP WHERE id = ?";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, id);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update last run time", e);
        }
    }

    // 设置相关的数据库操作
    public String getSetting(String key) {
        String sql = "SELECT value FROM settings WHERE key = ?";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, key);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("value");
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to get setting: " + key, e);
        }
        return null;
    }

    public void setSetting(String key, String value) {
        String sql = "INSERT OR REPLACE INTO settings (key, value, updated_time) VALUES (?, ?, CURRENT_TIMESTAMP)";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, key);
            pstmt.setString(2, value);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to set setting: " + key, e);
        }
    }

    // 辅助方法
    private Script extractScriptFromResultSet(ResultSet rs) throws SQLException {
        Script script = new Script();
        script.setId(rs.getInt("id"));
        script.setName(rs.getString("name"));
        script.setType(rs.getString("type"));
        script.setPath(rs.getString("path"));
        script.setDescription(rs.getString("description"));
        script.setCategory(rs.getString("category"));
        script.setInterpreter(rs.getString("interpreter"));
        script.setParameters(rs.getString("parameters"));
        script.setStatus(rs.getString("status"));
        script.setContent(rs.getString("content"));
        script.setCreatedTime(rs.getTimestamp("created_time"));
        script.setUpdatedTime(rs.getTimestamp("updated_time"));
        script.setLastRunTime(rs.getTimestamp("last_run_time"));
        return script;
    }

    private void setScriptParameters(PreparedStatement pstmt, Script script) throws SQLException {
        pstmt.setString(1, script.getName());
        pstmt.setString(2, script.getType());
        pstmt.setString(3, script.getPath());
        pstmt.setString(4, script.getDescription());
        pstmt.setString(5, script.getCategory());
        pstmt.setString(6, script.getInterpreter());
        pstmt.setString(7, script.getParameters());
        pstmt.setString(8, script.getContent());
    }

    private void initializeNetworkTemplatesTable() {
        String createNetworkTemplatesTable = """
    CREATE TABLE IF NOT EXISTS network_templates (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        name TEXT NOT NULL UNIQUE,
        interface_name TEXT NOT NULL,
        ip_address TEXT NOT NULL,
        subnet_mask TEXT,
        gateway TEXT,
        dns1 TEXT,
        dns2 TEXT,
        description TEXT,
        created_time DATETIME DEFAULT CURRENT_TIMESTAMP,
        updated_time DATETIME DEFAULT CURRENT_TIMESTAMP
    )
    """;

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(createNetworkTemplatesTable);

            // 插入一些示例数据
            initializeSampleTemplates();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to initialize network templates table", e);
        }
    }

    private void initializeSampleTemplates() {
        String[] sampleTemplates = {
                "INSERT OR IGNORE INTO network_templates (name, interface_name, ip_address, subnet_mask, gateway, dns1, dns2, description) VALUES ('办公网络', '以太网', '192.168.1.100', '255.255.255.0', '192.168.1.1', '8.8.8.8', '114.114.114.114', '公司内部办公网络配置')",
                "INSERT OR IGNORE INTO network_templates (name, interface_name, ip_address, subnet_mask, gateway, dns1, dns2, description) VALUES ('测试环境', 'WLAN', '10.0.0.50', '255.255.255.0', '10.0.0.1', '8.8.8.8', '8.8.4.4', '测试环境网络配置')",
                "INSERT OR IGNORE INTO network_templates (name, interface_name, ip_address, subnet_mask, gateway, dns1, dns2, description) VALUES ('开发网络', '以太网2', '172.16.1.100', '255.255.0.0', '172.16.1.1', '223.5.5.5', '223.6.6.6', '开发环境网络配置')"
        };

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            for (String sql : sampleTemplates) {
                try {
                    stmt.execute(sql);
                } catch (SQLException e) {
                    // 忽略重复插入的错误
                    System.out.println("Sample template already exists: " + e.getMessage());
                }
            }
        } catch (SQLException e) {
            // 忽略错误，表格可能已经存在
        }
    }

    // 网络模板相关的数据库操作
    public List<NetworkTemplate> getAllNetworkTemplates() {
        List<NetworkTemplate> templates = new ArrayList<>();
        String sql = "SELECT * FROM network_templates ORDER BY name";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                templates.add(extractNetworkTemplateFromResultSet(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to get network templates", e);
        }
        return templates;
    }

    public void addNetworkTemplate(NetworkTemplate template) {
        String sql = """
    INSERT INTO network_templates (name, interface_name, ip_address, subnet_mask, gateway, dns1, dns2, description)
    VALUES (?, ?, ?, ?, ?, ?, ?, ?)
    """;

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            setNetworkTemplateParameters(pstmt, template);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to add network template", e);
        }
    }

    public void updateNetworkTemplate(NetworkTemplate template) {
        String sql = """
    UPDATE network_templates 
    SET name = ?, interface_name = ?, ip_address = ?, subnet_mask = ?, gateway = ?, 
        dns1 = ?, dns2 = ?, description = ?, updated_time = CURRENT_TIMESTAMP
    WHERE id = ?
    """;

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            setNetworkTemplateParameters(pstmt, template);
            pstmt.setInt(9, template.getId());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update network template", e);
        }
    }

    public void deleteNetworkTemplate(int id) {
        String sql = "DELETE FROM network_templates WHERE id = ?";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, id);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete network template", e);
        }
    }

    public NetworkTemplate getNetworkTemplateByName(String name) {
        String sql = "SELECT * FROM network_templates WHERE name = ?";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, name);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return extractNetworkTemplateFromResultSet(rs);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to get network template by name", e);
        }
        return null;
    }

    // 辅助方法
    private NetworkTemplate extractNetworkTemplateFromResultSet(ResultSet rs) throws SQLException {
        NetworkTemplate template = new NetworkTemplate();
        template.setId(rs.getInt("id"));
        template.setName(rs.getString("name"));
        template.setInterfaceName(rs.getString("interface_name"));
        template.setIpAddress(rs.getString("ip_address"));
        template.setSubnetMask(rs.getString("subnet_mask"));
        template.setGateway(rs.getString("gateway"));
        template.setDns1(rs.getString("dns1"));
        template.setDns2(rs.getString("dns2"));
        template.setDescription(rs.getString("description"));
        template.setCreatedTime(rs.getTimestamp("created_time"));
        template.setUpdatedTime(rs.getTimestamp("updated_time"));
        return template;
    }

    private void setNetworkTemplateParameters(PreparedStatement pstmt, NetworkTemplate template) throws SQLException {
        pstmt.setString(1, template.getName());
        pstmt.setString(2, template.getInterfaceName());
        pstmt.setString(3, template.getIpAddress());
        pstmt.setString(4, template.getSubnetMask());
        pstmt.setString(5, template.getGateway());
        pstmt.setString(6, template.getDns1());
        pstmt.setString(7, template.getDns2());
        pstmt.setString(8, template.getDescription());
    }
}