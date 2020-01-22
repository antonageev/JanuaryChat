package chat.server;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

public class MainApp {
    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(8189)){
            System.out.println("Сервер стартовал! Ожидаем подключения...");
            Socket socket = serverSocket.accept();
            System.out.println("Клиент подключен");
            DataInputStream in = new DataInputStream(socket.getInputStream());
            DataOutputStream out = new DataOutputStream(socket.getOutputStream());

            while (true){
                String msg = in.readUTF();
                if (endMsg(msg)) {
                    System.out.println("Запрос на выключение сервера от клиента \"/end\"... Выключение ");
                    break;
                }
                System.out.println("Сообщение от клиента: "+ msg);
                out.writeUTF("echo: " + msg);
            }

        } catch (IOException e){
            e.printStackTrace();
        };
    }

    public static boolean endMsg (String msg){
        return msg.equals("/end");
    }
}
