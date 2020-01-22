package chat.client;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;

import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.input.KeyEvent;
import sun.nio.ch.Net;

import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.URL;
import java.util.ResourceBundle;


public class Controller implements Initializable {

    @FXML
    TextField msgField;

    @FXML
    TextArea mainArea;

    @FXML
    Button btnSend;

    private Network network;

    @Override
    public void initialize(URL location, ResourceBundle resources) {

        try {
            network = new Network(8189);
            new Thread(() -> {
                try {
                    while (true) {
                        mainArea.appendText(network.readMsg() + "\n");
                    }
                } catch (IOException e) {
                    Platform.runLater(() -> {
                        Alert alert = new Alert(Alert.AlertType.WARNING, "Соединение с сервером прервано.");
                        alert.showAndWait();
                    });
                }
                finally {
                    network.close();
                }
            }).start();

        } catch (IOException e){
            throw new RuntimeException("Невозможно подключиться к серверу");
        }
    }

    public void sendMessageAction(ActionEvent actionEvent) {

        if (msgField.getText().trim().length() > 0) {
            try {
                network.sendMsg(msgField.getText());
                msgField.clear();
                msgField.requestFocus();
            }catch (IOException e){
                Alert alert = new Alert(Alert.AlertType.WARNING, "Невозможно отправить сообщение. Проверьте подключение");
                alert.showAndWait();
            }
        }

        checkButton(btnSend);
    }

    public void btnChange(KeyEvent keyEvent) {
        checkButton(btnSend);
    }

    public void checkButton(Button button){
        if (msgField.getText().trim().length() == 0){
            btnSend.setDisable(true);
        }
        else btnSend.setDisable(false);
    }
}
