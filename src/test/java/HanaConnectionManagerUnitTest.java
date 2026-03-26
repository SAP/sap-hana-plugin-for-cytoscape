import org.junit.Assert;
import org.junit.Test;
import org.sap.cytoscape.internal.hdb.HanaConnectionManager;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.SQLException;

public class HanaConnectionManagerUnitTest {

    private static HanaConnectionManager newManager() throws Exception {
        return new HanaConnectionManager();
    }

    private static void injectConnection(HanaConnectionManager manager, Connection connection) throws Exception {
        Field f = HanaConnectionManager.class.getDeclaredField("connection");
        f.setAccessible(true);
        f.set(manager, connection);
    }

    private static Connection stubConnection(boolean isValid, boolean isClosed) {
        return (Connection) Proxy.newProxyInstance(
            ClassLoader.getSystemClassLoader(),
            new Class[]{Connection.class},
            (proxy, method, args) -> {
                switch (method.getName()) {
                    case "isValid":  return isValid;
                    case "isClosed": return isClosed;
                    default: return null;
                }
            }
        );
    }

    @Test
    public void testIsConnected_null() throws Exception {
        HanaConnectionManager manager = newManager();
        // connection field stays null (default)
        Assert.assertFalse(manager.isConnected());
    }

    @Test
    public void testIsConnected_valid() throws Exception {
        HanaConnectionManager manager = newManager();
        injectConnection(manager, stubConnection(true, false));
        Assert.assertTrue(manager.isConnected());
    }

    /**
     * Regression test for the stale-connection bug.
     * The old implementation used isClosed() which returns false for a stale connection,
     * causing isConnected() to incorrectly return true.
     * The new implementation uses isValid() which correctly returns false for a stale connection.
     */
    @Test
    public void testIsConnected_stale() throws Exception {
        HanaConnectionManager manager = newManager();
        // isClosed()=false (object not explicitly closed) but isValid()=false (network dead)
        injectConnection(manager, stubConnection(false, false));
        Assert.assertFalse(manager.isConnected());
    }

    @Test
    public void testIsConnected_exception() throws Exception {
        HanaConnectionManager manager = newManager();
        Connection throwingConnection = (Connection) Proxy.newProxyInstance(
            ClassLoader.getSystemClassLoader(),
            new Class[]{Connection.class},
            (proxy, method, args) -> {
                if (method.getName().equals("isValid")) throw new SQLException("connection reset");
                return null;
            }
        );
        injectConnection(manager, throwingConnection);
        Assert.assertFalse(manager.isConnected());
    }

    /**
     * Regression test: disconnect() must call close() on the underlying connection
     * and leave isConnected() returning false.
     */
    @Test
    public void testDisconnect_closesConnection() throws Exception {
        HanaConnectionManager manager = newManager();
        boolean[] closed = {false};
        Connection conn = (Connection) Proxy.newProxyInstance(
            ClassLoader.getSystemClassLoader(),
            new Class[]{Connection.class},
            (proxy, method, args) -> {
                switch (method.getName()) {
                    case "close":   closed[0] = true; return null;
                    case "isValid": return true;
                    default:        return null;
                }
            }
        );
        injectConnection(manager, conn);
        manager.disconnect();
        Assert.assertTrue("close() should have been called on the connection", closed[0]);
        Assert.assertFalse("isConnected() should be false after disconnect", manager.isConnected());
    }

    @Test
    public void testDisconnect_nullSafe() throws Exception {
        HanaConnectionManager manager = newManager(); // connection == null
        manager.disconnect(); // must not throw
        Assert.assertFalse(manager.isConnected());
    }
}
