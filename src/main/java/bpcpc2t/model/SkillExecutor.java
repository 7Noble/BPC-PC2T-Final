package bpcpc2t.model;

/**
 * Rozhraní pro zaměstnance se specializovanou dovedností.
 * Každá skupina zaměstnanců implementuje vlastní logiku dovednosti.
 */
public interface SkillExecutor {

    /**
     * Spustí specializovanou dovednost zaměstnance.
     *
     * @param repository přístup k databázi zaměstnanců pro vyhledávání kolegů
     */
    void executeSkill(EmployeeRepository repository);

    /**
     * Vrátí popis dovednosti skupiny.
     *
     * @return textový popis dovednosti
     */
    String getSkillDescription();
}
