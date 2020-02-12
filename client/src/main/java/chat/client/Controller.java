package chat.client;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;

import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
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

    @FXML
    ListView<String> clientsList;

    private Network network;
    private boolean authenticated;
    private String nickname;
    private String confirmedLogin;

    public void setAuthenticated (boolean authenticated){
        this.authenticated = authenticated;
        loginBox.setVisible(!authenticated);
        loginBox.setManaged(!authenticated);
        msgBox.setVisible(authenticated);
        msgBox.setManaged(authenticated);
        clientsList.setVisible(authenticated);
        clientsList.setManaged(authenticated);
    }


    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setAuthenticated(false);

    }

    public void tryToConnect(){
        try {
            if (network != null && network.isConnected()) return;
            setAuthenticated(false);
            network = new Network(8189);
            Thread t1 = new Thread(() -> {
                try {
                    while (true) { // цикл авторизации
                        String msg = network.readMsg();
                        if (msg.startsWith("/authok ")) { //authok nick1 login1
                            nickname = msg.split(" ")[1];
                            confirmedLogin = msg.split(" ")[2];
                            mainArea.appendText("Вы зашли под ником "+ nickname + "\n");
                            setAuthenticated(true);
                            break;
                        }
                        mainArea.appendText(msg + "\n");
                    }
                    while (true) { // цикл общения с сервером
                        String msg = network.readMsg();
                        // сюда положим запись всех входящих сообщений в файл!
                        try (BufferedWriter writer = new BufferedWriter(new FileWriter("./client/history_["+confirmedLogin+"].txt", true))){
                            writer.write(msg + "\n");
                        } catch (IOException e){
                            System.out.println("ошибка доступа к файлу локальной истории. Сообщение записать не удалось");
                            e.printStackTrace();
                        }
                        // положили в файл локальной истории.
                        if (msg.startsWith("/")){
                            if (msg.startsWith("/change_nickOK ")){
                                String[] tokens = msg.split(" ");
                                nickname = tokens[1];
                                continue;
                            }
                            if (msg.startsWith("/clients_list ")){
                                Platform.runLater(() -> {
                                            clientsList.getItems().clear();
                                            String[] tokens = msg.split(" ");
                                            for (int i = 1; i < tokens.length; i++) {
                                                if (!nickname.equals(tokens[i])){
                                                    clientsList.getItems().add(tokens[i]);
                                                }
                                            }
                                        }
                                );
                                continue;
                            }
                            if (msg.equals("/end_confirm")) {
                                System.out.println("Закрытие соединения");
                                mainArea.appendText("Сервер прощается с вами.\n");
                                break;
                            }
                        } else {
                            mainArea.appendText(msg + "\n");
                        }
                    }
                } catch (IOException e) {
                    Platform.runLater(() -> {
                        Alert alert = new Alert(Alert.AlertType.WARNING, "Соединение с сервером прервано.");
                        alert.showAndWait();
                    });
                }
                finally {
                    network.close();
                    setAuthenticated(false);
                    nickname=null;
                }
            });

            t1.setDaemon(true);
            t1.start();

        } catch (IOException e){
            Alert alert = new Alert(Alert.AlertType.WARNING, "Невозможно подключиться к серверу");
            alert.showAndWait();
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
            tryToConnect();
            network.sendMsg("/auth " + loginField.getText() + " " + passField.getText());
            loginField.clear();
            passField.clear();
        } catch (IOException e) {
            Alert alert = new Alert(Alert.AlertType.WARNING, "Невозможно отправить сообщение. Проверьте подключение");
            alert.showAndWait();
        }
    }

    public void selectUserForWisp(MouseEvent mouseEvent) {
        if (mouseEvent.getClickCount()==2){
            msgField.setText("/w "+ clientsList.getSelectionModel().getSelectedItem() + " ");
            msgField.requestFocus();
            msgField.selectEnd();
        }
    }
}
