/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package drinkcounter.dao;

import com.google.appengine.api.datastore.Key;
import drinkcounter.model.Participant;
import org.synyx.hades.dao.GenericDao;

/**
 *
 * @author Toni
 */
public interface ParticipantDAO extends GenericDao<Participant, Key> {

}
