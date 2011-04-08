/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package drinkcounter;

import drinkcounter.model.User;
import java.util.List;

/**
 *
 * @author Toni
 */
public interface UserService {

    List<User> listUsers();

    User addUser(User user);

    User getUser(String userid);

    User getUserByOpenId(String openId);

    void deleteUser(String userId);
    
    boolean emailIsCorrect(String email);
    
    User getUserByEmail(String email);
}
