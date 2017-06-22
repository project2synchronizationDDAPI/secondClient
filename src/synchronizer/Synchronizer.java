/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package synchronizer;

import configrationfileitems.Field;
import configrationfileitems.ForgeinKeyInformation;
import configrationfileitems.Table;
import databaseConnector.DbConnection;
import databaseConnector.SqlLocalConnection;
import databaseConnector.enuRecordState;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import json.JSONParser;
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
    public void Sync(String databaseName, String username,String password, SqlLocalConnection localConnection){
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
            
            //get db Structure
            JSONArray databaseStructure=resultJSON.getJSONArray("tables");
            
            JSONParser parser=new JSONParser();
            System.out.println(databaseStructure);
            ArrayList<Table> tables=parser.parseTablesJSON(databaseStructure);
            //all data to be send
            JSONArray allData=new JSONArray();
            //this loop will make json contain all information for the tables
            for (Table table : tables) {
                String tableName=table.getTableName();
                //get non synchronized element
                ResultSet resultSet=localConnection.getInsertedElementFromTable(tableName);
                ResultSet resultSetDeletedElements=localConnection.getDeletedElementFromTable(tableName);
                JSONObject metaDataJSON =resultSetMetaDataJSON(resultSet);
                //convert inserted element result set to json
                JSONArray records= InsertedRecordToJSON(resultSet,table.getFields(),table.getForgeinKeys(),localConnection);
                System.out.println("inserted"+records);
                //convert deleted element result set to json
                JSONArray deletedElementsIds= resultSetDeletedRecordToJSON(resultSetDeletedElements);
                System.out.println(deletedElementsIds);
                //this json object contain all the data for table 
                JSONObject tableData=new JSONObject();
                tableData.put("tableName", tableName);
                tableData.put("lastSyncDate", lastSyncDateString);
                tableData.put("metaData", metaDataJSON.toString());
                tableData.put("insertedRecords", records.toString());
                tableData.put("deletedRecords", deletedElementsIds.toString());
                System.out.println(tableData);
                allData.put(tableData);
                String tablesString= sender.parseInsertedElementsJSON(tableData.toString());
                //****************************************************************
                JSONArray tablesJSON=new JSONArray(tablesString);
                for (int i = 0; i < tablesJSON.length(); i++) {
                JSONObject tableJSON=new JSONObject(tablesJSON.get(i).toString());
                JSONArray array=new JSONArray();
                tableJSON.toJSONArray(array);
                System.out.println("array"+array);
                System.out.println("names"+tableJSON.names());
                
                JSONArray oldIdsArray=new JSONArray(tableJSON.getString("oldIds"));
                JSONArray newIdsArray=new JSONArray(tableJSON.getString("newIds"));
                System.out.println("old ids" +oldIdsArray);
                System.out.println("new ids" +newIdsArray);
                for (int j = 0; j < oldIdsArray.length(); j++) {
                    localConnection.updateServerId(tableName,oldIdsArray.getInt(j) , newIdsArray.getInt(j));  
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
                    
                    String insertedRecordFromServer= insertedRecordsJSONParser(insertedJSON.toString(),table,localConnection);
                    System.out.println(insertedRecordFromServer);
                    localConnection.insertRecordValues(tableName, insertedRecordFromServer);
                }
                if (deletedJSON.length()>0) {
                    // [2,5] =>(2,5)
                    localConnection.deleteRecordsFromValues(tableName, deletedRecordsIdsJSONParser(deletedJSON.toString()));
                }
            } 
            
            }
            /*
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
    private Table findTableByName(String TableName,ArrayList<Table>tables){
        for (int i = 0; i < tables.size(); i++) {
            Table table=tables.get(i);
            if (table.getTableName().equalsIgnoreCase(TableName)) {
                return table;
            }
        }
        return null;
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
    
    private JSONArray InsertedRecordToJSON(ResultSet resultSet,ArrayList<Field> fields,ArrayList<ForgeinKeyInformation> forgeinKeyInformations,SqlLocalConnection localConnection){
        JSONArray result=new JSONArray();
        try {
            while(resultSet.next()) {
                JSONArray record=new JSONArray();
                for (Field field : fields) {
                    record.put(resultSet.getObject(field.getFieldName()));
                }
                if (forgeinKeyInformations.size()>0) {
                    for (ForgeinKeyInformation forgeinKeyInformation : forgeinKeyInformations) {
                        int forgeinValue=resultSet.getInt(forgeinKeyInformation.getForgeinKeyColumnName());
                        int serverId=localConnection.getServerIdByIdValue(forgeinKeyInformation.getForgeinKeyTableName(), forgeinValue);
                        if (serverId!=-1) {
                            record.put(serverId);
                        }
                    }
                }
                record.put(resultSet.getInt("syncState"));
                record.put(resultSet.getTimestamp("transactionDate"));
                System.out.println(record);
                result.put(record);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }
    //this method will make json contain the inserted rows values
    private JSONArray resultSetRecordToJSON(ResultSet resultSet){
        JSONArray records=new JSONArray();
        /*
            لازم جيب عدد الفيلدز تبع التيبل و حطهن بالجيسون
        بعدين اذا في فورين كي روح و شوف قيمتو بالتيبل اللي مرتبط في و جيب السيرفر اي دي و حط قيمتو محل القيمة يلي بدي حطها
        */
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
            JSONObject record=records.getJSONObject(i);
            if (i!=length-1) {
                recordsQueryValues+=convertRecordString(record.toString())+",";
            }else{
                recordsQueryValues+=convertRecordString(record.toString());
            }
        }
        System.out.println(recordsQueryValues);
        return recordsQueryValues;
    }
    private String insertedRecordsJSONParser(String insertedRecordsJSONString,Table table,SqlLocalConnection localConnection){
        JSONArray records=new JSONArray(insertedRecordsJSONString);
        String recordsQueryValues="";
        int length=records.length();
        //get the id of the first inserted id 
        for (int i = 0; i < length; i++) {
            JSONObject record=records.getJSONObject(i);
            if (i!=length-1) {
                recordsQueryValues+=convertRecordString(record,table,localConnection)+",";
            }else{
                recordsQueryValues+=convertRecordString(record,table,localConnection);
            }
        }
        System.out.println(recordsQueryValues);
        return recordsQueryValues;
    }
    private String convertRecordString(JSONObject recordJSON,Table table,SqlLocalConnection localConnection){
        String result="";
        String serverId="";
        ArrayList<Field> fields=table.getFields();
        for (Field field : fields) {
            if (field.getFieldName().equalsIgnoreCase("id")) {
                serverId = recordJSON.get(field.getFieldName()).toString();
            }else if (field.getFieldType().equalsIgnoreCase("varchar(50)")) {
                result+=",'"+recordJSON.get(field.getFieldName())+"'";
            }else{
                result+=","+recordJSON.get(field.getFieldName());
            }
        }
        ArrayList<ForgeinKeyInformation> forgeinKeyInformations=table.getForgeinKeys();
        for (ForgeinKeyInformation forgeinKeyInformation : forgeinKeyInformations) {
            String tableName=forgeinKeyInformation.getForgeinKeyTableName();
            String columnName = forgeinKeyInformation.getForgeinKeyColumnName();
            int forgienKeyValue=recordJSON.getInt(columnName);
            int id= localConnection.getIdByServerIdValue(tableName, forgienKeyValue);
            result+=","+id;
        }
        Date date = new Date();
        SimpleDateFormat ft = new SimpleDateFormat("yyyy-MM-dd kk:mm:ss");
        String currentTime = ft.format(date);
        System.out.println("resutlt" + result);
        if (result.isEmpty()) {
            result="("+enuRecordState.SYNCHRONIZED.ordinal()+ ",CAST('"+currentTime+"' AS DATETIME),"+serverId+")";
        }else{
            result="("+result.substring(result.indexOf(',')+1)+ ","+enuRecordState.SYNCHRONIZED.ordinal()+ ",CAST('"+currentTime+"' AS DATETIME),"+serverId+")";
        }
        return result;
    }
    //this will parse record value from json to  record value
    private String convertRecordString(String record){
        int lastComma=record.lastIndexOf(',')
            ,firstComma=record.indexOf(',');
        Date date = new Date();
        SimpleDateFormat ft = new SimpleDateFormat("yyyy-MM-dd kk:mm:ss");
        String currentTime = ft.format(date);
        String serverId =record.substring(1,firstComma);
        System.out.println("record" + record);
        System.out.println("server id "+ serverId);
        System.out.println(record.substring(firstComma+1,lastComma+1));
        String recordValue=record.substring(firstComma+1,lastComma+1);
        String recordValueafterReplaceStringQuote=recordValue.replace("\"", "'");
        System.out.println("recordValueafterReplaceStringQuote"+recordValueafterReplaceStringQuote);
        String recordValueWithoutSyncState=recordValueafterReplaceStringQuote.substring(0,recordValueafterReplaceStringQuote.lastIndexOf(',')-2);
        System.out.println("recordValueWithoutSyncState"+ recordValueWithoutSyncState);
        //System.out.println("("+record.substring(firstComma+1,lastComma+1).replace("\"", "'").substring(0,record.lastIndexOf(',')-2)+enuRecordState.SYNCHRONIZED.ordinal()+ ",CAST('"+currentTime+"' AS DATETIME))");
        String result="("+recordValueWithoutSyncState+","+enuRecordState.SYNCHRONIZED.ordinal()+ ",CAST('"+currentTime+"' AS DATETIME),"+serverId+")";        
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
