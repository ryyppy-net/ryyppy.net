package drinkcounter;

import drinkcounter.model.User;
import java.util.List;

/**
 *
 * @author Toni
 */
public interface UserService {

    void updateUser(User user);

    List<User> listUsers();

    User addUser(User user);

    User getUser(int userId);

    User getUserByOpenId(String openId);

    void deleteUser(int userId);
    
    boolean emailIsCorrect(String email);
    
    User getUserByEmail(String email);
}
