package org.wx.core.wxBase.unit;

import com.baomidou.mybatisplus.annotation.IdType;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.*;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

/**
 * 代码生成器
 * @author 无心
 * @date 2026/01/13
 */
public class CodeCreate {

    // ===================== 配置项 =====================
    private String host = "localhost";
    private String database = "mnls";
    private String username = "root";
    private String password = "123456";
    private String tableName = "app_level_config";
    private String packName = ".wxBusiness.account";
    private String tablePrefix = "app_";
    private String createUserName = "无心";
    private IdType idType = IdType.AUTO;

    /**
     * 生成Mapper XML文件
     */
    private void generateMapperXml(String entityName, String basePackage) {
        try {
            String packagePath = basePackage.replace(".", "/");
            // XML文件路径：mapper/xml 目录下
            String mapperXmlPath = System.getProperty("user.dir") + "/src/main/java/" + packagePath + "/mapper/xml/";

            // 创建xml目录
            new File(mapperXmlPath).mkdirs();

            // 生成XML内容（标准MyBatis Mapper XML格式）
            String xmlContent = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                    "<!DOCTYPE mapper PUBLIC \"-//mybatis.org//DTD Mapper 3.0//EN\" \"http://mybatis.org/dtd/mybatis-3-mapper.dtd\">\n" +
                    "<mapper namespace=\"" + basePackage + ".mapper." + entityName + "Mapper\">\n\n" +
                    "</mapper>";

            // 写入XML文件
            String xmlFilePath = mapperXmlPath + entityName + "Mapper.xml";
            Files.write(Paths.get(xmlFilePath), xmlContent.getBytes());
            System.out.println("   Mapper XML: " + xmlFilePath);

        } catch (Exception e) {
            System.err.println("   生成Mapper XML失败: " + e.getMessage());
        }
    }
    /**
     * 生成Mapper接口
     */
    private void generateMapper(String entityName, String basePackage) {
        try {
            String packagePath = basePackage.replace(".", "/");
            String mapperPath = System.getProperty("user.dir") + "/src/main/java/" + packagePath + "/mapper/";

            // 创建目录
            new File(mapperPath).mkdirs();

            // 生成Mapper内容
            String mapperContent =
                    "package " + basePackage + ".mapper;\n\n" +
                            "import com.baomidou.mybatisplus.core.mapper.BaseMapper;\n" +
                            "import " + basePackage + ".entity." + entityName + ";\n" +
                            "import org.apache.ibatis.annotations.Mapper;\n" +
                            "\n" +
                            "/**\n" +
                            " * " + entityName + " Mapper接口\n" +
                            " * @author " + createUserName + "\n" +
                            " * @date " + LocalDate.now() + "\n" +
                            " */\n" +
                            "@Mapper\n" +
                            "public interface " + entityName + "Mapper extends BaseMapper<" + entityName + "> {\n" +
                            "}\n";

            // 写入文件
            Files.write(Paths.get(mapperPath + entityName + "Mapper.java"), mapperContent.getBytes());
            System.out.println("   Mapper: " + mapperPath + entityName + "Mapper.java");

        } catch (Exception e) {
            System.err.println("   生成Mapper失败: " + e.getMessage());
        }
    }

    /**
     * 生成Service类
     */
    private void generateService(String entityName, String basePackage) {
        try {
            String packagePath = basePackage.replace(".", "/");
            String servicePath = System.getProperty("user.dir") + "/src/main/java/" + packagePath + "/service/";

            // 创建目录
            new File(servicePath).mkdirs();

            // 生成Service内容
            String serviceContent =
                    "package " + basePackage + ".service;\n\n" +
                            "import org.wx.core.wxBase.base.WxServiceImpl;\n" +
                            "import " + basePackage + ".entity." + entityName + ";\n" +
                            "import " + basePackage + ".mapper." + entityName + "Mapper;\n" +
                            "import org.springframework.stereotype.Service;\n" +
                            "\n" +
                            "/**\n" +
                            " * " + entityName + " Service实现类\n" +
                            " * @author " + createUserName + "\n" +
                            " * @date " + LocalDate.now() + "\n" +
                            " */\n" +
                            "@Service\n" +
                            "public class " + entityName + "Service extends WxServiceImpl<" + entityName + "Mapper, " + entityName + "> {\n" +
                            "}\n";

            // 写入文件
            Files.write(Paths.get(servicePath + entityName + "Service.java"), serviceContent.getBytes());
            System.out.println("   Service: " + servicePath + entityName + "Service.java");

        } catch (Exception e) {
            System.err.println("   生成Service失败: " + e.getMessage());
        }
    }

    /**
     * 获取实体类名
     */
    private String getEntityName() {
        String name = tableName;
        if (tablePrefix != null && !tablePrefix.isEmpty() && name.startsWith(tablePrefix)) {
            name = name.substring(tablePrefix.length());
        }
        return toCamelCase(name, true);
    }

    /**
     * 获取基础包名
     */
    private String getBasePackage() {
        String basePackName = getClass().getPackage().getName();
        basePackName = basePackName.substring(0, basePackName.lastIndexOf("."));
        basePackName = basePackName.substring(0, basePackName.lastIndexOf("."));
        return basePackName + packName;
    }

    /**
     * 下划线转驼峰
     */
    private String toCamelCase(String str, boolean firstLetterUpperCase) {
        if (str == null || str.isEmpty()) {
            return str;
        }

        StringBuilder result = new StringBuilder();
        boolean nextUpperCase = firstLetterUpperCase;

        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            if (c == '_') {
                nextUpperCase = true;
            } else {
                if (nextUpperCase) {
                    result.append(Character.toUpperCase(c));
                    nextUpperCase = false;
                } else {
                    result.append(Character.toLowerCase(c));
                }
            }
        }

        return result.toString();
    }

    // ===================== 增强版：从数据库读取实际字段（修复字段重复核心逻辑） =====================
    public void generateFromDatabase() {
        System.out.println("🔍 开始读取[" + database + "." + tableName + "]表结构...");

        Connection conn = null;
        ResultSet columns = null;
        Set<String> columnNameSet = new HashSet<>(); // 字段去重集合
        int totalReadCount = 0;    // 原始读取字段数
        int validFieldCount = 0;   // 最终有效字段数
        int duplicateCount = 0;    // 去重跳过数
        int superFieldCount = 0;   // 父类字段跳过数

        try {
            // 1. 注册MySQL驱动（适配8.x版本）
            Class.forName("com.mysql.cj.jdbc.Driver");

            // 2. 构建安全的JDBC连接URL
            String jdbcUrl = String.format(
                    "jdbc:mysql://%s:3306/%s?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true",
                    host, database
            );
            conn = DriverManager.getConnection(jdbcUrl, username, password);

            String entityName = getEntityName();
            String basePackage = getBasePackage();
            DatabaseMetaData metaData = conn.getMetaData();

            // 3. 核心修复：精准指定catalog（数据库名），杜绝跨库扫描字段
            // 参数说明：catalog(数据库名)、schemaPattern(MySQL填null)、tableName(精确表名)、columnNamePattern(所有列)
            columns = metaData.getColumns(database, null, tableName, null);

            StringBuilder entityFields = new StringBuilder();

            // 4. 遍历字段（带去重+非空校验）
            while (columns.next()) {
                totalReadCount++;
                // 获取字段信息并做非空处理
                String columnName = Optional.ofNullable(columns.getString("COLUMN_NAME")).orElse("");
                String columnType = Optional.ofNullable(columns.getString("TYPE_NAME")).orElse("");
                String remarks = Optional.ofNullable(columns.getString("REMARKS")).orElse("");

                // 跳过空列名
                if (columnName.isEmpty()) {
                    continue;
                }

                // 字段去重（同一表重复列名直接跳过）
                if (columnNameSet.contains(columnName)) {
                    duplicateCount++;
                    continue;
                }
                columnNameSet.add(columnName);

                // 跳过父类已有字段
                if (isSuperClassField(columnName)) {
                    superFieldCount++;
                    System.out.println("   🔴 跳过父类字段: " + columnName);
                    continue;
                }

                // 转换字段名和类型
                validFieldCount++;
                String fieldName = toCamelCase(columnName, false);
                String javaType = getJavaType(columnType);
                String annotation = getAnnotation(columnName);

                // 拼接字段代码
                if (!remarks.isEmpty()) {
                    entityFields.append("    /**\n     * ").append(remarks).append("\n     */\n");
                }
                if (annotation != null) {
                    entityFields.append("    ").append(annotation).append("\n");
                }
                entityFields.append("    private ").append(javaType).append(" ").append(fieldName).append(";\n\n");

                System.out.printf("   🟢 处理有效字段[%d]: %s -> %s (%s)%n", validFieldCount, columnName, fieldName, javaType);
            }

            // 打印统计日志，验证字段处理结果
            System.out.println("=====================================");
            System.out.println("🔢 字段处理统计（核心验证）：");
            System.out.println("   原始读取字段数：" + totalReadCount);
            System.out.println("   去重跳过字段数：" + duplicateCount);
            System.out.println("   父类跳过字段数：" + superFieldCount);
            System.out.println("   最终有效字段数：" + validFieldCount);
            System.out.println("=====================================");

            // 5. 生成实体类代码
            String entityContent =
                    "package " + basePackage + ".entity;\n\n" +
                            "import com.baomidou.mybatisplus.annotation.*;\n" +
                            "import lombok.Data;\n" +
                            "import lombok.EqualsAndHashCode;\n" +
                            "import org.wx.core.wxBase.base.WxBaseEntity;\n" +
                            "import java.math.BigDecimal;\n" +
                            "import java.time.LocalDate;\n" +
                            "import java.time.LocalDateTime;\n" +
                            "\n" +
                            "/**\n" +
                            " * " + entityName + " 实体类\n" +
                            " * @author " + createUserName + "\n" +
                            " * @date " + LocalDate.now() + "\n" +
                            " */\n" +
                            "@Data\n" +
                            "@EqualsAndHashCode(callSuper = true)\n" +
                            "@TableName(\"" + tableName + "\")\n" +
                            "public class " + entityName + " extends WxBaseEntity<" + entityName + "> {\n\n" +
                            entityFields +
                            "}\n";

            // 6. 写入实体类文件
            String packagePath = basePackage.replace(".", "/");
            String entityDir = System.getProperty("user.dir") + "/src/main/java/" + packagePath + "/entity/";
            new File(entityDir).mkdirs();
            String entityPath = entityDir + entityName + ".java";
            Files.write(Paths.get(entityPath), entityContent.getBytes());
            System.out.println("✅ 实体类生成成功：" + entityPath);

            // 7. 生成Mapper和Service
            generateMapper(entityName, basePackage);
            generateService(entityName, basePackage);
//            generateMapperXml(entityName,basePackage);
        } catch (Exception e) {
            System.err.println("❌ 从数据库生成失败: " + e.getMessage());
            e.printStackTrace();
        } finally {
            // 强制释放数据库资源，避免连接泄漏
            try {
                if (columns != null) columns.close();
                if (conn != null) conn.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 判断是否为父类字段（统一小写避免大小写问题）
     */
    private boolean isSuperClassField(String columnName) {
        if (columnName == null) return false;
        String lowerCol = columnName.toLowerCase();
        return lowerCol.equals("create_time") ||
                lowerCol.equals("update_time") ||
                lowerCol.equals("version"); // 可选：乐观锁字段
    }

    /**
     * SQL类型转Java类型（增强空值判断和类型映射）
     */
    private String getJavaType(String sqlType) {
        if (sqlType == null) return "String";

        sqlType = sqlType.toUpperCase();
        if (sqlType.contains("INT")) return "Integer";
        if (sqlType.contains("BIGINT")) return "Long";
        if (sqlType.contains("VARCHAR") || sqlType.contains("TEXT") || sqlType.contains("CHAR")) return "String";
        if (sqlType.contains("DECIMAL") || sqlType.contains("NUMERIC")) return "BigDecimal";
        if (sqlType.contains("DATETIME") || sqlType.contains("TIMESTAMP")) return "LocalDateTime";
        if (sqlType.contains("DATE")) return "LocalDate";
        if (sqlType.contains("BOOLEAN") || (sqlType.contains("TINYINT") && sqlType.contains("(1)"))) return "Boolean";
        return "Object";
    }

    /**
     * 获取字段注解（主键注解）
     */
    private String getAnnotation(String columnName) {
        if (columnName.equalsIgnoreCase("id")) {
            return "@TableId(type = IdType." + idType.name() + ")";
        }
        return null;
    }

    // ===================== Setter方法（支持链式调用） =====================
    public CodeCreate setHost(String host) {
        this.host = host;
        return this;
    }

    public CodeCreate setDatabase(String database) {
        this.database = database;
        return this;
    }

    public CodeCreate setUsername(String username) {
        this.username = username;
        return this;
    }

    public CodeCreate setPassword(String password) {
        this.password = password;
        return this;
    }

    public CodeCreate setTableName(String tableName) {
        this.tableName = tableName;
        return this;
    }

    public CodeCreate setPackName(String packName) {
        this.packName = packName;
        return this;
    }

    public CodeCreate setTablePrefix(String tablePrefix) {
        this.tablePrefix = tablePrefix;
        return this;
    }

    public CodeCreate setCreateUserName(String createUserName) {
        this.createUserName = createUserName;
        return this;
    }

    public CodeCreate setIdType(IdType idType) {
        this.idType = idType;
        return this;
    }

    // ===================== 批量生成多表（可选扩展） =====================
    public void batchGenerateFromDatabase(String[] tableNames) {
        if (tableNames == null || tableNames.length == 0) {
            System.out.println("⚠️  请传入有效表名数组！");
            return;
        }
        for (String table : tableNames) {
            System.out.println("\n=====================================");
            System.out.println("🚀 开始生成表：" + table);
            System.out.println("=====================================");
            this.setTableName(table).generateFromDatabase();
        }
        System.out.println("\n🎉 所有表生成完成！");
    }

    // ===================== 运行入口 =====================
    public static void main(String[] args) {
        CodeCreate generator = new CodeCreate();
        // 方式1：生成单表
        generator.generateFromDatabase();
    }
}