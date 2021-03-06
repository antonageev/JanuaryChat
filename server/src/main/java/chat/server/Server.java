package chat.server;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {
    private List<ClientHandler> clients;
    private AuthManager basicAuthManager;
    private final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private ExecutorService executorService;

    public AuthManager getBasicAuthManager() {
        return basicAuthManager;
    }

    public Server(int port){
        clients = new ArrayList<>();
        try {
            basicAuthManager = new BasicAuthManager();
            executorService = Executors.newCachedThreadPool();
            try (ServerSocket serverSocket = new ServerSocket(port)) {
                System.out.println("Сервер стартовал! Ожидаем подключения...");
                while (true) {
                    Socket socket = serverSocket.accept();
                    System.out.println("Клиент подключен");
                    new ClientHandler(this, socket, executorService);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        } catch (RuntimeException e){
            e.printStackTrace();
        } finally {
            if (basicAuthManager != null) {
                basicAuthManager.stop();
            }
            executorService.shutdown();
        }
    }

    public void broadcastMsg (String msg, boolean withDateTime){
        if (withDateTime) msg = String.format("[%s] %s", LocalDateTime.now().format(DTF), msg);
        for (ClientHandler o : clients){
            o.sendMsg(msg);
        }
        // Блок записи истории чата.
        try (BufferedWriter writer = new BufferedWriter(new FileWriter("./server/chatHistory.txt", true))){
            String prefix = "";
            if (msg.startsWith("/")) {
                prefix = "s->"; //присваиваем сервисным сообщениям префикс "s->",
            } else {
                prefix = "h->"; //обычным сообщениям префикс "h->" - только такие сообщения будут попадать в окно чата.
            }
            writer.write(prefix + msg + "\n");
        }catch (IOException e){
            System.out.println("Не удалось записасть сообщение в общую историю чата");
            e.printStackTrace();
        }
    }

    public void broadcastClientsList (){
        StringBuilder stringBuilder = new StringBuilder("/clients_list ");
        for (ClientHandler o : clients){
            stringBuilder.append(o.getNickname()).append(" ");
        }
        stringBuilder.setLength(stringBuilder.length()-1);
        broadcastMsg(stringBuilder.toString(), false);
    }

    public void sendPrivateMsg (ClientHandler sender, String receiverNickName, String msg){
        if (sender.getNickname().equals(receiverNickName)) {
            sender.sendMsg("Нельзя шептаться с самим собой");
            return;
        }
        for (ClientHandler o: clients){
            if (o.getNickname().equals(receiverNickName)){
                o.sendMsg("Личное от " + sender.getNickname() + " k "+ o.getNickname()+": " + msg);
                sender.sendMsg("Личное от " + sender.getNickname() + " k "+ o.getNickname()+": " + msg);
                return;
            }
        }
        sender.sendMsg(receiverNickName + " не подключен к чату");
    }

    public synchronized void  subscribe(ClientHandler clientHandler){
        broadcastMsg(clientHandler.getNickname() + " подключился к чату!", false);
        clients.add(clientHandler);
        broadcastClientsList();
    }

    public synchronized void unsubscribe (ClientHandler clientHandler){
        clients.remove(clientHandler);
        broadcastMsg(clientHandler.getNickname() + " вышел из чата", false);
        broadcastClientsList();
    }

    public boolean isNickBusy(String nickname){
        for (ClientHandler o : clients){
            if (o.getNickname().equals(nickname)) return true;
        }
        return false;
    }
    // не знаю, правильно ли все общение и проверки класть в Server?
    // для этого пришлось передавать в метод changNickName параметр clientHandler.
    // Есть ли более предпочтительное место размещения блоков кода метода changeNickName с точки зрения
    // архитектуры приложения и логики?
    public void changeNickName(ClientHandler clientHandler, String oldNickName, String newNickName){
        if (isNickBusy(newNickName)){
            clientHandler.sendMsg("Ник "+ newNickName + " занят");
            return;
        }
        if (basicAuthManager.setNewNickName(oldNickName, newNickName)){
            broadcastMsg("Пользователь "+ oldNickName + " сменил ник на "+ newNickName, true);
            clientHandler.setNickname(newNickName);
            clientHandler.sendMsg("/change_nickOK "+ newNickName);
            broadcastClientsList();
            return;
        }
        System.out.println("changeNickname() from Server: не удалось поменять ник");
    }

}
