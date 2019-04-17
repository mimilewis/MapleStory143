package client;

public class CharacterNameAndId {

    private int id;
    private String name, group;
    private boolean visible;

    public CharacterNameAndId() {
    }

    public CharacterNameAndId(int id, String name, String group, boolean visible) {
        super();
        this.id = id;
        this.name = name;
        this.group = group;
        this.visible = visible;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getGroup() {
        return group;
    }

    public boolean isVisible() {
        return visible;
    }
}
