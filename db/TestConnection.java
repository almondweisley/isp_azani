package db;

import java.sql.Connection;
import java.sql.SQLException;

public class TestConnection {
    public static void main(String[] args){
        try(Connection con = DBConnection.getConnection()) {
            System.out.println("Connected to catalog: " + con.getCatalog());
            System.out.println("Server version: " + con.getMetaData().getDatabaseProductVersion());
        } catch (SQLException e){
            System.out.println("Connection failed: " + e.getMessage());
        }
    }
}
