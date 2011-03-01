/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package drinkcounter.model;
import com.google.appengine.api.datastore.Key;
import java.io.Serializable;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;

/**
 *
 * @author Toni
 */

@MappedSuperclass
public abstract class AbstractEntity implements Serializable {
    private Key storeKey;

    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    public Key getStoreKey() {
        return storeKey;
    }

    public void setStoreKey(Key key) {
        this.storeKey = key;
    }
}
