package chat.server;

import java.sql.ResultSet;
import java.sql.SQLException;

public class BasicAuthManager implements AuthManager {
    private DBConnection dbConnection;

    public BasicAuthManager() throws ClassNotFoundException, SQLException {
            this.dbConnection = new DBConnection();
    }

    public void closeAuthConnections(){
        dbConnection.disconnect();
    }

    @Override
    public String getNickNameByLoginAndPassword(String login, String password) {
        try (ResultSet resultSet = dbConnection.getStmt().executeQuery("SELECT nickname FROM users WHERE login = '"+login +"' AND pass ='"+password+"';")){
            String result;
            // Уникальность поля login и nickname контролируется на уровне БД уникальными ключами,
            // поэтому гарантируется попадание в resultSet не более одной строки.
            if (resultSet.next()){
                result = resultSet.getString(1);
                return result;
            }
        } catch (SQLException e){
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public boolean setNewNickName(String oldNickName, String newNickName) {
        int result = 0;
        try {
            result = dbConnection.getStmt().executeUpdate("UPDATE users SET nickname = '"+newNickName+"' WHERE nickname = '"+oldNickName+"';");
        } catch (SQLException e) {
            e.printStackTrace();
        }
        if (result > 0) return true;
        System.out.println("setNewNickname() from BasicAuthManager: Пользователь с ником " + oldNickName + " не найден");
        return false;
    }

}
