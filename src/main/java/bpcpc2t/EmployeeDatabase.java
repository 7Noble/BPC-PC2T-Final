package bpcpc2t;

import bpcpc2t.model.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Hlavní databáze zaměstnanců.
 * Implementuje {@link EmployeeRepository} pro přístup ze skupinových dovedností.
 * Interně využívá {@link LinkedList} jako dynamickou datovou strukturu.
 */
public class EmployeeDatabase implements EmployeeRepository {

    /** Dynamická datová struktura pro ukládání zaměstnanců */
    private final List<Employee> employees = new LinkedList<>();

    // ── CRUD operace ───────────────────────────────────────────────────────────

    public void addEmployee(Employee employee) {
        employees.add(employee);
    }

    /**
     * Odebere zaměstnance a zároveň odstraní všechny vazby na tohoto zaměstnance
     * z ostatních záznamů.
     *
     * @param id ID zaměstnance k odebrání
     * @return true pokud byl zaměstnanec nalezen a odebrán
     */
    public boolean removeEmployee(int id) {
        boolean removed = employees.removeIf(e -> e.getId() == id);
        if (removed) {
            // Odstraní všechny reference na odebraného zaměstnance
            for (Employee e : employees) {
                e.removeCooperationWith(id);
            }
        }
        return removed;
    }

    @Override
    public Employee findById(int id) {
        return employees.stream()
                .filter(e -> e.getId() == id)
                .findFirst()
                .orElse(null);
    }

    @Override
    public List<Employee> getAllEmployees() {
        return Collections.unmodifiableList(employees);
    }

    public boolean isEmpty() {
        return employees.isEmpty();
    }

    public int size() {
        return employees.size();
    }

    // ── Spolupráce ─────────────────────────────────────────────────────────────

    /**
     * Přidá nebo aktualizuje spolupráci mezi dvěma zaměstnanci.
     *
     * @return false pokud některý z ID neexistuje
     */
    public boolean addCooperation(int employeeId, int colleagueId, CooperationLevel level) {
        if (employeeId == colleagueId) return false;
        Employee emp = findById(employeeId);
        Employee col = findById(colleagueId);
        if (emp == null || col == null) return false;

        emp.addCooperation(new Cooperation(colleagueId, level));
        return true;
    }

    public boolean removeCooperation(int employeeId, int colleagueId) {
        Employee emp = findById(employeeId);
        if (emp == null || !emp.hasCooperationWith(colleagueId)) return false;
        emp.removeCooperationWith(colleagueId);
        return true;
    }

    public boolean updateEmployee(int id, String name, String surname, int birthYear) {
        Employee emp = findById(id);
        if (emp == null) return false;
        emp.setName(name);
        emp.setSurname(surname);
        emp.setBirthYear(birthYear);
        return true;
    }

    // ── Výpisy ─────────────────────────────────────────────────────────────────

    /**
     * Vrátí abecední výpis zaměstnanců seřazených podle příjmení, seskupených dle skupin.
     *
     * @return mapa skupinový název → seřazený seznam zaměstnanců
     */
    public Map<String, List<Employee>> getAlphabeticByGroup() {
        return employees.stream()
                .sorted(Comparator.comparing(Employee::getSurname)
                        .thenComparing(Employee::getName))
                .collect(Collectors.groupingBy(
                        Employee::getGroupName,
                        TreeMap::new,
                        Collectors.toList()
                ));
    }

    /**
     * Vrátí počet zaměstnanců v každé skupině.
     */
    public Map<String, Long> getCountByGroup() {
        return employees.stream()
                .collect(Collectors.groupingBy(Employee::getGroupName, Collectors.counting()));
    }

    // ── Statistiky ─────────────────────────────────────────────────────────────

    /**
     * Vrátí převažující úroveň spolupráce napříč celou databází.
     */
    public CooperationLevel getOverallDominantLevel() {
        Map<CooperationLevel, Long> counts = employees.stream()
                .flatMap(e -> e.getCooperations().stream())
                .collect(Collectors.groupingBy(Cooperation::getLevel, Collectors.counting()));

        if (counts.isEmpty()) return null;

        return counts.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);
    }

    /**
     * Vrátí zaměstnance s nejvíce vazbami (spolupracovníky).
     */
    public Employee getEmployeeWithMostConnections() {
        return employees.stream()
                .max(Comparator.comparingInt(e -> e.getCooperations().size()))
                .orElse(null);
    }

    /**
     * Vrátí celkový počet záznamů spolupráce v databázi.
     */
    public int getTotalCooperationsCount() {
        return employees.stream()
                .mapToInt(e -> e.getCooperations().size())
                .sum();
    }
}
