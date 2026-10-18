package drinkcounter.repository;

import drinkcounter.model.User;
import java.util.Optional;
import org.springframework.data.repository.CrudRepository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 *
 * @author Toni
 */
public interface UserRepository extends CrudRepository<User, Integer> {

    /**
     * Joins the caller's transaction but starts none, so the signed-in user loaded for every
     * page is a single database round trip rather than BEGIN, SELECT and COMMIT.
     */
    @Override
    @Transactional(propagation = Propagation.SUPPORTS, readOnly = true)
    Optional<User> findById(Integer id);

    User findByOpenId(String openId);
    User findByEmail(String email);
    User findByPassphrase(String passphrase);
}
