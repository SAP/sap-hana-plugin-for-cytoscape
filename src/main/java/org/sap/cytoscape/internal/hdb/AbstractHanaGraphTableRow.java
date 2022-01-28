package org.sap.cytoscape.internal.hdb;

import java.util.HashMap;
import java.util.Map;

public abstract class AbstractHanaGraphTableRow {

    private HashMap<String, Object> fieldValues;

    AbstractHanaGraphTableRow(){
        this.fieldValues = new HashMap<>();
    }

    public void addFieldValue(String columnName, Object value){
        this.fieldValues.put(columnName, value);
    }

    public void addFieldValues(Map<String, Object> newFieldValues){
        this.fieldValues.putAll(newFieldValues);
    }

    public Map<String, Object> getFieldValues(){
        return this.fieldValues;
    }

    public <T> T getFieldValueCast(String fieldName, Class<T> targetClassType){

        Object value = getFieldValueRaw(fieldName);

        if(value == null){
            return null;
        }else if (targetClassType.isAssignableFrom(String.class)) {
            return (T)value.toString();
        } else if (targetClassType.isAssignableFrom(Integer.class)) {
            return (T)Integer.valueOf(value.toString());
        } else if (targetClassType.isAssignableFrom(Long.class)) {
            return (T)Long.valueOf(value.toString());
        } else if (targetClassType.isAssignableFrom(Boolean.class)) {
            return (T)Boolean.valueOf(value.toString());
        } else if (targetClassType.isAssignableFrom(Double.class)) {
            return (T)Double.valueOf(value.toString());
        } else {
            throw new IllegalArgumentException("Bad type.");
        }
    }

    public Object getFieldValueRaw(String fieldName){
        if(!this.fieldValues.containsKey(fieldName)){
            return null;
        }

        return this.fieldValues.get(fieldName);
    }

}
