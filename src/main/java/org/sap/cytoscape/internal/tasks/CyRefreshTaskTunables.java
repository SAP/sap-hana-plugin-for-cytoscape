package org.sap.cytoscape.internal.tasks;

import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNetworkManager;
import org.cytoscape.work.ProvidesTitle;
import org.cytoscape.work.Tunable;
import org.cytoscape.work.util.ListSingleSelection;
import org.sap.cytoscape.internal.hdb.HanaConnectionManager;
import org.sap.cytoscape.internal.hdb.HanaDbObject;
import org.sap.cytoscape.internal.utils.CyNetworkKey;
import org.sap.cytoscape.internal.utils.CyUtils;

import java.sql.SQLException;

public class CyRefreshTaskTunables {
    /**
     *
     * @return Title of the input parameter dialog
     */
    @ProvidesTitle
    public String getTitle() { return "Networks linked to the connected HANA instance"; }

    @Tunable(description="Network", groups = {"Network Selection"}, required = true, gravity = 1)
    public ListSingleSelection<CyNetworkKey> networkSelection;

    public CyRefreshTaskTunables(CyNetworkManager networkManager, HanaConnectionManager connectionManager){

        String connectedInstance = null;
        try{
            connectedInstance = connectionManager.getInstanceIdentifier();
        } catch (SQLException e){
            connectedInstance = null;
        }

        CyNetworkKey[] networkKeys = new CyNetworkKey[networkManager.getNetworkSet().size()];
        int i=0;
        for(CyNetwork network : networkManager.getNetworkSet()){
            Long suid = network.getSUID();

            String hInstance = CyUtils.getSapHanaInstanceFromNetworkTable(network.getDefaultNetworkTable(), suid);
            HanaDbObject hWorkspace = CyUtils.getSapHanaWorkspaceFromNetworkTable(network.getDefaultNetworkTable(), suid);

            if(hInstance != null && hWorkspace != null){
                // valid database linkage is existing
                if(hInstance.equals(connectedInstance)){
                    // only consider networks that are linked to the connected instance
                    String name = network.getDefaultNetworkTable().getAllRows().get(0).get("name", String.class);
                    networkKeys[i++] = new CyNetworkKey(suid, name);
                }
            }
        }

        this.networkSelection = new ListSingleSelection<>(networkKeys);
    }
}
