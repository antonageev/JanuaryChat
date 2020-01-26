package chat.client;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class Network {
    private DataInputStream in;
    private DataOutputStream out;
    private Socket socket;

    public Network(int port) throws IOException{
        socket = new Socket("localhost", port);
        in = new DataInputStream(socket.getInputStream());
        out = new DataOutputStream(socket.getOutputStream());
    }

    public void sendMsg(String msg) throws IOException{
        out.writeUTF(msg);
    }

    public String readMsg() throws IOException{
        return in.readUTF();
    }

    public void close(){
        try{
            if (in!=null){
                System.out.println("закрытие in");
                in.close();
            }
        } catch (IOException e){
            e.printStackTrace();
        }
        try{
            if (out!=null){
                System.out.println("закрытие out");
                out.close();
            }
        } catch (IOException e){
            e.printStackTrace();
        }
        try{
            if (socket!=null){
                System.out.println("закрытие socket");
                socket.close();
            }
        } catch (IOException e){
            e.printStackTrace();
        }
        System.out.println("Соединения закрыты");
    }
}
