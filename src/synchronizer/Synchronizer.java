/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package synchronizer;

import databaseConnector.DbConnection;
import databaseConnector.SqlLocalConnection;
import databaseConnector.enuRecordState;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;
import org.json.JSONArray;
import org.json.JSONObject;
import sender.InsertedElementsJSONSender;



/**
 *
 * @author Bcc
 */
public class Synchronizer {
    //the sync method will connect to the server and send the data for open connection
    //then if the connection succeed then will get all the data from the client and send it
    //then we will get all non synchronized data and procces it on my database
    public void Sync(String databaseName, String username,String password, DbConnection db){
        // check connection for the user
        JSONObject userInfo= userInfoToJSON(databaseName, username, password);
        InsertedElementsJSONSender sender=new InsertedElementsJSONSender();
        //get the result of the connection
        String connectionResult= sender.parseUserInfoJSON(userInfo.toString());
        JSONObject resultJSON=new JSONObject(connectionResult);
        boolean result=resultJSON.getBoolean("connectionState");
        
        //connection succeed
        if (result) {
            System.out.println("connect");
            //get last sycn date for the user
            String lastSyncDateString = resultJSON.getString("lastSyncDate");
            //get non synchronized element
            ResultSet resultSet=db.getInsertedElementFromTable("names");
            ResultSet resultSetDeletedElements=db.getDeletedElementFromTable("names");
            
            JSONObject metaDataJSON =resultSetMetaDataJSON(resultSet);
            //convert inserted element result set to json
            JSONArray records= resultSetRecordToJSON(resultSet);
            System.out.println(records);
            //convert deleted element result set to json
            JSONArray deletedElementsIds= resultSetDeletedRecordToJSON(resultSetDeletedElements);
            System.out.println(deletedElementsIds);
            //this json object contain all the data to send
            JSONObject allData=new JSONObject();
            allData.put("lastSyncDate", lastSyncDateString);
            allData.put("metaData", metaDataJSON.toString());
            allData.put("insertedRecords", records.toString());
            allData.put("deletedRecords", deletedElementsIds.toString());
            //****************************************************************
            //send and recive the result from server
            String tablesString= sender.parseInsertedElementsJSON(allData.toString());
            
            JSONObject tablesJSON=new JSONObject(tablesString);
            JSONObject tableJSON=new JSONObject(tablesJSON.getString("names"));
            JSONArray oldIdsArray=new JSONArray(tableJSON.getString("oldIds"));
            JSONArray newIdsArray=new JSONArray(tableJSON.getString("newIds"));
            System.out.println("old ids" +oldIdsArray);
            System.out.println("new ids" +newIdsArray);
            SqlLocalConnection localConnection=new SqlLocalConnection();
            
            for (int i = 0; i < oldIdsArray.length(); i++) {
                localConnection.updateServerId("names",oldIdsArray.getInt(i) , newIdsArray.getInt(i));  
            }
            JSONArray tableCoulmnsNamesJSON=new JSONArray(tableJSON.getString("metaData"));
            JSONArray insertedJSON=new JSONArray(tableJSON.getString("inserted"));
            JSONArray deletedJSON=new JSONArray(tableJSON.getString("deleted"));
            JSONArray updatedJSON=new JSONArray(tableJSON.getString("updated"));
            System.out.println(tableCoulmnsNamesJSON);
            System.out.println("inserted "+insertedJSON);
            System.out.println("updated "+updatedJSON);
            System.out.println("deleted "+deletedJSON);
            
            if (insertedJSON.length()>0) {
                String insertedRecordFromServer= insertedRecordsJSONParser(insertedJSON.toString());
                localConnection.insertRecordValues("names", insertedRecordFromServer);
            }
            // [2,5] =>(2,5)
            localConnection.deleteRecordsFromValues("names", deletedRecordsIdsJSONParser(deletedJSON.toString()));
            


            //localConnection.deleteAllRecordsHasInsertedStatues("names");
            
            //localConnection.insertForSync("names", "(server_id,name,syncState,transactionDate)",insertedRecordsJSONParser(insertedJSON.toString()));
            /*
            int lastInsertedIdInTable=localConnection.getLastIdFromTable("names");
            System.out.println("lastInsertedIdInTable="+lastInsertedIdInTable);
            localConnection.resetIdToValue("names", lastInsertedIdInTable);
            */
        }else{
            //we must define exception and throw it here
            System.out.println("not ALLOWD");
            
        }
    }
    //this method will make json contain the metadata for table
    private JSONObject resultSetMetaDataJSON(ResultSet resultSet){
        JSONObject metaDataJSON=new JSONObject();
        try {
            ResultSetMetaData metaData=resultSet.getMetaData();
            int columnCount=metaData.getColumnCount();
            for (int i = 1; i <= columnCount; i++) {
                metaDataJSON.put(Integer.toString(i), metaData.getColumnName(i));   
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return metaDataJSON;
    }
    //this method will make json contain the inserted rows values
    private JSONArray resultSetRecordToJSON(ResultSet resultSet){
        JSONArray records=new JSONArray();
        try {
            while(resultSet.next()) {
                JSONArray record=new JSONArray();
                record.put(resultSet.getInt(1));
                record.put(resultSet.getString(2));
                record.put(resultSet.getInt(3));
                record.put(resultSet.getTimestamp(4));
                System.out.println(record);
                records.put(record);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return records;
    } 
    //this method will make json contain the ids for deleted rows
    private JSONArray resultSetDeletedRecordToJSON(ResultSet resultSet){
        JSONArray reslutIds=new JSONArray();
        try {                
            while (resultSet.next()) {
                reslutIds.put(resultSet.getInt(1));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return reslutIds;
    }
    //this method will make json contain the data for open connection method
    private JSONObject userInfoToJSON(String dbName,String username,String password){
        JSONObject userInfoJSON=new JSONObject();
        userInfoJSON.put("databaseName", dbName);
        userInfoJSON.put("username", username);
        String hashPassword=password;
        //System.out.println("password enc ::"+Encryption.encryptText(password));
        //System.out.println("password dec ::"+Encryption.decryptText(hashPassword));
        
        userInfoJSON.put("password", hashPassword);
        return userInfoJSON;
    }
    //the will parse the inserted records from json and return the string value contain the new records from client
    //like (3,ali,1,2017-5-9 15:15:15),(3,ali,1,2017-5-9 15:15:15)
    private String insertedRecordsJSONParser(String insertedRecordsJSONString){
        JSONArray records=new JSONArray(insertedRecordsJSONString);
        String recordsQueryValues="";
        int length=records.length();
        //get the id of the first inserted id 
        for (int i = 0; i < length; i++) {
            JSONArray record=records.getJSONArray(i);
            if (i!=length-1) {
                recordsQueryValues+=convertRecordString(record.toString())+",";
            }else{
                recordsQueryValues+=convertRecordString(record.toString());
            }
        }
        System.out.println(recordsQueryValues);
        return recordsQueryValues;
    }
    //this will parse record value from json to  record value
    private String convertRecordString(String record){
        int lastComma=record.lastIndexOf(',')
            ,firstComma=record.indexOf(',');
        Date date = new Date();
        SimpleDateFormat ft = new SimpleDateFormat("yyyy-MM-dd kk:mm:ss");
        String currentTime = ft.format(date);
        String server_id =record.substring(1,firstComma);
        System.out.println("record" + record);
        System.out.println("server id "+ server_id);
        System.out.println(record.substring(firstComma+1,lastComma+1));
        String recordValue=record.substring(firstComma+1,lastComma+1);
        String recordValueafterReplaceStringQuote=recordValue.replace("\"", "'");
        System.out.println("recordValueafterReplaceStringQuote"+recordValueafterReplaceStringQuote);
        String recordValueWithoutSyncState=recordValueafterReplaceStringQuote.substring(0,recordValueafterReplaceStringQuote.lastIndexOf(',')-2);
        System.out.println("recordValueWithoutSyncState"+ recordValueWithoutSyncState);
        //System.out.println("("+record.substring(firstComma+1,lastComma+1).replace("\"", "'").substring(0,record.lastIndexOf(',')-2)+enuRecordState.SYNCHRONIZED.ordinal()+ ",CAST('"+currentTime+"' AS DATETIME))");
        String result="("+recordValueWithoutSyncState+","+enuRecordState.SYNCHRONIZED.ordinal()+ ",CAST('"+currentTime+"' AS DATETIME),"+server_id+")";        
        System.out.println("result"+result);
        return result;
    }
    //this will convert deletedIds json to value that i can use for sql local server query
    // [2,5,6,8]  will be => (2,5,6,8) 
    private String deletedRecordsIdsJSONParser(String deletedIds){
        int temp= deletedIds.length()-1;
        return deletedIds.substring(1,temp);
    }
}
