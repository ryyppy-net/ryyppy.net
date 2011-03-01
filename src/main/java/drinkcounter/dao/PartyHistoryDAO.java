/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package drinkcounter.dao;

import drinkcounter.model.PartyHistory;
import java.util.List;
import org.synyx.hades.dao.GenericDao;

/**
 *
 * @author toni
 */
public interface PartyHistoryDAO extends GenericDao<PartyHistory, Integer>{
    List<PartyHistory> findByPartyId(String partyId);
}
