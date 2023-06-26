package org.sap.cytoscape.internal;

import org.cytoscape.model.CyNetworkFactory;
import org.cytoscape.model.CyNetworkManager;
import org.cytoscape.service.util.AbstractCyActivator;
import org.cytoscape.work.ServiceProperties;
import org.cytoscape.work.TaskFactory;
import org.cytoscape.work.swing.GUITunableHandlerFactory;
import org.cytoscape.work.swing.SimpleGUITunableHandlerFactory;
import org.sap.cytoscape.internal.tasks.CyCreateWorkspaceTaskFactory;
import org.sap.cytoscape.internal.tasks.CyLoadTaskFactory;
import org.sap.cytoscape.internal.tasks.CyRefreshTaskFactory;
import org.sap.cytoscape.internal.utils.IOUtils;
import org.osgi.framework.BundleContext;
import org.sap.cytoscape.internal.hdb.HanaConnectionManager;
import org.sap.cytoscape.internal.tasks.CyConnectTaskFactory;

import java.io.IOException;
import java.util.Properties;

public class CyActivator extends AbstractCyActivator {

    private final Properties menuConfiguration;

    public CyActivator() throws IOException {
        super();
        this.menuConfiguration = IOUtils.loadResourceProperties("MenuConfiguration.properties");
    }

    public void start(BundleContext bc) {
        // fetch api stuff
        CyNetworkFactory networkFactory = getService(bc, CyNetworkFactory.class);
        CyNetworkManager networkManager = getService(bc, CyNetworkManager.class);

        try {
            HanaConnectionManager connectionManager = new HanaConnectionManager();

            // connect
            CyConnectTaskFactory connectFactory = new CyConnectTaskFactory(connectionManager);
            Properties connectProps = new Properties();
            connectProps.setProperty(ServiceProperties.PREFERRED_MENU, this.menuConfiguration.getProperty("CONNECT_PREFERRED_MENU"));
            connectProps.setProperty(ServiceProperties.TITLE, this.menuConfiguration.getProperty("CONNECT_TITLE"));
            connectProps.setProperty(ServiceProperties.MENU_GRAVITY, this.menuConfiguration.getProperty("CONNECT_MENU_GRAVITY"));
            connectProps.setProperty(ServiceProperties.INSERT_SEPARATOR_BEFORE, this.menuConfiguration.getProperty("CONNECT_SEPARATOR_BEFORE"));
            connectProps.setProperty(ServiceProperties.INSERT_SEPARATOR_AFTER, this.menuConfiguration.getProperty("CONNECT_SEPARATOR_AFTER"));
            registerService(bc, connectFactory, TaskFactory.class, connectProps);

            // create graph workspace from network
            CyCreateWorkspaceTaskFactory createFactory = new CyCreateWorkspaceTaskFactory(networkManager, connectionManager);
            Properties createProps = new Properties();
            createProps.setProperty(ServiceProperties.PREFERRED_MENU, this.menuConfiguration.getProperty("CREATE_PREFERRED_MENU"));
            createProps.setProperty(ServiceProperties.TITLE, this.menuConfiguration.getProperty("CREATE_TITLE"));
            createProps.setProperty(ServiceProperties.MENU_GRAVITY, this.menuConfiguration.getProperty("CREATE_MENU_GRAVITY"));
            createProps.setProperty(ServiceProperties.INSERT_SEPARATOR_BEFORE, this.menuConfiguration.getProperty("CREATE_SEPARATOR_BEFORE"));
            createProps.setProperty(ServiceProperties.INSERT_SEPARATOR_AFTER, this.menuConfiguration.getProperty("CREATE_SEPARATOR_AFTER"));
            registerService(bc, createFactory, TaskFactory.class, createProps);

            // load graph workspace
            CyLoadTaskFactory loadFactory = new CyLoadTaskFactory(networkFactory, networkManager, connectionManager);
            Properties loadProps = new Properties();
            loadProps.setProperty(ServiceProperties.PREFERRED_MENU, this.menuConfiguration.getProperty("LOAD_PREFERRED_MENU"));
            loadProps.setProperty(ServiceProperties.TITLE, this.menuConfiguration.getProperty("LOAD_TITLE"));
            loadProps.setProperty(ServiceProperties.MENU_GRAVITY, this.menuConfiguration.getProperty("LOAD_MENU_GRAVITY"));
            loadProps.setProperty(ServiceProperties.INSERT_SEPARATOR_BEFORE, this.menuConfiguration.getProperty("LOAD_SEPARATOR_BEFORE"));
            loadProps.setProperty(ServiceProperties.INSERT_SEPARATOR_AFTER, this.menuConfiguration.getProperty("LOAD_SEPARATOR_AFTER"));
            registerService(bc, loadFactory, TaskFactory.class, loadProps);

            // refresh current network from SAP HANA
            CyRefreshTaskFactory refreshFactory = new CyRefreshTaskFactory(networkFactory, networkManager, connectionManager);
            Properties refreshProps = new Properties();
            refreshProps.setProperty(ServiceProperties.PREFERRED_MENU, this.menuConfiguration.getProperty("REFRESH_PREFERRED_MENU"));
            refreshProps.setProperty(ServiceProperties.TITLE, this.menuConfiguration.getProperty("REFRESH_TITLE"));
            refreshProps.setProperty(ServiceProperties.MENU_GRAVITY, this.menuConfiguration.getProperty("REFRESH_MENU_GRAVITY"));
            refreshProps.setProperty(ServiceProperties.INSERT_SEPARATOR_BEFORE, this.menuConfiguration.getProperty("REFRESH_SEPARATOR_BEFORE"));
            refreshProps.setProperty(ServiceProperties.INSERT_SEPARATOR_AFTER, this.menuConfiguration.getProperty("REFRESH_SEPARATOR_AFTER"));
            registerService(bc, refreshFactory, TaskFactory.class, refreshProps);

            // load result of openCypher query

            // load single node (for later exploration via context menu)

            // TODO handle heterogeneous graphs
        } catch (Exception e){
            System.err.println("Failed to activate SAP HANA plug-in for Cytoscape");
            System.err.println(e);
        }
    }

}
