package org.sap.cytoscape.internal.tasks;

import org.cytoscape.model.CyNetworkManager;
import org.cytoscape.work.AbstractTaskFactory;
import org.cytoscape.work.TaskIterator;
import org.sap.cytoscape.internal.hdb.HanaConnectionManager;

public class CyCreateWorkspaceTaskFactory extends AbstractTaskFactory {

    private final CyNetworkManager networkManager;
    private final HanaConnectionManager connectionManager;

    public CyCreateWorkspaceTaskFactory(CyNetworkManager networkManager, HanaConnectionManager connectionManager){
        super();
        this.networkManager = networkManager;
        this.connectionManager = connectionManager;
    }

    @Override
    public TaskIterator createTaskIterator() {
        CyCreateWorkspaceTask newTask = new CyCreateWorkspaceTask(this.networkManager, this.connectionManager);
        return new TaskIterator(newTask);
    }
}
