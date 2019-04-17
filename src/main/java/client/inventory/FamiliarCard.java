package client.inventory;

import lombok.Data;

@Data
public class FamiliarCard {
    private short skill;
    private byte level, grade;
    private int option1, option2, option3;

    public FamiliarCard() {
    }

    public FamiliarCard(short skill, byte level, byte grade, int option1, int option2, int option3) {
        this.skill = skill;
        this.level = (byte) Math.max(1, level);
        this.grade = grade;
        this.option1 = option1;
        this.option2 = option2;
        this.option3 = option3;
    }

    public FamiliarCard(byte grade) {
        this.grade = grade;
        this.level = 1;
    }

    public void copy(FamiliarCard fc) {
        this.skill = fc.getSkill();
        this.level = fc.getLevel();
        this.grade = fc.getGrade();
        this.option1 = fc.getOption1();
        this.option2 = fc.getOption2();
        this.option3 = fc.getOption3();
    }

    public int getOption(int type) {
        switch (type) {
            case 0:
                return option1;
            case 1:
                return option2;
            case 2:
                return option3;
        }
        return 0;
    }
}
