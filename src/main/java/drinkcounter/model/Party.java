package drinkcounter.model;

import java.util.ArrayList;
import java.util.List;
import javax.persistence.Entity;
import javax.persistence.ManyToMany;
import javax.persistence.Transient;

/**
 *
 * @author Toni
 */
@Entity
public class Party extends AbstractEntity{
    private List<User> users;
    private String id;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @Transient
    public String getName(){
        return getId();
    }

    @ManyToMany
    public List<User> getUsers() {
        if(users == null){
            users =  new ArrayList<User>();
        }
        return users;
    }

    public void setUsers(List<User> users) {
        this.users = users;
    }

    public void addUser(User user){
        getUsers().add(user);
    }
}
