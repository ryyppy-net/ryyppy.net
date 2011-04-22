/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package drinkcounter.model;
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
    private Integer id;

    @Id
    @GeneratedValue(strategy=GenerationType.AUTO)
    public Integer getId() {
        return id;
    }

    public void setId(Integer key) {
        this.id = key;
    }
}
