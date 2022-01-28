package org.sap.cytoscape.internal.hdb;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class HanaQueryResult {

    private List<Object[]> data;
    private HanaColumnInfo[] columnMetadata;

    public HanaQueryResult(int nColumns){
        data = new ArrayList<>();
        columnMetadata = new HanaColumnInfo[nColumns];
    }

    public void setColumnMetadata(int idx, HanaColumnInfo metadata){
        columnMetadata[idx] = metadata;
    }

    public void addRecord(Object[] newRecord){
        data.add(newRecord);
    }

    public List<Object[]> getRecordList(){
        return Collections.unmodifiableList(this.data);
    }

    public HanaColumnInfo[] getColumnMetadata(){
        return columnMetadata.clone();
    }
}
