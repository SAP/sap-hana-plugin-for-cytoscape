package org.sap.cytoscape.internal.hdb;

/**
 * Describes a column of an SAP HANA database
 */
public class HanaColumnInfo{
    /**
     * Schema name
     */
    public String schema;

    /**
     * Table name
     */
    public String table;

    /**
     * Column name
     */
    public String name;

    /**
     *
     */
    public HanaDataType dataType;

    /**
     *
     */
    public boolean primaryKey;

    /**
     *
     */
    public boolean notNull;

    public HanaColumnInfo(String schema, String table, String name, int sqlType) {
        this(schema, table, name, sqlType, false);
    }

    public HanaColumnInfo(String schema, String table, String name, Class javaType) {
        this(schema, table, name, javaType, false);
    }

    public HanaColumnInfo(String schema, String table, String name, int sqlType, boolean primaryKey){
        this(schema, table, name, sqlType, primaryKey, false);
    }

    public HanaColumnInfo(String schema, String table, String name, Class javaType, boolean primaryKey){
        this(schema, table, name, javaType, primaryKey, false);
    }

    public HanaColumnInfo(String schema, String table, String name, int sqlType, boolean primaryKey, boolean notNull){
        this.schema = schema;
        this.table = table;
        this.name = name;
        this.dataType = new HanaDataType(sqlType);
        this.primaryKey = primaryKey;
        this.notNull = notNull;
    }

    public HanaColumnInfo(String schema, String table, String name, Class javaType, boolean primaryKey, boolean notNull){
        this.schema = schema;
        this.table = table;
        this.name = name;
        this.dataType = new HanaDataType(javaType);
        this.primaryKey = primaryKey;
        this.notNull = notNull;
    }
}
