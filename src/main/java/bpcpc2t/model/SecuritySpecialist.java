package bpcpc2t.model;

import java.util.List;

/**
 * Bezpečnostní specialista – specializace zaměstnance.
 *
 * Dovednost: Vypočítá rizikové skóre na základě počtu spolupracovníků
 * a průměrné kvality spolupráce.
 *
 * Algoritmus rizikového skóre:
 *   – Každá spolupráce přispívá váhovaným rizikem: BAD=3, AVERAGE=2, GOOD=1
 *   – Celkové riziko = součet vah všech spoluprací
 *   – Penalizace za rozsah = ln(počet + 1)  (více kontaktů = větší expozice)
 *   – Skóre = (průměrná váha) × (1 + ln(počet + 1)) × (10 / 3)
 *   – Rozsah výsledku: cca 0 – 100 (čím vyšší, tím rizikovější)
 */
public class SecuritySpecialist extends Employee {

    public SecuritySpecialist(int id, String name, String surname, int birthYear) {
        super(id, name, surname, birthYear);
    }

    @Override
    public String getGroupName() {
        return "Bezpečnostní specialista";
    }

    @Override
    public String getTypeName() {
        return "SecuritySpecialist";
    }

    @Override
    public String getSkillDescription() {
        return "Výpočet rizikového skóre spolupráce";
    }

    /**
     * Vypočítá a vypíše rizikové skóre zaměstnance.
     *
     * @param repository není v této dovednosti využíváno (skóre závisí jen na vlastních datech)
     */
    @Override
    public void executeSkill(EmployeeRepository repository) {
        System.out.println("  ── Výsledek dovednosti: Bezpečnostní specialista ──");

        List<Cooperation> cooperations = getCooperations();

        if (cooperations.isEmpty()) {
            System.out.println("  Žádné záznamy spolupráce – rizikové skóre: 0,00");
            return;
        }

        int badCount = 0, averageCount = 0, goodCount = 0;
        for (Cooperation c : cooperations) {
            switch (c.getLevel()) {
                case BAD     -> badCount++;
                case AVERAGE -> averageCount++;
                case GOOD    -> goodCount++;
            }
        }

        int count             = cooperations.size();
        double avgWeight      = (double)(badCount * 3 + averageCount * 2 + goodCount) / count;
        double exposureFactor = 1.0 + Math.log(count + 1);
        double riskScore      = calculateRiskScore();

        System.out.printf("  Celkem spolupracovníků : %d%n", count);
        System.out.printf("    Špatná spolupráce    : %d%n", badCount);
        System.out.printf("    Průměrná spolupráce  : %d%n", averageCount);
        System.out.printf("    Dobrá spolupráce     : %d%n", goodCount);
        System.out.printf("  Průměrná riziková váha : %.2f / 3.00%n", avgWeight);
        System.out.printf("  Faktor expozice        : %.4f%n", exposureFactor);
        System.out.printf("  ┌─────────────────────────────────────┐%n");
        System.out.printf("  │  RIZIKOVÉ SKÓRE : %6.2f / 100,00  │%n", riskScore);
        System.out.printf("  └─────────────────────────────────────┘%n");
        System.out.printf("  Hodnocení: %s%n", getRiskLabel(riskScore));
    }

    /** Vrátí textové hodnocení rizikového skóre. */
    public double calculateRiskScore() {
        List<Cooperation> cooperations = getCooperations();
        if (cooperations.isEmpty()) return 0.0;

        int totalWeight = cooperations.stream()
                .mapToInt(c -> c.getLevel() == CooperationLevel.BAD ? 3
                             : c.getLevel() == CooperationLevel.AVERAGE ? 2 : 1)
                .sum();

        double avgWeight      = (double) totalWeight / cooperations.size();
        double exposureFactor = 1.0 + Math.log(cooperations.size() + 1);
        return avgWeight * exposureFactor * (10.0 / 3.0);
    }

    private String getRiskLabel(double score) {
        if (score < 15)  return "NÍZKÉ  – minimální bezpečnostní hrozba";
        if (score < 30)  return "STŘEDNÍ – sledovat vývoj spolupráce";
        if (score < 50)  return "ZVÝŠENÉ – doporučena revize kontaktů";
        return "VYSOKÉ – okamžitá bezpečnostní kontrola";
    }
}
