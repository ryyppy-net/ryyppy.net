package drinkcounter.repository;

import drinkcounter.model.Party;
import java.util.List;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

/**
 *
 * @author Toni
 */
public interface PartyRepository extends CrudRepository<Party, Integer> {
    @Query("select count(*) FROM Party p, IN(p.participants) u WHERE p.id = :partyId and u.id = :userId")
    long countUserParticipations(@Param("partyId") int partyId, @Param("userId") int userId);

    @Query(
            "select count(p) from Party p join p.participants a join p.participants b where a.id = :userId and b.id = :otherUserId")
    long countSharedParties(@Param("userId") int userId, @Param("otherUserId") int otherUserId);

    @Query("select p from Party p join p.participants u where u.id = :userId order by p.startTime desc, p.id desc")
    List<Party> findNewestOfParticipant(@Param("userId") int userId, Limit limit);

    @Query("""
            select p from Party p left join fetch p.participants
            where p.id in (select p2.id from Party p2 join p2.participants u where u.id = :userId
                           order by p2.startTime desc, p2.id desc limit :count)
            order by p.startTime desc, p.id desc""")
    List<Party> findNewestOfParticipantWithParticipants(@Param("userId") int userId, @Param("count") int count);
}
