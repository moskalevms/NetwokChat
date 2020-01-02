package javafx.controller;

import javafx.*;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import javafx.util.Callback;


import java.io.IOException;
import java.net.URL;
import java.util.Optional;
import java.util.ResourceBundle;

public class Controller implements Initializable, MessageSender {

    @FXML
    public TextField tfMessage;

    @FXML
    public ListView<Message> lvMessages;

    @FXML
    public Button btSendMessage;

    @FXML
    public Button messageClean;

    @FXML
    public TextField loginField;

    @FXML
    public PasswordField passField;

    @FXML
    public HBox authPanel;

    @FXML
    public HBox msgPanel;

    @FXML
    public TreeView<String> onlineUsersTree;
    public TreeView<String> offlineUsersTree;


    Stage primaryStage;

    private ObservableList<Message> messageList;

    private ObservableList<String> userList;

    private Network network;

    private HistoryStore historyStore;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        messageList = FXCollections.observableArrayList();

        lvMessages.setItems(messageList);
        lvMessages.setCellFactory(new Callback<ListView<Message>, ListCell<Message>>() {
            @Override
            public ListCell<Message> call(ListView<Message> param) {
                return new MessageCellController();
            }
        });


        onlineUsersTree.setRoot(createRoot("Online"));
        offlineUsersTree.setRoot(createRoot("Offline"));

        network = new Network("localhost", 7777, this, onlineUsersTree, offlineUsersTree);
        authPanel.setVisible(true);
        msgPanel.setVisible(false);
    }

    private TreeItem<String> createRoot(String name) {
        TreeItem<String> onlineRoot = new TreeItem<>(name);
        onlineRoot.setExpanded(true);
        return onlineRoot;
    }

    public void setPrimaryStage(Stage primaryStage) {
        this.primaryStage = primaryStage;
    }

    public void onSendMessageClicked() {
        String text = tfMessage.getText();
        if (text != null && !text.isEmpty() && onlineUsersTree.getSelectionModel().getSelectedItem() != null) {
            String userTo = onlineUsersTree.getSelectionModel().getSelectedItem().getValue();
            Message msg = new Message(network.getUsername(), userTo, text);
//            messageList.add(msg);
            tfMessage.clear();
            tfMessage.requestFocus();

            network.sendMessageToUser(msg);
        }
        else {
            showModalAlert("Сетевой чат",
                    "Отправка сообщения",
                    "Сообщение пустое или не выбран онлайн юзер",
                    Alert.AlertType.WARNING);
        }
    }

     public void sendAuth() {
        try {
            network.authorize(loginField.getText(), passField.getText());
            primaryStage.setTitle("Network Chat: " + loginField.getText());
            messageList.clear();
            messageList.addAll(network.getHistory());
        } catch (AuthException ex) {
            ex.printStackTrace();
            showModalAlert("Сетевой чат",
                    "Авторизация",
                    "Ошибка авторизации",
                    Alert.AlertType.ERROR);
            return;
        } catch (IOException ex) {
            ex.printStackTrace();
            showModalAlert("Сетевой чат",
                    "Авторизация",
                    "Ошибка сети",
                    Alert.AlertType.ERROR);
            return;
        }
        authPanel.setVisible(false);
        msgPanel.setVisible(true);
    }

    @Override
    public void submitMessage(Message msg) {
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                messageList.add(msg);
            }
        });
    }


    public void closeNetworkConnection() {
        network.close();
    }

    private static void showModalAlert(String title, String header, String message, Alert.AlertType alertType) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(message);

        alert.showAndWait();
    }

    public void changeLogin(ActionEvent actionEvent) {
        TextInputDialog dialog = new TextInputDialog(network.getUsername());
        dialog.setTitle("Сменить никнейм");
        dialog.setHeaderText("Введите новый никнейм");
        dialog.setContentText("Никнейм:");
        Optional<String> newNick = dialog.showAndWait();
        newNick.ifPresent(nick -> {
            network.sendChangeLoginRequest(nick);
        });
    }

}
