/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package drinkcounter;

import drinkcounter.model.PartyHistory;
import java.util.List;

/**
 *
 * @author paloniemit
 */
public interface HistoryService {
    public List<PartyHistory> getPartyHistory(String partyId);
    public void takeHistorySnapshot(String partyId);
    
}
