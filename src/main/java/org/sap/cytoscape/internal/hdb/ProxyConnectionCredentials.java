package org.sap.cytoscape.internal.hdb;

public class ProxyConnectionCredentials extends AbstractConnectionCredentials{

    /**
     * Determines if HTTP or SOCKS is used
     */
    public boolean isHttpProxy;

    public ProxyConnectionCredentials(boolean isHttpProxy, String host, String port, String username, String password) {
        super(host, port, username, password);
        this.isHttpProxy = isHttpProxy;
    }
}
