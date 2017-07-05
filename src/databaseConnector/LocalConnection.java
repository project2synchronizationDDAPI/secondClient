/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package databaseConnector;

import java.sql.Connection;
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



public class LocalConnection{
    DbConnection conn;
    public LocalConnection(DbConnection dbConn){
    conn=dbConn;
}
    
    //insert row to table 
    public void doInsert(String tableName,String values) {
            Date date = new Date();
            SimpleDateFormat ft = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"); 
            conn.insert(tableName,values, enuRecordState.INSERTED.ordinal()+"", ft.format(date));
            System.out.println("inserted");
        }
    // insert more than one row
    public void insertRecordValues(String tableName,String values){
        conn.insert(tableName, values);
     }
    //get all inserted row has syncState = Inserted
    public ResultSet getInsertedElementFromTable(String tableName){
        try {
            return getElementFromTableDebendOnState(tableName, enuRecordState.INSERTED);
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }
    //get all updated row has syncState = updated
    public ResultSet getUpdatedElementFromTable(String tableName){
        try {
            return getElementFromTableDebendOnState(tableName, enuRecordState.UPDATED);
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }
    //get all Deleted row has syncState = Deleted
    public ResultSet getDeletedElementFromTable(String tableName){
        return conn.select("server_id", tableName, conn.creteria.equals("syncState", enuRecordState.DELETED.ordinal()+""));
    }
    public int getServerIdByIdValue(String tableName,int id){
        try {
             ResultSet rs =conn.select("server_id", tableName, conn.creteria.equals("id", id+""));
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
        ResultSet rs =conn.select("id", tableName, conn.creteria.equals("server_id", serverId+""));
        try {
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
        return conn.select("*", tableName, conn.creteria.equals("syncState", state.ordinal()+""));
    }
    //change syncState for all rows from oldState to newState
    private void changeSyncState(String tableName,enuRecordState newState,enuRecordState oldState){ 
        conn.Update(tableName, "syncState", newState.ordinal()+"", conn.creteria.equals("syncState", oldState.ordinal()+""));
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
        conn.delete(tableName, conn.creteria.equals("syncState",state.ordinal()+""));
    }
    // i don't use this method خلو بيلزم لأنو غريب هالكود 
    /*public void insertFromResultSet(String tableName,ResultSet resultSet){
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
    */
    // we use this method to block the identity constraint and then insert all the rows that we get from the server
    public void insertForSync(String tableName,String metaData , String values){
        if (values !="") {
            System.out.println("SET IDENTITY_INSERT names ON \n" +
                        "INSERT INTO "+tableName+" "+metaData+" VALUES "+values+" \n" +
                        "SET IDENTITY_INSERT names OFF");
            
            conn.setIDENTITY_INSERTNames("ON");
            conn.insert(tableName, metaData, values);
            conn.setIDENTITY_INSERTNames("OFF");
        }
    }
    //this method return the auto increment id to any value i want
    public void resetIdToValue(String tableName,int id){
        conn.autoIncrementIDENT(tableName, id);
    }
    
    // this method for delete
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
            ResultSet rs;
            rs=conn.selectTop(1, "syncState", tableName, conn.creteria.equals("id", id+""));
            //may rs=0
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
       conn.Update(tableName, "syncState", newState.ordinal()+"", conn.creteria.equals("id",id+""));
    }
    //delete record depend on id
    private void deleteRecord(String tableName,int id){
       conn.delete(tableName, conn.creteria.equals("id",id+""));
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
        conn.Update(tableName, "name", name, conn.creteria.equals("id",id+""));
    }
    //delete all record has ids using in operator
    public void deleteRecordsFromValues(String tableName,String idsValues){
       conn.delete(tableName, conn.creteria.IN("server_id",idsValues));
       System.out.println("DELETE FROM "+tableName+" WHERE server_id IN ("+idsValues+")");
    }
    //get max Id from table 
    //we using it to get last inserted id here
    public int getLastIdFromTable(String tableName){
        int id=0;
        try {
            ResultSet rs = conn.selectMax("id", tableName);
            while (rs.next()) {                
                id=rs.getInt(1);
            }
        } catch (SQLException e) {            
            e.printStackTrace();
        }
        return id;
    } 
    public void updateServerId(String tableName,int id,int serverId){
        conn.Update(tableName, "syncState", enuRecordState.SYNCHRONIZED.ordinal()+"", conn.creteria.equals("id",id+""));
        conn.Update(tableName, "server_id", serverId+"", conn.creteria.equals("id",id+""));
    }
    //*********************************  OLD FUNCTIONS *************************************
    public String getTableNameByTableId(int tableId) throws SQLException{
        String c1=conn.creteria.equals("xtype", "'U'");
        String c2=conn.creteria.equals("id", tableId+"");        
        ResultSet rs =conn.selectTop(1,"name", "SYSOBJECTS", conn.creteria.AND(c1, c2));
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
}