package org.sap.cytoscape.internal.hdb;

import org.cytoscape.model.*;

import java.sql.Types;
import java.util.*;

/**
 * Represents a graph workspace in SAP HANA
 */
public class HanaGraphWorkspace{

    private static final String SAPHANA_SOURCE_COL = "SAPHANA_SOURCE_SUID";
    private static final String SAPHANA_TARGET_COL = "SAPHANA_TARGET_SUID";

    private List<HanaEdgeTableRow> edgeTable;

    private List<HanaNodeTableRow> nodeTable;

    private HanaDbObject workspaceDbObject;

    private HanaDbObject nodeTableDbObject;

    private HanaDbObject edgeTableDbObject;

    private String nodeKeyColName;

    private String edgeKeyColName;

    private String edgeSourceColName;

    private String edgeTargetColName;

    private Map<String, HanaColumnInfo> edgeFields;

    private Map<String, HanaColumnInfo> nodeFields;

    /**
     *
     * @param targetTable
     * @param cyTable
     * @return
     */
    private static List<HanaColumnInfo> getHanaColumnInfo(HanaDbObject targetTable, CyTable cyTable){
        List<HanaColumnInfo> columnList = new ArrayList<>();
        for(CyColumn col : cyTable.getColumns()){
            columnList.add(new HanaColumnInfo(
                    targetTable.schema,
                    targetTable.name,
                    col.getName(),
                    col.getType(),
                    col.isPrimaryKey()
            ));
        }
        return columnList;
    }

    /**
     * Constructs empty HanaGraphWorkspace
     */
    public HanaGraphWorkspace(){
        this.edgeFields = new HashMap<>();
        this.nodeFields = new HashMap<>();
        this.nodeTable = new ArrayList<>();
        this.edgeTable = new ArrayList<>();
    }

    /**
     * Constructs graph workspace with schema and name
     *
     * @param workspaceDbObject
     */
    public HanaGraphWorkspace(HanaDbObject workspaceDbObject){
        this();
        this.workspaceDbObject = workspaceDbObject;
    }

    /**
     * Constructs a HanaGraphWorkspace from a given CyNetwork
     *
     * @param schema
     * @param workspaceName
     * @param nodeTableName
     * @param edgeTableName
     * @param cyNetwork
     */
    public HanaGraphWorkspace(String schema, String workspaceName, String nodeTableName, String edgeTableName, CyNetwork cyNetwork){

        this();

        this.workspaceDbObject = new HanaDbObject(schema, workspaceName);
        this.nodeTableDbObject = new HanaDbObject(schema, nodeTableName);
        this.edgeTableDbObject = new HanaDbObject(schema, edgeTableName);

        // get node table
        CyTable nodeTable = cyNetwork.getDefaultNodeTable();
        HanaDbObject newHanaNodeTable = new HanaDbObject(schema, nodeTableName);
        List<HanaColumnInfo> hanaNodeCols = getHanaColumnInfo(newHanaNodeTable, nodeTable);

        // read node structure
        for(HanaColumnInfo colInfo : hanaNodeCols){
            this.nodeFields.put(colInfo.name, colInfo);
            if(colInfo.primaryKey){
                this.nodeKeyColName = colInfo.name;
            }
        }

        if(this.nodeKeyColName == null || this.nodeKeyColName.isEmpty()){
            this.nodeKeyColName = "SUID";
        }

        // read node values
        for(CyRow row : nodeTable.getAllRows()){
            HanaNodeTableRow newNodeTableRow = new HanaNodeTableRow();
            newNodeTableRow.setKeyFieldName(this.nodeKeyColName);
            newNodeTableRow.addFieldValues(row.getAllValues());
            this.nodeTable.add(newNodeTableRow);
        }

        // get edge table
        CyTable edgeTable = cyNetwork.getDefaultEdgeTable();
        HanaDbObject newHanaEdgeTable = new HanaDbObject(schema, edgeTableName);
        List<HanaColumnInfo> hanaEdgeCols = getHanaColumnInfo(newHanaEdgeTable, edgeTable);

        // read edge structure
        for(HanaColumnInfo colInfo : hanaEdgeCols){
            this.edgeFields.put(colInfo.name, colInfo);
            if(colInfo.primaryKey){
                this.edgeKeyColName = colInfo.name;
            }
        }

        // not every edge table in cytoscape contains source and target info.
        // see sample dataset "Ivacaftor Coauthor".
        this.edgeSourceColName = SAPHANA_SOURCE_COL;
        this.edgeTargetColName = SAPHANA_TARGET_COL;
        this.edgeFields.put(SAPHANA_SOURCE_COL, new HanaColumnInfo(schema, edgeTableName, SAPHANA_SOURCE_COL, Types.BIGINT, false, true));
        this.edgeFields.put(SAPHANA_TARGET_COL, new HanaColumnInfo(schema, edgeTableName, SAPHANA_TARGET_COL, Types.BIGINT, false, true));

        if(this.edgeKeyColName == null || this.edgeKeyColName.isEmpty()){
            this.edgeKeyColName = "SUID";
        }

        // read edge values
        for(CyEdge edge : cyNetwork.getEdgeList()){
            CyRow row = edgeTable.getRow(edge.getSUID());

            HanaEdgeTableRow newEdgeTableRow = new HanaEdgeTableRow();
            newEdgeTableRow.setKeyFieldName(this.edgeKeyColName);
            newEdgeTableRow.addFieldValues(row.getAllValues());

            newEdgeTableRow.setSourceFieldName(this.edgeSourceColName);
            newEdgeTableRow.setTargetFieldName(this.edgeTargetColName);
            newEdgeTableRow.addFieldValue(this.edgeSourceColName, edge.getSource().getSUID());
            newEdgeTableRow.addFieldValue(this.edgeTargetColName, edge.getTarget().getSUID());

            this.edgeTable.add(newEdgeTableRow);
        }
    }

    /**
     * Checks if metadata is complete
     * (i.e. table contents can be loaded given the existing metadata)
     *
     * @return true, if workspace content can be loaded given the existing metadata
     */
    public boolean isMetadataComplete(){

        if(workspaceDbObject == null) return false;
        if(workspaceDbObject.schema == null || workspaceDbObject.schema.length() == 0) return false;
        if(workspaceDbObject.name == null || workspaceDbObject.name.length() == 0) return false;

        if(!this.nodeFields.containsKey(nodeKeyColName)) return false;
        if(!this.edgeFields.containsKey(edgeKeyColName)) return false;
        if(!this.edgeFields.containsKey(edgeSourceColName)) return false;
        if(!this.edgeFields.containsKey(edgeTargetColName)) return false;

        // not checking for db objects for node and edge tables
        // when loading data these objects are not required.
        // once heterogeneous graphs are supported, these fields will probably change anyway

        return true;
    }

    public List<HanaEdgeTableRow> getEdgeTable() {
        return edgeTable;
    }

    public void setEdgeTable(List<HanaEdgeTableRow> edgeTable) {
        this.edgeTable = edgeTable;
    }

    public void clearEdgeTable(){
        this.setEdgeTable(new ArrayList<>());
    }

    public List<HanaNodeTableRow> getNodeTable() {
        return nodeTable;
    }

    public void setNodeTable(List<HanaNodeTableRow> nodeTable) {
        this.nodeTable = nodeTable;
    }

    public void clearNodeTable(){
        this.setNodeTable(new ArrayList<>());
    }

    public HanaDbObject getWorkspaceDbObject() {
        return this.workspaceDbObject;
    }

    public HanaDbObject getNodeTableDbObject(){
        return this.nodeTableDbObject;
    }

    public HanaDbObject getEdgeTableDbObject(){
        return this.edgeTableDbObject;
    }

    public HanaColumnInfo getNodeKeyColInfo() {
        return this.nodeFields.get(nodeKeyColName);
    }

    public HanaColumnInfo getEdgeKeyColInfo() {
        return this.edgeFields.get(edgeKeyColName);
    }

    public HanaColumnInfo getEdgeSourceColInfo() {
        return this.edgeFields.get(edgeSourceColName);
    }

    public HanaColumnInfo getEdgeTargetColInfo() {
        return this.edgeFields.get(edgeTargetColName);
    }

    public void addEdgeAttributeCol(HanaColumnInfo newCol) {
        this.edgeFields.put(newCol.name, newCol);
    }

    public void addEdgeKeyCol(HanaColumnInfo newCol) {
        this.edgeKeyColName = newCol.name;
        this.edgeFields.put(newCol.name, newCol);
    }

    public void addEdgeSourceCol(HanaColumnInfo newCol) {
        this.edgeSourceColName = newCol.name;
        this.edgeFields.put(newCol.name, newCol);
    }

    public void addEdgeTargetCol(HanaColumnInfo newCol) {
        this.edgeTargetColName = newCol.name;
        this.edgeFields.put(newCol.name, newCol);
    }

    public void addNodeAttributeCol(HanaColumnInfo newCol) {
        this.nodeFields.put(newCol.name, newCol);
    }

    public void addNodeKeyCol(HanaColumnInfo newCol) {
        this.nodeKeyColName = newCol.name;
        this.nodeFields.put(newCol.name, newCol);
    }

    public ArrayList<HanaColumnInfo> getNodeFieldList(){
        return new ArrayList(this.nodeFields.values());
    }

    public HanaColumnInfo getNodeFieldInfo(String name){
        return this.nodeFields.get(name);
    }

    public ArrayList<HanaColumnInfo> getEdgeFieldList() {
        return new ArrayList(this.edgeFields.values());
    }

    public HanaColumnInfo getEdgeFieldInfo(String name){
        return this.edgeFields.get(name);
    }

    public List<Map<String, Object>> getNodeTableData() {
        List<Map<String, Object>> nodeData = new ArrayList<>();
        for(HanaNodeTableRow row : this.nodeTable){
            nodeData.add(row.getFieldValues());
        }
        return nodeData;
    }

    public List<Map<String, Object>> getEdgeTableData() {
        List<Map<String, Object>> edgeData = new ArrayList<>();
        for(HanaEdgeTableRow row : this.edgeTable){
            edgeData.add(row.getFieldValues());
        }
        return edgeData;
    }
}
