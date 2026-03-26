import org.junit.Assert;
import org.junit.Test;
import org.sap.cytoscape.internal.hdb.*;
import org.sap.cytoscape.internal.utils.CyNetworkKey;

import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

public class OtherHdbTest {
    @Test
    public void testNodeSuidCast(){
        HanaNodeTableRow nodeTableRow = new HanaNodeTableRow();
        nodeTableRow.setKeyFieldName("SUID");
        // put number as a string
        nodeTableRow.getFieldValues().put("SUID", "1234");

        long test = nodeTableRow.getKeyValue(Long.class);

        Assert.assertEquals(1234, test);
    }

    @Test
    public void testHanaTypeConversion(){
        Assert.assertEquals(Types.NVARCHAR, new HanaDataType(String.class).getSqlDataType());
        Assert.assertEquals(Types.INTEGER, new HanaDataType(Integer.class).getSqlDataType());
        Assert.assertEquals(Types.DOUBLE, new HanaDataType(Double.class).getSqlDataType());
        Assert.assertEquals(Types.BIGINT, new HanaDataType(Long.class).getSqlDataType());
        Assert.assertEquals(Types.OTHER, new HanaDataType(new ArrayList()).getSqlDataType());
    }

    // -------------------------------------------------------------------------
    // HanaDataType — DDL, Java-Cyto types, Boolean path
    // -------------------------------------------------------------------------

    @Test
    public void testHanaDataType_getHanaDdl() {
        Assert.assertEquals("INTEGER",        new HanaDataType(Types.INTEGER).getHanaDdl());
        Assert.assertEquals("BIGINT",         new HanaDataType(Types.BIGINT).getHanaDdl());
        Assert.assertEquals("BOOLEAN",        new HanaDataType(Types.BOOLEAN).getHanaDdl());
        Assert.assertEquals("DOUBLE",         new HanaDataType(Types.DOUBLE).getHanaDdl());
        Assert.assertEquals("NVARCHAR(5000)", new HanaDataType(Types.NVARCHAR).getHanaDdl());
        Assert.assertEquals("INTEGER",        new HanaDataType(Types.SMALLINT).getHanaDdl());
        Assert.assertEquals("DOUBLE",         new HanaDataType(Types.REAL).getHanaDdl());
    }

    @Test
    public void testHanaDataType_getJavaCytoDataType() {
        Assert.assertEquals(Boolean.class, new HanaDataType(Types.BOOLEAN).getJavaCytoDataType());
        Assert.assertEquals(Integer.class, new HanaDataType(Types.INTEGER).getJavaCytoDataType());
        Assert.assertEquals(Integer.class, new HanaDataType(Types.SMALLINT).getJavaCytoDataType());
        Assert.assertEquals(Long.class,    new HanaDataType(Types.BIGINT).getJavaCytoDataType());
        Assert.assertEquals(Double.class,  new HanaDataType(Types.DOUBLE).getJavaCytoDataType());
        Assert.assertEquals(Double.class,  new HanaDataType(Types.REAL).getJavaCytoDataType());
        Assert.assertEquals(String.class,  new HanaDataType(Types.NVARCHAR).getJavaCytoDataType());
    }

    @Test
    public void testHanaDataType_booleanClassConversion() {
        Assert.assertEquals(Types.BOOLEAN, new HanaDataType(Boolean.class).getSqlDataType());
    }

    // -------------------------------------------------------------------------
    // AbstractHanaGraphTableRow — Boolean / Double / unknown type / missing key
    // -------------------------------------------------------------------------

    @Test
    public void testGetFieldValueCast_boolean() {
        HanaNodeTableRow row = new HanaNodeTableRow();
        row.addFieldValue("FLAG", "true");
        Assert.assertEquals(Boolean.TRUE, row.getFieldValueCast("FLAG", Boolean.class));
    }

    @Test
    public void testGetFieldValueCast_double() {
        HanaNodeTableRow row = new HanaNodeTableRow();
        row.addFieldValue("SCORE", "3.14");
        Assert.assertEquals(3.14, row.getFieldValueCast("SCORE", Double.class), 1e-10);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetFieldValueCast_unknownType() {
        HanaNodeTableRow row = new HanaNodeTableRow();
        row.addFieldValue("DATA", "value");
        row.getFieldValueCast("DATA", List.class);
    }

    @Test
    public void testGetFieldValueRaw_missingKey() {
        HanaNodeTableRow row = new HanaNodeTableRow();
        Assert.assertNull(row.getFieldValueRaw("NONEXISTENT"));
    }

    @Test
    public void testGetFieldValueCast_nullForMissingKey() {
        HanaNodeTableRow row = new HanaNodeTableRow();
        Assert.assertNull(row.getFieldValueCast("NONEXISTENT", String.class));
    }

    // -------------------------------------------------------------------------
    // HanaDbObject — toString
    // -------------------------------------------------------------------------

    @Test
    public void testHanaDbObjectToString() {
        HanaDbObject obj = new HanaDbObject("MY_SCHEMA", "MY_TABLE");
        Assert.assertEquals("\"MY_SCHEMA\".\"MY_TABLE\"", obj.toString());
    }

    // -------------------------------------------------------------------------
    // HanaQueryResult — unmodifiable list + defensive clone
    // -------------------------------------------------------------------------

    @Test(expected = UnsupportedOperationException.class)
    public void testHanaQueryResult_recordListIsUnmodifiable() {
        HanaQueryResult result = new HanaQueryResult(1);
        result.getRecordList().add(new Object[]{"X"});
    }

    @Test
    public void testHanaQueryResult_columnMetadataIsClone() {
        HanaQueryResult result = new HanaQueryResult(2);
        result.setColumnMetadata(0, new HanaColumnInfo("S", "T", "C0", Types.INTEGER));
        result.setColumnMetadata(1, new HanaColumnInfo("S", "T", "C1", Types.NVARCHAR));

        HanaColumnInfo[] first = result.getColumnMetadata();
        first[0] = null; // mutate the returned array

        HanaColumnInfo[] second = result.getColumnMetadata();
        Assert.assertNotNull("getColumnMetadata must return a fresh clone each time", second[0]);
    }

    // -------------------------------------------------------------------------
    // CyNetworkKey — constructor + getters
    // -------------------------------------------------------------------------

    @Test
    public void testCyNetworkKey_getters() {
        CyNetworkKey key = new CyNetworkKey(42L, "MyNetwork");
        Assert.assertEquals(Long.valueOf(42L), key.getSUID());
        Assert.assertEquals("MyNetwork", key.getName());
        Assert.assertEquals("MyNetwork", key.toString());
    }

    // -------------------------------------------------------------------------
    // HanaNodeTableRow — getKeyValueRaw
    // -------------------------------------------------------------------------

    @Test
    public void testHanaNodeTableRow_getKeyValueRaw() {
        HanaNodeTableRow row = new HanaNodeTableRow();
        row.setKeyFieldName("ID");
        row.addFieldValue("ID", 99);
        Assert.assertEquals(99, row.getKeyValueRaw());
    }

    // -------------------------------------------------------------------------
    // HanaEdgeTableRow — typed getters
    // -------------------------------------------------------------------------

    @Test
    public void testHanaEdgeTableRow_getters() {
        HanaEdgeTableRow row = new HanaEdgeTableRow();
        row.setKeyFieldName("EDGE_ID");
        row.setSourceFieldName("SRC");
        row.setTargetFieldName("TGT");
        row.addFieldValue("EDGE_ID", "E1");
        row.addFieldValue("SRC", "NodeA");
        row.addFieldValue("TGT", "NodeB");

        Assert.assertEquals("E1",    row.getKeyValue(String.class));
        Assert.assertEquals("NodeA", row.getSourceValue(String.class));
        Assert.assertEquals("NodeB", row.getTargetValue(String.class));
    }
}
