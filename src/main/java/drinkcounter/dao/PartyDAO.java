/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package drinkcounter.dao;

import drinkcounter.model.Party;
import org.synyx.hades.dao.GenericDao;

/**
 *
 * @author Toni
 */
public interface PartyDAO extends GenericDao<Party, Integer> {
    Party findById(String id);
}
