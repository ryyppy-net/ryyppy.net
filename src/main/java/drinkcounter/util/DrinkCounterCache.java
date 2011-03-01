/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package drinkcounter.util;

import java.util.Collections;
import javax.cache.Cache;
import javax.cache.CacheException;
import javax.cache.CacheManager;

/**
 *
 * @author Toni
 */
public class DrinkCounterCache {
    private Cache cache;

    public DrinkCounterCache() {
        try {
            cache = CacheManager.getInstance().getCacheFactory().createCache(Collections.EMPTY_MAP);
        } catch (CacheException ex) {
            throw new RuntimeException(ex);
        }
    }

    public void setPartyHistoryCache(String partyId, byte[] history){
        cache.put("history-"+partyId, history);
    }

    public void clearPartyHistoryCache(String partyId){
        cache.remove("history-"+partyId);
    }

    public byte[] getPartyHistoryCache(String partyId){
        return (byte[]) cache.get("history-" + partyId);
    }

    public void setPartyDrinksCache(String partyId, byte[] history){
        cache.put("display-"+partyId, history);
    }

    public void clearPartyDrinksCache(String partyId){
        cache.remove("display-"+partyId);
    }

    public byte[] getPartyDrinksCache(String partyId){
        return (byte[]) cache.get("display-" + partyId);
    }
}
