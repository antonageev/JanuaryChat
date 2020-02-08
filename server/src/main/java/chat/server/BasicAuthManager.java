package chat.server;

import java.util.ArrayList;
import java.util.List;

public class BasicAuthManager implements AuthManager {
    private class Entry{
        private String login;
        private String password;
        private String nickname;

        public Entry(String login, String password, String nickname) {
            this.login = login;
            this.password = password;
            this.nickname = nickname;
        }

        public void setNickname(String nickname) {
            this.nickname = nickname;
        }
    }

    private List<Entry> users;

    public BasicAuthManager() {
        this.users = new ArrayList<>();
        users.add(new Entry("login1", "pass1", "user1"));
        users.add(new Entry("login2", "pass2", "user2"));
        users.add(new Entry("login3", "pass3", "user3"));
    }

    @Override
    public String getNickNameByLoginAndPassword(String login, String password) {
        for (Entry o : users){
            if (o.login.equals(login) && (o.password.equals(password))) return o.nickname;
        }
        return null;
    }

    @Override
    public boolean setNewNickName(String oldNickName, String newNickName) {
        for (Entry o : users){
            if (o.nickname.equals(oldNickName)){
                o.setNickname(newNickName);
                return true;
            }
        }
        System.out.println("setNewNickname() from BasicAuthManager: Пользователь с ником " + oldNickName + " не найден");
        return false;
    }
}
