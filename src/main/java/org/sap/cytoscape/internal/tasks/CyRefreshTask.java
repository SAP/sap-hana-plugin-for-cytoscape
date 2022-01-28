package org.sap.cytoscape.internal.tasks;

import org.cytoscape.model.*;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.ContainsTunables;
import org.cytoscape.work.TaskMonitor;
import org.sap.cytoscape.internal.hdb.*;
import org.sap.cytoscape.internal.utils.CyNetworkKey;
import org.sap.cytoscape.internal.utils.CyUtils;

import java.util.HashMap;
import java.util.List;

public class CyRefreshTask extends AbstractTask {

    @ContainsTunables
    public CyRefreshTaskTunables tunables;

    private final CyNetworkFactory networkFactory;
    private final CyNetworkManager networkManager;
    private final HanaConnectionManager connectionManager;

    public CyRefreshTask(
            CyNetworkFactory networkFactory,
            CyNetworkManager networkManager,
            HanaConnectionManager connectionManager
    ) {
        this.networkFactory = networkFactory;
        this.networkManager = networkManager;
        this.connectionManager = connectionManager;

        CyConnectTask.tryConnect(this.connectionManager);

        this.tunables = new CyRefreshTaskTunables(this.networkManager, this.connectionManager);
    }

    @Override
    public void run(TaskMonitor taskMonitor) throws Exception {

        taskMonitor.setTitle("SAP HANA: Refreshing Cytoscape Graph");
        taskMonitor.setProgress(0d);

        taskMonitor.setStatusMessage("Starting to refresh Cytoscape graph");

        if(!this.connectionManager.isConnected()){
            // user has not yet executed the connect task
            taskMonitor.showMessage(
                    TaskMonitor.Level.ERROR,
                    "Connection to SAP HANA has not been established. Please connect first."
            );
            taskMonitor.setProgress(1d);
            return;
        }

        // retrieve selected network
        CyNetworkKey selectedNetworkKey = tunables.networkSelection.getSelectedValue();
        CyNetwork selectedNetwork = this.networkManager.getNetwork(selectedNetworkKey.getSUID());

        String linkedHanaInstance = CyUtils.getSapHanaInstanceFromNetworkTable(selectedNetwork.getDefaultNetworkTable(), selectedNetworkKey.getSUID());
        HanaDbObject linkedHanaWorkspace = CyUtils.getSapHanaWorkspaceFromNetworkTable(selectedNetwork.getDefaultNetworkTable(), selectedNetworkKey.getSUID());

        // we have already checked that this network is linked to the current instance
        // let's do it a second time to be sure. things can be messy.
        if(!linkedHanaInstance.equals(connectionManager.getInstanceIdentifier())){
            taskMonitor.showMessage(
                    TaskMonitor.Level.ERROR,
                    "Mismatch between linked and connected SAP HANA Instance"
            );
            taskMonitor.setProgress(1d);
            return;
        }

        // load data from SAP HANA
        HanaGraphWorkspace graphWorkspace =
                connectionManager.loadGraphWorkspace(linkedHanaWorkspace);

        // add new attributes
        CyUtils.enhanceCyTableWithAttributes(selectedNetwork.getDefaultNodeTable(), graphWorkspace.getNodeFieldList());

        // add new attributes
        CyUtils.enhanceCyTableWithAttributes(selectedNetwork.getDefaultEdgeTable(), graphWorkspace.getEdgeFieldList());

        // measure progress based on number of nodes and edges
        int nGraphObjects = graphWorkspace.getEdgeTable().size() + graphWorkspace.getNodeTable().size();
        int progress = 0;

        taskMonitor.setStatusMessage("Refreshing node data");

        // refresh nodes
        List<CyNode> networkNodes = selectedNetwork.getNodeList();
        HashMap<String, CyNode> remainingNetworkNodesByHanaKey = new HashMap<>();
        HashMap<String, CyNode> nodesByHanaKey = new HashMap<>();

        // build map of existing nodes by hana key
        for(CyNode networkNode : networkNodes) {
            CyRow nodeRow = selectedNetwork.getDefaultNodeTable().getRow(networkNode.getSUID());
            Class fieldType = selectedNetwork.getDefaultNodeTable().getColumn(graphWorkspace.getNodeKeyColInfo().name).getType();
            String hanaKey = nodeRow.get(graphWorkspace.getNodeKeyColInfo().name, fieldType).toString();
            remainingNetworkNodesByHanaKey.put(hanaKey, networkNode);
        }

        // update and add nodes from hana
        for(HanaNodeTableRow row : graphWorkspace.getNodeTable()){
            String hanaKey = row.getKeyValue(String.class);
            CyNode currentNode = null;
            if(remainingNetworkNodesByHanaKey.containsKey(hanaKey)) {
                // update attributes
                currentNode = remainingNetworkNodesByHanaKey.get(hanaKey);
                CyUtils.updateNetworkNodeAttributes(selectedNetwork, currentNode, graphWorkspace, row);
                // we have "seen" this node now.
                remainingNetworkNodesByHanaKey.remove(hanaKey);
            } else {
                // add this as a new node
                currentNode = CyUtils.addNewNodeToNetwork(selectedNetwork, graphWorkspace, row);
            }

            nodesByHanaKey.put(hanaKey, currentNode);
            taskMonitor.setProgress(progress++ / (double)nGraphObjects);
        }

        // remove nodes that are non-existing anymore
        taskMonitor.setStatusMessage("Removing obsolete nodes");
        selectedNetwork.removeNodes(remainingNetworkNodesByHanaKey.values());

        // refresh edges
        taskMonitor.setStatusMessage("Refreshing edge data");

        List<CyEdge> networkEdges = selectedNetwork.getEdgeList();
        HashMap<String, CyEdge> remainingNetworkEdgesByHanaKey = new HashMap<>();

        // build map of existing edges by hana key
        for(CyEdge networkEdge : networkEdges) {
            CyRow edgeRow = selectedNetwork.getDefaultEdgeTable().getRow(networkEdge.getSUID());
            Class fieldType = selectedNetwork.getDefaultEdgeTable().getColumn(graphWorkspace.getEdgeKeyColInfo().name).getType();
            String hanaKey = edgeRow.get(graphWorkspace.getEdgeKeyColInfo().name, fieldType).toString();
            remainingNetworkEdgesByHanaKey.put(hanaKey, networkEdge);
        }


        for(HanaEdgeTableRow row: graphWorkspace.getEdgeTable()){
            String hanaKey = row.getKeyValue(String.class);
            if(remainingNetworkEdgesByHanaKey.containsKey(hanaKey)) {
                // update attributes
                CyUtils.updateNetworkEdgeAttributes(selectedNetwork, remainingNetworkEdgesByHanaKey.get(hanaKey), graphWorkspace, row);
                // we have "seen" this edge now.
                remainingNetworkEdgesByHanaKey.remove(hanaKey);
            } else {
                // add this as a new edge
                CyUtils.addNewEdgeToNetwork(selectedNetwork, graphWorkspace, row, nodesByHanaKey);
            }
            taskMonitor.setProgress(progress++ / (double)nGraphObjects);
        }

        // remove edges that are non-existing anymore
        taskMonitor.setStatusMessage("Removing obsolete edges");
        selectedNetwork.removeEdges(remainingNetworkEdgesByHanaKey.values());

        taskMonitor.setProgress(1d);
        taskMonitor.setStatusMessage("Finished refreshing Cytoscape network from SAP HANA");
    }
}
