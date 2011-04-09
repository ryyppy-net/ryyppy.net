package drinkcounter.authentication;

import drinkcounter.UserService;
import drinkcounter.model.Party;
import drinkcounter.model.User;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;

/**
 *
 * @author murgo
 */
public class AuthenticationChecks {
    @Autowired
    private UserService userService; // stupid autowiring isn't working
    
    public void setUserService(UserService us) {
        userService = us;
    }
    
    public void checkRightsForParty(String openId, int partyId) {
        if (openId == null)
            throw new NotLoggedInException();
        
        User user = userService.getUserByOpenId(openId);
        if (user == null)
            throw new NotLoggedInException();
        
        // TODO optimize by query
        List<Party> parties = user.getParties();
        for (Party p : parties) {
            if (p.getId() == partyId) {
                return;
            }
        }
        throw new NotEnoughRightsException();
    }

    public void checkHighLevelRightsToUser(String openId, int userId) {
        if (openId == null)
            throw new NotLoggedInException();
        
        User user = userService.getUserByOpenId(openId);
        if (user == null)
            throw new NotLoggedInException();
        
        if (user.getId() == userId) return;
        
        //TODO optimize
        List<Party> parties = user.getParties();
        for (Party p : parties) {
            List<User> participants = p.getParticipants();
            for (User u : participants) {
                if (u.getId() == userId)
                    return;
            }
        }
        throw new NotEnoughRightsException();
    }

    public void checkLowLevelRightsToUser(String openId, int userId) {
        if (openId == null)
            throw new NotLoggedInException();
        
        User user = userService.getUserByOpenId(openId);
        if (user == null)
            throw new NotLoggedInException();
        
        if (user.getId() == userId) return;
        throw new NotEnoughRightsException();
    }

    public void checkLogin(String openId) {
        if (openId == null)
            throw new NotLoggedInException();
        
        User user = userService.getUserByOpenId(openId);
        if (user == null)
            throw new NotLoggedInException();
    }
}
