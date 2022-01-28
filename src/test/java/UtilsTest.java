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

}
