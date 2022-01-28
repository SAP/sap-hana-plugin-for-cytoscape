package org.sap.cytoscape.internal.tasks;

import org.cytoscape.model.*;
import org.cytoscape.work.*;
import org.sap.cytoscape.internal.hdb.HanaConnectionManager;
import org.sap.cytoscape.internal.utils.CyNetworkKey;
import org.sap.cytoscape.internal.hdb.HanaGraphWorkspace;
import org.sap.cytoscape.internal.utils.CyUtils;

public class CyCreateWorkspaceTask extends AbstractTask {

    @ContainsTunables
    public CyCreateWorkspaceTaskTunables tunables;

    private final CyNetworkManager networkManager;
    private final HanaConnectionManager connectionManager;

    public CyCreateWorkspaceTask(CyNetworkManager networkManager, HanaConnectionManager connectionManager){
        this.networkManager = networkManager;
        this.connectionManager = connectionManager;

        CyConnectTask.tryConnect(this.connectionManager);

        this.tunables = new CyCreateWorkspaceTaskTunables(this.networkManager, this.connectionManager);
    }

    @Override
    public void run(TaskMonitor taskMonitor) throws Exception {
        taskMonitor.setTitle("SAP HANA: Create Graph Workspace");
        taskMonitor.setProgress(0d);

        taskMonitor.setStatusMessage("Starting to create Graph Workspace " + tunables.schema + "." + tunables.getWorkspaceName());

        if(!this.connectionManager.isConnected()){
            // user has not yet executed the connect task
            taskMonitor.showMessage(
                    TaskMonitor.Level.ERROR,
                    "Connection to SAP HANA has not been established. Please connect first."
            );
            taskMonitor.setProgress(1d);
            return;
        }

        // check/create schema
        if(!this.connectionManager.schemaExists(tunables.schema)){
            if(tunables.createSchema){
                this.connectionManager.createSchema(tunables.schema);
                taskMonitor.setStatusMessage("Schema " + tunables.schema + " has been created");
            }else{
                taskMonitor.showMessage(TaskMonitor.Level.ERROR, "Schema " + tunables.schema + " does not exist.");
                taskMonitor.setProgress(1d);
                return;
            }
        }

        // retrieve selected network
        CyNetworkKey selectedNetworkKey = tunables.networkSelection.getSelectedValue();
        CyNetwork selectedNetwork = this.networkManager.getNetwork(selectedNetworkKey.getSUID());


        taskMonitor.setStatusMessage("Assembling Graph Workspace");
        HanaGraphWorkspace newWorkspace =
                new HanaGraphWorkspace(tunables.schema, tunables.getWorkspaceName(), tunables.nodeTableName, tunables.edgeTableName, selectedNetwork);

        if(!newWorkspace.isMetadataComplete()){
            taskMonitor.showMessage(TaskMonitor.Level.ERROR, "Error assembling workspace. Metadata is not complete.");
            taskMonitor.setProgress(1d);
            return;
        }

        taskMonitor.setStatusMessage("Creating node table");
        // create nodes table
        this.connectionManager.createTable(
                newWorkspace.getNodeTableDbObject(),
                newWorkspace.getNodeFieldList()
        );

        taskMonitor.setStatusMessage("Uploading node records");
        //insert values
        this.connectionManager.bulkInsertData(
                newWorkspace.getNodeTableDbObject(),
                newWorkspace.getNodeFieldList(),
                newWorkspace.getNodeTableData()
        );

        taskMonitor.setStatusMessage("Creating edge table");
        // create edges table
        this.connectionManager.createTable(
                newWorkspace.getEdgeTableDbObject(),
                newWorkspace.getEdgeFieldList()
        );

        taskMonitor.setStatusMessage("Uploading edge records");
        //insert values
        this.connectionManager.bulkInsertData(
                newWorkspace.getEdgeTableDbObject(),
                newWorkspace.getEdgeFieldList(),
                newWorkspace.getEdgeTableData()
        );

        taskMonitor.setStatusMessage("Linking Cytoscape Network to SAP HANA Graph Workspace");
        CyUtils.enhanceCyNetworkWithDatabaseLinkInformation(
                selectedNetwork.getDefaultNetworkTable(),
                selectedNetwork.getSUID(),
                connectionManager.getInstanceIdentifier(),
                newWorkspace.getWorkspaceDbObject()
        );

        taskMonitor.setStatusMessage("Creating Graph Workspace");
        // create graph workspace
        this.connectionManager.createGraphWorkspace(newWorkspace);
    }
}
