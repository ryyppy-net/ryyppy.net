package drinkcounter.repository;

import drinkcounter.model.User;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.repository.CrudRepository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 *
 * @author Toni
 */
public interface UserRepository extends CrudRepository<User, Integer> {

    /**
     * The signed-in user is loaded for most pages; outside a caller's transaction this is one
     * database round trip.
     */
    @Override
    @Transactional(propagation = Propagation.SUPPORTS, readOnly = true)
    Optional<User> findById(Integer id);

    /** The user and all of their drinks, in one round trip. */
    @EntityGraph(attributePaths = "drinks")
    @Transactional(propagation = Propagation.SUPPORTS, readOnly = true)
    Optional<User> findWithDrinksById(Integer id);

    User findByOpenId(String openId);
    User findByEmail(String email);
    User findByPassphrase(String passphrase);
}
