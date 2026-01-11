/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package drinkcounter;

import drinkcounter.util.PathInfoHelper;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 *
 * @author Toni
 */
public class PathInfoHelperTest {

    @Test
    public void testSplit(){
        String pathInfo = "/bileet/add-drink";
        String[] split = PathInfoHelper.splitPathInfo(pathInfo);
        String[] expected = new String[]{"bileet", "add-drink"};
        assertArrayEquals(expected, split);
    }
}
