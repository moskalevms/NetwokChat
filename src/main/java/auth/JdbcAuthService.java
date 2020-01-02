package auth;

/**
 * Авторизация пользователей из БД
 */


import org.apache.commons.lang3.StringUtils;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;



public class JdbcAuthService implements AuthService {

    static {
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    private final Connection connection;
    private final PreparedStatement authStatement;
    private final PreparedStatement changeLoginStatement;

    public JdbcAuthService(String databaseUrl) {
        try {
            connection = DriverManager.getConnection("jdbc:sqlite:" + databaseUrl);
            authStatement = connection.prepareStatement("SELECT * FROM Users WHERE LOGIN = ? AND PASSWORD = ?");
            changeLoginStatement = connection.prepareStatement("UPDATE Users SET LOGIN = ? WHERE LOGIN = ?");
        } catch (SQLException e) {
            throw new AuthException("Failed to connect to database", e);
        }
    }


    @Override
    public void close() throws Exception {
        connection.close();
    }


    public static void main(String[] args) throws SQLException {
//        Map<String, String> testValues = Map.of(
//                "Oleg", "123",
//                "Petr", "234",
//                "Ivan", "345");

        Map<String, String> testValues = new HashMap<>();

        Connection connection = DriverManager.getConnection("jdbc:sqlite:ChatClient.db");
        connection.createStatement().execute("CREATE TABLE IF NOT EXISTS Users\n" +
                "(\n" +
                "  ID        INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL UNIQUE,\n" +
                "  LOGIN       TEXT                              NOT NULL,\n" +
                "  PASSWORD    TEXT                              NOT NULL\n" +
                ");");

        connection.createStatement().executeUpdate("DELETE FROM Users");
        final PreparedStatement ps = connection.prepareStatement("INSERT INTO Users (LOGIN, PASSWORD) VALUES(?, ?)");

        testValues.forEach((login, password) -> {
            try {
                ps.setString(1, login);
                ps.setString(2, password);
                ps.executeUpdate();
            } catch (SQLException e) {
                throw new RuntimeException("Failed to execute insert", e);
            }
        });

    }

    @Override
    public boolean authUser(String username, String password) {
        if (StringUtils.isAnyBlank(username, password)) {
            return false;
        }
        try {
            authStatement.setString(1, username);
            authStatement.setString(2, password);
            ResultSet resultSet = authStatement.executeQuery();
            return resultSet.next();
        } catch (SQLException e) {
            throw new AuthException(String.format("Failed to retrieve users with login %s and password %s.", username, password), e);
        }
    }

    @Override
    public User getAuthUser(String username, String password) {
        if (StringUtils.isAnyBlank(username, password)) {
            return null;
        }

        try {
            authStatement.setString(1, username);
            authStatement.setString(2, password);
            ResultSet resultSet = authStatement.executeQuery();
            while (resultSet.next()) {
                int id = resultSet.getInt(1);
                String login = resultSet.getString(2);
                String pass = resultSet.getString(3);
                return new User(id, login, pass);
            }
        } catch (SQLException e) {
            throw new AuthException(String.format("Failed to retrieve users with login %s and password %s.", username, password), e);
        }
        return null;
    }

    @Override
    public List<String> getAllUsers() {
        try {
            ResultSet rs = connection.createStatement().executeQuery("SELECT LOGIN FROM Users");
            List<String> result = new ArrayList<>();
            while (rs.next()) {
                result.add(rs.getString(1));
            }
            return result;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public void changeLogin(String oldLogin, String newLogin) {
        try {
            changeLoginStatement.setString(1, newLogin);
            changeLoginStatement.setString(2, oldLogin);
            changeLoginStatement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
