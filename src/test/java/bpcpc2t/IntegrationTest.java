package bpcpc2t;

import bpcpc2t.model.*;
import bpcpc2t.storage.FileManager;
import bpcpc2t.storage.SqliteManager;

import java.io.File;
import java.util.List;
import java.util.Map;

public class IntegrationTest {

    private static int passed = 0;
    private static int failed = 0;

    public static void main(String[] args) throws Exception {
        cleanup();

        testEmployeeIdGeneration();
        testAddEmployees();
        testCooperationCRUD();
        testRemoveCooperation();
        testRemoveEmployeeCascade();
        testFindById();
        testStatistics();
        testGroupCounts();
        testAlphabeticByGroup();
        testDataAnalystSkill();
        testDataAnalystNoMatch();
        testSecuritySpecialistSkill();
        testFileRoundtrip();
        testSqliteRoundtrip();
        testEditEmployee();
        testFactoryUnknownType();

        System.out.println();
        System.out.println("==========================================");
        System.out.printf("VYSLEDOK: %d uspesnych, %d neuspesnych%n", passed, failed);
        System.out.println("==========================================");

        cleanup();
        if (failed > 0) System.exit(1);
    }

    static void testEmployeeIdGeneration() {
        section("ID generation");
        resetIdCounter();
        DataAnalyst a = new DataAnalyst(Employee.generateId(), "A", "A", 1990);
        DataAnalyst b = new DataAnalyst(Employee.generateId(), "B", "B", 1990);
        check(a.getId() == 1, "First generated ID = 1");
        check(b.getId() == 2, "Second generated ID = 2");
        Employee.updateNextId(100);
        DataAnalyst c = new DataAnalyst(Employee.generateId(), "C", "C", 1990);
        check(c.getId() == 101, "After updateNextId(100), next = 101");
    }

    static void testAddEmployees() {
        section("Add employees");
        resetIdCounter();
        EmployeeDatabase db = new EmployeeDatabase();
        db.addEmployee(new DataAnalyst(Employee.generateId(), "Jan", "Novak", 1990));
        db.addEmployee(new DataAnalyst(Employee.generateId(), "Petr", "Svoboda", 1985));
        db.addEmployee(new SecuritySpecialist(Employee.generateId(), "Eva", "Dvorakova", 1992));
        db.addEmployee(new SecuritySpecialist(Employee.generateId(), "Tomas", "Cerny", 1988));
        check(db.size() == 4, "Database has 4 employees");
        check(db.findById(1) != null, "ID 1 exists");
        check(db.findById(99) == null, "ID 99 does not exist");
        check(db.findById(1) instanceof DataAnalyst, "ID 1 is DataAnalyst");
        check(db.findById(3) instanceof SecuritySpecialist, "ID 3 is SecuritySpecialist");
    }

    static void testCooperationCRUD() {
        section("Cooperation add");
        resetIdCounter();
        EmployeeDatabase db = makeBasicDb();

        check(db.addCooperation(1, 2, CooperationLevel.GOOD), "Add 1->2 GOOD");
        check(db.addCooperation(1, 3, CooperationLevel.AVERAGE), "Add 1->3 AVERAGE");
        check(!db.addCooperation(1, 1, CooperationLevel.GOOD), "Self-cooperation rejected");
        check(!db.addCooperation(1, 99, CooperationLevel.GOOD), "Missing colleague rejected");
        check(!db.addCooperation(99, 1, CooperationLevel.GOOD), "Missing employee rejected");

        Employee one = db.findById(1);
        check(one.getCooperations().size() == 2, "Employee 1 has 2 cooperations");
        check(one.hasCooperationWith(2), "Employee 1 cooperates with 2");

        db.addCooperation(1, 2, CooperationLevel.BAD);
        check(one.getCooperations().size() == 2, "Re-add does not duplicate");
        CooperationLevel level = one.getCooperations().stream()
                .filter(c -> c.getColleagueId() == 2)
                .findFirst().get().getLevel();
        check(level == CooperationLevel.BAD, "Re-add updates level to BAD");
    }

    static void testRemoveCooperation() {
        section("Remove cooperation");
        resetIdCounter();
        EmployeeDatabase db = makeBasicDb();
        db.addCooperation(1, 2, CooperationLevel.GOOD);
        db.addCooperation(1, 3, CooperationLevel.GOOD);

        check(db.removeCooperation(1, 2), "Remove 1->2");
        check(!db.findById(1).hasCooperationWith(2), "1 no longer cooperates with 2");
        check(db.findById(1).hasCooperationWith(3), "1 still cooperates with 3");
        check(!db.removeCooperation(1, 2), "Remove non-existing returns false");
        check(!db.removeCooperation(99, 1), "Remove from missing employee returns false");
    }

    static void testRemoveEmployeeCascade() {
        section("Remove employee with cascade");
        resetIdCounter();
        EmployeeDatabase db = makeBasicDb();
        db.addCooperation(2, 1, CooperationLevel.GOOD);
        db.addCooperation(3, 1, CooperationLevel.AVERAGE);
        db.addCooperation(4, 1, CooperationLevel.BAD);

        check(db.removeEmployee(1), "Remove employee 1");
        check(db.findById(1) == null, "ID 1 gone");
        check(db.size() == 3, "Size now 3");
        check(!db.findById(2).hasCooperationWith(1), "2's link to 1 cascaded");
        check(!db.findById(3).hasCooperationWith(1), "3's link to 1 cascaded");
        check(!db.findById(4).hasCooperationWith(1), "4's link to 1 cascaded");
        check(!db.removeEmployee(99), "Remove missing returns false");
    }

    static void testFindById() {
        section("Find by ID");
        resetIdCounter();
        EmployeeDatabase db = makeBasicDb();
        Employee e = db.findById(1);
        check(e != null, "Found employee 1");
        check(e.getName().equals("Jan"), "Name is Jan");
        check(e.getSurname().equals("Novak"), "Surname is Novak");
        check(e.getBirthYear() == 1990, "BirthYear 1990");
        String detailed = e.getDetailedInfo();
        check(detailed.contains("Jan"), "DetailedInfo contains name");
        check(detailed.contains("Datový analytik"), "DetailedInfo contains group");
    }

    static void testStatistics() {
        section("Statistics");
        resetIdCounter();
        EmployeeDatabase db = makeBasicDb();
        db.addCooperation(1, 2, CooperationLevel.GOOD);
        db.addCooperation(1, 3, CooperationLevel.GOOD);
        db.addCooperation(1, 4, CooperationLevel.GOOD);
        db.addCooperation(2, 1, CooperationLevel.BAD);
        db.addCooperation(3, 1, CooperationLevel.AVERAGE);

        check(db.getOverallDominantLevel() == CooperationLevel.GOOD, "Dominant level GOOD");
        check(db.getEmployeeWithMostConnections().getId() == 1, "Most connections: emp 1");
        check(db.getTotalCooperationsCount() == 5, "Total = 5");

        EmployeeDatabase empty = new EmployeeDatabase();
        check(empty.getOverallDominantLevel() == null, "Empty dominant = null");
        check(empty.getEmployeeWithMostConnections() == null, "Empty most connections = null");
    }

    static void testGroupCounts() {
        section("Group counts");
        resetIdCounter();
        EmployeeDatabase db = makeBasicDb();
        Map<String, Long> counts = db.getCountByGroup();
        check(counts.get("Datový analytik") == 2L, "2 DataAnalysts");
        check(counts.get("Bezpečnostní specialista") == 2L, "2 SecuritySpecialists");
    }

    static void testAlphabeticByGroup() {
        section("Alphabetic listing");
        resetIdCounter();
        EmployeeDatabase db = makeBasicDb();
        Map<String, List<Employee>> grouped = db.getAlphabeticByGroup();

        List<String> groupNames = grouped.keySet().stream().toList();
        check(groupNames.get(0).equals("Bezpečnostní specialista"),
                "First group alphabetically: Bezpečnostní specialista");
        check(groupNames.get(1).equals("Datový analytik"),
                "Second group: Datový analytik");

        List<Employee> sec = grouped.get("Bezpečnostní specialista");
        check(sec.get(0).getSurname().equals("Cerny"), "Cerny before Dvorakova");
        check(sec.get(1).getSurname().equals("Dvorakova"), "Dvorakova second");
        List<Employee> da = grouped.get("Datový analytik");
        check(da.get(0).getSurname().equals("Novak"), "Novak before Svoboda");
    }

    static void testDataAnalystSkill() {
        section("DataAnalyst skill (with match)");
        resetIdCounter();
        EmployeeDatabase db = makeBasicDb();

        db.addCooperation(1, 2, CooperationLevel.GOOD);
        db.addCooperation(1, 3, CooperationLevel.GOOD);
        db.addCooperation(1, 4, CooperationLevel.GOOD);
        db.addCooperation(2, 3, CooperationLevel.GOOD);
        db.addCooperation(2, 4, CooperationLevel.GOOD);
        db.addCooperation(3, 4, CooperationLevel.GOOD);

        DataAnalyst a = (DataAnalyst) db.findById(1);
        String out = capture(() -> a.executeSkill(db));
        check(out.contains("Petr Svoboda"), "Best match is Petr Svoboda");
        check(out.contains("Společných spolupracovníků: 2"), "2 common colleagues");
    }

    static void testDataAnalystNoMatch() {
        section("DataAnalyst skill (no common)");
        resetIdCounter();
        EmployeeDatabase db = makeBasicDb();
        db.addCooperation(1, 2, CooperationLevel.GOOD);

        DataAnalyst a = (DataAnalyst) db.findById(1);
        String out = capture(() -> a.executeSkill(db));
        check(out.contains("Žádný ze spolupracovníků"), "No-match message shown");
    }

    static void testSecuritySpecialistSkill() {
        section("SecuritySpecialist skill");
        resetIdCounter();
        EmployeeDatabase db = makeBasicDb();
        db.addCooperation(3, 1, CooperationLevel.BAD);
        db.addCooperation(3, 2, CooperationLevel.AVERAGE);
        db.addCooperation(3, 4, CooperationLevel.GOOD);

        SecuritySpecialist s = (SecuritySpecialist) db.findById(3);
        double score = s.calculateRiskScore();
        check(score > 0 && score < 100, "Score in range (0-100)");

        String out = capture(() -> s.executeSkill(db));
        check(out.contains("RIZIKOVÉ SKÓRE"), "Risk score header");
        check(out.contains("Hodnocení"), "Has rating label");

        EmployeeDatabase db2 = new EmployeeDatabase();
        db2.addEmployee(new SecuritySpecialist(Employee.generateId(), "Solo", "Solo", 1990));
        SecuritySpecialist solo = (SecuritySpecialist) db2.findById(5);
        check(solo.calculateRiskScore() == 0.0, "Empty risk score = 0");
        String emptyOut = capture(() -> solo.executeSkill(db2));
        check(emptyOut.contains("Žádné záznamy"), "Empty skill output");
    }

    static void testFileRoundtrip() throws Exception {
        section("File save/load roundtrip");
        resetIdCounter();
        EmployeeDatabase db = makeBasicDb();
        db.addCooperation(1, 2, CooperationLevel.GOOD);
        db.addCooperation(1, 3, CooperationLevel.AVERAGE);
        db.addCooperation(1, 4, CooperationLevel.BAD);

        File tmp = new File("test_emp.emp");
        FileManager.saveEmployee(db.findById(1), tmp.getPath());
        check(tmp.exists(), "File created");
        check(tmp.length() > 0, "File not empty");

        Employee loaded = FileManager.loadEmployee(tmp.getPath());
        check(loaded.getId() == 1, "Loaded ID 1");
        check(loaded.getName().equals("Jan"), "Loaded name Jan");
        check(loaded.getSurname().equals("Novak"), "Loaded surname Novak");
        check(loaded.getBirthYear() == 1990, "Loaded year 1990");
        check(loaded instanceof DataAnalyst, "Loaded type DataAnalyst");
        check(loaded.getCooperations().size() == 3, "Loaded 3 cooperations");
        check(loaded.hasCooperationWith(2), "Has coop with 2");
        check(loaded.hasCooperationWith(3), "Has coop with 3");
        check(loaded.hasCooperationWith(4), "Has coop with 4");

        CooperationLevel lvl = loaded.getCooperations().stream()
                .filter(c -> c.getColleagueId() == 4).findFirst().get().getLevel();
        check(lvl == CooperationLevel.BAD, "Level preserved (BAD)");

        tmp.delete();
    }

    static void testSqliteRoundtrip() {
        section("SQLite save/load roundtrip");
        new File("employees.db").delete();
        resetIdCounter();
        EmployeeDatabase db = makeBasicDb();
        db.addCooperation(1, 2, CooperationLevel.GOOD);
        db.addCooperation(2, 3, CooperationLevel.BAD);
        db.addCooperation(4, 1, CooperationLevel.AVERAGE);

        SqliteManager.initDatabase();
        check(SqliteManager.saveAll(db.getAllEmployees()), "SQLite save success");

        resetIdCounter();
        List<Employee> loaded = SqliteManager.loadAll();
        check(loaded.size() == 4, "Loaded 4 employees");

        Employee one = loaded.stream().filter(e -> e.getId() == 1).findFirst().orElse(null);
        check(one != null && one.getName().equals("Jan"), "Loaded Jan with ID 1");
        check(one.hasCooperationWith(2), "1->2 preserved");

        Employee four = loaded.stream().filter(e -> e.getId() == 4).findFirst().orElse(null);
        check(four != null && four.hasCooperationWith(1), "4->1 preserved");

        Employee newOne = new DataAnalyst(Employee.generateId(), "X", "X", 1990);
        check(newOne.getId() == 5, "Next ID after load = 5");

        new File("employees.db").delete();
    }

    static void testEditEmployee() {
        section("Edit employee");
        resetIdCounter();
        EmployeeDatabase db = makeBasicDb();
        check(db.updateEmployee(1, "NewJan", "NewNovak", 2000), "Update success");
        Employee e = db.findById(1);
        check(e.getName().equals("NewJan"), "Name updated");
        check(e.getSurname().equals("NewNovak"), "Surname updated");
        check(e.getBirthYear() == 2000, "Year updated");
        check(!db.updateEmployee(99, "X", "X", 2000), "Update missing returns false");
    }

    static void testFactoryUnknownType() {
        section("Employee factory");
        try {
            Employee.create("UnknownType", 1, "X", "X", 1990);
            check(false, "Should throw on unknown type");
        } catch (IllegalArgumentException e) {
            check(true, "Throws IllegalArgumentException on unknown type");
        }
        Employee e1 = Employee.create("DataAnalyst", 1, "A", "A", 1990);
        check(e1 instanceof DataAnalyst, "Factory creates DataAnalyst");
        Employee e2 = Employee.create("SecuritySpecialist", 2, "B", "B", 1990);
        check(e2 instanceof SecuritySpecialist, "Factory creates SecuritySpecialist");
    }

    static EmployeeDatabase makeBasicDb() {
        EmployeeDatabase db = new EmployeeDatabase();
        db.addEmployee(new DataAnalyst(Employee.generateId(), "Jan", "Novak", 1990));
        db.addEmployee(new DataAnalyst(Employee.generateId(), "Petr", "Svoboda", 1985));
        db.addEmployee(new SecuritySpecialist(Employee.generateId(), "Eva", "Dvorakova", 1992));
        db.addEmployee(new SecuritySpecialist(Employee.generateId(), "Tomas", "Cerny", 1988));
        return db;
    }

    static void resetIdCounter() {

        try {
            java.lang.reflect.Field f = Employee.class.getDeclaredField("nextId");
            f.setAccessible(true);
            f.setInt(null, 1);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    static String capture(Runnable r) {
        java.io.PrintStream old = System.out;
        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        System.setOut(new java.io.PrintStream(baos, true, java.nio.charset.StandardCharsets.UTF_8));
        try {
            r.run();
        } finally {
            System.setOut(old);
        }
        return baos.toString(java.nio.charset.StandardCharsets.UTF_8);
    }

    static void section(String title) {
        System.out.println();
        System.out.println("--- " + title + " ---");
    }

    static void check(boolean condition, String message) {
        if (condition) {
            passed++;
            System.out.println("  [OK]   " + message);
        } else {
            failed++;
            System.out.println("  [FAIL] " + message);
        }
    }

    static void cleanup() {
        new File("employees.db").delete();
        new File("test_emp.emp").delete();
    }
}
