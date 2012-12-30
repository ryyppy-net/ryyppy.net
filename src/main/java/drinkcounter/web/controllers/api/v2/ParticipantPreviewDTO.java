/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package drinkcounter.web.controllers.api.v2;

import com.timgroup.jgravatar.Gravatar;
import com.timgroup.jgravatar.GravatarDefaultImage;
import drinkcounter.model.User;

/**
 *
 * @author thardas
 */
public class ParticipantPreviewDTO {
    private String name;
    private String profilePictureUrl;
    private String email;

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getProfilePictureUrl() {
        return profilePictureUrl;
    }

    public void setProfilePictureUrl(String profilePictureUrl) {
        this.profilePictureUrl = profilePictureUrl;
    }
    
    public static ParticipantPreviewDTO fromUser(User user) {
        ParticipantPreviewDTO participant = new ParticipantPreviewDTO();
        participant.setName(user.getName());        
        if(user.getEmail() != null){
            participant.setProfilePictureUrl(new Gravatar().setDefaultImage(GravatarDefaultImage.WAVATAR).getUrl(user.getEmail()));
        }
        return participant;
    }
}
