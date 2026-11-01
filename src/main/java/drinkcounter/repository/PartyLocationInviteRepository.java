package drinkcounter.repository;

import drinkcounter.model.PartyLocationInvite;
import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

public interface PartyLocationInviteRepository extends CrudRepository<PartyLocationInvite, Integer> {

    /** Invites placed inside the box whose parties have not ended at {@code now}. */
    @Query("""
            select new drinkcounter.repository.InvitedParty(i, p)
            from PartyLocationInvite i join Party p on p.id = i.partyId
            where i.latitude between :minLat and :maxLat
              and i.longitude between :minLon and :maxLon
              and p.endTime > :now""")
    List<InvitedParty> findInBoxOfActiveParties(@Param("minLat") double minLat, @Param("maxLat") double maxLat,
            @Param("minLon") double minLon, @Param("maxLon") double maxLon, @Param("now") Instant now);
}
