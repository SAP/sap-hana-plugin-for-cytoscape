import org.junit.Assert;
import org.junit.Test;
import org.sap.cytoscape.internal.exceptions.GraphInconsistencyException;
import org.sap.cytoscape.internal.hdb.*;

import java.sql.Types;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class HanaGraphWorkspaceTest {

    // -------------------------------------------------------------------------
    // isMetadataComplete
    // -------------------------------------------------------------------------

    @Test
    public void testIsMetadataComplete_nullWorkspaceDbObject() {
        HanaGraphWorkspace ws = new HanaGraphWorkspace();
        Assert.assertFalse(ws.isMetadataComplete());
    }

    @Test
    public void testIsMetadataComplete_nullSchema() {
        HanaGraphWorkspace ws = new HanaGraphWorkspace(new HanaDbObject(null, "WS"));
        Assert.assertFalse(ws.isMetadataComplete());
    }

    @Test
    public void testIsMetadataComplete_emptySchema() {
        HanaGraphWorkspace ws = new HanaGraphWorkspace(new HanaDbObject("", "WS"));
        Assert.assertFalse(ws.isMetadataComplete());
    }

    @Test
    public void testIsMetadataComplete_nullName() {
        HanaGraphWorkspace ws = new HanaGraphWorkspace(new HanaDbObject("SCHEMA", null));
        Assert.assertFalse(ws.isMetadataComplete());
    }

    @Test
    public void testIsMetadataComplete_emptyName() {
        HanaGraphWorkspace ws = new HanaGraphWorkspace(new HanaDbObject("SCHEMA", ""));
        Assert.assertFalse(ws.isMetadataComplete());
    }

    @Test
    public void testIsMetadataComplete_missingEdgeColumns() {
        // workspace object is set but no edge columns added yet
        HanaGraphWorkspace ws = new HanaGraphWorkspace(new HanaDbObject("SCHEMA", "WS"));
        Assert.assertFalse(ws.isMetadataComplete());
    }

    @Test
    public void testIsMetadataComplete_complete() {
        HanaGraphWorkspace ws = new HanaGraphWorkspace(new HanaDbObject("SCHEMA", "WS"));
        ws.addEdgeKeyCol(new HanaColumnInfo("SCHEMA", "EDGE_T", "EDGE_ID", Types.INTEGER, true));
        ws.addEdgeSourceCol(new HanaColumnInfo("SCHEMA", "EDGE_T", "SRC", Types.INTEGER, false));
        ws.addEdgeTargetCol(new HanaColumnInfo("SCHEMA", "EDGE_T", "TGT", Types.INTEGER, false));
        // edge-only graph → no node key column required
        Assert.assertTrue(ws.isMetadataComplete());
    }

    // -------------------------------------------------------------------------
    // inferNodesFromEdges
    // -------------------------------------------------------------------------

    @Test(expected = GraphInconsistencyException.class)
    public void testInferNodesFromEdges_throwsForNonEdgeOnlyGraph() throws GraphInconsistencyException {
        HanaGraphWorkspace ws = new HanaGraphWorkspace(new HanaDbObject("SCHEMA", "WS"));
        // addNodeKeyCol sets isEdgeOnlyGraph = false
        ws.addNodeKeyCol(new HanaColumnInfo("SCHEMA", "NODE_T", "NODE_ID", Types.INTEGER, true));
        ws.inferNodesFromEdges();
    }

    @Test
    public void testInferNodesFromEdges_deduplicates() throws GraphInconsistencyException {
        HanaGraphWorkspace ws = new HanaGraphWorkspace(new HanaDbObject("SCHEMA", "WS"));
        ws.addEdgeKeyCol(new HanaColumnInfo("SCHEMA", "EDGE_T", "EDGE_ID", Types.INTEGER, true));
        ws.addEdgeSourceCol(new HanaColumnInfo("SCHEMA", "EDGE_T", "SRC", Types.NVARCHAR, false));
        ws.addEdgeTargetCol(new HanaColumnInfo("SCHEMA", "EDGE_T", "TGT", Types.NVARCHAR, false));

        // Build three edges: A→B, B→C, A→C — unique nodes: A, B, C
        HanaEdgeTableRow e1 = makeEdge("SRC", "TGT", "A", "B");
        HanaEdgeTableRow e2 = makeEdge("SRC", "TGT", "B", "C");
        HanaEdgeTableRow e3 = makeEdge("SRC", "TGT", "A", "C");
        ws.setEdgeTable(Arrays.asList(e1, e2, e3));

        ws.inferNodesFromEdges();

        List<HanaNodeTableRow> nodes = ws.getNodeTable();
        Assert.assertEquals("Expected 3 unique nodes", 3, nodes.size());
    }

    @Test
    public void testInferNodesFromEdges_singleEdge() throws GraphInconsistencyException {
        HanaGraphWorkspace ws = new HanaGraphWorkspace(new HanaDbObject("SCHEMA", "WS"));
        ws.addEdgeKeyCol(new HanaColumnInfo("SCHEMA", "EDGE_T", "EDGE_ID", Types.INTEGER, true));
        ws.addEdgeSourceCol(new HanaColumnInfo("SCHEMA", "EDGE_T", "SRC", Types.NVARCHAR, false));
        ws.addEdgeTargetCol(new HanaColumnInfo("SCHEMA", "EDGE_T", "TGT", Types.NVARCHAR, false));

        ws.setEdgeTable(Arrays.asList(makeEdge("SRC", "TGT", "X", "Y")));

        ws.inferNodesFromEdges();
        Assert.assertEquals(2, ws.getNodeTable().size());
    }

    // -------------------------------------------------------------------------
    // getNodeFieldList insertion order
    // -------------------------------------------------------------------------

    @Test
    public void testGetNodeFieldList_preservesInsertionOrder() {
        HanaGraphWorkspace ws = new HanaGraphWorkspace(new HanaDbObject("S", "W"));
        ws.addNodeKeyCol(new HanaColumnInfo("S", "T", "FIRST", Types.INTEGER, true));
        ws.addNodeAttributeCol(new HanaColumnInfo("S", "T", "SECOND", Types.NVARCHAR));
        ws.addNodeAttributeCol(new HanaColumnInfo("S", "T", "THIRD", Types.DOUBLE));

        ArrayList<HanaColumnInfo> fields = ws.getNodeFieldList();
        Assert.assertEquals("FIRST",  fields.get(0).name);
        Assert.assertEquals("SECOND", fields.get(1).name);
        Assert.assertEquals("THIRD",  fields.get(2).name);
    }

    // -------------------------------------------------------------------------
    // helpers
    // -------------------------------------------------------------------------

    private static HanaEdgeTableRow makeEdge(String srcField, String tgtField, String src, String tgt) {
        HanaEdgeTableRow row = new HanaEdgeTableRow();
        row.setSourceFieldName(srcField);
        row.setTargetFieldName(tgtField);
        row.addFieldValue(srcField, src);
        row.addFieldValue(tgtField, tgt);
        return row;
    }
}
