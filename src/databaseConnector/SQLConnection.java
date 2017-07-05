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
import java.sql.SQLException;
import java.sql.Statement;
import util.Config;


class SQLCreteria implements Creteria{

    @Override
    public String equals(String left, String right) {
        return left+'='+right;
    }
    @Override
    public String IN(String left, String right){
        return left+" IN ("+right+")";
    }
    @Override
    public String AND(String left, String right){
        return left+" AND "+right;
    }
    
}
    /**
 *
 * @author Bcc
 */
public class SQLConnection extends DbConnection{
    Connection conn ;    
    public SQLConnection() {
        try {
            Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
            String connectionUrl = "jdbc:sqlserver://localhost:1433;databaseName="+Config.DATABASE_NAME+";integratedSecurity=true;";
            conn = DriverManager.getConnection(connectionUrl);
        } catch (Exception e) {
            e.printStackTrace();
        }
        creteria=new SQLCreteria();
    }
    @Override
     public void insert(String tableName, String values ,String state, String date){
         try {
            //System.out.println("INSERT INTO "+tableName+" VALUES "+values);
            PreparedStatement stmt = conn.prepareStatement("INSERT INTO "+tableName+" VALUES ('" + values + "',"+state+",convert(datetime,'"+date+"'),NULL)");
            stmt.executeUpdate();          
        } catch (SQLException e) {
            e.printStackTrace();
        }
     }
    @Override
     public void insert(String tableName, String values ){
         try {
            System.out.println("INSERT INTO "+tableName+" VALUES "+values);
            PreparedStatement stmt = conn.prepareStatement("INSERT INTO "+tableName+" VALUES "+values);
            stmt.executeUpdate();          
        } catch (SQLException e) {
            e.printStackTrace();
        }
     }
     
     @Override
     public void insert(String tableName, String metaData, String values){
         try {
             PreparedStatement stmt = conn.prepareStatement("INSERT INTO "+tableName+" "+metaData+" VALUES "+values);
             stmt.executeUpdate();
         }catch (SQLException e) {
                e.printStackTrace();
            }
     }
     
     @Override
     public ResultSet select(String cols,String tableName, String creteria)
     {
     try {
     Statement s = conn.createStatement();
         return s.executeQuery("SELECT "+cols+" FROM "+tableName+" WHERE "+creteria);
    } catch (SQLException e) {
            e.printStackTrace();
        }
     return null;
     }
     
     @Override
     public ResultSet selectTop(int top,String cols,String tableName, String creteria){
         ResultSet rs=null;
         try {
            Statement s = conn.createStatement();
            rs = s.executeQuery("SELECT TOP "+top+" "+cols+" FROM "+tableName+" WHERE "+creteria);
         }catch (SQLException e) {
            e.printStackTrace();
        }
         return rs;
     }
     
     @Override
     public ResultSet selectMax(String col, String tableName){
         ResultSet rs=null;
         try {
            Statement s = conn.createStatement();
            rs = s.executeQuery("SELECT MAX("+col+") FROM "+tableName);
            }
         catch (SQLException e) {
            e.printStackTrace();
        }
         return rs;
     }
     
     @Override
     public void Update(String tableName, String colName, String newValue, String creteria){
         try {
            PreparedStatement stmt = conn.prepareStatement("UPDATE "+tableName+" SET "+colName+" = "+newValue+" WHERE "+creteria);
            stmt.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
     }

     @Override
     public void delete(String tableName, String creteria){
          try {
            PreparedStatement stmt = conn.prepareStatement("DELETE FROM "+tableName+" WHERE "+creteria);
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
     }
     
     @Override
     public void setIDENTITY_INSERTNames(String on_off){
         try {
             PreparedStatement stmt = conn.prepareStatement("SET IDENTITY_INSERT names "+on_off);
             stmt.executeUpdate();
         }catch (SQLException e) {
                e.printStackTrace();
            }
     }
     
     @Override
     public void autoIncrementIDENT(String tableName,int id){
         try {
            PreparedStatement stmt = conn.prepareStatement("DBCC CHECKIDENT ('"+tableName+"', RESEED, "+id+")");
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
     }
//conn.Update(tableName, "syncState", newState.ordinal()+"", conn.creteria.equals("id",id+""));
//conn.delete(tableName, conn.creteria.equals("id",id+""));


}
