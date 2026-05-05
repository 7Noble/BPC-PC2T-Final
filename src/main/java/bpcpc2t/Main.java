package bpcpc2t;

import bpcpc2t.model.*;
import bpcpc2t.storage.FileManager;
import bpcpc2t.storage.SqliteManager;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

public class Main {

    private static final EmployeeDatabase db = new EmployeeDatabase();
    private static final Scanner scanner = new Scanner(System.in, "UTF-8");

    public static void main(String[] args) {
        System.out.println("╔══════════════════════════════════════════╗");
        System.out.println("║   Databázový systém zaměstnanců  v1.7    ║");
        System.out.println("╚══════════════════════════════════════════╝");

        loadFromSqlite();

        boolean running = true;
        while (running) {
            printMenu();
            int choice = readInt("Volba: ");
            System.out.println();

            switch (choice) {
                case 1  -> addEmployee();
                case 2  -> addCooperation();
                case 3  -> removeEmployee();
                case 4  -> findEmployeeById();
                case 5  -> runSkill();
                case 6  -> printAlphabeticList();
                case 7  -> printStatistics();
                case 8  -> printGroupCounts();
                case 9  -> saveEmployeeToFile();
                case 10 -> loadEmployeeFromFile();
                case 11 -> removeCooperation();
                case 12 -> editEmployee();
                case 0  -> { saveToSqliteAndExit(); running = false; }
                default -> System.out.println("  Neplatná volba, zkuste znovu.\n");
            }
        }

        scanner.close();
    }

    private static void printMenu() {
        System.out.println("──────────────────────────────────────────");
        System.out.println(" 1. Přidání zaměstnance");
        System.out.println(" 2. Přidání spolupráce");
        System.out.println(" 3. Odebrání zaměstnance");
        System.out.println(" 4. Vyhledání zaměstnance dle ID");
        System.out.println(" 5. Spuštění dovednosti zaměstnance");
        System.out.println(" 6. Abecední výpis zaměstnanců dle skupin");
        System.out.println(" 7. Statistiky");
        System.out.println(" 8. Počet zaměstnanců ve skupinách");
        System.out.println(" 9. Uložení zaměstnance do souboru");
        System.out.println("10. Načtení zaměstnance ze souboru");
        System.out.println("11. Odebrání spolupráce");
        System.out.println("12. Úprava zaměstnance");
        System.out.println(" 0. Uložit a ukončit");
        System.out.println("──────────────────────────────────────────");
    }

    private static void addEmployee() {
        System.out.println("  Vyberte skupinu:");
        System.out.println("    1 – Datový analytik");
        System.out.println("    2 – Bezpečnostní specialista");
        int groupChoice = readInt("  Skupina: ");

        if (groupChoice != 1 && groupChoice != 2) {
            System.out.println("  Neplatná skupina.\n");
            return;
        }

        String name      = readString("  Jméno:     ");
        String surname   = readString("  Příjmení:  ");
        int    birthYear = readYear("  Rok nar.:  ");

        int id = Employee.generateId();
        Employee employee = (groupChoice == 1)
                ? new DataAnalyst(id, name, surname, birthYear)
                : new SecuritySpecialist(id, name, surname, birthYear);

        db.addEmployee(employee);
        System.out.printf("  Zaměstnanec přidán s ID: %d%n%n", id);
    }

    private static void addCooperation() {
        int employeeId  = readInt("  ID zaměstnance:   ");
        int colleagueId = readInt("  ID spolupracovníka: ");

        System.out.println("  Úroveň spolupráce: 1=špatná  2=průměrná  3=dobrá");
        int levelChoice = readInt("  Volba: ");

        CooperationLevel level = switch (levelChoice) {
            case 1 -> CooperationLevel.BAD;
            case 2 -> CooperationLevel.AVERAGE;
            case 3 -> CooperationLevel.GOOD;
            default -> { System.out.println("  Neplatná úroveň."); yield null; }
        };

        if (level != null) {
            boolean ok = db.addCooperation(employeeId, colleagueId, level);
            System.out.println(ok
                    ? "  Spolupráce přidána.\n"
                    : "  Chyba: zaměstnanec nebo kolega neexistuje, nebo stejná ID.\n");
        }
    }

    private static void removeEmployee() {
        int id = readInt("  ID zaměstnance k odebrání: ");
        boolean removed = db.removeEmployee(id);
        System.out.println(removed
                ? "  Zaměstnanec odebrán včetně všech vazeb.\n"
                : "  Zaměstnanec s ID " + id + " nebyl nalezen.\n");
    }

    private static void findEmployeeById() {
        int id = readInt("  ID zaměstnance: ");
        Employee emp = db.findById(id);
        if (emp == null) {
            System.out.println("  Zaměstnanec s ID " + id + " nebyl nalezen.\n");
        } else {
            System.out.println(emp.getDetailedInfo());
        }
    }

    private static void runSkill() {
        int id = readInt("  ID zaměstnance: ");
        Employee emp = db.findById(id);
        if (emp == null) {
            System.out.println("  Zaměstnanec s ID " + id + " nebyl nalezen.\n");
            return;
        }
        System.out.printf("  Spouštím dovednost pro: %s%n", emp.getFullName());
        emp.executeSkill(db);
        System.out.println();
    }

    private static void printAlphabeticList() {
        if (db.isEmpty()) {
            System.out.println("  Databáze je prázdná.\n");
            return;
        }

        Map<String, List<Employee>> grouped = db.getAlphabeticByGroup();
        System.out.println("  ── Abecední výpis zaměstnanců dle skupin ──");
        grouped.forEach((group, employees) -> {
            System.out.printf("%n  [%s]%n", group);
            employees.forEach(e -> System.out.printf("    %3d. %s %s (nar. %d, kolegů: %d)%n",
                    e.getId(), e.getSurname(), e.getName(),
                    e.getBirthYear(), e.getCooperations().size()));
        });
        System.out.println();
    }

    private static void printStatistics() {
        System.out.println("  ── Statistiky databáze ──");

        if (db.isEmpty()) {
            System.out.println("  Databáze je prázdná.\n");
            return;
        }

        CooperationLevel dominant = db.getOverallDominantLevel();
        System.out.printf("  Převažující kvalita spolupráce: %s%n",
                dominant != null ? dominant.getDisplayName() : "žádná data");

        Employee mostConnected = db.getEmployeeWithMostConnections();
        if (mostConnected != null) {
            System.out.printf("  Zaměstnanec s nejvíce vazbami: %s (ID: %d, vazeb: %d)%n",
                    mostConnected.getFullName(),
                    mostConnected.getId(),
                    mostConnected.getCooperations().size());
        }

        System.out.printf("  Celkem zaměstnanců: %d%n", db.size());
        System.out.printf("  Celkem vazeb:       %d%n", db.getTotalCooperationsCount());
        System.out.println();
    }

    private static void printGroupCounts() {
        System.out.println("  ── Počet zaměstnanců ve skupinách ──");
        Map<String, Long> counts = db.getCountByGroup();
        if (counts.isEmpty()) {
            System.out.println("  Databáze je prázdná.\n");
            return;
        }
        counts.forEach((group, count) ->
                System.out.printf("  %-30s : %d%n", group, count));
        System.out.printf("  %-30s : %d%n", "CELKEM", db.size());
        System.out.println();
    }

    private static void saveEmployeeToFile() {
        int id = readInt("  ID zaměstnance: ");
        Employee emp = db.findById(id);
        if (emp == null) {
            System.out.println("  Zaměstnanec s ID " + id + " nebyl nalezen.\n");
            return;
        }

        String defaultFile = "employee_" + id + ".emp";
        System.out.print("  Soubor [" + defaultFile + "]: ");
        String input = scanner.nextLine().trim();
        String filePath = input.isEmpty() ? defaultFile : input;

        try {
            FileManager.saveEmployee(emp, filePath);
            System.out.printf("  Zaměstnanec uložen do: %s%n%n", filePath);
        } catch (IOException e) {
            System.out.println("  Chyba při ukládání: " + e.getMessage() + "\n");
        }
    }

    private static void loadEmployeeFromFile() {
        String filePath = readString("  Cesta k souboru: ");

        try {
            Employee loaded = FileManager.loadEmployee(filePath);

            if (db.findById(loaded.getId()) != null) {
                db.removeEmployee(loaded.getId());
                System.out.printf("  Existující záznam ID=%d byl přepsán.%n", loaded.getId());
            }

            db.addEmployee(loaded);
            System.out.printf("  Načten: %s (ID: %d)%n%n", loaded.getFullName(), loaded.getId());
        } catch (IOException | IllegalStateException | IllegalArgumentException e) {
            System.out.println("  Chyba při načítání: " + e.getMessage() + "\n");
        }
    }

    private static void removeCooperation() {
        int employeeId  = readInt("  ID zaměstnance:   ");
        int colleagueId = readInt("  ID spolupracovníka: ");
        boolean ok = db.removeCooperation(employeeId, colleagueId);
        System.out.println(ok
                ? "  Spolupráce odebrána.\n"
                : "  Záznam nenalezen – zkontrolujte obě ID.\n");
    }

    private static void editEmployee() {
        int id = readInt("  ID zaměstnance: ");
        Employee emp = db.findById(id);
        if (emp == null) {
            System.out.println("  Zaměstnanec s ID " + id + " nebyl nalezen.\n");
            return;
        }

        System.out.printf("  Aktuální data: %s %s, nar. %d%n",
                emp.getName(), emp.getSurname(), emp.getBirthYear());
        System.out.println("  (Ponechte prázdné pro zachování stávající hodnoty)");

        System.out.print("  Jméno     [" + emp.getName() + "]: ");
        String name = scanner.nextLine().trim();
        if (name.isEmpty()) name = emp.getName();

        System.out.print("  Příjmení  [" + emp.getSurname() + "]: ");
        String surname = scanner.nextLine().trim();
        if (surname.isEmpty()) surname = emp.getSurname();

        System.out.print("  Rok nar.  [" + emp.getBirthYear() + "]: ");
        String yearInput = scanner.nextLine().trim();
        int birthYear = emp.getBirthYear();
        if (!yearInput.isEmpty()) {
            try {
                int parsed = Integer.parseInt(yearInput);
                if (parsed >= 1900 && parsed <= 2010) {
                    birthYear = parsed;
                } else {
                    System.out.println("  Neplatný rok – zachována původní hodnota.");
                }
            } catch (NumberFormatException e) {
                System.out.println("  Neplatný vstup – zachována původní hodnota.");
            }
        }

        db.updateEmployee(id, name, surname, birthYear);
        System.out.println("  Zaměstnanec aktualizován.\n");
    }

    private static void loadFromSqlite() {
        try {
            SqliteManager.initDatabase();
            List<Employee> loaded = SqliteManager.loadAll();
            if (loaded.isEmpty()) {
                System.out.println("  [DB] Žádná záložní data – začínáme s prázdnou databází.\n");
            } else {
                loaded.forEach(db::addEmployee);
                System.out.printf("  [DB] Načteno %d zaměstnanců ze zálohy SQLite.%n%n", loaded.size());
            }
        } catch (Exception e) {
            System.out.println("  [DB] SQLite není dostupné – pokračujeme bez zálohy.\n");
        }
    }

    private static void saveToSqliteAndExit() {
        try {
            boolean ok = SqliteManager.saveAll(db.getAllEmployees());
            System.out.println(ok
                    ? "\n  [DB] Data uložena do SQLite. Nashledanou!"
                    : "\n  [DB] Varování: SQLite záloha se nezdařila.");
        } catch (Exception e) {
            System.out.println("\n  [DB] SQLite není dostupné – data nebyla zálohována.");
        }
    }

    private static int readInt(String prompt) {
        while (true) {
            System.out.print(prompt);
            String line = scanner.nextLine().trim();
            try {
                return Integer.parseInt(line);
            } catch (NumberFormatException e) {
                System.out.println("  Zadejte celé číslo.");
            }
        }
    }

    private static String readString(String prompt) {
        while (true) {
            System.out.print(prompt);
            String line = scanner.nextLine().trim();
            if (!line.isEmpty()) return line;
            System.out.println("  Pole nesmí být prázdné.");
        }
    }

    private static int readYear(String prompt) {
        while (true) {
            int year = readInt(prompt);
            if (year >= 1900 && year <= 2010) return year;
            System.out.println("  Zadejte reálný rok narození (1900–2010).");
        }
    }
}
