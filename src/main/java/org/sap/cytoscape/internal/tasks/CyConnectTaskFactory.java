package org.sap.cytoscape.internal.tasks;

import org.cytoscape.work.AbstractTaskFactory;
import org.cytoscape.work.TaskIterator;
import org.sap.cytoscape.internal.hdb.HanaConnectionManager;


public class CyConnectTaskFactory extends AbstractTaskFactory{

    private final HanaConnectionManager connectionManager;

    public CyConnectTaskFactory(
            HanaConnectionManager connectionManager
    ){
        super();

        this.connectionManager = connectionManager;
    }

    public TaskIterator createTaskIterator(){
        CyConnectTask newTask = new CyConnectTask(this.connectionManager);
        return new TaskIterator(newTask);
    }

    public boolean isReady() {
        return true;
    }
}
