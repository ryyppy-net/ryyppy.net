/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package drinkcounter.dao;

import drinkcounter.model.Party;
import org.synyx.hades.dao.GenericDao;
import org.synyx.hades.dao.Param;
import org.synyx.hades.dao.Query;

/**
 *
 * @author Toni
 */
public interface PartyDAO extends GenericDao<Party, Integer> {
    Party findByName(String name);
    @Query("select count(*) FROM Party p, IN(p.participants) u WHERE p.id = :partyId and u.id = :userId")
    long countUserParticipations(@Param("partyId") int partyId,@Param("userId") int userId);
}
