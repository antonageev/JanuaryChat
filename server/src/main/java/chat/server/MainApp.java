package chat.server;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

public class MainApp {
    public static void main(String[] args) {
        new Server(8189);
    }

}
