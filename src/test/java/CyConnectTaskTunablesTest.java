import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.sap.cytoscape.internal.tasks.CyConnectTaskTunables;
import org.sap.cytoscape.internal.utils.IOUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Properties;

public class CyConnectTaskTunablesTest {

    private String originalUserHome;
    private File tempHome;

    @Before
    public void setUp() throws IOException {
        originalUserHome = System.getProperty("user.home");
        tempHome = Files.createTempDirectory("saphana_test_home").toFile();
        System.setProperty("user.home", tempHome.getAbsolutePath());
    }

    @After
    public void tearDown() {
        System.setProperty("user.home", originalUserHome);
        deleteRecursively(tempHome);
    }

    private void deleteRecursively(File f) {
        if (f.isDirectory()) {
            for (File child : f.listFiles()) deleteRecursively(child);
        }
        f.delete();
    }

    /**
     * When the cache file exists but required keys (host, port, username) are absent,
     * the constructor must leave those fields as "" rather than null, so that
     * saveToCacheFile() does not NPE when it calls Properties.setProperty(key, null).
     */
    @Test
    public void testConstructor_requiredKeysAbsentInCacheDoNotLeaveNullFields() throws IOException {
        // Write a completely empty cache file — all keys absent
        IOUtils.cacheProperties(new Properties());

        CyConnectTaskTunables tunables = new CyConnectTaskTunables();

        Assert.assertNotNull("host must not be null when key is absent from cache", tunables.host);
        Assert.assertNotNull("port must not be null when key is absent from cache", tunables.port);
        Assert.assertNotNull("username must not be null when key is absent from cache", tunables.username);
    }

    /**
     * When the cache file exists but the hdb.password key is absent (e.g. written by an
     * older version of the plug-in), the constructor must not silently crash and must
     * leave the password field as "" rather than null.
     */
    @Test
    public void testConstructor_passwordKeyAbsentInCacheDoesNotLeaveNullPassword() throws IOException {
        Properties cachedProps = new Properties();
        cachedProps.setProperty("hdb.host", "myhost.hana.cloud");
        cachedProps.setProperty("hdb.port", "443");
        cachedProps.setProperty("hdb.username", "myuser");
        // hdb.password key intentionally absent — simulates a cache written by an older version
        IOUtils.cacheProperties(cachedProps);

        CyConnectTaskTunables tunables = new CyConnectTaskTunables();

        Assert.assertEquals("myhost.hana.cloud", tunables.host);
        Assert.assertEquals("myuser", tunables.username);
        Assert.assertNotNull("password must not be null when hdb.password key is absent from cache", tunables.password);
    }

    /**
     * saveToCacheFile() must not throw when optional fields (advancedProperties, proxy
     * host/port/username/password) are null. This is the normal case for a user who does
     * not configure a proxy or advanced JDBC properties.
     */
    @Test
    public void testSaveToCacheFile_doesNotThrowWhenOptionalFieldsAreNull() throws IOException {
        CyConnectTaskTunables tunables = new CyConnectTaskTunables();
        // Simulate a user who filled in only the required fields
        tunables.host = "myhost.hana.cloud";
        tunables.port = "443";
        tunables.username = "myuser";
        tunables.password = "mypassword";
        tunables.savePassword = true;
        // advancedProperties, proxyHost, proxyPort, proxyUsername, proxyPassword left as null

        tunables.saveToCacheFile();

        // Verify the file was actually written and can be read back
        Properties loaded = IOUtils.loadProperties();
        Assert.assertEquals("myhost.hana.cloud", loaded.getProperty("hdb.host"));
        Assert.assertEquals("mypassword", loaded.getProperty("hdb.password"));
    }
}
