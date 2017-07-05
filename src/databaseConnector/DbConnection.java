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
 
public abstract class DbConnection {
    
    public abstract void doSelect(String tableName);
    public abstract void doInsert(String tableName,String name);
    public abstract void doDelete(String tableName,int id);
    public abstract void doUpdate(String tableName,String name,int id);
    public abstract ResultSet getInsertedElementFromTable(String tableName);
    public abstract ResultSet getDeletedElementFromTable(String tableName);
    public abstract ResultSet getUpdatedElementFromTable(String tableName);
}
* */
public abstract class DbConnection {
    public Creteria creteria;
    
    public abstract void insert(String tableName, String values ,String state, String date);
    public abstract void insert(String tableName, String values );
    public abstract void insert(String tableName, String metaData, String values);
    public abstract ResultSet select(String cols,String tableName, String creteria);
    public abstract ResultSet selectTop(int top,String cols,String tableName, String creteria);
    public abstract ResultSet selectMax(String col, String tableName);
    public abstract void Update(String tableName, String colName, String newValue, String creteria);
    public abstract void delete(String tableName, String creteria);
    public abstract void setIDENTITY_INSERTNames(String on_off);
    public abstract void autoIncrementIDENT(String tableName,int id);
}

interface Creteria{
    public abstract String equals(String left, String right);
    public abstract String IN(String left, String right);
    public abstract String AND(String left, String right);
}