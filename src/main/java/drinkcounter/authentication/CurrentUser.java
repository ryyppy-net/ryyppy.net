/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package drinkcounter.authentication;

import drinkcounter.model.User;
import org.springframework.stereotype.Component;

/**
 *
 * @author Toni
 */
public interface CurrentUser {
    User getUser();
}
