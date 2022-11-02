package org.sap.cytoscape.internal.hdb;

import org.sap.cytoscape.internal.exceptions.HanaConnectionManagerException;
import org.sap.cytoscape.internal.utils.IOUtils;

import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import static org.sap.cytoscape.internal.utils.CyLogging.*;
import static org.sap.cytoscape.internal.utils.HanaUtils.*;

/**
 * Handles communication with the database and SAP HANA specific stuff
 */
public class HanaConnectionManager {

    /**
     * Internal connection object
     */
    private Connection connection;

    /**
     * Holding all SQL statement that are required
     */
    private final Properties sqlStrings;

    /**
     * HANA version and edition (Cloud, On prem)
     * For instance HANA Cloud: fa/CE2021.18
     * HANA on prem: fa/hana2sp05
     */
    private String buildVersion;

    /**
     * Default constructor
     */
    public HanaConnectionManager() throws IOException {
        this.connection = null;
        this.sqlStrings = IOUtils.loadResourceProperties("SqlStrings.sql");
    }

    /**
     * Establish connection to a HANA database
     *
     * @param host      Host address
     * @param port      Port number
     * @param connectionProperties  Properties to be used (at least user and password)
     */
    public void connect(String host, String port, Properties connectionProperties) throws SQLException {

        this.connection = null;

        try {
            this.connection = DriverManager.getConnection("jdbc:sap://" + host + ":" + port + "/", connectionProperties);

            if (this.connection.isValid(1500)){
                this.buildVersion = this.executeQuerySingleValue(this.sqlStrings.getProperty("GET_BUILD"), null, String.class);
            }

            info("Connected to HANA database: "+host+" ("+this.buildVersion+")");
        } catch (SQLException e) {
            err("Error connecting to HANA instance:"+host);
            err(e.toString());
            throw e;
        }
    }

    /**
     * Establish connection to a HANA database
     *
     * @param cred  Connection credentials
     */
    public void connect(HanaConnectionCredentials cred) throws SQLException {

        Properties connectionProperties = new Properties();
        connectionProperties.setProperty("autocommit", "true");
        connectionProperties.setProperty("user", cred.username);
        connectionProperties.setProperty("password", cred.password);
        connectionProperties.setProperty("useProxy", "false");

        if(cred.proxyConnectionCredentials != null) {
            connectionProperties.setProperty("useProxy", "true");
            connectionProperties.setProperty("proxyHttp", String.valueOf(cred.proxyConnectionCredentials.isHttpProxy));
            connectionProperties.setProperty("proxyHostname", cred.proxyConnectionCredentials.host);
            connectionProperties.setProperty("proxyPort", cred.proxyConnectionCredentials.port);
            connectionProperties.setProperty("useProxyAuth", "false");
            if(cred.proxyConnectionCredentials.username.length() > 0){
                connectionProperties.setProperty("useProxyAuth", "true");
                connectionProperties.setProperty("proxyUsername", cred.proxyConnectionCredentials.username);
                connectionProperties.setProperty("proxyPassword", cred.proxyConnectionCredentials.password);
            }
        }

        this.connect(cred.host, cred.port, connectionProperties);
    }

    /**
     * Checks if this instance of HanaConnectionManager is connected to
     * SAP HANA database
     *
     * @return True, if connection has been established
     */
    public boolean isConnected(){
        if(this.connection == null){
            return false;
        }else{
            try{
                return !this.connection.isClosed();
            }catch (Exception e){
                return false;
            }
        }
    }

    /**
     * Executes a statement on the database
     *
     * @param statement The statement to execute
     */
    public void execute(String statement) throws SQLException {
        try{
            Statement stmt = this.connection.createStatement();
            stmt.execute(statement);
        } catch (SQLException e){
            err("Could not execute statement: " + statement);
            err(e.toString());
            throw e;
        }
    }

    /**
     * Executes a query statement on the database
     *
     * @param statement The statement to execute
     * @param params    SQL parameters
     * @return          The ResultSet of the query; Null in case of errors
     */
    private ResultSet executeQuery(String statement, HanaSqlParameter[] params) throws SQLException {
        try{
            PreparedStatement stmt = this.connection.prepareStatement(statement);

            if(params != null) {
                for (int i = 0; i < params.length; i++) {
                    stmt.setObject(i+1, params[i].parameterValue, params[i].hanaDataType.getSqlDataType());
                }
            }

            return stmt.executeQuery();
        } catch (SQLException e){
            err("Could not execute statement: " + statement );
            err(e.getMessage());
            throw e;
        }
    }

    /**
     * Executes a query statement on the database
     *
     * @param statement The statement to execute
     * @return          The result of the query as a list; Null in case of errors
     */
    public HanaQueryResult executeQueryList(String statement) throws SQLException {
        return this.executeQueryList(statement, null);
    }

    /**
     * Executes a query statement on the database
     *
     * @param statement The statement to execute
     * @param params    SQL parameters
     * @return          The result of the query as a list; Null in case of errors
     */
    public HanaQueryResult executeQueryList(String statement, HanaSqlParameter[] params) throws SQLException {
        ResultSet resultSet = this.executeQuery(statement, params);

        try {
            ResultSetMetaData metaData = resultSet.getMetaData();
            int nCols = metaData.getColumnCount();
            HanaQueryResult queryResult = new HanaQueryResult(nCols);
            for(int col = 1; col <= nCols; col++) {
                queryResult.setColumnMetadata(col-1, new HanaColumnInfo(
                        metaData.getSchemaName(col),
                        metaData.getTableName(col),
                        metaData.getColumnName(col),
                        metaData.getColumnType(col),
                        false,
                        metaData.isNullable(col) == 0
                ));
            }

            while (resultSet.next()) {
                Object[] newRow = new Object[nCols];
                for (int col = 1; col <= nCols; col++) {
                    newRow[col - 1] = resultSet.getObject(col);
                }
                queryResult.addRecord(newRow);
            }

            return queryResult;
        } catch (SQLException e) {
            err("Could not fetch data. " + statement);
            err(e.toString());
            throw e;
        }
    }

    /**
     * Executes a query statement on the database that return a single value
     *
     * @param statement The statement to execute
     * @param params    SQL parameters
     * @param type      Class type of the single value
     * @param <T>       Template type inferred from class type
     * @return          Single value returned by the query; Null in case of errors
     */
    public <T> T executeQuerySingleValue(String statement, HanaSqlParameter[] params, Class<T> type) throws SQLException {
        ResultSet resultSet = this.executeQuery(statement, params);

        try {
            resultSet.next();
            return resultSet.getObject(1, type);
        } catch (SQLException e) {
            err("Could not fetch data. " + statement);
            err(e.toString());
            throw e;
        }
    }

    /**
     * Executes the same statement multiple times with different parameters as batch
     *
     * @param statement         Statement to execute
     * @param batchParameter    List of parameter configurations
     * @throws SQLException     sql error
     */
    private void executeBatch(String statement, List<HanaSqlParameter[]> batchParameter) throws SQLException {
        PreparedStatement batchStmt = this.connection.prepareStatement(statement);

        for(HanaSqlParameter[] recordParameter : batchParameter){
            for(int i=0; i<recordParameter.length; i++){
                Object value = recordParameter[i].parameterValue;
                if(value == null){
                    batchStmt.setNull(i+1, Types.VARCHAR);
                }else{
                    batchStmt.setString(i+1, value.toString());
                }
            }
            batchStmt.addBatch();
        }
        batchStmt.executeBatch();
    }

    /**
     * Retrieves the current schema from the database
     *
     * @return  Name of the currently active schema
     */
    public String getCurrentSchema() throws SQLException {
        return this.executeQuerySingleValue(this.sqlStrings.getProperty("SELECT_CURRENT_SCHEMA"), null, String.class);
    }

    /**
     *
     * @return
     * @throws SQLException
     */
    public String getInstanceIdentifier() throws SQLException {
        return this.connection.getMetaData().getURL();
    }

    /**
     * Retrieves a list of all graph workspaces on the SAP HANA instance
     *
     * @return  List of all available graph workspaces
     */
    public List<HanaDbObject> listGraphWorkspaces() throws SQLException {
        HanaQueryResult queryResult = this.executeQueryList(this.sqlStrings.getProperty("LIST_GRAPH_WORKSPACES"));

        List<HanaDbObject> workspaceList = new ArrayList<>();
        for(Object[] row : queryResult.getRecordList()){
            workspaceList.add(new HanaDbObject(toStrNull(row[0]), toStrNull(row[1])));
        }

        return workspaceList;
    }

    /**
     * Loads the metadata of a given graph workspace object with pre-populated
     * workspaceDbObject (i.e. schema and name are already given)
     *
     * @param graphWorkspace    HanaGraphWorkspace with pre-populated workspaceDbObject
     */
    private void loadWorkspaceMetadata(HanaGraphWorkspace graphWorkspace) throws SQLException, HanaConnectionManagerException {

        String propName="LOAD_WORKSPACE_METADATA_HANA_" +
                (isCloudEdition(this.buildVersion)? "CLOUD":"ONPREM");

        debug("Reading graph metadata with "+propName);
        HanaQueryResult wsMetadata = this.executeQueryList(
                    this.sqlStrings.getProperty(propName),
                    new HanaSqlParameter[]{
                            new HanaSqlParameter(graphWorkspace.getWorkspaceDbObject().schema, Types.VARCHAR),
                            new HanaSqlParameter(graphWorkspace.getWorkspaceDbObject().name, Types.VARCHAR)
                    }
            );
        
        for(Object[] row : wsMetadata.getRecordList()){
            // Types will be set when retrieving actual data
            HanaColumnInfo newColInfo = new HanaColumnInfo(toStrNull(row[2]), toStrNull(row[3]), toStrNull(row[4]), Types.OTHER);

            switch(toStrNull(row[0])){
                case "EDGE":
                    switch(toStrNull(row[1])){
                        case "KEY":
                            graphWorkspace.addEdgeKeyCol(newColInfo);
                            break;
                        case "SOURCE":
                            graphWorkspace.addEdgeSourceCol(newColInfo);
                            break;
                        case "TARGET":
                            graphWorkspace.addEdgeTargetCol(newColInfo);
                            break;
                        default:
                            graphWorkspace.addEdgeAttributeCol(newColInfo);
                    }
                    break;
                case "VERTEX":
                    switch (toStrNull(row[1])){
                        case "KEY":
                            graphWorkspace.addNodeKeyCol(newColInfo);
                            break;
                        default:
                            graphWorkspace.addNodeAttributeCol(newColInfo);
                    }
                    break;
            }
        }

        if(!graphWorkspace.isMetadataComplete()){
            err("Incomplete graph workspace definition in GRAPH_WORKSPACE_COLUMNS");
            throw new HanaConnectionManagerException("Incomplete graph workspace definition in GRAPH_WORKSPACE_COLUMNS");
        }
    }

    /**
     * Loads the content of the node table for a HanaGraphWorkspace object with complete metadata
     *
     * @param graphWorkspace    HANA Graph Workspace with complete metadata
     */
    private void loadNetworkNodes(HanaGraphWorkspace graphWorkspace) throws SQLException {
        info("Loading network nodes of "+ graphWorkspace.getWorkspaceDbObject().toString());

        String fields = "";
        ArrayList<HanaColumnInfo> fieldList = graphWorkspace.getNodeFieldList();

        for(HanaColumnInfo col : fieldList){
            fields += quoteIdentifier(col.name) + ",";
        }
        fields = fields.substring(0, fields.length()-1);

        HanaQueryResult nodeTable = this.executeQueryList(String.format(
                this.sqlStrings.getProperty("GENERIC_SELECT_PROJECTION"),
                fields,
                graphWorkspace.getNodeKeyColInfo().schema,
                graphWorkspace.getNodeKeyColInfo().table
        ), null);

        graphWorkspace.clearNodeTable();
        // reflect types, that have actually been retrieved:
        for(HanaColumnInfo colInfo: nodeTable.getColumnMetadata()){
            graphWorkspace.getNodeFieldInfo(colInfo.name).dataType = colInfo.dataType;
        }

        for(Object[] row : nodeTable.getRecordList()){
            HanaNodeTableRow newRow = new HanaNodeTableRow();
            newRow.setKeyFieldName(graphWorkspace.getNodeKeyColInfo().name);
            for(int i=0; i<row.length; i++){
                newRow.addFieldValue(fieldList.get(i).name, row[i]);
            }
            graphWorkspace.getNodeTable().add(newRow);
        }
    }

    /**
     * Loads the content of the edge table for a HanaGraphWorkspace object with complete metadata
     *
     * @param graphWorkspace    HANA Graph Workspace with complete metadata
     */
    private void loadNetworkEdges(HanaGraphWorkspace graphWorkspace) throws SQLException {
        info("Loading network edges of "+ graphWorkspace.getWorkspaceDbObject().toString());
        String fields = "";
        ArrayList<HanaColumnInfo> fieldList = graphWorkspace.getEdgeFieldList();

        for(HanaColumnInfo col : fieldList){
            fields += quoteIdentifier(col.name) + ",";
        }
        fields = fields.substring(0, fields.length()-1);

        HanaQueryResult edgeTable = this.executeQueryList(String.format(
                this.sqlStrings.getProperty("GENERIC_SELECT_PROJECTION"),
                fields,
                graphWorkspace.getEdgeKeyColInfo().schema,
                graphWorkspace.getEdgeKeyColInfo().table
        ), null);

        graphWorkspace.clearEdgeTable();
        // reflect types, that have actually been retrieved:
        for(HanaColumnInfo colInfo: edgeTable.getColumnMetadata()){
            graphWorkspace.getEdgeFieldInfo(colInfo.name).dataType = colInfo.dataType;
        }

        for(Object[] row : edgeTable.getRecordList()){
            HanaEdgeTableRow newRow = new HanaEdgeTableRow();
            newRow.setKeyFieldName(graphWorkspace.getEdgeKeyColInfo().name);
            newRow.setSourceFieldName(graphWorkspace.getEdgeSourceColInfo().name);
            newRow.setTargetFieldName(graphWorkspace.getEdgeTargetColInfo().name);
            for(int i=0; i<row.length; i++){
                newRow.addFieldValue(fieldList.get(i).name, row[i]);
            }
            graphWorkspace.getEdgeTable().add(newRow);
        }
    }

    /**
     * Loads the complete graph workspace (i.e. metadata, nodes, edges)
     * into a new instance of HanaGraphWorkspace
     *
     * @param schema                Schema of the workspace to be loaded
     * @param graphWorkspaceName    Name of the workspace to be loaded
     * @return                      HanaGraphWorkspace Object
     */
    public HanaGraphWorkspace loadGraphWorkspace(String schema, String graphWorkspaceName) throws SQLException, HanaConnectionManagerException {

        HanaGraphWorkspace graphWorkspace =
                new HanaGraphWorkspace(new HanaDbObject(schema, graphWorkspaceName));

        loadWorkspaceMetadata(graphWorkspace);
        loadNetworkNodes(graphWorkspace);
        loadNetworkEdges(graphWorkspace);

        return graphWorkspace;
    }

    /**
     * Loads the complete graph workspace (i.e. metadata, nodes, edges)
     * into a new instance of HanaGraphWorkspace
     *
     * @param graphWorkspace    Schema and Name of the workspace to be loaded
     * @return                  HanaGraphWorkspace Object
     */
    public HanaGraphWorkspace loadGraphWorkspace(HanaDbObject graphWorkspace) throws SQLException, HanaConnectionManagerException {
        return loadGraphWorkspace(graphWorkspace.schema, graphWorkspace.name);
    }

    /**
     *  Determines if a schema is existing on the instance
     *
     * @param schema    The schema name to check
     * @return          True, if schema is existing. False, if not.
     * @throws SQLException sql error
     */
    public boolean schemaExists(String schema) throws SQLException {
        int schemaExists = executeQuerySingleValue(
                sqlStrings.getProperty("SCHEMA_EXISTS"),
                new HanaSqlParameter[]{
                        new HanaSqlParameter(schema, Types.VARCHAR)
                },
                Integer.class
        );

        return schemaExists > 0;
    }

    /**
     * Creates a schema with the given name on the instance. Will throw
     * Exception if schema already exists.
     *
     * @param schema    Name of the schema to create
     * @throws SQLException sql error
     */
    public void createSchema(String schema) throws SQLException {
        execute(String.format(sqlStrings.getProperty("CREATE_SCHEMA"), schema));
    }

    /**
     * Creates a new table on the database instance
     *
     * @param newTableLocation  Schema and name of the table to create
     * @param newCols           Column definition of the table to create
     * @throws SQLException     sql error
     */
    public void createTable(HanaDbObject newTableLocation, List<HanaColumnInfo> newCols) throws SQLException {
        String fieldList = "";
        for(HanaColumnInfo col : newCols){
            fieldList += quoteIdentifier(col.name) + " " + col.dataType.getHanaDdl();

            if(col.primaryKey){
                fieldList += " PRIMARY KEY";
            }else if (col.notNull){
                fieldList += " NOT NULL";
            }
            fieldList+=",";
        }
        fieldList = fieldList.substring(0, fieldList.length() - 1);

        String createStmt = String.format(
                this.sqlStrings.getProperty("CREATE_TABLE"),
                newTableLocation.schema,
                newTableLocation.name,
                fieldList
        );

        this.execute(createStmt);
    }

    /**
     * Bulk inserts data into a HANA table
     *
     * @param targetTable       Schema and name of the target table
     * @param columnInfoList    List of relevant columns
     * @param data              Data for the relevant columns
     * @throws SQLException     sql error
     */
    public void bulkInsertData(HanaDbObject targetTable, List<HanaColumnInfo> columnInfoList, List<Map<String, Object>> data) throws SQLException {

        String fields = "";
        String values = "";
        for(HanaColumnInfo colInfo : columnInfoList){
            fields += quoteIdentifier(colInfo.name) + ",";
            values += "?,";
        }
        fields = fields.substring(0, fields.length()-1);
        values = values.substring(0, values.length()-1);

        String insertStatement = String.format(
                this.sqlStrings.getProperty("INSERT_INTO_TABLE"),
                targetTable.schema,
                targetTable.name,
                fields,
                values
        );

        ArrayList<HanaSqlParameter[]> batchParams = new ArrayList<>();

        for(Map<String, Object> record : data){
            HanaSqlParameter[] recordParams = new HanaSqlParameter[columnInfoList.size()];
            int i=0;
            for(HanaColumnInfo colInfo : columnInfoList){
                Object objValue = record.get(colInfo.name);
                recordParams[i++] = new HanaSqlParameter(objValue == null ? null : objValue.toString(), Types.VARCHAR);
            }
            batchParams.add(recordParams);
        }

        executeBatch(insertStatement, batchParams);
    }

    /**
     * Creates a new graph workspace on the instance. Will throw an exception
     * if the workspace already exists. Not that only the workspace objects will
     * be created. It is assumed that linked tables and its contents are already
     * existing.
     *
     * @param graphWorkspace    The graph workspace to create
     * @throws SQLException     sql error
     */
    public void createGraphWorkspace(HanaGraphWorkspace graphWorkspace) throws SQLException {

        this.execute(String.format(
                // basic statement
                this.sqlStrings.getProperty("CREATE_GRAPH_WORKSPACE"),
                // workspace schema
                graphWorkspace.getWorkspaceDbObject().schema,
                // workspace name
                graphWorkspace.getWorkspaceDbObject().name,
                // edge table schema
                graphWorkspace.getEdgeTableDbObject().schema,
                // edge table name
                graphWorkspace.getEdgeTableDbObject().name,
                // edge source col
                graphWorkspace.getEdgeSourceColInfo().name,
                // edge target col
                graphWorkspace.getEdgeTargetColInfo().name,
                // edge key col
                graphWorkspace.getEdgeKeyColInfo().name,
                // node table schema
                graphWorkspace.getNodeTableDbObject().schema,
                // node table name
                graphWorkspace.getNodeTableDbObject().name,
                // node table key col
                graphWorkspace.getNodeKeyColInfo().name
                ));
    }

}
