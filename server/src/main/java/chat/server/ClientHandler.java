package chat.server;

import javax.swing.text.html.parser.Entity;
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
                            String nickFromAuthManager = server.getAuthManager().getNickNameByLoginAndPassword(tokens[1], tokens[2]);
                            if (nickFromAuthManager != null) {
                                if (server.isNickBusy(nickFromAuthManager)){
                                    sendMsg("Данный пользователь уже в чате");
                                    continue;
                                }
                                nickname = nickFromAuthManager;
                                server.subscribe(this);
                                sendMsg("/authok " + nickname);
                                break;
                            } else {
                                sendMsg("Указан неверный логин/пароль");
                            }
                        }
                    }
                    while (true) {
                        String msg = in.readUTF();
                        if (msg.startsWith("/")) {
                            if (msg.equals("/end")) {
                                out.writeUTF("/end_confirm");
                                System.out.println("Запрос на выключение сервера от клиента \"/end\"... Выключение ");
                                server.broadcastMsg(nickname + " вышел из чата");
                                break;
                            }
                            if (msg.startsWith("/w")){
                                String[] tokens = msg.split(" ", 3);
                                ClientHandler addressee = null;
                                for (ClientHandler o: server.getClients()){
                                    if (o.getNickname().equals(tokens[1])){
                                        addressee = o;
                                    }
                                }
                                if (addressee!=null){
                                    server.broadcastMsg(this, addressee, "Личное от " + nickname + " k "+ addressee.getNickname()+": " + tokens[2]);
                                } else {
                                    sendMsg(tokens[1] + " не подключен к чату");
                                }
                            }
                        } else {
                            server.broadcastMsg(nickname + ": "+ msg);
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
