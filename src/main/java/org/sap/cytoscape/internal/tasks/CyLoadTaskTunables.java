package org.sap.cytoscape.internal.tasks;

import org.cytoscape.work.ProvidesTitle;
import org.cytoscape.work.Tunable;
import org.cytoscape.work.util.ListSingleSelection;
import org.sap.cytoscape.internal.hdb.HanaConnectionManager;
import org.sap.cytoscape.internal.hdb.HanaDbObject;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class CyLoadTaskTunables {

    /**
     *
     * @return Title of the input parameter dialog
     */
    @ProvidesTitle
    public String getTitle() { return "Select Graph Workspace"; }

    /**
     * Dropdown box for selection of graph workspace. Will be pre-populated on the constructor.
     */
    @Tunable(description="Schema/Name", groups = {"Graph Workspace"}, required = true, params="lookup=contains", gravity=1)
    public ListSingleSelection<String> workspaceSelection;

    /**
     * Maps graph workspaces by their name in the tunable dropdown box
     */
    public HashMap<String, HanaDbObject> graphWorkspaces;

    public CyLoadTaskTunables(HanaConnectionManager connectionManager){
        if(connectionManager.isConnected()){

            try {
                List<HanaDbObject> workspaceList = connectionManager.listGraphWorkspaces();
                graphWorkspaces = new HashMap<>();

                for (HanaDbObject ws : workspaceList) {
                    String namedItem = ws.schema + "." + ws.name;
                    graphWorkspaces.put(namedItem, ws);
                }
                // pre-populate available workspaces
                String[] wsArray = graphWorkspaces.keySet().toArray(new String[0]);
                Arrays.sort(wsArray);
                this.workspaceSelection = new ListSingleSelection<String>(wsArray);
            } catch (SQLException e){
                this.workspaceSelection = new ListSingleSelection<>();
            }

        }else{
            // since there is no option, Cytoscape will skip the dialog and start the run method.
            this.workspaceSelection = new ListSingleSelection<>();
        }
    }

}
