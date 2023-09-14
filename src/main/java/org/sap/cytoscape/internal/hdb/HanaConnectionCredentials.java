package org.sap.cytoscape.internal.hdb;

import org.sap.cytoscape.internal.exceptions.HanaConnectionManagerException;

import java.util.Properties;

/**
 * Connection credentials to connect to an SAP HANA database
 */
public class HanaConnectionCredentials extends AbstractConnectionCredentials {

    public String advancedProperties;

    public boolean isHanaCloud;

    public ProxyConnectionCredentials proxyConnectionCredentials;

    /**
     * Construct database connection credentials
     *
     * @param host                       Hostname
     * @param port                       Port
     * @param username                   Database User
     * @param password                   Database User Password
     * @param isHanaCloud                If set to true the HANA instance is
     *                                   considered to be a HANA Cloud instance, if
     *                                   false it will be determined during
     *                                   connection.
     * @param advancedProperties         Advanced JDBC Properties
     * @param proxyConnectionCredentials Proxy details. Leave NULL if no proxy used.
     */
    public HanaConnectionCredentials(String host, String port, String username, String password, boolean isHanaCloud,
            String advancedProperties, ProxyConnectionCredentials proxyConnectionCredentials) {
        super(host, port, username, password);
        this.isHanaCloud = isHanaCloud;
        this.advancedProperties = advancedProperties;
        this.proxyConnectionCredentials = proxyConnectionCredentials;
    }

    public Properties generateAdvancedProperties() throws HanaConnectionManagerException {
        Properties resultProperties = new Properties();

        if(this.advancedProperties != null && this.advancedProperties.length() > 0) {
            try{
                String[] splitBySemicolon = this.advancedProperties.split(";");
                for(String keyValEntry : splitBySemicolon) {
                    String[] splitByAssignment = keyValEntry.split("=");
                    resultProperties.setProperty(splitByAssignment[0], splitByAssignment[1]);
                }
            } catch (Exception e) {
                throw new HanaConnectionManagerException("Invalid advanced JDBC properties");
            }

        }

        return resultProperties;
    }
}
