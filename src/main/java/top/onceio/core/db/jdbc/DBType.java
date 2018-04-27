package top.onceio.core.db.jdbc;

/**
 * 列出了支持的数据库类型
 * 
 * @author 
 */
public enum DBType {

    /**
     * IBM DB2
     */
    DB2,
    /**
     * Postgresql
     */
    PSQL,
    /**
     * Oracle
     */
    ORACLE,
    /**
     * MS-SqlServer
     */
    SQLSERVER,
    /**
     * MySql
     */
    MYSQL,
    /**
     * H2Database
     */
    H2,
    /**
     * SQLITE
     */
    SQLITE,
    /**
     * 
     */
    HSQL,
    /**
     * 
     */
    DERBY,
    /**
     * 
     */
    GBASE,
    /**
     * 
     */
    SYBASE,
    /**
     * DM
     */
    DM,
    /**
     * 其他数据库
     */
    OTHER;

    public static DBType fromName(String productName) {
    	if(productName.equals("PostgreSQL")) {
    		return DBType.PSQL;
    	}
    	return DBType.OTHER;
    }
}
