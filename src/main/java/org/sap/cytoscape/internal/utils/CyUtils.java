package org.sap.cytoscape.internal.utils;

import org.cytoscape.model.*;
import org.sap.cytoscape.internal.exceptions.GraphIncosistencyException;
import org.sap.cytoscape.internal.hdb.*;

import java.util.HashMap;
import java.util.List;

import static org.sap.cytoscape.internal.utils.CyLogging.*;

public class CyUtils {

    /**
     *
     * @param cyTable
     * @param fieldList
     */
    public static void enhanceCyTableWithAttributes(CyTable cyTable, List<HanaColumnInfo> fieldList){
        for(HanaColumnInfo hanaCol : fieldList){
            CyColumn col = cyTable.getColumn(hanaCol.name);

            if(col == null) {
                // try to re-use columns, that are already existing. This might cause clashes with the Cytoscape
                // data model, but makes loading of networks, that have been created with Cytoscape, easier.
                cyTable.createColumn(hanaCol.name, hanaCol.dataType.getJavaCytoDataType(), false);
            }
        }
    }

    /**
     *
     * @param cyNetworkTable
     * @param networkSuid
     * @param sapHanaInstance
     * @param sapHanaWorkspace
     */
    public static void enhanceCyNetworkWithDatabaseLinkInformation(CyTable cyNetworkTable, Long networkSuid, String sapHanaInstance, HanaDbObject sapHanaWorkspace){

        // add instance information
        CyColumn instanceCol = cyNetworkTable.getColumn("sap_hana_instance");
        if(instanceCol == null) {
            cyNetworkTable.createColumn("sap_hana_instance", String.class, false);
        }
        cyNetworkTable.getRow(networkSuid).set("sap_hana_instance", sapHanaInstance);

        // add workspace information
        CyColumn workspaceSchemaCol = cyNetworkTable.getColumn("sap_hana_workspace_schema");
        if(workspaceSchemaCol == null) {
            cyNetworkTable.createColumn("sap_hana_workspace_schema", String.class, false);
        }
        cyNetworkTable.getRow(networkSuid).set("sap_hana_workspace_schema", sapHanaWorkspace.schema);

        CyColumn workspaceNameCol = cyNetworkTable.getColumn("sap_hana_workspace_name");
        if(workspaceNameCol == null) {
            cyNetworkTable.createColumn("sap_hana_workspace_name", String.class, false);
        }
        cyNetworkTable.getRow(networkSuid).set("sap_hana_workspace_name", sapHanaWorkspace.name);
    }

    /**
     *
     * @param cyNetworkTable
     * @param networkSuid
     * @return
     */
    public static String getSapHanaInstanceFromNetworkTable(CyTable cyNetworkTable, long networkSuid){
        return cyNetworkTable.getRow(networkSuid).get("sap_hana_instance", String.class);
    }

    /**
     *
     * @param cyNetworkTable
     * @param networkSuid
     * @return
     */
    public static HanaDbObject getSapHanaWorkspaceFromNetworkTable(CyTable cyNetworkTable, long networkSuid){
        String schema = cyNetworkTable.getRow(networkSuid).get("sap_hana_workspace_schema", String.class);
        String name = cyNetworkTable.getRow(networkSuid).get("sap_hana_workspace_name", String.class);

        return new HanaDbObject(schema, name);
    }

    /**
     *
     * @param network
     * @param graphWorkspace
     * @param row
     * @return
     */
    public static CyNode addNewNodeToNetwork(CyNetwork network, HanaGraphWorkspace graphWorkspace, HanaNodeTableRow row){
        CyNode newNode = network.addNode();
        CyRow newRow = network.getDefaultNodeTable().getRow(newNode.getSUID());

        newRow.set("name", row.getKeyValue(String.class));

        for(HanaColumnInfo field : graphWorkspace.getNodeFieldList()){
            // convert to target type in case an existing cytoscape field is re-used
            Class fieldType = network.getDefaultNodeTable().getColumn(field.name).getType();
            newRow.set(field.name, row.getFieldValueCast(field.name, fieldType));
        }

        return newNode;
    }

    /**
     *
     * @param network
     * @param graphWorkspace
     * @param row
     * @param nodesByHanaKey
     * @return
     */
    public static CyEdge addNewEdgeToNetwork(CyNetwork network, HanaGraphWorkspace graphWorkspace, HanaEdgeTableRow row, HashMap<String, CyNode> nodesByHanaKey) throws GraphIncosistencyException {
        CyNode sourceNode = nodesByHanaKey.get(row.getSourceValue(String.class));
        if (sourceNode == null) {
            err("Source node with id " + row.getSourceValue(String.class) + " is not existing.");
            throw new GraphIncosistencyException("Source node with id " + row.getSourceValue(String.class) + " is not existing.");
        }
        CyNode targetNode = nodesByHanaKey.get(row.getTargetValue(String.class));
        if (targetNode == null) {
            throw new GraphIncosistencyException("Target node with id " + row.getTargetValue(String.class) + " is not existing.");
        }

        CyEdge newEdge = network.addEdge(sourceNode, targetNode, true);
        CyRow newRow = network.getDefaultEdgeTable().getRow(newEdge.getSUID());

        // generate an edge name
        String sourceNodeName = network.getDefaultNodeTable().getRow(sourceNode.getSUID()).get("name", String.class);
        String targetNodeName = network.getDefaultNodeTable().getRow(targetNode.getSUID()).get("name", String.class);
        newRow.set("name", sourceNodeName + " -> " + targetNodeName);

        for(HanaColumnInfo field : graphWorkspace.getEdgeFieldList()){
            // convert to target type in case an existing cytoscape field is re-used
            Class fieldType = network.getDefaultEdgeTable().getColumn(field.name).getType();
            newRow.set(field.name, row.getFieldValueCast(field.name, fieldType));
        }

        return newEdge;
    }

    /**
     *
     * @param node
     * @param graphWorkspace
     * @param row
     */
    public static void updateNetworkNodeAttributes(CyNetwork network, CyNode node, HanaGraphWorkspace graphWorkspace, HanaNodeTableRow row){
        CyRow networkRow = network.getDefaultNodeTable().getRow(node.getSUID());

        for(HanaColumnInfo field : graphWorkspace.getNodeFieldList()){
            // convert to target type in case an existing cytoscape field is re-used
            Class fieldType = network.getDefaultNodeTable().getColumn(field.name).getType();
            networkRow.set(field.name, row.getFieldValueCast(field.name, fieldType));
        }
    }

    /**
     *
     * @param edge
     * @param graphWorkspace
     * @param row
     */
    public static void updateNetworkEdgeAttributes(CyNetwork network, CyEdge edge, HanaGraphWorkspace graphWorkspace, HanaEdgeTableRow row){
        CyRow networkRow = network.getDefaultEdgeTable().getRow(edge.getSUID());

        for(HanaColumnInfo field : graphWorkspace.getEdgeFieldList()){
            // convert to target type in case an existing cytoscape field is re-used
            Class fieldType = network.getDefaultEdgeTable().getColumn(field.name).getType();
            networkRow.set(field.name, row.getFieldValueCast(field.name, fieldType));
        }
    }
}
