import org.junit.Assert;
import org.junit.Test;
import org.sap.cytoscape.internal.hdb.HanaDataType;
import org.sap.cytoscape.internal.hdb.HanaNodeTableRow;

import java.sql.Types;
import java.util.ArrayList;

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
}
