package auth;

import java.util.List;



public interface AuthService extends AutoCloseable {

    boolean authUser(String username, String password);

    User getAuthUser(String username, String password);

    List<String> getAllUsers();

    void changeLogin(String oldLogin, String newLogin);

}
