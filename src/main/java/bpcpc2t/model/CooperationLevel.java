package bpcpc2t.model;

public enum CooperationLevel {
    BAD("špatná", 1),
    AVERAGE("průměrná", 2),
    GOOD("dobrá", 3);

    private final String displayName;
    private final int score;

    CooperationLevel(String displayName, int score) {
        this.displayName = displayName;
        this.score = score;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getScore() {
        return score;
    }

    public static CooperationLevel fromDisplayName(String name) {
        for (CooperationLevel level : values()) {
            if (level.displayName.equalsIgnoreCase(name) || level.name().equalsIgnoreCase(name)) {
                return level;
            }
        }
        throw new IllegalArgumentException("Neznámá úroveň spolupráce: " + name);
    }

    @Override
    public String toString() {
        return displayName;
    }
}
