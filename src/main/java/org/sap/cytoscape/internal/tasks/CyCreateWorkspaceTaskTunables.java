package org.sap.cytoscape.internal.tasks;

import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNetworkManager;
import org.cytoscape.work.ProvidesTitle;
import org.cytoscape.work.Tunable;
import org.cytoscape.work.util.ListChangeListener;
import org.cytoscape.work.util.ListSelection;
import org.cytoscape.work.util.ListSingleSelection;
import org.sap.cytoscape.internal.hdb.HanaConnectionManager;
import org.sap.cytoscape.internal.utils.CyNetworkKey;

import java.sql.SQLException;

public class CyCreateWorkspaceTaskTunables {

    /**
     * ChangeListener for proposing a workspace name based on the selected network
     */
    class ProposeNameListener implements ListChangeListener<CyNetworkKey> {
        private CyCreateWorkspaceTaskTunables tunables;
        public ProposeNameListener(CyCreateWorkspaceTaskTunables tunables) {
            this.tunables = tunables;
        }
        @Override
        public void selectionChanged(ListSelection<CyNetworkKey> source) {
            ListChangeListener.super.selectionChanged(source);
            tunables.setWorkspaceName(((ListSingleSelection<CyNetworkKey>)source).getSelectedValue().getName());
        }
    }

    public CyCreateWorkspaceTaskTunables(CyNetworkManager networkManager, HanaConnectionManager connectionManager){
        try {
            this.schema = connectionManager.getCurrentSchema();
        } catch (SQLException e) {};

        CyNetworkKey[] networkKeys = new CyNetworkKey[networkManager.getNetworkSet().size()];
        int i=0;
        for(CyNetwork network : networkManager.getNetworkSet()){
            Long suid = network.getSUID();
            String name = network.getDefaultNetworkTable().getAllRows().get(0).get("name", String.class);
            networkKeys[i++] = new CyNetworkKey(suid, name);
        }

        this.networkSelection = new ListSingleSelection<>(networkKeys);
        this.networkSelection.addListener(new ProposeNameListener(this));
        this.setWorkspaceName(this.networkSelection.getSelectedValue().getName());
    }

    /**
     *
     * @return Title of the input parameter dialog
     */
    @ProvidesTitle
    public String getTitle() { return "Create Graph Workspace"; }

    @Tunable(description="Network", groups = {"Network Selection"}, required = true, gravity = 1)
    public ListSingleSelection<CyNetworkKey> networkSelection;

    @Tunable(description="Schema", groups={"New Graph Workspace"}, required = true, gravity = 2)
    public String schema;

    private String workspaceName;
    @Tunable(description="Name", groups={"New Graph Workspace"}, listenForChange="networkSelection", required = true, gravity = 3)
    public String getWorkspaceName(){ return this.workspaceName; }
    public void setWorkspaceName(String workspaceName){
        this.edgeTableName = workspaceName + "_EDGES";
        this.nodeTableName = workspaceName + "_NODES";
        this.workspaceName = workspaceName;
    }

    @Tunable(description="Create Schema", groups={"Options"}, gravity = 4)
    public boolean createSchema = true;

    @Tunable(description="Node Table", groups={"New Tables"}, gravity = 5, params="displayState=collapsed", listenForChange={"WorkspaceName","networkSelection"})
    public String nodeTableName;

    @Tunable(description="Edge Table", groups={"New Tables"}, gravity = 6, params="displayState=collapsed", listenForChange={"WorkspaceName","networkSelection"})
    public String edgeTableName;
}
