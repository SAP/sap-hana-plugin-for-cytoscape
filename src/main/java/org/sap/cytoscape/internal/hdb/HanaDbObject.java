package org.sap.cytoscape.internal.hdb;

/**
 * Describes an object in an SAP HANA database (e.g. table, procedure, graph workspace, ...)
 */
public class HanaDbObject {
    /**
     * Schema name
     */
    public String schema;

    /**
     * Object name
     */
    public String name;

    public HanaDbObject(String schema, String name){
        this.schema = schema;
        this.name = name;
    }

    public String toString(){
        return '"' + this.schema + "\".\"" + this.name+'"';
    }
}
