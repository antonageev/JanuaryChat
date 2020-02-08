package chat.server;

public interface AuthManager {
    String getNickNameByLoginAndPassword(String login, String password);
    boolean setNewNickName(String oldNickName, String newNickName);
}
