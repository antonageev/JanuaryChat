package chat.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class Server {
    private List<ClientHandler> clients;
    private AuthManager authManager;

    public AuthManager getAuthManager() {
        return authManager;
    }

    public List<ClientHandler> getClients() {
        return clients;
    }

    public Server(int port){
        clients = new ArrayList<>();
        authManager = new BasicAuthManager();
        try (ServerSocket serverSocket = new ServerSocket(port)){
            System.out.println("Сервер стартовал! Ожидаем подключения...");
            while (true) {
                Socket socket = serverSocket.accept();
                System.out.println("Клиент подключен");
                new ClientHandler(this, socket);
            }
        } catch (IOException e){
            e.printStackTrace();
        }
    }

    public void broadcastMsg (String msg){
        for (ClientHandler o : clients){
            o.sendMsg(msg);
        }
    }

    public void broadcastMsg (ClientHandler from, ClientHandler addressee, String msg){
        from.sendMsg(msg);
        addressee.sendMsg(msg);
    }

    public synchronized void  subscribe(ClientHandler clientHandler){
        broadcastMsg(clientHandler.getNickname() + " подключился к чату!");
        clients.add(clientHandler);
    }

    public synchronized void unsubscribe (ClientHandler clientHandler){
        broadcastMsg(clientHandler.getNickname() + " вышел из чата");
        clients.remove(clientHandler);
    }

    public boolean isNickBusy(String nickname){
        for (ClientHandler o : clients){
            if (o.getNickname().equals(nickname)) return true;
        }
        return false;
    }

}
