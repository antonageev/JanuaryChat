package chat.server;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class ClientHandler {
    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;
    private Server server;
    private String nickname;

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public ClientHandler(Server server, Socket socket) throws IOException {
        this.server = server;
        this.socket = socket;
        this.in = new DataInputStream(socket.getInputStream());
        this.out = new DataOutputStream(socket.getOutputStream());
        new Thread(() ->{
                try {
                    while (true) {
                        String msg = in.readUTF();
                        if (msg.startsWith("/auth")) {
                            String[] tokens = msg.split(" ", 3);
                            String nickFromAuthManager = server.getBasicAuthManager().getNickNameByLoginAndPassword(tokens[1], tokens[2]);
                            if (nickFromAuthManager != null) {
                                if (server.isNickBusy(nickFromAuthManager)){
                                    sendMsg("Данный пользователь уже в чате");
                                    continue;
                                }
                                nickname = nickFromAuthManager;
                                sendMsg("/authok " + nickname);
                                server.subscribe(this);
                                break;
                            } else {
                                sendMsg("Указан неверный логин/пароль");
                            }
                        }
                    }
                    while (true) {
                        String msg = in.readUTF();
                        if (msg.startsWith("/")) {
                            if (msg.startsWith("/w ")){
                                String[] tokens = msg.split(" ", 3);
                                server.sendPrivateMsg(this, tokens[1], tokens[2]);
                                continue;
                            }
                            //в ClientHandler только проверка нового ника на пробелы
                            // дальше закидываем задачу на смену ника вездесущему серверу
                            if (msg.startsWith("/change_nick ")){
                                String[] tokens = msg.split(" ");
                                if (tokens.length > 2) {
                                    sendMsg("Новый ник должен быть без пробелов");
                                    continue;
                                }
                                server.changeNickName(this, nickname, tokens[1]);
                            }
                            if (msg.equals("/end")) {
                                out.writeUTF("/end_confirm");
                                System.out.println("Запрос на выключение сервера от клиента \"/end\"... Выключение ");
                                break;
                            }
                        } else {
                            server.broadcastMsg(nickname + ": "+ msg, true);
                        }
                    }
                } catch (IOException e){
                    e.printStackTrace();
                }
                finally {
                    close();
                }
        }).start();
    }


    public void sendMsg(String msg){
        try {
            out.writeUTF(msg);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void close() {
        server.unsubscribe(this);
        nickname = null;
        if (in!=null){
            try {
                in.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (out!=null){
            try {
                out.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (socket!=null){
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

}
