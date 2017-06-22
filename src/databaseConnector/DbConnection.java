/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package databaseConnector;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 *
 * @author Bcc
 */
public abstract class DbConnection {
    protected Connection con ;
    public abstract void doSelect(String tableName);
    public abstract void doInsert(String tableName,String name);
    public abstract void doDelete(String tableName,int id);
    public abstract void doUpdate(String tableName,String name,int id);
    public abstract ResultSet getInsertedElementFromTable(String tableName);
    public abstract ResultSet getDeletedElementFromTable(String tableName);
    public abstract ResultSet getUpdatedElementFromTable(String tableName);
}
