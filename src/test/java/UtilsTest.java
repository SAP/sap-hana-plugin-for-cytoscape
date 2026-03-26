import org.junit.Assert;
import org.junit.Test;
import org.sap.cytoscape.internal.hdb.HanaConnectionCredentials;
import org.sap.cytoscape.internal.utils.IOUtils;

import java.io.File;
import java.io.IOException;
import java.util.Properties;
import java.util.UUID;

public class UtilsTest {

    @Test
    public void testCacheAndRestoreProperties(){
        String testFileAbsPath = "";
        try{
            File testFile = File.createTempFile("saphana_cytoscape", ".properties");
            testFile.deleteOnExit();
            testFileAbsPath = testFile.getAbsolutePath();
        } catch (IOException e){
            Assert.fail();
        }

        Properties props = new Properties();
        int nProps = 10;
        for (int i=0; i<nProps; i++){
            props.setProperty(UUID.randomUUID().toString(), UUID.randomUUID().toString());
        }

        try{
            IOUtils.cacheProperties(testFileAbsPath, props);
        }catch (IOException e){
            Assert.fail();
        }

        try{
            Properties restoreProps = IOUtils.loadProperties(testFileAbsPath);

            Assert.assertEquals(props.size(), restoreProps.size());
            for(String key : props.stringPropertyNames()){
                Assert.assertEquals(props.getProperty(key), restoreProps.getProperty(key));
            }

            IOUtils.clearCachedCredentials(testFileAbsPath);
        }catch(IOException e) {
            Assert.fail();
        }
    }

    @Test
    public void testGetCacheFile_endsWithExpectedFilename(){
        String cacheFile = IOUtils.getCacheFile();
        Assert.assertNotNull(cacheFile);
        Assert.assertTrue(
                "Cache file path must end with saphana_cytoscape_cache.properties",
                cacheFile.endsWith("saphana_cytoscape_cache.properties"));
    }

    @Test
    public void testClearCachedCredentials_deletesFile() throws IOException {
        File tempFile = File.createTempFile("saphana_test_clear", ".properties");
        Assert.assertTrue("Temp file must exist before clearing", tempFile.exists());

        IOUtils.clearCachedCredentials(tempFile.getAbsolutePath());

        Assert.assertFalse("File should be deleted after clearCachedCredentials", tempFile.exists());
    }

    @Test
    public void testClearCachedCredentials_nonExistentFileNoException() {
        String nonExistent = System.getProperty("java.io.tmpdir") + "/this_file_does_not_exist_" + UUID.randomUUID() + ".properties";
        try {
            IOUtils.clearCachedCredentials(nonExistent);
            // no exception expected — File.delete() on a non-existent file simply returns false
        } catch (Exception e) {
            Assert.fail("clearCachedCredentials must not throw for non-existent file: " + e);
        }
    }

    @Test(expected = IOException.class)
    public void testLoadProperties_nonExistentFileThrows() throws IOException {
        String nonExistent = System.getProperty("java.io.tmpdir") + "/no_such_file_" + UUID.randomUUID() + ".properties";
        IOUtils.loadProperties(nonExistent);
    }

}
