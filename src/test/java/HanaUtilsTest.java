import org.junit.Assert;
import org.junit.Test;
import org.sap.cytoscape.internal.utils.HanaUtils;

public class HanaUtilsTest {

    // -------------------------------------------------------------------------
    // toStrNull
    // -------------------------------------------------------------------------

    @Test
    public void testToStrNull_nonNull() {
        Assert.assertEquals("hello", HanaUtils.toStrNull("hello"));
    }

    @Test
    public void testToStrNull_null() {
        Assert.assertEquals("DB_NULL", HanaUtils.toStrNull(null));
    }

    @Test
    public void testToStrNull_nonStringObject() {
        Assert.assertEquals("42", HanaUtils.toStrNull(42));
    }

    // -------------------------------------------------------------------------
    // quoteIdentifier
    // -------------------------------------------------------------------------

    @Test
    public void testQuoteIdentifier_simple() {
        Assert.assertEquals("\"MY_TABLE\"", HanaUtils.quoteIdentifier("MY_TABLE"));
    }

    @Test
    public void testQuoteIdentifier_emptyString() {
        Assert.assertEquals("\"\"", HanaUtils.quoteIdentifier(""));
    }

    @Test
    public void testQuoteIdentifier_containsDoubleQuote() {
        // An embedded " must be escaped as "" per SQL standard identifier quoting.
        // Without escaping, foo"bar produces "foo"bar" which breaks out of the identifier.
        Assert.assertEquals("\"foo\"\"bar\"", HanaUtils.quoteIdentifier("foo\"bar"));
    }

    // -------------------------------------------------------------------------
    // isCloudEdition
    // -------------------------------------------------------------------------

    @Test
    public void testIsCloudEdition_true() {
        Assert.assertTrue(HanaUtils.isCloudEdition("fa/CE.2021.28"));
    }

    @Test
    public void testIsCloudEdition_false() {
        Assert.assertFalse(HanaUtils.isCloudEdition("2.00.060.00.1644229716"));
    }

    @Test
    public void testIsCloudEdition_null() {
        Assert.assertFalse(HanaUtils.isCloudEdition(null));
    }

    @Test
    public void testIsCloudEdition_emptyString() {
        Assert.assertFalse(HanaUtils.isCloudEdition(""));
    }
}
