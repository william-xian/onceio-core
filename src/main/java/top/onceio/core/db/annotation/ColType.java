package top.onceio.core.db.annotation;


public enum ColType {

    CHAR,

    BOOLEAN,
    VARCHAR,

    /**
     * 长文本，对应 Clob
     */
    TEXT,

    /**
     * 二进制，对应 Blob
     */
    BINARY,
    TIMESTAMP,
    DATETIME,
    DATE,
    TIME,

    /**
     * 整型：根据字段的宽度来决定具体的数据库字段类型
     */
    INT,

    /**
     * 浮点：根据字段的宽度和精度来决定具体的数据库字段类型
     */
    FLOAT,

    /**
     * JSON：PostgreSQL 的 JSON 类型
     */
    PSQL_JSON,

    /**
     * 数组：PostgreSQL 的数组类型
     */
    PSQL_ARRAY,

    /**
     * JSON：MySQL 的 JSON 类型
     */
    MYSQL_JSON,
    /**
     * 自动
     */
    AUTO
}