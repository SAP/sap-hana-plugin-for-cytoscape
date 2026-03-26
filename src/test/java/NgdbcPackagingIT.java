import org.junit.Test;

import java.io.File;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import static org.junit.Assert.*;

public class NgdbcPackagingIT {

    @Test
    public void testNgdbcIsEmbedded() throws Exception {
        String jarPath = System.getProperty("bundle.jar");
        assertNotNull("bundle.jar system property not set", jarPath);

        try (JarFile jar = new JarFile(new File(jarPath))) {

            // ngdbc must be present in lib/
            assertNotNull("lib/ngdbc.jar not found in bundle",
                    jar.getEntry("lib/ngdbc.jar"));

            // Bundle-ClassPath manifest header must reference the embedded JAR
            Manifest manifest = jar.getManifest();
            String bundleClassPath = manifest.getMainAttributes().getValue("Bundle-ClassPath");
            assertNotNull("Bundle-ClassPath header missing from MANIFEST.MF", bundleClassPath);
            assertTrue("Bundle-ClassPath must contain lib/ngdbc.jar",
                    bundleClassPath.contains("lib/ngdbc.jar"));

            // Test-only JARs must NOT be embedded in the production bundle
            assertNull("junit must not be embedded in production bundle",
                    jar.getEntry("lib/junit-4.13.2.jar"));
            assertNull("hamcrest must not be embedded in production bundle",
                    jar.getEntry("lib/hamcrest-core-1.3.jar"));
        }
    }
}
