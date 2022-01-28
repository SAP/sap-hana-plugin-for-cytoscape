package org.sap.cytoscape.internal.hdb;

/**
 * Connection credentials to connect to an SAP HANA database
 */
public class HanaConnectionCredentials {
    /**
     * Host address
     */
    public String host;

    /**
     * Port number
     */
    public String port;

    /**
     * Database username
     */
    public String username;

    /**
     * User password
     */
    public String password;

    public HanaConnectionCredentials(String host, String port, String username, String password){
        this.host = host;
        this.port = port;
        this.username = username;
        this.password = password;
    }
}
