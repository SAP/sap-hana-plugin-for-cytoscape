package org.sap.cytoscape.internal.tasks;

import org.cytoscape.work.ProvidesTitle;
import org.cytoscape.work.Tunable;
import org.sap.cytoscape.internal.hdb.HanaConnectionCredentials;
import org.sap.cytoscape.internal.tunables.PasswordString;
import org.sap.cytoscape.internal.utils.IOUtils;

import java.io.IOException;
import java.util.Properties;

public class CyConnectTaskTunables {
    /**
     *
     * @return Title of the input parameter dialog
     */
    @ProvidesTitle
    public String getTitle() { return "Connect to SAP HANA instance"; }

    /**
     * host address
     */
    @Tunable(description="Host", groups={"SAP HANA Database"}, required = true, gravity = 1)
    public String host;

    /**
     * Port number (e.g. 443 for SAP HANA Cloud)
     */
    @Tunable(description="Port", groups={"SAP HANA Database"}, required = true, gravity = 2)
    public String port;

    /**
     * Database username
     */
    @Tunable(description="Username", groups={"User Credentials"}, required = true, gravity = 3)
    public String username;

    /**
     * User password
     */
    @Tunable(description="Password", groups={"User Credentials"}, required = true, gravity = 4)
    public PasswordString password;

    /**
     * Checkbox if password shall be stored in an unsecure way
     */
    @Tunable(description="Save Password (plain text)", gravity = 5)
    public boolean savePassword;

    @Tunable(description="Auto-Connect from Cache", dependsOn = "savePassword=true", gravity = 6)
    public boolean autoConnect;

    /**
     *
     */
    public CyConnectTaskTunables(){
        try{

            Properties props = IOUtils.loadProperties();

            this.host = props.getProperty("hdb.host");
            this.port = props.getProperty("hdb.port");
            this.username = props.getProperty("hdb.username");
            this.password = new PasswordString(props.getProperty("hdb.password"));

            // assume that the user still wants to store the password, if this
            // has been done before
            this.savePassword = this.password.getPassword().length() > 0;
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
        return new HanaConnectionCredentials(
                this.host, this.port, this.username, this.password.getPassword()
        );
    }

    /**
     *
     * @throws IOException
     */
    public void saveToCacheFile() throws IOException {

        Properties credProps = new Properties();
        credProps.setProperty("hdb.host", this.host);
        credProps.setProperty("hdb.port", this.port);
        credProps.setProperty("hdb.username", this.username);

        if (savePassword) {
            credProps.setProperty("hdb.password", this.password.getPassword());
        } else {
            // overwrite previously saved passwords
            credProps.setProperty("hdb.password", "");
        }

        credProps.setProperty("hdb.autoconnect", String.valueOf(this.autoConnect));

        IOUtils.cacheProperties(credProps);
    }
}
