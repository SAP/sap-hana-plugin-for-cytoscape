package org.sap.cytoscape.internal.hdb;

import java.util.HashMap;

/**
 * Represents a record in an edge table on SAP HANA
 */
public class HanaEdgeTableRow extends AbstractHanaGraphTableRow{

    private String keyFieldName;

    private String sourceFieldName;

    private String targetFieldName;

    public HanaEdgeTableRow(){
        super();
    }

    public void setKeyFieldName(String keyFieldName) {
        this.keyFieldName = keyFieldName;
    }

    public void setSourceFieldName(String sourceFieldName) {
        this.sourceFieldName = sourceFieldName;
    }

    public void setTargetFieldName(String targetFieldName) {
        this.targetFieldName = targetFieldName;
    }

    public <T> T getKeyValue(Class<T> targetClassType){
        return this.getFieldValueCast(this.keyFieldName, targetClassType);
    }

    public <T> T getSourceValue(Class<T> targetClassType){
        return this.getFieldValueCast(this.sourceFieldName, targetClassType);
    }

    public <T> T getTargetValue(Class<T> targetClassType){
        return this.getFieldValueCast(this.targetFieldName, targetClassType);
    }
}
