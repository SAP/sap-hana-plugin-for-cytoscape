package org.sap.cytoscape.internal.hdb;

/**
 * Describes a generic SQL parameter
 */
public class HanaSqlParameter{
    /**
     * Value of the parameter
     */
    public Object parameterValue;
    /**
     * SQL Type of the parameter
     */
    public HanaDataType hanaDataType;

    public HanaSqlParameter(Object parameterValue, int sqlType){
        this.parameterValue = parameterValue;
        this.hanaDataType = new HanaDataType(sqlType);
    }
}
