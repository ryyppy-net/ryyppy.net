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
    private CurrentUser currentUser;

    public void setCurrentUser(CurrentUser currentUser) {
        this.currentUser = currentUser;
    }
    
    public void checkRightsForParty(int partyId) {
        User user = currentUser.getUser();
        if (user == null){
            throw new NotLoggedInException();
        }
        
        // TODO optimize by query
        List<Party> parties = user.getParties();
        for (Party p : parties) {
            if (p.getId() == partyId) {
                return;
            }
        }
        throw new NotEnoughRightsException();
    }

    public void checkHighLevelRightsToUser(int userId) {
        
        User user = currentUser.getUser();
        if (user == null){
            throw new NotLoggedInException();
        }
        
        if (user.getId() == userId) return;
        
        //TODO optimize by query
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

    public void checkLowLevelRightsToUser(int userId) {
        User user = currentUser.getUser();
        if(user == null){
            throw new NotLoggedInException();
        }
        
        if (user.getId() == userId) return;
        throw new NotEnoughRightsException();
    }
}
