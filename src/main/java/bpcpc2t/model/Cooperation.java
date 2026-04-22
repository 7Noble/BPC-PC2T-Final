package bpcpc2t.model;

public class Cooperation {

    private final int colleagueId;
    private CooperationLevel level;

    public Cooperation(int colleagueId, CooperationLevel level) {
        this.colleagueId = colleagueId;
        this.level = level;
    }

    public int getColleagueId() {
        return colleagueId;
    }

    public CooperationLevel getLevel() {
        return level;
    }

    public void setLevel(CooperationLevel level) {
        this.level = level;
    }

    @Override
    public String toString() {
        return "kolega ID=" + colleagueId + " [" + level.getDisplayName() + "]";
    }
}
