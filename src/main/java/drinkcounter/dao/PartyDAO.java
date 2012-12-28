package drinkcounter.dao;

import drinkcounter.model.Party;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
/**
 *
 * @author Toni
 */
public interface PartyDAO extends CrudRepository<Party, Integer> {
    @Query("select count(*) FROM Party p, IN(p.participants) u WHERE p.id = :partyId and u.id = :userId")
    long countUserParticipations(@Param("partyId") int partyId,@Param("userId") int userId);
}
