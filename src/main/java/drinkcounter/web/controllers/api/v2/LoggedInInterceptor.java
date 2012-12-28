package drinkcounter.web.controllers.api.v2;

import drinkcounter.authentication.CurrentUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Prevents anonymous users from using API. Returns 401 when user is not logged in
 */
public class LoggedInInterceptor extends HandlerInterceptorAdapter {

    @Autowired
    private CurrentUser currentUser;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if(currentUser.getUser() == null){
            response.sendError(401);
            return false;
        }
        return true;
    }
}
