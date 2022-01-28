package org.sap.cytoscape.internal.tasks;

import org.cytoscape.model.CyNetworkFactory;
import org.cytoscape.model.CyNetworkManager;
import org.cytoscape.work.AbstractTaskFactory;
import org.cytoscape.work.TaskIterator;
import org.sap.cytoscape.internal.hdb.HanaConnectionManager;

public class CyRefreshTaskFactory extends AbstractTaskFactory {

    private final CyNetworkFactory networkFactory;
    private final CyNetworkManager networkManager;
    private final HanaConnectionManager connectionManager;

    public CyRefreshTaskFactory(
            CyNetworkFactory networkFactory,
            CyNetworkManager networkManager,
            HanaConnectionManager connectionManager
    ){
        super();

        this.networkFactory = networkFactory;
        this.networkManager = networkManager;
        this.connectionManager = connectionManager;
    }

    public TaskIterator createTaskIterator(){
        CyRefreshTask newTask = new CyRefreshTask(this.networkFactory, this.networkManager, this.connectionManager);
        return new TaskIterator(newTask);
    }

    public boolean isReady() {
        return true;
    }
}
