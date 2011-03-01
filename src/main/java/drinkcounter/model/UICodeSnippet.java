/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package drinkcounter.model;

import javax.persistence.Entity;
import javax.persistence.Lob;

/**
 *
 * @author Toni
 */
@Entity
public class UICodeSnippet extends AbstractEntity{
    private String snippet;
    private String snippetKey;

    public String getSnippetKey() {
        return snippetKey;
    }

    public void setSnippetKey(String snippetKey) {
        this.snippetKey = snippetKey;
    }

    @Lob
    public String getSnippet() {
        return snippet;
    }

    public void setSnippet(String snippet) {
        this.snippet = snippet;
    }


}
