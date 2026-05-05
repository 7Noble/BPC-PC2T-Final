package bpcpc2t.model;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Datový analytik – specializace zaměstnance.
 *
 * Dovednost: Určí, se kterým spolupracovníkem má analytik nejvíce
 * společných spolupracovníků (průnik kolegů).
 */
public class DataAnalyst extends Employee {

    public DataAnalyst(int id, String name, String surname, int birthYear) {
        super(id, name, surname, birthYear);
    }

    @Override
    public String getGroupName() {
        return "Datový analytik";
    }

    @Override
    public String getTypeName() {
        return "DataAnalyst";
    }

    @Override
    public String getSkillDescription() {
        return "Nalezení spolupracovníka s nejvíce společnými kolegy";
    }

    /**
     * Dovednost: pro každého z vlastních spolupracovníků spočítá průnik
     * jejich kolegů s vlastními kolegy a vypíše nejlepší shodu.
     */
    @Override
    public void executeSkill(EmployeeRepository repository) {
        List<Cooperation> myCooperations = getCooperations();

        if (myCooperations.isEmpty()) {
            System.out.println("  Nemáte žádné spolupracovníky – dovednost nelze spustit.");
            return;
        }

        // Množina ID vlastních spolupracovníků
        Set<Integer> myColleagueIds = myCooperations.stream()
                .map(Cooperation::getColleagueId)
                .collect(Collectors.toSet());

        int bestCount = -1;
        Employee bestMatch = null;

        for (Cooperation c : myCooperations) {
            Employee colleague = repository.findById(c.getColleagueId());
            if (colleague == null) continue;

            // Kolegové tohoto spolupracovníka (bez mě samotného)
            Set<Integer> theirIds = colleague.getCooperations().stream()
                    .map(Cooperation::getColleagueId)
                    .filter(id -> id != this.getId())
                    .collect(Collectors.toSet());

            // Průnik: kolegové, kteří jsou zároveň i moji kolegové
            long commonCount = theirIds.stream()
                    .filter(myColleagueIds::contains)
                    .count();

            if (commonCount > bestCount) {
                bestCount = (int) commonCount;
                bestMatch = colleague;
            }
        }

        System.out.println("  ── Výsledek dovednosti: Datový analytik ──");
        if (bestMatch != null && bestCount > 0) {
            System.out.printf("  Spolupracovník s nejvíce společnými kolegy:%n");
            System.out.printf("    %s (ID: %d)%n", bestMatch.getFullName(), bestMatch.getId());
            System.out.printf("    Společných spolupracovníků: %d%n", bestCount);
        } else {
            System.out.println("  Žádný ze spolupracovníků nemá společné kolegy s vámi.");
        }
    }
}
