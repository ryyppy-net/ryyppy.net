package drinkcounter.web.controllers.api.v2;

import drinkcounter.DrinkCounterService;
import drinkcounter.authentication.CurrentUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Checks that current user is a member of a party that he is trying to access. Sends forbidden exception if user doesn't
 * have enough rights
 */
public class PartyAuthorizationInterceptor implements HandlerInterceptor {

    @Autowired
    private CurrentUser currentUser;

    @Autowired
    private DrinkCounterService drinkCounterService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String partyIdStr = new AntPathMatcher().extractUriTemplateVariables("/v2/parties/{partyId}/**", request.getPathInfo()).get("partyId");
        if(partyIdStr == null){
            // we aren't accessing any party in particular, no need for authorization checks
            return true;
        }
        Integer partyId = Integer.parseInt(partyIdStr);
        if(!drinkCounterService.isUserParticipant(partyId, currentUser.getUser().getId())){
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return false;
        }
        return true;
    }
}
