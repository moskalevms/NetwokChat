package Server;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import auth.JdbcAuthService;
import auth.AuthService;

import auth.User;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class ChatServer {

    private static final Logger log = LoggerFactory.getLogger(ChatServer.class);


    private static final String USER_CONNECTED_PATTERN = "/userconn";
    private static final String USER_DISCONN_PATTERN = "/userdissconn";
    private static final Pattern AUTH_PATTERN = Pattern.compile("^/auth (\\w+) (\\w+)$");

    private AuthService authService = new JdbcAuthService("ChatClient.db");

    private Map<User, ClientHandler> clientHandlerMap = Collections.synchronizedMap(new HashMap<>());
    private ExecutorService executorService;

    public static void main(String[] args) throws Exception {
        ChatServer chatServer = new ChatServer();
        chatServer.start(7777);
    }

    public void start(int port) throws Exception {
        executorService = Executors.newCachedThreadPool();

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            log.info("Server started!");
            while (true) {
                Socket socket = serverSocket.accept();
                DataInputStream inp = new DataInputStream(socket.getInputStream());
                DataOutputStream out = new DataOutputStream(socket.getOutputStream());
                log.info("New client connected!");

                try {
                    String authMessage = inp.readUTF();
                    Matcher matcher = AUTH_PATTERN.matcher(authMessage);
                    if (matcher.matches()) {
                        String username = matcher.group(1);
                        String password = matcher.group(2);
                        User authUser = authService.getAuthUser(username, password);
                        if ( authUser != null) {
                            clientHandlerMap.put(authUser, new ClientHandler(authUser, socket, this));
                            out.writeUTF("/auth successful");
                            out.flush();
                            broadcastUserConnected();

                            log.info("Authorization for user {} successful", username);
                            updateUsersListForAllClient();
                        } else {
                            log.info("Authorization for user {} failed", username);
                            out.writeUTF("/auth fails");
                            out.flush();
                            socket.close();
                        }
                    } else {
                        log.warn("Incorrect authorization message {}", authMessage);
                        out.writeUTF("/auth fails");
                        out.flush();
                        socket.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } catch (IOException e) {
            log.error("Failed to read data from socket.", e);
            e.printStackTrace();
        } finally {
            authService.close();
            executorService.shutdown();
        }
    }

    public void sendMessage(User userFrom, String userTo, String msg) throws IOException {
        clientHandlerMap.forEach((user, handler) -> {
            if (user.login.equals(userTo)) {
                try {
                    handler.sendMessage(userFrom.login, msg);
                } catch (IOException e) {
                    log.info("User {} not found. Message from {} is lost.", userTo, userFrom);
                }
            }
        });
    }


    public void unsubscribeClient(ClientHandler clientHandler) {
        clientHandlerMap.remove(clientHandler.getUser());
        broadcastUserDisconnected();
    }

    public void broadcastUserConnected() {
        // TODO сообщать о том, что конкретный пользователь подключился
    }

    public void broadcastUserDisconnected() {
        // TODO сообщать о том, что конкретный пользователь отключился
    }

    public void changeLogin(User user, String newLogin) {
        authService.changeLogin(user.login, newLogin);
        user.login = newLogin;
        updateUsersListForAllClient();
    }

    private void updateUsersListForAllClient() {
        List<String> onlineUsers = clientHandlerMap.keySet().stream()
                .map(user -> user.login)
                .collect(Collectors.toList());

        List<String> offlineUsers = authService.getAllUsers();
        offlineUsers.removeAll(onlineUsers);

//        for (User user : clientHandlerMap.keySet()) {
//            ClientHandler handler = clientHandlerMap.get(user);
//            try {
//                handler.sendUsersList("ONLINE_USER_LIST " + String.join(":", onlineUsers));
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//        }

        clientHandlerMap.forEach((login, handler) -> {
            try {
                handler.sendUsersList("ONLINE_USER_LIST " + String.join(":", onlineUsers));
                handler.sendUsersList("OFFLINE_USER_LIST " + String.join(":", offlineUsers));
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    public ExecutorService getExecutorService() {
        return executorService;
    }
}
