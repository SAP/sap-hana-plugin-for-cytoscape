import org.cytoscape.model.CyNetworkFactory;
import org.cytoscape.model.CyNetworkManager;
import org.cytoscape.work.TaskMonitor;
import org.junit.Assert;
import org.junit.Test;
import org.sap.cytoscape.internal.hdb.HanaConnectionManager;
import org.sap.cytoscape.internal.tasks.CyLoadTask;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.util.ArrayList;
import java.util.List;

/**
 * Unit tests for CyLoadTask error handling.
 */
public class CyLoadTaskTest {

    // -----------------------------------------------------------------------
    // Stubs
    // -----------------------------------------------------------------------

    /** A TaskMonitor stub that records all showMessage calls. */
    private static class RecordingTaskMonitor implements TaskMonitor {
        final List<String> errors = new ArrayList<>();

        @Override public void showMessage(Level level, String message) {
            if (level == Level.ERROR) errors.add(message);
        }
        @Override public void setTitle(String title) {}
        @Override public void setProgress(double progress) {}
        @Override public void setStatusMessage(String statusMessage) {}
    }

    /**
     * Builds a HanaConnectionManager stub that reports connected=true and returns
     * an empty ResultSet for any query (simulating no graph workspaces).
     */
    private static HanaConnectionManager connectedManagerWithNoWorkspaces() throws Exception {
        // Empty ResultSet stub
        ResultSetMetaData emptyMeta = (ResultSetMetaData) Proxy.newProxyInstance(
            ClassLoader.getSystemClassLoader(),
            new Class[]{ResultSetMetaData.class},
            (proxy, method, args) -> {
                if (method.getName().equals("getColumnCount")) return 0;
                return null;
            }
        );
        ResultSet emptyRs = (ResultSet) Proxy.newProxyInstance(
            ClassLoader.getSystemClassLoader(),
            new Class[]{ResultSet.class},
            (proxy, method, args) -> {
                if (method.getName().equals("next")) return false;
                if (method.getName().equals("getMetaData")) return emptyMeta;
                return null;
            }
        );
        PreparedStatement emptyStmt = (PreparedStatement) Proxy.newProxyInstance(
            ClassLoader.getSystemClassLoader(),
            new Class[]{PreparedStatement.class},
            (proxy, method, args) -> {
                if (method.getName().equals("executeQuery")) return emptyRs;
                if (method.getName().equals("getResultSet")) return emptyRs;
                return null;
            }
        );
        Connection conn = (Connection) Proxy.newProxyInstance(
            ClassLoader.getSystemClassLoader(),
            new Class[]{Connection.class},
            (proxy, method, args) -> {
                switch (method.getName()) {
                    case "isValid":             return true;
                    case "isClosed":            return false;
                    case "prepareStatement":    return emptyStmt;
                    default:                    return null;
                }
            }
        );

        HanaConnectionManager manager = new HanaConnectionManager();
        Field f = HanaConnectionManager.class.getDeclaredField("connection");
        f.setAccessible(true);
        f.set(manager, conn);
        return manager;
    }

    private static CyNetworkFactory dummyNetworkFactory() {
        return (CyNetworkFactory) Proxy.newProxyInstance(
            ClassLoader.getSystemClassLoader(),
            new Class[]{CyNetworkFactory.class},
            (proxy, method, args) -> null
        );
    }

    private static CyNetworkManager dummyNetworkManager() {
        return (CyNetworkManager) Proxy.newProxyInstance(
            ClassLoader.getSystemClassLoader(),
            new Class[]{CyNetworkManager.class},
            (proxy, method, args) -> null
        );
    }

    // -----------------------------------------------------------------------
    // Tests
    // -----------------------------------------------------------------------

    /**
     * Regression test: when the connected HANA instance has no valid graph workspaces,
     * run() must show a friendly error message and return — not throw a NullPointerException
     * or a cryptic HanaConnectionManagerException.
     */
    @Test
    public void testRunShowsErrorWhenNoWorkspacesExist() throws Exception {
        HanaConnectionManager manager = connectedManagerWithNoWorkspaces();
        CyLoadTask task = new CyLoadTask(dummyNetworkFactory(), dummyNetworkManager(), manager);

        RecordingTaskMonitor monitor = new RecordingTaskMonitor();
        task.run(monitor);

        Assert.assertFalse("Expected at least one ERROR message", monitor.errors.isEmpty());
        String errorMsg = monitor.errors.get(0).toLowerCase();
        Assert.assertTrue(
            "Error message should mention 'graph workspace'",
            errorMsg.contains("graph workspace")
        );
    }
}
