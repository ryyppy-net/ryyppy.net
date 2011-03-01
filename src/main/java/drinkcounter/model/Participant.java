/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package drinkcounter.model;

import com.google.appengine.api.datastore.KeyFactory;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ManyToOne;
import javax.persistence.Transient;

/**
 *
 * @author Toni
 */
@Entity
public class Participant extends AbstractEntity{
    public enum Sex {
        MALE(0.75f), FEMALE(0.66f);
        public float factor;
        Sex(float factor){
            this.factor = factor;
        }
    }
    public static final int STANDARD_DRINK_ALCOHOL_GRAMS = 12;
    private String name;
    private Party party;
    private float bloodAlcoholGrams;
    private float weight = 70;
    private Sex sex = Sex.MALE;
    private int totalDrinks;
    
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Transient
    public String getId(){
        return KeyFactory.keyToString(getStoreKey());
    }

    @ManyToOne(fetch=FetchType.LAZY)
    public Party getParty() {
        return party;
    }

    public void setParty(Party party) {
        this.party = party;
    }

    public void drink(){
        setBloodAlcoholGrams(getBloodAlcoholGrams() + STANDARD_DRINK_ALCOHOL_GRAMS);
        setTotalDrinks(getTotalDrinks() + 1);
    }

    /**
     * Burns alcohol based on time passed and weight
     * @param hours
     * @return
     */
    public float passTime(float hours){
        if(bloodAlcoholGrams == 0){
            // nothing to burn so no need to calculate anything
            return 0;
        }
        float burnRate = hours * (weight / 10.0f);
        float newAlcoholAmount = bloodAlcoholGrams - burnRate;
        if(newAlcoholAmount < 0){
            newAlcoholAmount = 0;
        }
        float burnedAlcohol = bloodAlcoholGrams - newAlcoholAmount;
        setBloodAlcoholGrams(newAlcoholAmount);
        return burnedAlcohol;
    }

    @Transient
    public float getPromilles(){
        return bloodAlcoholGrams / (sex.factor * weight );
    }

    /**
     * Weight in kilos
     * @return
     */
    public float getWeight() {
        return weight;
    }

    /**
     * Weight in kilos
     * @param weightInKilos
     */
    public void setWeight(float weightInKilos) {
        this.weight = weightInKilos;
    }

    public void setSex(Sex sex) {
        this.sex = sex;
    }

    public Sex getSex() {
        return sex;
    }

    public float getBloodAlcoholGrams() {
        return bloodAlcoholGrams;
    }

    public void setBloodAlcoholGrams(float bloodAlcoholGrams) {
        if(bloodAlcoholGrams > 0 ){
            this.bloodAlcoholGrams = bloodAlcoholGrams;
        }else{
            this.bloodAlcoholGrams = 0;
        }
    }

    public void setTotalDrinks(Integer totalDrinks){
        this.totalDrinks = totalDrinks != null ? totalDrinks : 0;
    }

    public Integer getTotalDrinks(){
        return this.totalDrinks;
    }

}
