/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package drinkcounter.security;

import drinkcounter.authentication.DrinkcounterUserDetails;
import drinkcounter.dao.PartyDAO;
import drinkcounter.dao.UserDAO;
import drinkcounter.model.Party;
import drinkcounter.model.User;
import java.io.Serializable;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.core.Authentication;

/**
 *
 * @author Toni
 */
public class DrinkPermissionEvaluator implements PermissionEvaluator, ApplicationContextAware {
    
    /*
     * Problems with hades transactions and spring security annotations. Cannot use autowiring :(
     */
    private ApplicationContext applicationContext;

    @Override
    public boolean hasPermission(Authentication authentication, Object targetDomainObject, Object permission) {
        if(!targetDomainObject.getClass().equals(Party.class)){
            return true;
        }
        Party party = (Party) targetDomainObject;
        return party.getParticipants().contains(getUser(authentication));
    }

    @Override
    public boolean hasPermission(Authentication authentication, Serializable targetId, String targetType, Object permission) {
        if(!targetType.equals("Party")){
            return true;
        }
        Party party = getPartyDAO().readByPrimaryKey((Integer)targetId);
        return party.getParticipants().contains(getUser(authentication));
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
    
    private User getUser(Authentication authentication){
        return applicationContext.getBean(UserDAO.class).readByPrimaryKey(getUserId(authentication));
    }
    
    private PartyDAO getPartyDAO(){
        return applicationContext.getBean(PartyDAO.class);
    }
    
    private int getUserId(Authentication authentication){
        return ((DrinkcounterUserDetails)authentication.getPrincipal()).getUserId();
    }
}
