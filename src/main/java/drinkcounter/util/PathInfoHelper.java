package drinkcounter.util;

import java.util.Arrays;

/**
 *
 * @author Toni
 */
public class PathInfoHelper {
    public static String[] splitPathInfo(String pathInfo){
        String[] splitted = pathInfo.split("/");
        if(splitted.length < 2){
            return new String[] {};
        }
        return Arrays.copyOfRange(splitted, 1, splitted.length);
    }
}
