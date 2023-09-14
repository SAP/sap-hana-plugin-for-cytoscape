package org.sap.cytoscape.internal.tasks;

import org.cytoscape.work.ProvidesTitle;
import org.cytoscape.work.Tunable;
import org.cytoscape.work.util.ListSingleSelection;
import org.sap.cytoscape.internal.hdb.HanaConnectionCredentials;
import org.sap.cytoscape.internal.hdb.ProxyConnectionCredentials;
import org.sap.cytoscape.internal.utils.IOUtils;

import java.io.IOException;
import java.util.Properties;

public class CyConnectTaskTunables {
    /**
     *
     */
    private static final String KEY_HDB_ISHANACLOUD = "hdb.ishanacloud";

    /**
     *
     * @return Title of the input parameter dialog
     */
    @ProvidesTitle
    public String getTitle() { return "Connect to SAP HANA instance"; }

    /**
     * host address
     */
    @Tunable(description="Host", groups={"SAP HANA Database"}, required = true, gravity = 10)
    public String host;

    /**
     * Port number (e.g. 443 for SAP HANA Cloud)
     */
    @Tunable(description="Port", groups={"SAP HANA Database"}, required = true, gravity = 20)
    public String port;

    /**
     * Determines if the HANA instance is to be considere a HANA Cloud (regardless
     * of the content of the build_branch value that the host returns)
     */
    @Tunable(description = "Hana Cloud", groups = {
            "SAP HANA Database" }, gravity = 24, tooltip = "If not checked there will be an automatic check whether it's a HANA Cloud or HANA On-prem.")
    public boolean isHanaCloud = false;

    /**
     * Specifies advanced option to pass as part of the connection string. This is supposed
     * to be a semicolon separated list of connection properties.
     */
    @Tunable(description="Advanced Properties", groups={"SAP HANA Database"}, required = false, gravity = 25)
    public String advancedProperties;

    /**
     * Database username
     */
    @Tunable(description="Username", groups={"User Credentials"}, required = true, gravity = 30)
    public String username;

    /**
     * User password
     */
    @Tunable(description="Password", groups={"User Credentials"}, required = true, gravity = 40, params="password=true")
    public String password;

    /**
     * Determines if proxy configuration shall be used
     */
    @Tunable(description="Enable Proxy", groups={"SAP HANA Database", "Proxy Configuration"}, gravity = 70, params="displayState=collapsed")
    public boolean enableProxyConfiguration;

    /**
     * Type of proxy: HTTP or SOCKS
     */
    @Tunable(description="Type", groups={"SAP HANA Database", "Proxy Configuration"}, gravity = 80, params="displayState=collapsed", dependsOn = "enableProxyConfiguration=true")
    public ListSingleSelection<String> proxyType = new ListSingleSelection<String>("HTTP", "SOCKS");

    /**
     * Hostname for proxy
     */
    @Tunable(description="Host", groups={"SAP HANA Database", "Proxy Configuration"}, gravity = 90, params="displayState=collapsed", dependsOn = "enableProxyConfiguration=true")
    public String proxyHost;

    /**
     * Port for proxy
     */
    @Tunable(description="Port", groups={"SAP HANA Database", "Proxy Configuration"}, gravity = 100, params="displayState=collapsed", dependsOn = "enableProxyConfiguration=true")
    public String proxyPort;

    /**
     * Username for proxy
     */
    @Tunable(description="Username", groups={"SAP HANA Database", "Proxy Configuration"}, gravity = 110, params="displayState=collapsed", dependsOn = "enableProxyConfiguration=true")
    public String proxyUsername;

    /**
     * Password for proxy
     */
    @Tunable(description="Password", groups={"SAP HANA Database", "Proxy Configuration"}, gravity = 120, params="password=true;displayState=collapsed", dependsOn = "enableProxyConfiguration=true")
    public String proxyPassword;

    /**
     * Checkbox if password shall be stored in an unsecure way
     */
    @Tunable(description="Save Password (plain text)", gravity = 130)
    public boolean savePassword;

    /**
     * Checkbox if connection should be established automatically if credentials have been stored
     */
    @Tunable(description="Auto-Connect from Cache", dependsOn = "savePassword=true", gravity = 140)
    public boolean autoConnect;

    /**
     *
     */
    public CyConnectTaskTunables(){
        try{

            Properties props = IOUtils.loadProperties();

            this.host = props.getProperty("hdb.host");
            this.port = props.getProperty("hdb.port");
            this.advancedProperties = props.getProperty("hdb.advancedproperties");
            this.username = props.getProperty("hdb.username");
            this.password = props.getProperty("hdb.password");
            this.isHanaCloud = Boolean.parseBoolean(props.getProperty(KEY_HDB_ISHANACLOUD, "false"));

            this.enableProxyConfiguration = Boolean.parseBoolean(props.getProperty("hdb.proxy.enabled", "false"));
            if(props.getProperty("hdb.proxy.type") != null){
                this.proxyType.setSelectedValue(props.getProperty("hdb.proxy.type"));
            }
            this.proxyHost = props.getProperty("hdb.proxy.host");
            this.proxyPort = props.getProperty("hdb.proxy.port");
            this.proxyUsername = props.getProperty("hdb.proxy.username");
            this.proxyPassword = props.getProperty("hdb.proxy.password");

            // assume that the user still wants to store the password, if this
            // has been done before
            this.savePassword = this.password.length() > 0 || (this.enableProxyConfiguration == true && this.proxyPassword.length() > 0);
            this.autoConnect = Boolean.parseBoolean(props.getProperty("hdb.autoconnect"));

        }catch (IOException e){
            // file was probably not yet existing
        }
    }

    /**
     *
     * @return
     */
    public HanaConnectionCredentials getHanaConnectionCredentials(){
        ProxyConnectionCredentials proxyConnectionCredentials = null;

        if(this.enableProxyConfiguration){
            proxyConnectionCredentials = new ProxyConnectionCredentials(
                    this.proxyType.getSelectedValue().equals("HTTP"),
                    this.proxyHost,
                    this.proxyPort,
                    this.proxyUsername,
                    this.proxyPassword
            );
        }

        return new HanaConnectionCredentials(
                this.host,
                this.port,
                this.username,
                this.password,
                this.isHanaCloud,
                this.advancedProperties,
                proxyConnectionCredentials
        );
    }

    /**
     *
     * @throws IOException
     */
    public void saveToCacheFile() throws IOException {

        Properties credProps = new Properties();
        credProps.setProperty("hdb.host", this.host);
        credProps.setProperty("hdb.advancedproperties", this.advancedProperties);
        credProps.setProperty("hdb.port", this.port);
        credProps.setProperty("hdb.username", this.username);
        credProps.setProperty(KEY_HDB_ISHANACLOUD, String.valueOf(this.isHanaCloud));

        credProps.setProperty("hdb.proxy.enabled", String.valueOf(this.enableProxyConfiguration));
        credProps.setProperty("hdb.proxy.type", this.proxyType.getSelectedValue());
        credProps.setProperty("hdb.proxy.host", this.proxyHost);
        credProps.setProperty("hdb.proxy.port", this.proxyPort);
        credProps.setProperty("hdb.proxy.username", this.proxyUsername);

        if (savePassword) {
            credProps.setProperty("hdb.password", this.password);
            credProps.setProperty("hdb.proxy.password", this.proxyPassword);
        } else {
            // overwrite previously saved passwords
            credProps.setProperty("hdb.password", "");
            credProps.setProperty("hdb.proxy.password", "");
        }

        credProps.setProperty("hdb.autoconnect", String.valueOf(this.autoConnect));

        IOUtils.cacheProperties(credProps);
    }
}
