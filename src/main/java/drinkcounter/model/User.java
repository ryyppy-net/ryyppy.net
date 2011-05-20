/*
1439 (@murgo) mikähän meillä ois yhen juojan nimi
1439 (@murgo) kun ei tuo nykyinen participant enää oikein kuvasta sitä
1440 (@murgo) jos ne eriytetään
1440 (@murgo) tai siis kun ne eriytetään
1440 (@murgo) oisko se sit Drinker vai User vai Person vai
1440 (@ville_salonen) Drinker
1440 (@ville_salonen) User ja Person on tylsemmille projekteille.
1441 (@ville_salonen) Tietty mitäs sit, jos halutaan ottaa kovemmat huumeet mukaan, niin heroiininpiikittäjä
                      ei oo enää Drinker!
1441 (@murgo) User kävis tähänkin :)
1442 (@ville_salonen) No, ihan miten vaan :)
1442 (@murgo) hehe, kait tajusit, hehe
1442 (@ville_salonen) Ah :D
1442 (@murgo) http://en.wikipedia.org/wiki/User
 */

package drinkcounter.model;

import drinkcounter.AlcoholServiceImpl;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.ListIterator;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.ManyToMany;
import javax.persistence.OneToMany;
import javax.persistence.OrderBy;
import javax.persistence.Table;
import javax.persistence.Transient;

/**
 *
 * @author Toni, Lauri
 */
@Entity
@Table(name="Users")
public class User extends AbstractEntity{
    public enum Sex {
        MALE(0.75f), FEMALE(0.66f);
        public float factor;
        Sex(float factor){
            this.factor = factor;
        }
    }
    public enum AuthMethod {
        OPENID, FACEBOOK
    }

    private String email;
    private List<Party> parties;
    private String name;
    private float weight = 70;
    private Sex sex = Sex.MALE;
    private List<Drink> drinks = new ArrayList<Drink>();
    private String passphrase;
    
    // authentication attributes
    private String openId;
    private AuthMethod authMethod = AuthMethod.OPENID;

    /**
     * This user is a guest in the system, can be removed after sober
     */
    private boolean guest;
    
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setOpenId(String openId) {
        this.openId = openId;
    }

    public String getOpenId() {
        return openId;
    }

    @Enumerated(EnumType.STRING)
    public AuthMethod getAuthMethod() {
        if(authMethod == null){
            authMethod = AuthMethod.OPENID;
        }
        return authMethod;
    }

    public void setAuthMethod(AuthMethod authMethod) {
        this.authMethod = authMethod;
    }

    @ManyToMany(mappedBy="participants")
    public List<Party> getParties() {
        return parties;
    }

    public void setParties(List<Party> parties) {
        this.parties = parties;
    }

    public void drink(Drink drink){
        AlcoholServiceImpl.getInstance().drinkAdded(this, drink);
        getDrinks().add(drink);
    }

    /**
     * Weight in kilos
     * @return
     */
    public float getWeight() {
        return weight;
    }

    public void setPassphrase(String passphrase) {
        this.passphrase = passphrase;
    }
    
    public String getPassphrase() {
        return passphrase;
    }


    /**
     * Weight in kilos
     * @param weightInKilos
     */
    public void setWeight(float weightInKilos) {
        this.weight = weightInKilos;
        AlcoholServiceImpl.getInstance().initializeUser(this);
    }

    public void setSex(Sex sex) {
        this.sex = sex;
        AlcoholServiceImpl.getInstance().initializeUser(this);
    }

    @Enumerated(EnumType.STRING)
    public Sex getSex() {
        return sex;
    }

    public int drinksSince(Date date){
        int count = 0;
        // iterate from last to first
        ListIterator<Drink> iterator = drinks.listIterator(drinks.size());
        while(iterator.hasPrevious()){
            Drink drink = iterator.previous();
            if (drink.getTimeStamp().before(date))
                break;
            count++;
        }
        return count;
    }

    public boolean isGuest() {
        return guest;
    }

    public void setGuest(boolean guest) {
        this.guest = guest;
    }

    /**
     * @return the drinks
     */
    @OneToMany(mappedBy="drinker")
    @OrderBy("timeStamp asc")
    public List<Drink> getDrinks() {
        return drinks;
    }

    /**
     * Must be sorted by time ascending
     * TODO when bored, create index for this, or better yet use named queries when needed
     * @param drinks the drinks to set
     */
    public void setDrinks(List<Drink> drinks) {
        this.drinks = drinks;
    }

    public String getEmail() {
        return this.email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
    
    public void removeDrink(Drink drink) {
        getDrinks().remove(drink);
        drink.setDrinker(null);
        AlcoholServiceImpl.getInstance().drinkRemoved(this, drink);
    }

    @Transient
    public float getPromilles() {
        return AlcoholServiceImpl.getInstance().getPromilles(this);
    }

    @Transient
    public int getTotalDrinks() {
        return AlcoholServiceImpl.getInstance().getTotalDrinks(this);
    }

    public List<Float> getPromillesAtInterval(Date startTime, Date endTime, int intervalMs) {
        return AlcoholServiceImpl.getInstance().getPromillesAtInterval(this, startTime, endTime, intervalMs);
    }

    @Transient
    public float getBloodAlcoholGrams() {
        return AlcoholServiceImpl.getInstance().getBloodAlcoholGrams(this);
    }
}
