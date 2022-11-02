package org.sap.cytoscape.internal.hdb;

/**
 * Connection credentials to connect to an SAP HANA database
 */
public class HanaConnectionCredentials extends AbstractConnectionCredentials {

    public ProxyConnectionCredentials proxyConnectionCredentials;

    /**
     * Construct database connection credentials
     *
     * @param host  Hostname
     * @param port  Port
     * @param username  Database User
     * @param password  Database User Password
     * @param proxyConnectionCredentials    Proxy details. Leave NULL if no proxy used.
     */
    public HanaConnectionCredentials(String host, String port, String username, String password, ProxyConnectionCredentials proxyConnectionCredentials) {
        super(host, port, username, password);
        this.proxyConnectionCredentials = proxyConnectionCredentials;
    }
}
