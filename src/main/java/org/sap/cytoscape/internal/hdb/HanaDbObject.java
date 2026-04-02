package org.sap.cytoscape.internal.hdb;

import org.sap.cytoscape.internal.utils.HanaUtils;

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
        return HanaUtils.quoteIdentifier(this.schema) + "." + HanaUtils.quoteIdentifier(this.name);
    }
}
