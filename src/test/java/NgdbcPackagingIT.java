import org.junit.Test;

import java.io.DataInputStream;
import java.io.File;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import static org.junit.Assert.*;

/**
 * Packaging integration test verifying the FELIX-6396 build-time workaround was applied correctly.
 * https://issues.apache.org/jira/browse/FELIX-6396
 *
 * Remove this entire test class when the FELIX-6396 workaround is removed (see pom.xml).
 */
public class NgdbcPackagingIT {

    @Test
    public void testNgdbcIsEmbedded() throws Exception {
        String jarPath = System.getProperty("bundle.jar");
        assertNotNull("bundle.jar system property not set", jarPath);

        try (JarFile jar = new JarFile(new File(jarPath))) {

            // ngdbc Driver class must be inlined directly in the bundle
            JarEntry driverEntry = jar.getJarEntry("com/sap/db/jdbc/Driver.class");
            assertNotNull("ngdbc Driver class not found in bundle — ngdbc was not inlined",
                    driverEntry);

            // Driver.class must be the Java 9 override (major version 53), NOT the Java 8 base
            // (major version 52). The Java 8 base has a static initializer that throws
            // AssertionError on JVM > 8. Workaround for FELIX-6396 (pre-R7 OSGi ignores
            // META-INF/versions/), so we promote the Java 9 class to the base path at build time.
            try (DataInputStream dis = new DataInputStream(jar.getInputStream(driverEntry))) {
                dis.readInt(); // magic 0xCAFEBABE
                dis.readUnsignedShort(); // minor version
                int majorVersion = dis.readUnsignedShort();
                assertEquals("Driver.class must be Java 9 bytecode (major version 53) — " +
                        "Java 8 base class (major 52) throws AssertionError on Java 17",
                        53, majorVersion);
            }

            // The no-op SocketUtils shim must be present (it replaces both ngdbc variants
            // which are unusable in Cytoscape's OSGi runtime on Java 17)
            assertNotNull("SocketUtils shim not found in bundle",
                    jar.getEntry("com/sap/db/util/SocketUtils.class"));

            // The bundle must NOT declare Multi-Release: true and must NOT contain
            // META-INF/versions overrides — both would re-introduce the runtime errors
            Manifest manifest = jar.getManifest();
            assertNotEquals("Bundle must not declare Multi-Release: true",
                    "true", manifest.getMainAttributes().getValue("Multi-Release"));
            assertNull("META-INF/versions/11 SocketUtils must not be in bundle",
                    jar.getEntry("META-INF/versions/11/com/sap/db/util/SocketUtils.class"));
            assertNull("META-INF/versions/9 Driver must not be in bundle (must be at base path)",
                    jar.getEntry("META-INF/versions/9/com/sap/db/jdbc/Driver.class"));

            // Test-only JARs must NOT be embedded in the production bundle
            assertNull("junit must not be embedded in production bundle",
                    jar.getEntry("lib/junit-4.13.2.jar"));
            assertNull("hamcrest must not be embedded in production bundle",
                    jar.getEntry("lib/hamcrest-core-1.3.jar"));
        }
    }
}
