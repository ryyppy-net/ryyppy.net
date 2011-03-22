/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package drinkcounter.dao;

import drinkcounter.model.User;
import org.synyx.hades.dao.GenericDao;

/**
 *
 * @author Toni
 */
public interface UserDAO extends GenericDao<User, Integer> {

    User findByOpenId(String openId);
}
