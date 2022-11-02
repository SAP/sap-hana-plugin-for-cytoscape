package org.sap.cytoscape.internal.hdb;

/**
 * General purpose connection credentials to inherit from
 */
public abstract class AbstractConnectionCredentials {
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

    public AbstractConnectionCredentials(String host, String port, String username, String password){
        this.host = host;
        this.port = port;
        this.username = username;
        this.password = password;
    }
}
