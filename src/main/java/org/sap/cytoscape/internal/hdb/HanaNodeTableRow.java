package org.sap.cytoscape.internal.hdb;

import java.util.HashMap;

/**
 * Represents a record in a node table on SAP HANA
 */
public class HanaNodeTableRow extends AbstractHanaGraphTableRow{

    private String keyFieldName;

    public HanaNodeTableRow(){
        super();
    }

    public void setKeyFieldName(String keyFieldName){
        this.keyFieldName = keyFieldName;
    }

    public <T> T getKeyValue(Class<T> targetClassType){
        return this.getFieldValueCast(this.keyFieldName, targetClassType);
    }

    public Object getKeyValueRaw(){
        return this.getFieldValueRaw(this.keyFieldName);
    }

}
