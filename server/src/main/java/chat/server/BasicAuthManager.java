package chat.server;

import java.io.PipedInputStream;
import java.sql.*;

public class BasicAuthManager implements AuthManager {
    private Connection connection;
    private Statement stmt;
    private PreparedStatement psGetNickByLoginAndPass;
    private PreparedStatement psSetNewNickName;

    public BasicAuthManager() throws RuntimeException {
        start();
    }


    @Override
    public void start() {
        try {
            Class.forName("org.postgresql.Driver");
            this.connection = DriverManager.getConnection("jdbc:postgresql://localhost:5432/postgres?currentSchema=chat",
                    "postgres", "powerbrain");
            this.stmt = connection.createStatement();
            psGetNickByLoginAndPass = connection.prepareStatement("SELECT nickname FROM users WHERE login = ? AND pass = ?;");
            psSetNewNickName = connection.prepareStatement("UPDATE users SET nickname = ? WHERE nickname = ?;");
        } catch (ClassNotFoundException | SQLException e) {
            throw new AuthServiceException("Unable to connect to DB");
        }
    }

    @Override
    public void stop() {
        try {
            if (psSetNewNickName != null) {
                psSetNewNickName.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        try {
            if (psGetNickByLoginAndPass != null) {
                psGetNickByLoginAndPass.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        try {
            if (stmt != null) {
                stmt.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        try {
            if (connection != null){
                connection.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public String getNickNameByLoginAndPassword(String login, String password) {
        try{
            psGetNickByLoginAndPass.setString(1, login);
            psGetNickByLoginAndPass.setString(2, password);
            try (ResultSet resultSet = psGetNickByLoginAndPass.executeQuery()){
                if (!resultSet.next()){
                    return null;
                }
                return resultSet.getString(1);
            }
            // Уникальность поля login и nickname контролируется на уровне БД уникальными ключами,
            // поэтому гарантируется попадание в resultSet не более одной строки.

        } catch (SQLException e){
            throw new AuthServiceException("Unable to get Name by Login and Password");
        }
    }

    @Override
    public boolean setNewNickName(String oldNickName, String newNickName) {
        int result = 0;
        try {
            psSetNewNickName.setString(1, newNickName);
            psSetNewNickName.setString(2, oldNickName);
            result = psSetNewNickName.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        if (result > 0) return true;
        System.out.println("setNewNickname() from BasicAuthManager: Пользователь с ником " + oldNickName + " не найден");
        return false;
    }

}
