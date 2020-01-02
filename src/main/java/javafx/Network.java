package javafx;

import javafx.application.Platform;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.HistoryStore;

import java.io.Closeable;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class Network implements Closeable {

    private static final String AUTH_PATTERN = "/auth %s %s";
    private static final String MESSAGE_SEND_PATTERN = "/w %s %s";
    private static final String USER_LIST_PATTERN = "/userlist";
    private static final Pattern MESSAGE_PATTERN = Pattern.compile("^/w (\\w+) (.+)");
    private static final String CHANGE_LOGIN = "/w changeLogin %s";

    private Socket socket;
    private DataOutputStream out;
    private DataInputStream in;
    private final MessageSender messageSender;
    private TreeView<String> onlineLoginTree;
    private TreeView<String> offlineUsersTree;
    private final Thread receiver;

    private String username;
    private final String hostName;
    private final int port;
    private HistoryStore historyStore;

    public Network(String hostName, int port, MessageSender messageSender,
                   TreeView<String> onlineLoginTree, TreeView<String> offlineUsersTree) {
        this.hostName = hostName;
        this.port = port;
        this.messageSender = messageSender;
        this.onlineLoginTree = onlineLoginTree;
        this.offlineUsersTree = offlineUsersTree;

        this.receiver = createReceiverThread();
    }

    private Thread createReceiverThread() {
        return new Thread(new Runnable() {
            @Override
            public void run() {
                while (!Thread.currentThread().isInterrupted()) {
                    try {
                        String text = in.readUTF();

                        System.out.println("New message " + text);
                        if (text.startsWith("ONLINE_USER_LIST")) {
                            String[] users = text.replace("ONLINE_USER_LIST ", "").split(":");
                            Platform.runLater(new Runnable() {
                                @Override
                                public void run() {
                                    onlineLoginTree.getRoot().getChildren().clear();
                                    Stream.of(users)
                                            .map(TreeItem<String>::new)
                                            .forEach(onlineLoginTree.getRoot().getChildren()::add);
                                }
                            });
                        }
                        if (text.startsWith("OFFLINE_USER_LIST")) {
                            String[] users = text.replace("OFFLINE_USER_LIST ", "").split(":");
                            Platform.runLater(new Runnable() {
                                @Override
                                public void run() {
                                    offlineUsersTree.getRoot().getChildren().clear();
                                    Stream.of(users)
                                            .map(TreeItem<String>::new)
                                            .forEach(offlineUsersTree.getRoot().getChildren()::add);
                                }
                            });
                        }

                        Matcher matcher = MESSAGE_PATTERN.matcher(text);
                        if (matcher.matches()) {
                            Message msg = new Message(matcher.group(1), username,
                                    matcher.group(2));
                            historyStore.saveRecord(msg);
                            messageSender.submitMessage(msg);
                        } else if (text.startsWith(USER_LIST_PATTERN)) {
                            // TODO обновить список подключенных пользователей
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                System.out.printf("Network connection is closed for user %s%n", username);
            }
        });
    }

    public List<Message> getHistory() {
        return historyStore.getLastMessages();
    }

    public void sendMessageToUser(Message message) {
        sendMessage(String.format(MESSAGE_SEND_PATTERN, message.getUserTo(), message.getText()));
        historyStore.saveRecord(message);
        messageSender.submitMessage(message);

    }

    public void sendChangeLoginRequest(String newLogin) {
        sendMessage(String.format(CHANGE_LOGIN, newLogin));
        historyStore.rename(newLogin);
    }

    private void sendMessage(String msg) {
        try {
            out.writeUTF(msg);
            out.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void authorize(String username, String password) throws IOException {
        socket = new Socket(hostName, port);
        out = new DataOutputStream(socket.getOutputStream());
        in = new DataInputStream(socket.getInputStream());

        out.writeUTF(String.format(AUTH_PATTERN, username, password));
        String response = in.readUTF();
        if (response.equals("/auth successful")) {
            this.username = username;
            historyStore = new HistoryStore(username);
            receiver.start();
        } else {
            throw new AuthException();
        }
    }

    public String getUsername() {
        return username;
    }

    @Override
    public void close() {
        try {
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        receiver.interrupt();
        try {
            receiver.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
