/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package drinkcounter.web.controllers.api.v2;

import com.google.gson.Gson;
import drinkcounter.UserService;
import drinkcounter.authentication.CurrentUser;
import drinkcounter.model.User;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpSession;
import org.apache.commons.collections.MapUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 *
 * @author Toni
 */
@Controller
@RequestMapping("/v2")
public class UserApiController {
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private CurrentUser currentUser;
    
    private Gson gson = new Gson();
    
    @RequestMapping(value="/users/{userId}", method=RequestMethod.GET)
    public @ResponseBody String getUser(@PathVariable Integer userId) {
        User user = userService.getUser(userId);
        UserDTO userDTO = UserDTO.fromUser(user);
        return gson.toJson(userDTO);
    }
    
    @RequestMapping(value="/users", method= RequestMethod.GET)
    public @ResponseBody String getUsers(){
        User user = currentUser.getUser();
        Map<String, Integer[]> users = Collections.singletonMap("users", new Integer[]{user.getId()});
        return gson.toJson(users);
    }
    
    
}
