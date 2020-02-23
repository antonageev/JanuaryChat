package chat.server;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;

public class ClientHandler {
    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;
    private Server server;
    private String nickname;
    private String confirmedLogin;
    private ExecutorService handlerExecutorService;
    private static final Logger LOGGER = LogManager.getLogger("chat.server.ClientHandler");

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public ClientHandler(Server server, Socket socket, ExecutorService executorService) throws IOException {
        this.server = server;
        this.socket = socket;
        this.handlerExecutorService = executorService;
        this.in = new DataInputStream(socket.getInputStream());
        this.out = new DataOutputStream(socket.getOutputStream());
        handlerExecutorService.execute(() ->{
                try {
                    while (true) {
                        String msg = in.readUTF();
                        if (msg.startsWith("/auth")) {
                            String[] tokens = msg.split(" ", 3);
                            String nickFromAuthManager = server.getBasicAuthManager().getNickNameByLoginAndPassword(tokens[1], tokens[2]);
                            if (nickFromAuthManager != null) {
                                if (server.isNickBusy(nickFromAuthManager)){
                                    sendMsg("Данный пользователь уже в чате");
                                    LOGGER.info("Попытка подключиться к чату с использованием Логина активного пользователя");
                                    continue;
                                }
                                nickname = nickFromAuthManager;
                                confirmedLogin = tokens[1];
                                sendMsg("/authok " + nickname + " "+ confirmedLogin); //Добавил логин, чтобы его можно было читать в Controller
                                server.subscribe(this);
                                sendMsg(getHistory());
                                LOGGER.info("Клиент " + nickFromAuthManager + " подключился к чату и получил историю");
                                break;
                            } else {
                                sendMsg("Указан неверный логин/пароль");
                                LOGGER.info("Клиентом указан неверный логин/пароль");
                            }
                        }
                    }
                    while (true) {
                        String msg = in.readUTF();
                        LOGGER.info("Клиент прислал сообщение/команду: " + msg);
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
                                    LOGGER.warn("Нарушение "+ nickname + " структуры запроса на смену ника");
                                    continue;
                                }
                                server.changeNickName(this, nickname, tokens[1]);
                            }
                            if (msg.equals("/end")) {
                                out.writeUTF("/end_confirm");
                                LOGGER.info("Запрос на выключение сервера от клиента \"/end\"... Выключение ");
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
        });
    }

    public String getHistory(){
        StringBuilder stringBuilder = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new FileReader("./server/chatHistory.txt"))){
            List<String> stringList = new ArrayList<>();
            stringList = Files.readAllLines(Paths.get("./server/chatHistory.txt"));

            int startPosition = 0;
            if (stringList.size()>100) startPosition = stringList.size()-100;
                for (int i = startPosition; i < stringList.size(); i++) {
                    if (stringList.get(i).startsWith("h->")){ //отправляются только те сообщения, которые идут через broadcastMsg()
                    stringBuilder.append(stringList.get(i)).append("\n");
                    }
                }
        } catch (IOException e){
            System.out.println("Не удалось открыть файл с историей для чтения");
            e.printStackTrace();
        }
        return stringBuilder.toString();
    }


    public void sendMsg(String msg){
        try {
            out.writeUTF(msg);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void close() {
        LOGGER.info("Закрытие всех соединений в ClientHandler.Close()");
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
