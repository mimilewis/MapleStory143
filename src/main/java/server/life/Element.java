package server.life;

public enum Element {

    NEUTRAL(0), 自然(1), 火(2, true), 冰(3, true), 雷(4), 毒(5), 神圣(6, true), 黑暗(7);
    private final int value;
    private boolean special = false;

    Element(int v) {
        this.value = v;
    }

    Element(int v, boolean special) {
        this.value = v;
        this.special = special;
    }

    public static Element getFromChar(char c) {
        switch (Character.toUpperCase(c)) {
            case 'F':
                return 火;
            case 'I':
                return 冰;
            case 'L':
                return 雷;
            case 'S':
                return 毒;
            case 'H':
                return 神圣;
            case 'P':
                return 自然;
            case 'D':
                return 黑暗;
        }
        throw new IllegalArgumentException("unknown elemnt char " + c);
    }

    public static Element getFromId(int c) {
        for (Element e : Element.values()) {
            if (e.value == c) {
                return e;
            }
        }
        throw new IllegalArgumentException("unknown elemnt id " + c);
    }

    public boolean isSpecial() {
        return special;
    }

    public int getValue() {
        return value;
    }
}
