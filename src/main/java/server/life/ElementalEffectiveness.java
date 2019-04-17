package server.life;

public enum ElementalEffectiveness {

    正常(1.0), 免疫(0.0), 增强(0.5), 虚弱(1.5);
    private final double value;

    ElementalEffectiveness(double val) {
        this.value = val;
    }

    public static ElementalEffectiveness getByNumber(int num) {
        switch (num) {
            case 1:
                return 免疫;
            case 2:
                return 增强;
            case 3:
                return 虚弱;
            default:
                throw new IllegalArgumentException("Unkown effectiveness: " + num);
        }
    }

    public double getValue() {
        return value;
    }
}
