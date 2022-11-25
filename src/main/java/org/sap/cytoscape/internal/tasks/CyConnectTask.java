package org.sap.cytoscape.internal.tasks;

import org.cytoscape.work.*;
import org.sap.cytoscape.internal.exceptions.HanaConnectionManagerException;
import org.sap.cytoscape.internal.hdb.HanaConnectionCredentials;
import org.sap.cytoscape.internal.hdb.HanaConnectionManager;

import java.io.*;
import java.sql.SQLException;

/**
 * The task establishes the connection to an SAP HANA instance
 */
public class CyConnectTask extends AbstractTask {

    /**
     * Will establish a connection if credentials have been saved and auto connect is active.
     * Can be used by other tasks to use auto connect feature.
     *
     * @param connectionManager The connection manager to connect
     * @return  True, if connection is established
     */
    public static boolean tryConnect(HanaConnectionManager connectionManager){
        if (!connectionManager.isConnected()) {
            CyConnectTaskTunables tunables = new CyConnectTaskTunables();
            if(tunables.autoConnect){
                HanaConnectionCredentials cred = tunables.getHanaConnectionCredentials();
                try {
                    connectionManager.connect(cred);
                } catch (SQLException | HanaConnectionManagerException e){
                    return false;
                }
            }
        }
        return connectionManager.isConnected();
    }

    @ContainsTunables
    public CyConnectTaskTunables tunables;

    /**
     * Connection manager for database communication
     */
    private final HanaConnectionManager connectionManager;

    /**
     * Constructor loads credentials from the file system that have been cached before.
     *
     * @param connectionManager HanaConnectionManager object
     */
    public CyConnectTask(
            HanaConnectionManager connectionManager
    ){
        this.connectionManager = connectionManager;
        this.tunables = new CyConnectTaskTunables();
    }

    /**
     * Establishes connection to an SAP HANA database. The credentials will be
     * cached on the file system.
     *
     * @param taskMonitor   TaskMonitor to report progress
     * @throws Exception    In case of errors
     */
    @Override
    public void run(TaskMonitor taskMonitor) throws Exception {

        taskMonitor.setTitle("SAP HANA: Connect Database");
        taskMonitor.setProgress(0d);

        taskMonitor.setStatusMessage("Starting to establish connection to " + tunables.host);

        // save credentials to properties file
        try{
            tunables.saveToCacheFile();
        }catch(IOException e){
            taskMonitor.showMessage(TaskMonitor.Level.ERROR, "Unable to cache login credentials");
            taskMonitor.showMessage(TaskMonitor.Level.ERROR, e.toString());
        }

        // establish connection
        try{
            connectionManager.connect(tunables.getHanaConnectionCredentials());
            taskMonitor.showMessage(TaskMonitor.Level.INFO, "Successfully connected to " + tunables.host);
        } catch (SQLException | HanaConnectionManagerException e){
            taskMonitor.showMessage(TaskMonitor.Level.ERROR, "Could not establish connection to " + tunables.host);
            taskMonitor.showMessage(TaskMonitor.Level.ERROR, e.toString());
        }

        taskMonitor.setProgress(1d);
    }

}
