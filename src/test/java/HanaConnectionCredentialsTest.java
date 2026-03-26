import org.junit.Assert;
import org.junit.Test;
import org.sap.cytoscape.internal.exceptions.HanaConnectionManagerException;
import org.sap.cytoscape.internal.hdb.HanaConnectionCredentials;

import java.util.Properties;

import static org.junit.Assert.fail;

public class HanaConnectionCredentialsTest {

    @Test
    public void testGenerateAdvancedProperties(){
        HanaConnectionCredentials cred = new HanaConnectionCredentials(null, null, null, null, null, null);
        Properties props = null;

        try{
            cred.advancedProperties = "key=value";
            props = cred.generateAdvancedProperties();
            Assert.assertEquals(1, props.size());
            Assert.assertEquals("value", props.getProperty("key"));
        } catch (Exception e) {
            fail();
        }

        try{
            cred.advancedProperties = "key=value;";
            props = cred.generateAdvancedProperties();
            Assert.assertEquals(1, props.size());
            Assert.assertEquals("value", props.getProperty("key"));
        } catch (Exception e) {
            fail();
        }

        try{
            cred.advancedProperties = "key=value;key1=value1";
            props = cred.generateAdvancedProperties();
            Assert.assertEquals(2, props.size());
            Assert.assertEquals("value", props.getProperty("key"));
            Assert.assertEquals("value1", props.getProperty("key1"));
        } catch (Exception e) {
            fail();
        }

        try{
            cred.advancedProperties = "key=value;key1=value1;";
            props = cred.generateAdvancedProperties();
            Assert.assertEquals(2, props.size());
            Assert.assertEquals("value", props.getProperty("key"));
            Assert.assertEquals("value1", props.getProperty("key1"));
        } catch (Exception e) {
            fail();
        }

        try{
            cred.advancedProperties = "keyvalue";
            props = cred.generateAdvancedProperties();
            fail();
        } catch (HanaConnectionManagerException e) {
            //success
        } catch (Exception e) {
            fail();
        }

        try{
            cred.advancedProperties = "";
            props = cred.generateAdvancedProperties();
            Assert.assertEquals(0, props.size());
        } catch (Exception e) {
            fail();
        }

        try{
            cred.advancedProperties = null;
            props = cred.generateAdvancedProperties();
            Assert.assertEquals(0, props.size());
        } catch (Exception e) {
            fail();
        }

    }

    @Test
    public void testGenerateAdvancedProperties_emptyValue(){
        // "key=".split("=") returns ["key"] only — Java drops the trailing empty token.
        // Accessing index [1] then throws ArrayIndexOutOfBoundsException, which is caught
        // and rethrown as HanaConnectionManagerException.
        // This test documents the actual behaviour of the existing code.
        HanaConnectionCredentials cred = new HanaConnectionCredentials(null, null, null, null, null, null);
        try {
            cred.advancedProperties = "key=";
            cred.generateAdvancedProperties();
            fail("Expected HanaConnectionManagerException for entry with empty value");
        } catch (HanaConnectionManagerException e) {
            // expected — empty value after '=' is not supported by current implementation
        } catch (Exception e) {
            fail("Unexpected exception type: " + e);
        }
    }

    @Test
    public void testGenerateAdvancedProperties_valueContainsEquals(){
        // Current implementation splits on ALL '=' characters, so 'key=val=ue'
        // results in key="key", value="val" (the trailing "=ue" is silently dropped).
        // This test documents the actual behaviour of the existing code.
        HanaConnectionCredentials cred = new HanaConnectionCredentials(null, null, null, null, null, null);
        try {
            cred.advancedProperties = "key=val=ue";
            Properties props = cred.generateAdvancedProperties();
            Assert.assertEquals(1, props.size());
            Assert.assertEquals("val", props.getProperty("key"));
        } catch (Exception e) {
            fail();
        }
    }

}
