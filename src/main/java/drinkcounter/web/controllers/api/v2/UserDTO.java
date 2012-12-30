/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package drinkcounter.web.controllers.api.v2;

import drinkcounter.model.User;
import drinkcounter.model.User.Sex;
import java.util.List;

/**
 *
 * @author Toni
 */
public class UserDTO {
    private Integer id;
    private String name;
    private String email;
    private User.Sex sex;
    private Float weight;
    private Float promilles;
    private Integer totalDrinks;
    private String profilePictureUrl;
    private List<HistoryPoint> history;

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Sex getSex() {
        return sex;
    }

    public void setSex(Sex sex) {
        this.sex = sex;
    }

    public Float getWeight() {
        return weight;
    }

    public void setWeight(Float weight) {
        this.weight = weight;
    }

    public Float getPromilles() {
        return promilles;
    }

    public void setPromilles(Float promilles) {
        this.promilles = promilles;
    }

    public Integer getTotalDrinks() {
        return totalDrinks;
    }

    public void setTotalDrinks(Integer totalDrinks) {
        this.totalDrinks = totalDrinks;
    }
    
    public String getProfilePictureUrl() {
        return profilePictureUrl;
    }

    public void setProfilePictureUrl(String profilePictureUrl) {
        this.profilePictureUrl = profilePictureUrl;
    }
    
    public List<HistoryPoint> getHistory() {
        return history;
    }
    
    public void setHistory(List<HistoryPoint> history) {
        this.history = history;
    }
    
    public static UserDTO fromUser(User user){
        UserDTO userDTO = new UserDTO();
        userDTO.setId(user.getId());
        userDTO.setName(user.getName());
        userDTO.setEmail(user.getEmail());
        userDTO.setSex(user.getSex());
        userDTO.setWeight(user.getWeight());
        userDTO.setPromilles(user.getPromilles());
        userDTO.setTotalDrinks(user.getTotalDrinks());
        userDTO.setProfilePictureUrl(GravatarService.getGravatarUrl(user));
        return userDTO;
    }
}
