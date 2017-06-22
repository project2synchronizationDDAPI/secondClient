/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package databaseConnector;

import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import util.Config;

/**
 *
 * @author Bcc
 */
public class SqlLocalConnection extends DbConnection{
    public SqlLocalConnection() {
        try {
            Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
            String connectionUrl = "jdbc:sqlserver://localhost:1433;databaseName="+Config.DATABASE_NAME+";integratedSecurity=true;";
            con = DriverManager.getConnection(connectionUrl);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    //insert row to table 
    @Override
    public void doInsert(String tableName,String values) {
        try {            
            Date date = new Date();
            SimpleDateFormat ft = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"); 
            PreparedStatement stmt = con.prepareStatement("INSERT INTO "+tableName+" VALUES ('" + values + "',"+enuRecordState.INSERTED.ordinal()+",convert(datetime,'"+ft.format(date)+"'),NULL)" );
            stmt.executeUpdate();
            System.out.println("inserted");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    // insert more than one row
    public void insertRecordValues(String tableName,String values){
        try {
            System.out.println("INSERT INTO "+tableName+" VALUES "+values);
            PreparedStatement stmt = con.prepareStatement("INSERT INTO "+tableName+" VALUES "+values);
            stmt.executeUpdate();          
        } catch (SQLException e) {
            e.printStackTrace();
        }
     }
    //get all inserted row has syncState = Inserted
    @Override
    public ResultSet getInsertedElementFromTable(String tableName){
        try {
            return getElementFromTableDebendOnState(tableName, enuRecordState.INSERTED);
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }
    //get all updated row has syncState = updated
    @Override
    public ResultSet getUpdatedElementFromTable(String tableName){
        try {
            return getElementFromTableDebendOnState(tableName, enuRecordState.UPDATED);
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }
    //get all Deleted row has syncState = Deleted
    @Override
    public ResultSet getDeletedElementFromTable(String tableName){
        try {
            Statement s = con.createStatement();
            ResultSet rs = s.executeQuery("SELECT server_id FROM "+tableName+" WHERE syncState="+enuRecordState.DELETED.ordinal());
            return rs;
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }
    public int getServerIdByIdValue(String tableName,int id){
        try {
            Statement s = con.createStatement();
            ResultSet rs = s.executeQuery("SELECT server_id FROM "+tableName+" WHERE id = "+id);
            if (rs!=null) {
                rs.next();
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }
    public int getIdByServerIdValue(String tableName,int serverId){
        try {
            Statement s = con.createStatement();
            ResultSet rs = s.executeQuery("SELECT id FROM "+tableName+" WHERE server_id = "+serverId);
            if (rs!=null) {
                rs.next();
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }
    //get all row has syncState = some value 
    private ResultSet getElementFromTableDebendOnState(String tableName,enuRecordState state)throws SQLException{
        Statement s = con.createStatement();
        ResultSet rs = s.executeQuery("SELECT * FROM "+tableName+" WHERE syncState="+state.ordinal());
        return rs;
    }
    //change syncState for all rows from oldState to newState
    private void changeSyncState(String tableName,enuRecordState newState,enuRecordState oldState){ 
        try {
            PreparedStatement stmt = con.prepareStatement("UPDATE "+tableName+" SET syncState = "+newState.ordinal()+" WHERE syncState = "+oldState.ordinal());
            stmt.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    //change syncState for all rows from inserted to sync
    public void changeSyncStateFromInsertedToSync(String tableName){
        changeSyncState(tableName, enuRecordState.SYNCHRONIZED, enuRecordState.INSERTED);
    }
    //delete all rows has inserted syncState
    public void deleteAllRecordsHasInsertedStatues(String tableName){
        deleteAllRecordDependOnState(tableName, enuRecordState.INSERTED);
    }
    //delete all rows depend on syncState
    private void deleteAllRecordDependOnState(String tableName,enuRecordState state){
        try {
            PreparedStatement stmt = con.prepareStatement("DELETE FROM "+tableName+" WHERE syncState = "+state.ordinal());
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    // i don't use this method خلو بيلزم لأنو غريب هالكود 
    public void insertFromResultSet(String tableName,ResultSet resultSet){
        try {
            ResultSetMetaData meta = resultSet.getMetaData();

            List<String> columns = new ArrayList<>();
            for (int i = 1; i <= meta.getColumnCount(); i++)
                columns.add(meta.getColumnName(i));

            try (PreparedStatement s2 = con.prepareStatement(
                    "INSERT INTO " + tableName + " ("
                  + columns.stream().collect(Collectors.joining(", "))
                  + ") VALUES ("
                  + columns.stream().map(c -> "?").collect(Collectors.joining(", "))
                  + ")"
            )) {

                while (resultSet.next()) {
                    for (int i = 1; i <= meta.getColumnCount(); i++){
                        s2.setObject(i, resultSet.getObject(i));
                    
                    }
                    s2.addBatch();
                }

                s2.executeBatch();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    // we use this method to block the identity constraint and then insert all the rows that we get from the server
    public void insertForSync(String tableName,String metaData , String values){
        if (values !="") {
            try {
                System.out.println("SET IDENTITY_INSERT names ON \n" +
                        "INSERT INTO "+tableName+" "+metaData+" VALUES "+values+" \n" +
                        "SET IDENTITY_INSERT names OFF");
                PreparedStatement stmt = con.prepareStatement("SET IDENTITY_INSERT names ON \n" +
                        "INSERT INTO "+tableName+" "+metaData+" VALUES "+values+" \n" +
                        " SET IDENTITY_INSERT names OFF");
                stmt.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }
    //this method return the auto increment id to any value i want
    public void resetIdToValue(String tableName,int id){
        try {
            PreparedStatement stmt = con.prepareStatement("DBCC CHECKIDENT ('"+tableName+"', RESEED, "+id+")");
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
    // this method for delete
    @Override
    public void doDelete(String tableName,int id){
        
        /*
        1-get flag value for the row
        2-depend of the flag do what i have to do
        */
        int flag=getFlagValue(tableName, id);
        try {
            switch (flag){
                case 0:
                    //if syncState is syncronized then update the flag and make it deleted
                    updateFlagValue(tableName, id, enuRecordState.DELETED);
                    break;
                case 1:
                    //if syncState is inserted then delete the row
                    deleteRecord(tableName, id);
                    break;
                case 2:
                    //if syncState is deleted then throw an exception because the client already delete the record
                    throw new Exception("deleted row");
                case 3:
                    updateFlagValue(tableName, id, enuRecordState.DELETED);
                    break;
            } 
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    //get flag value for row
    private int getFlagValue(String tableName,int id){
        int result=-1;
        try {
            Statement s = con.createStatement();
            ResultSet rs = s.executeQuery("SELECT TOP 1 syncState FROM "+tableName+" WHERE id="+id);
            while (rs.next()) {                
                result=rs.getInt(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return result;
    }
    //update flag value for row
    private void updateFlagValue(String tableName,int id,enuRecordState newState){
       try {
            PreparedStatement stmt = con.prepareStatement("UPDATE "+tableName+" SET syncState = "+newState.ordinal()+" WHERE id = "+id);
            stmt.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    //delete record depend on id
    private void deleteRecord(String tableName,int id){
       try {
            PreparedStatement stmt = con.prepareStatement("DELETE FROM "+tableName+" WHERE id = "+id);
            stmt.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    //delete record depend on id
    public void doUpdate(String tableName , String name , int id){
        /*
        1-get flag value for the row
        2-depend of the flag do what i have to do
        */
        int flag=getFlagValue(tableName, id);
        try {
            switch (flag){
                case 0:
                    //if syncState is syncronized then update the flag and make it deleted
                    updateFlagValue(tableName, id, enuRecordState.UPDATED);
                    break;
                case 1:
                    //if syncState is inserted then update the row without change his syncState
                    updateRecord(tableName, name, id);
                    break;
                case 2:
                    //if syncState is deleted then throw an exception because the client already delete the record
                    throw new Exception("deleted row");
                case 3:
                    updateRecord(tableName, name, id);
                    break;
               
            } 
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    //update record name
    private void updateRecord(String tableName , String name , int id){
        try {
            PreparedStatement stmt = con.prepareStatement("UPDATE "+tableName+" SET name = '"+name+"' WHERE id = "+id);
            stmt.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    //delete all record has ids using in operator
    public void deleteRecordsFromValues(String tableName,String idsValues){
       try {
           System.out.println("DELETE FROM "+tableName+" WHERE server_id IN ("+idsValues+")");
            PreparedStatement stmt = con.prepareStatement("DELETE FROM "+tableName+" WHERE server_id IN ("+idsValues+")");
            stmt.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    //get max Id from table 
    //we using it to get last inserted id here
    public int getLastIdFromTable(String tableName){
        int id=0;
        try {
            Statement s = con.createStatement();
            ResultSet rs = s.executeQuery("SELECT MAX(id) FROM "+tableName);
            while (rs.next()) {                
                id=rs.getInt(1);
            }
        } catch (SQLException e) {
            
            e.printStackTrace();
        }
        return id;
    } 
    public void updateServerId(String tableName,int id,int serverId){
        
        try {
            PreparedStatement stmt = con.prepareStatement("UPDATE "+tableName+" SET syncState = "+enuRecordState.SYNCHRONIZED.ordinal()+",server_id="+serverId+" WHERE id = "+id);
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    //*********************************  OLD FUNCTIONS *************************************
    public String getTableNameByTableId(int tableId) throws SQLException{
        Statement s1 = con.createStatement();

        ResultSet rs = s1.executeQuery("SELECT TOP 1 name FROM SYSOBJECTS WHERE xtype='U' AND id="+tableId);

        String tableName="";
        while (rs.next()) {
            tableName = rs.getString(1);
        }
        return tableName;
    }
    /*public int getLastIdFromTable(String tableName){
        
        try {
            Statement s1 = con.createStatement();
            ResultSet rs = s1.executeQuery("SELECT IDENT_CURRENT('"+tableName+"')");
            while (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException ex) {
            Logger.getLogger(SqlLocalConnection.class.getName()).log(Level.SEVERE, null, ex);
        }
        return 0;
    }
    */
    
    @Override
    public void doSelect(String tableName) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}