import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.sap.cytoscape.internal.hdb.*;
import org.sap.cytoscape.internal.utils.IOUtils;

import java.io.IOException;
import java.sql.SQLException;
import java.sql.Types;
import java.util.*;

public class HanaConnectionManagerTest {

    private static HanaConnectionManager connectionManager;
    private static Properties sqlStringsTest;
    private static String testSchema;

    private static HanaConnectionCredentials getTestCredentials(){
        try{
            Properties connectProps = IOUtils.loadResourceProperties("testcredentials.properties");

            HanaConnectionCredentials testCred = new HanaConnectionCredentials(
                    connectProps.getProperty("host"),
                    connectProps.getProperty("port"),
                    connectProps.getProperty("username"),
                    connectProps.getProperty("password")
            );

            return testCred;
        }catch (Exception e){
            System.err.println("Cannot load connection details for test instance");
            return null;
        }
    }

    private static HanaConnectionManager connectToTestInstance() throws SQLException, IOException {
        HanaConnectionManager connectionManager = new HanaConnectionManager();
        connectionManager.connect(getTestCredentials());
        return connectionManager;
    }

    private static void createSspGraph() throws SQLException {
        connectionManager.execute((sqlStringsTest.getProperty("CREATE_SSP_TABLES")));
        connectionManager.execute((sqlStringsTest.getProperty("CREATE_SSP_WORKSPACE")));
    }

    private static void createFlightsGraph() throws SQLException {
        connectionManager.execute((sqlStringsTest.getProperty("CREATE_FLIGHTS_TABLES")));
        connectionManager.execute((sqlStringsTest.getProperty("INSERT_FLIGHTS_TABLES_VALUES")));
        connectionManager.execute((sqlStringsTest.getProperty("CREATE_FLIGHTS_WORKSPACE")));
    }

    @BeforeClass
    public static void setUp() throws IOException, SQLException {
        sqlStringsTest = IOUtils.loadResourceProperties("SqlStringsTest.sql");
        testSchema = "CYTOSCAPE_TEST_" + UUID.randomUUID().toString();

        connectionManager = connectToTestInstance();

        // create a test schema with random name
        connectionManager.createSchema(testSchema);
        connectionManager.execute(String.format(sqlStringsTest.getProperty("SET_SCHEMA"), testSchema));

        createSspGraph();
        createFlightsGraph();
    }

    @AfterClass
    public static void cleanUp() throws SQLException {
        connectionManager.execute(String.format(sqlStringsTest.getProperty("DROP_SCHEMA_CASCADE"), testSchema));
    }

    @Test
    public void testInitialSetup(){
        Assert.assertNotNull(connectionManager);
        Assert.assertTrue(connectionManager.isConnected());
        try {
            Assert.assertTrue(connectionManager.schemaExists(testSchema));
            Assert.assertEquals(testSchema, connectionManager.getCurrentSchema());
        } catch (SQLException e){
            Assert.fail(e.toString());
        }
    }

    @Test
    public void testGetCurrentSchema(){
        String currentSchema = null;

        try{
            currentSchema = connectionManager.getCurrentSchema();
        } catch (SQLException e){
            Assert.fail(e.toString());
        }

        Assert.assertNotNull(currentSchema);
        Assert.assertTrue(currentSchema.length() > 0);
    }

    @Test
    public void testSomeQuery(){

        HanaQueryResult queryResult = null;
        try {
            queryResult = connectionManager.executeQueryList(
                    sqlStringsTest.getProperty("SOME_QUERY"),
                    null
            );
        } catch (SQLException e){
            Assert.fail(e.toString());
        }

        List<Object[]> resultList = queryResult.getRecordList();

        if(resultList == null || resultList.size() != 2) Assert.fail();

        Object[] firstRow = resultList.get(0);
        if(!firstRow[0].equals("A")) Assert.fail();
        if(!firstRow[1].equals("B")) Assert.fail();

        Object[] secondRow = resultList.get(1);
        if(!secondRow[0].equals("C")) Assert.fail();
        if(!secondRow[1].equals("D")) Assert.fail();
    }

    @Test
    public void testLoadGraphWorkspace(){
        HanaGraphWorkspace sspWorkspace = null;
        try {
            sspWorkspace = connectionManager.loadGraphWorkspace(connectionManager.getCurrentSchema(), "SSP");
        } catch (Exception e){
            Assert.fail(e.toString());
        }

        Assert.assertNotNull(sspWorkspace);
        Assert.assertTrue(sspWorkspace.isMetadataComplete());
        Assert.assertEquals(6, sspWorkspace.getEdgeTable().size());
        Assert.assertEquals(4, sspWorkspace.getNodeTable().size());
    }

    @Test
    public void testLoadGraphWorkspaceWithGeometries(){
        HanaGraphWorkspace flightsWorkspace = null;
        try {
            flightsWorkspace = connectionManager.loadGraphWorkspace(this.connectionManager.getCurrentSchema(), "FLIGHTS");
        } catch (Exception e){
            Assert.fail(e.toString());
        }

        Assert.assertNotNull(flightsWorkspace);
        Assert.assertTrue(flightsWorkspace.isMetadataComplete());
        Assert.assertEquals(31, flightsWorkspace.getEdgeTable().size());
        Assert.assertEquals(8, flightsWorkspace.getNodeTable().size());
    }

    @Test
    public void testListGraphWorkspace(){
        List<HanaDbObject> workspaceList = null;
        String currentSchema = null;

        try {
            workspaceList = connectionManager.listGraphWorkspaces();
            currentSchema = connectionManager.getCurrentSchema();
        } catch (SQLException e){
            Assert.fail(e.toString());
        }

        boolean foundSSPWorkspace = false;
        boolean foundFlightsWorkspace = false;
        for(HanaDbObject ws : workspaceList){
            if(ws.schema.equals(currentSchema) && ws.name.equals("SSP")){
                foundSSPWorkspace = true;
            }else if(ws.schema.equals(currentSchema) && ws.name.equals("FLIGHTS")){
                foundFlightsWorkspace = true;
            } else if(ws.schema.equals(currentSchema)){
                Assert.fail("Unknown workspace retrieved");
            }
        }

        Assert.assertTrue(foundSSPWorkspace);
        Assert.assertTrue(foundFlightsWorkspace);
    }

    @Test
    public void testSchemaExists(){
        try {
            Assert.assertTrue(connectionManager.schemaExists("SYS"));
            Assert.assertFalse(connectionManager.schemaExists("THIS_SCHEMA_DOES_NOT_EXIST"));
        } catch (SQLException e){
            Assert.fail(e.toString());
        }
    }

    @Test
    public void testCreateTable(){
        try {
            String newTableName = "TEST_TABLE";
            HanaDbObject newTable = new HanaDbObject(connectionManager.getCurrentSchema(), newTableName);
            List<HanaColumnInfo> newCols = Arrays.asList(
                    new HanaColumnInfo(newTable.schema, newTable.name, "COL1", Types.INTEGER),
                    new HanaColumnInfo(newTable.schema, newTable.name, "COL2", Types.NVARCHAR),
                    new HanaColumnInfo(newTable.schema, newTable.name, "COL3", Types.DOUBLE)
            );

            connectionManager.createTable(newTable, newCols);

            HanaQueryResult queryResult = connectionManager.executeQueryList(String.format(
                    sqlStringsTest.getProperty("GENERIC_SELECT_PROJECTION"),
                    "*",
                    connectionManager.getCurrentSchema(),
                    newTableName
            ));

            Assert.assertEquals(3, queryResult.getColumnMetadata().length);
            for(HanaColumnInfo colInfo : queryResult.getColumnMetadata()){
                if(colInfo.name.equals("COL1")){
                    Assert.assertEquals(Types.INTEGER, colInfo.dataType.getSqlDataType());
                } else if(colInfo.name.equals("COL2")){
                    Assert.assertEquals(Types.NVARCHAR, colInfo.dataType.getSqlDataType());
                } else if(colInfo.name.equals("COL3")){
                    Assert.assertEquals(Types.DOUBLE, colInfo.dataType.getSqlDataType());
                } else {
                    Assert.fail("Unknown column");
                }
            }
        } catch (SQLException e){
            Assert.fail(e.toString());
        }
    }

    @Test
    public void testBulkInsert(){
        HanaDbObject targetTable = new HanaDbObject(testSchema, "TEST_BULK_INSERT_TABLE");
        List<HanaColumnInfo> columnInfoList = Arrays.asList(
                new HanaColumnInfo(targetTable.schema, targetTable.name, "COL1", Types.NVARCHAR),
                new HanaColumnInfo(targetTable.schema, targetTable.name, "COL2", Types.NVARCHAR),
                new HanaColumnInfo(targetTable.schema, targetTable.name, "COL3", Types.NVARCHAR)
        );

        int nRecords = 10;
        List<Map<String, Object>> data = new ArrayList<>();
        for(int i = 0; i < nRecords; i++){
            Map<String, Object> record = new HashMap<>();
            record.put("COL1", UUID.randomUUID().toString());
            record.put("COL2", UUID.randomUUID().toString());
            record.put("COL3", UUID.randomUUID().toString());
            data.add(record);
        }

        try {
            connectionManager.createTable(targetTable, columnInfoList);
            connectionManager.bulkInsertData(targetTable, columnInfoList, data);
            int actualRecords = connectionManager.executeQuerySingleValue(String.format(
                    sqlStringsTest.getProperty("COUNT_RECORDS"),
                    targetTable.schema,
                    targetTable.name
            ), null, Integer.class);
            Assert.assertEquals(nRecords, actualRecords);
        } catch (SQLException e){
            Assert.fail(e.toString());
        }
    }

    @Test
    public void testResultSetSqlTypes(){
        try{
            HanaGraphWorkspace sspWorkspace = connectionManager.loadGraphWorkspace(connectionManager.getCurrentSchema(), "SSP");

            for(HanaColumnInfo colInfo : sspWorkspace.getNodeFieldList()){
                if(colInfo.name.equals("ID")){
                    Assert.assertEquals(Types.INTEGER, colInfo.dataType.getSqlDataType());
                }
                if(colInfo.name.equals("NAME")){
                    Assert.assertEquals(Types.NVARCHAR, colInfo.dataType.getSqlDataType());
                }
            }
        }catch(Exception e){
            Assert.fail(e.toString());
        }
    }

    @Test
    public void testResultSetJavaTypes(){
        try{
            HanaGraphWorkspace sspWorkspace = connectionManager.loadGraphWorkspace(connectionManager.getCurrentSchema(), "SSP");

            for(HanaNodeTableRow row : sspWorkspace.getNodeTable()){
                Object idValue = row.getFieldValueRaw("ID");
                Assert.assertNotNull(idValue);
                Assert.assertEquals(Integer.class, idValue.getClass());
                Assert.assertNotEquals(String.class, idValue.getClass());

                Object nameValue = row.getFieldValueRaw("NAME");
                Assert.assertNotNull(nameValue);
                Assert.assertNotEquals(Integer.class, nameValue.getClass());
                Assert.assertEquals(String.class, nameValue.getClass());
            }

        }catch(Exception e){
            Assert.fail(e.toString());
        }
    }
}
