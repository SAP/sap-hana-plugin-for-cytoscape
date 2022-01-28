package org.sap.cytoscape.internal.tasks;

import org.cytoscape.model.*;
import org.cytoscape.work.*;
import org.sap.cytoscape.internal.hdb.*;
import org.sap.cytoscape.internal.utils.CyUtils;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;

/**
 * This task loads all the nodes and edges of a given graph workspace on SAP HANA
 */
public class CyLoadTask extends AbstractTask {

    @ContainsTunables
    public CyLoadTaskTunables tunables;

    private final CyNetworkFactory networkFactory;
    private final CyNetworkManager networkManager;
    private final HanaConnectionManager connectionManager;

    /**
     * Constructor uses the connectionManager to initially retrieve the list of available graph
     * workspaces on the system.
     *
     * @param networkFactory    Creation of networks
     * @param networkManager    Registering networks in the client
     * @param connectionManager Manage connection to SAP HANA
     */
    public CyLoadTask(
            CyNetworkFactory networkFactory,
            CyNetworkManager networkManager,
            HanaConnectionManager connectionManager
    ) {
        this.networkFactory = networkFactory;
        this.networkManager = networkManager;
        this.connectionManager = connectionManager;

        CyConnectTask.tryConnect(this.connectionManager);

        this.tunables = new CyLoadTaskTunables(this.connectionManager);
    }

    /**
     * Loads all edges and nodes from the select graph workspace on SAP HANA
     *
     * @param taskMonitor   TaskMonitor to report progress
     * @throws Exception    In case of errors
     */
    @Override
    public void run(TaskMonitor taskMonitor) throws Exception {

        taskMonitor.setTitle("SAP HANA: Load Graph Workspace");
        taskMonitor.setProgress(0d);

        taskMonitor.setStatusMessage("Starting to load Graph Workspace");

        if(!this.connectionManager.isConnected()){
            // user has not yet executed the connect task
            taskMonitor.showMessage(
                    TaskMonitor.Level.ERROR,
                    "Connection to SAP HANA has not been established. Please connect first."
            );
            taskMonitor.setProgress(1d);
            return;
        }

        // retrieve selected graph workspace
        String selectedWorkspaceKey = tunables.workspaceSelection.getSelectedValue();
        HanaDbObject selectedWorkspace = tunables.graphWorkspaces.get(selectedWorkspaceKey);

        taskMonitor.setStatusMessage("Downloading data from Graph Workspace " + selectedWorkspaceKey + " in SAP HANA");

        // load data from SAP HANA
        HanaGraphWorkspace graphWorkspace =
                connectionManager.loadGraphWorkspace(selectedWorkspace);

        // start network creation in Cytoscape
        CyNetwork newNetwork = this.networkFactory.createNetwork();

        // visible name of the network in the client
        newNetwork.getDefaultNetworkTable().getRow(newNetwork.getSUID()).set("name", selectedWorkspaceKey);

        // link to hana instance and graph workspace to enable operations such as 'refresh'
        CyUtils.enhanceCyNetworkWithDatabaseLinkInformation(
                newNetwork.getDefaultNetworkTable(),
                newNetwork.getSUID(),
                connectionManager.getInstanceIdentifier(),
                graphWorkspace.getWorkspaceDbObject()
        );

        // create node attributes
        CyUtils.enhanceCyTableWithAttributes(newNetwork.getDefaultNodeTable(), graphWorkspace.getNodeFieldList());

        // create edge attributes
        CyUtils.enhanceCyTableWithAttributes(newNetwork.getDefaultEdgeTable(), graphWorkspace.getEdgeFieldList());

        // measure progress based on number of nodes and edges
        int nGraphObjects = graphWorkspace.getEdgeTable().size() + graphWorkspace.getNodeTable().size();
        int progress = 0;

        taskMonitor.setStatusMessage("Creating nodes");

        // create nodes
        HashMap<String, CyNode> nodesByHanaKey = new HashMap<>();
        for(HanaNodeTableRow row : graphWorkspace.getNodeTable()){
            CyNode newNode = CyUtils.addNewNodeToNetwork(newNetwork, graphWorkspace, row);
            nodesByHanaKey.put(row.getKeyValue(String.class), newNode);
            taskMonitor.setProgress(progress++ / (double)nGraphObjects);
        }

        taskMonitor.setStatusMessage("Creating edges");

        // create edges
        for(HanaEdgeTableRow row: graphWorkspace.getEdgeTable()){
            CyUtils.addNewEdgeToNetwork(newNetwork, graphWorkspace, row, nodesByHanaKey);
            taskMonitor.setProgress(progress++ / (double)nGraphObjects);
        }

        networkManager.addNetwork(newNetwork);

        taskMonitor.setProgress(1d);
        taskMonitor.setStatusMessage("Finished creating network from Graph Workspace in SAP HANA");
    }
}
