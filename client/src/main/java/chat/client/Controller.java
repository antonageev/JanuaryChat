package chat.client;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;

import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
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
    TextField msgField, loginField;

    @FXML
    TextArea mainArea;

    @FXML
    Button btnSend;

    @FXML
    PasswordField passField;

    @FXML
    HBox loginBox, msgBox;

    private Network network;
    private boolean authenticated;
    private String nickname;

    public void setAuthenticated (boolean authenticated){
        this.authenticated = authenticated;
        loginBox.setVisible(!authenticated);
        loginBox.setManaged(!authenticated);
        msgBox.setVisible(authenticated);
        msgBox.setManaged(authenticated);
    }


    @Override
    public void initialize(URL location, ResourceBundle resources) {

        try {
            setAuthenticated(false);
            network = new Network(8189);
            Thread t1 = new Thread(() -> {
                try {
                    while (true) { // цикл авторизации
                        String msg = network.readMsg();
                        if (msg.startsWith("/authok")) { //authok nick1
                            nickname = msg.split(" ")[1];
                            setAuthenticated(true);
                            break;
                        }
                        mainArea.appendText(msg + "\n");
                    }
                    while (true) { // цикл общения с сервером
                        String msg = network.readMsg();
                        if (msg.equals("/end_confirm")) {
                            System.out.println("Закрытие соединения");
                            mainArea.appendText("Сервер прощается с вами.");
                            break;
                        }
                        mainArea.appendText(msg + "\n");
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
            });

            t1.setDaemon(true);
            t1.start();

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

    public void tryToAuth(ActionEvent actionEvent) {
        try {
            network.sendMsg("/auth " + loginField.getText() + " " + passField.getText());
            loginField.clear();
            passField.clear();
        } catch (IOException e) {
            Alert alert = new Alert(Alert.AlertType.WARNING, "Невозможно отправить сообщение. Проверьте подключение");
            alert.showAndWait();
        }
    }
}
