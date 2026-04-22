package bpcpc2t.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Abstraktní třída reprezentující zaměstnance technologické firmy.
 * Každý zaměstnanec patří do skupiny a má specializovanou dovednost.
 */
public abstract class Employee implements SkillExecutor {

    private static int nextId = 1;

    private final int id;
    private String name;
    private String surname;
    private int birthYear;

    /** Dynamická datová struktura – seznam spolupracovníků */
    private final List<Cooperation> cooperations;

    protected Employee(int id, String name, String surname, int birthYear) {
        this.id = id;
        this.name = name;
        this.surname = surname;
        this.birthYear = birthYear;
        this.cooperations = new ArrayList<>();
        updateNextId(id);
    }

    // ── ID management ──────────────────────────────────────────────────────────

    public static int generateId() {
        return nextId++;
    }

    public static void updateNextId(int usedId) {
        if (usedId >= nextId) {
            nextId = usedId + 1;
        }
    }

    // ── Abstraktní metody ──────────────────────────────────────────────────────

    /** Vrátí název skupiny zaměstnance. */
    public abstract String getGroupName();

    /** Vrátí typ skupiny jako řetězec pro serializaci (např. "DataAnalyst"). */
    public abstract String getTypeName();

    // ── Spolupráce ─────────────────────────────────────────────────────────────

    public void addCooperation(Cooperation cooperation) {
        cooperations.removeIf(c -> c.getColleagueId() == cooperation.getColleagueId());
        cooperations.add(cooperation);
    }

    public void removeCooperationWith(int colleagueId) {
        cooperations.removeIf(c -> c.getColleagueId() == colleagueId);
    }

    public boolean hasCooperationWith(int colleagueId) {
        return cooperations.stream().anyMatch(c -> c.getColleagueId() == colleagueId);
    }

    public List<Cooperation> getCooperations() {
        return cooperations;
    }

    // ── Statistiky ─────────────────────────────────────────────────────────────

    /**
     * Vrátí převažující úroveň spolupráce, nebo null pokud nejsou žádné záznamy.
     */
    public CooperationLevel getDominantCooperationLevel() {
        if (cooperations.isEmpty()) return null;

        Map<CooperationLevel, Long> counts = cooperations.stream()
                .collect(Collectors.groupingBy(Cooperation::getLevel, Collectors.counting()));

        return counts.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);
    }

    /**
     * Vrátí průměrné skóre kvality spolupráce (BAD=1, AVERAGE=2, GOOD=3).
     */
    public double getAverageCooperationScore() {
        if (cooperations.isEmpty()) return 0.0;
        return cooperations.stream()
                .mapToInt(c -> c.getLevel().getScore())
                .average()
                .orElse(0.0);
    }

    // ── Základní informace ─────────────────────────────────────────────────────

    public int getId() { return id; }
    public String getName() { return name; }
    public String getSurname() { return surname; }
    public int getBirthYear() { return birthYear; }
    public String getFullName() { return name + " " + surname; }

    public void setName(String name) { this.name = name; }
    public void setSurname(String surname) { this.surname = surname; }
    public void setBirthYear(int birthYear) { this.birthYear = birthYear; }

    // ── Výpis ──────────────────────────────────────────────────────────────────

    public String getBasicInfo() {
        return String.format(
                "ID: %d | %s %s | nar. %d | skupina: %s | spolupracovníků: %d",
                id, name, surname, birthYear, getGroupName(), cooperations.size()
        );
    }

    public String getDetailedInfo() {
        StringBuilder sb = new StringBuilder();
        sb.append("══════════════════════════════════════════\n");
        sb.append(String.format("  Zaměstnanec #%d%n", id));
        sb.append("══════════════════════════════════════════\n");
        sb.append(String.format("  Jméno:       %s %s%n", name, surname));
        sb.append(String.format("  Rok narození:%d%n", birthYear));
        sb.append(String.format("  Skupina:     %s%n", getGroupName()));
        sb.append(String.format("  Dovednost:   %s%n", getSkillDescription()));
        sb.append(String.format("  Spolupracovníků: %d%n", cooperations.size()));

        if (!cooperations.isEmpty()) {
            CooperationLevel dominant = getDominantCooperationLevel();
            sb.append(String.format("  Převažující kvalita: %s%n",
                    dominant != null ? dominant.getDisplayName() : "–"));
            sb.append(String.format("  Průměrné skóre: %.2f / 3.00%n", getAverageCooperationScore()));
            sb.append("  Seznam spolupráce:%n".formatted());
            for (Cooperation c : cooperations) {
                sb.append("    • ").append(c).append("\n");
            }
        }
        return sb.toString();
    }

    @Override
    public String toString() {
        return String.format("[%s] %s (ID: %d, nar. %d)", getGroupName(), getFullName(), id, birthYear);
    }
}
