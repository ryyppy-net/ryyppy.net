package drinkcounter.repository;

import drinkcounter.model.PartyJoinLink;
import java.util.Optional;
import org.springframework.data.repository.CrudRepository;

public interface PartyJoinLinkRepository extends CrudRepository<PartyJoinLink, Integer> {

    Optional<PartyJoinLink> findByToken(String token);
}
