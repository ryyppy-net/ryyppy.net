package drinkcounter.repository;

import drinkcounter.model.User;
import org.springframework.data.repository.CrudRepository;

/**
 *
 * @author Toni
 */
public interface UserRepository extends CrudRepository<User, Integer> {
    User findByOpenId(String openId);
    User findByEmail(String email);
    User findByPassphrase(String passphrase);
}
