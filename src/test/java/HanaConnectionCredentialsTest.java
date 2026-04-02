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
        // "key=".split("=", 2) returns ["key", ""] — the value is an empty string.
        // This is accepted and stored as a property with an empty value.
        HanaConnectionCredentials cred = new HanaConnectionCredentials(null, null, null, null, null, null);
        try {
            cred.advancedProperties = "key=";
            Properties props = cred.generateAdvancedProperties();
            Assert.assertEquals(1, props.size());
            Assert.assertEquals("", props.getProperty("key"));
        } catch (Exception e) {
            fail("Empty value should be accepted: " + e);
        }
    }

    @Test
    public void testGenerateAdvancedProperties_valuePreservedWhenItContainsEquals(){
        // Base64-encoded values (SSL certificates, hdbuserstore keys) commonly contain '='.
        // split("=") splits on every '=' and silently truncates the value.
        // split("=", 2) limits to two tokens, preserving everything after the first '='.
        HanaConnectionCredentials cred = new HanaConnectionCredentials(null, null, null, null, null, null);
        try {
            cred.advancedProperties = "key=val=ue";
            Properties props = cred.generateAdvancedProperties();
            Assert.assertEquals(1, props.size());
            Assert.assertEquals("val=ue", props.getProperty("key"));
        } catch (Exception e) {
            fail();
        }
    }

}
