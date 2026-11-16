package drinkcounter.repository;

import drinkcounter.model.Party;
import drinkcounter.model.User;
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
    long countUserParticipations(@Param("partyId") int partyId,@Param("userId") int userId);

    @Query("select count(p) from Party p join p.participants a join p.participants b where a.id = :userId and b.id = :otherUserId")
    long countSharedParties(@Param("userId") int userId, @Param("otherUserId") int otherUserId);

    /** 1 when the user may delete the party: a member who started it, or its only member with an account. */
    @Query("""
            select count(p) from Party p join p.participants me
            where p.id = :partyId and me.id = :userId
              and (p.startedBy = :userId or not exists (
                    select other.id from Party q join q.participants other
                    where q.id = p.id and other.guest = false and other.id <> :userId))""")
    long countDeletableBy(@Param("partyId") int partyId, @Param("userId") int userId);

    /** The party's guests who are in no other party. */
    @Query("""
            select u from Party p join p.participants u
            where p.id = :partyId and u.guest = true
              and not exists (select q.id from Party q join q.participants g where g.id = u.id and q.id <> p.id)
            order by u.id""")
    List<User> findGuestsOnlyIn(@Param("partyId") int partyId);

    @Query("select p from Party p join p.participants u where u.id = :userId order by p.startTime desc, p.id desc")
    List<Party> findNewestOfParticipant(@Param("userId") int userId, Limit limit);

    @Query("""
            select p from Party p left join fetch p.participants
            where p.id in (select p2.id from Party p2 join p2.participants u where u.id = :userId
                           order by p2.startTime desc, p2.id desc limit :count)
            order by p.startTime desc, p.id desc""")
    List<Party> findNewestOfParticipantWithParticipants(@Param("userId") int userId, @Param("count") int count);
}
