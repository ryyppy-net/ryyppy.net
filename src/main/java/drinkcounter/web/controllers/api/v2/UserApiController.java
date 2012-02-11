/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package drinkcounter.web.controllers.api.v2;

import com.google.gson.Gson;
import drinkcounter.UserService;
import drinkcounter.model.User;
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
    
    private Gson gson = new Gson();
    
    @RequestMapping(value="/users/{userId}", method=RequestMethod.GET)
    public @ResponseBody String getUser(@PathVariable Integer userId) {
        User user = userService.getUser(userId);
        UserDTO userDTO = new UserDTO();
        userDTO.setId(user.getId());
        userDTO.setName(user.getName());
        userDTO.setEmail(user.getEmail());
        userDTO.setSex(user.getSex());
        return gson.toJson(userDTO);
    }
}
