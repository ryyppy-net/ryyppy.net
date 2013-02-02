package drinkcounter.model;

/**
 * Stub User class that represents a friend.
 */
public class Friend {
    private Integer id;
    private String name;
    private String gravatarUrl;

    public Friend(Integer id, String name, String gravatarUrl) {
        this.id = id;
        this.name = name;
        this.gravatarUrl = gravatarUrl;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getGravatarUrl() {
        return gravatarUrl;
    }

    public void setGravatarUrl(String gravatarUrl) {
        this.gravatarUrl = gravatarUrl;
    }
}
