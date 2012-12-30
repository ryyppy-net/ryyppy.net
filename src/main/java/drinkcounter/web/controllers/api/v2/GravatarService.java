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
public class GravatarService {
    public static String getGravatarUrl(User user) {
        if(user.getEmail() != null) {
            return getUrl(user.getEmail());
        }
        else {
            return getUrl(user.getName() + user.getWeight());
        }
    }

    private static String getUrl(String value) {
        return new Gravatar().setDefaultImage(GravatarDefaultImage.WAVATAR).getUrl(value);
    }
}
