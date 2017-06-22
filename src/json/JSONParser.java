/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package json;

import configrationfileitems.Field;
import configrationfileitems.ForgeinKeyInformation;
import configrationfileitems.Table;
import java.util.ArrayList;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 *
 * @author Bcc
 */
public class JSONParser {
    public  ArrayList<Table> parseTablesJSON(JSONArray tableArray){
        ArrayList<Table> tables=new ArrayList<>();
        for (int i = 0; i < tableArray.length(); i++) {
            JSONArray table=tableArray.getJSONArray(i);
            
            JSONObject tableNameContainer=table.getJSONObject(0);
            
            
            tables.add(new Table(
                    tableNameContainer.getString("name"),
                    fieldJSONParser(table.getJSONArray(1)),
                    forgeinKeyJSONParser(table.getJSONArray(2))
                        )
                    );
        }
        return tables;
    }
    //the will parse the field from json and return the field for table
    private ArrayList<Field> fieldJSONParser(JSONArray fieldArray){
        ArrayList<Field> fields=new ArrayList<>();
        for (int i = 0; i < fieldArray.length(); i+=2) {
            JSONObject fieldName=fieldArray.getJSONObject(i);
            JSONObject fieldType=fieldArray.getJSONObject(i+1);
            fields.add(new Field(fieldName.getString("fieldName"), fieldType.getString("fieldType")));
        }
        return fields;
    }
    //the will parse the frogein key from json and return the forgein key for table
    private ArrayList<ForgeinKeyInformation> forgeinKeyJSONParser(JSONArray forgeinKeyArray){
        ArrayList<ForgeinKeyInformation> forgeinKeys=new ArrayList<>();
        for (int i = 0; i < forgeinKeyArray.length(); i+=3) {
            JSONObject columnBasedTableName=forgeinKeyArray.getJSONObject(i);
            JSONObject forgeinKeyTableName=forgeinKeyArray.getJSONObject(i+1);
            JSONObject forgeinKeyColumnName=forgeinKeyArray.getJSONObject(i+2);
            forgeinKeys.add(new ForgeinKeyInformation(
                    forgeinKeyTableName.getString("forgeinKeyTableName"),
                    columnBasedTableName.getString("columnBasedTableName"),
                    forgeinKeyColumnName.getString("forgeinKeyColumnName")));
        }
        return forgeinKeys;
    }
}
