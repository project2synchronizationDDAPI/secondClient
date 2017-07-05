/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package databasesyncproject2;

import databaseConnector.DbConnection;
import databaseConnector.LocalConnection;
import databaseConnector.SQLConnection;
import synchronizer.Synchronizer;
import util.Config;

/**
 *
 * @author Bcc
 */

public class DatabaseSyncProject2 {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        // TODO code application logic here
        LocalConnection local=new LocalConnection(new SQLConnection());
        String tableName="car";
       /* local.doInsert(tableName, "ali");
        local.doInsert(tableName, "saeed");
        local.doInsert(tableName, "manar");
       */
        //local.getInsertedElementFromTable(tableName);
        /*local.doDelete(tableName, 5);
        local.doDelete(tableName, 25);
        local.doDelete(tableName, 15);
        */
        //local.doDelete(tableName, 18);
        //local.doInsert(tableName, "kia");
        //local.doInsert(tableName, "bmw");
        /*
        local.doInsert("door", "2");
        local.doInsert("door", "1");
        */
        //local.doDelete("door", 2);
        Synchronizer synchronizer=new Synchronizer();
        synchronizer.Sync(Config.DATABASE_NAME_SECOND_CLIENT,Config.USER_NAME,Config.PASSWORD,local);
    }
    
}
