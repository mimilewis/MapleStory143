package client.skills;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;

public class SkillMacro implements Serializable {

    private static final long serialVersionUID = -63413738569L;
    private int macroId;
    private int skill1;
    private int skill2;
    private int skill3;
    private String name;
    private int shout;
    private int position;

    @JsonCreator
    public SkillMacro(@JsonProperty("skill1") int skill1, @JsonProperty("skill2") int skill2, @JsonProperty("skill3") int skill3, @JsonProperty("name") String name, @JsonProperty("shout") int shout, @JsonProperty("position") int position) {
        this.skill1 = skill1;
        this.skill2 = skill2;
        this.skill3 = skill3;
        this.name = name;
        this.shout = shout;
        this.position = position;
    }

    public int getMacroId() {
        return macroId;
    }

    public void setMacroId(int macroId) {
        this.macroId = macroId;
    }

    public int getSkill1() {
        return skill1;
    }

    public void setSkill1(int skill1) {
        this.skill1 = skill1;
    }

    public int getSkill2() {
        return skill2;
    }

    public void setSkill2(int skill2) {
        this.skill2 = skill2;
    }

    public int getSkill3() {
        return skill3;
    }

    public void setSkill3(int skill3) {
        this.skill3 = skill3;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getShout() {
        return shout;
    }

    public void setShout(int shout) {
        this.shout = shout;
    }

    public int getPosition() {
        return position;
    }

    public void setPosition(int position) {
        this.position = position;
    }
}