package bpcpc2t.storage;

import bpcpc2t.model.*;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Správce SQLite databáze – záloha a obnova všech dat.
 *
 * Schéma:
 * <pre>
 *   employees   (id INTEGER PK, name TEXT, surname TEXT, birth_year INTEGER, type TEXT)
 *   cooperations(employee_id INTEGER, colleague_id INTEGER, level TEXT, PK(employee_id, colleague_id))
 * </pre>
 */
public class SqliteManager {

    private static final String DB_FILE = "employees.db";
    private static final String URL = "jdbc:sqlite:" + DB_FILE;

    static {
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            // ovladac nedostupny – program pobezi bez SQLite zalohy
        }
    }

    private SqliteManager() {}

    // ── Inicializace ───────────────────────────────────────────────────────────

    /**
     * Vytvoří tabulky, pokud neexistují.
     */
    public static void initDatabase() {
        try (Connection conn = DriverManager.getConnection(URL);
             Statement stmt = conn.createStatement()) {

            stmt.execute("""
                    CREATE TABLE IF NOT EXISTS employees (
                        id         INTEGER PRIMARY KEY,
                        name       TEXT    NOT NULL,
                        surname    TEXT    NOT NULL,
                        birth_year INTEGER NOT NULL,
                        type       TEXT    NOT NULL
                    )
                    """);

            stmt.execute("""
                    CREATE TABLE IF NOT EXISTS cooperations (
                        employee_id  INTEGER NOT NULL,
                        colleague_id INTEGER NOT NULL,
                        level        TEXT    NOT NULL,
                        PRIMARY KEY (employee_id, colleague_id),
                        FOREIGN KEY (employee_id) REFERENCES employees(id)
                    )
                    """);

        } catch (SQLException e) {
            System.err.println("[SQLite] Chyba při inicializaci: " + e.getMessage());
        }
    }

    // ── Uložení ────────────────────────────────────────────────────────────────

    /**
     * Uloží všechna data do SQLite (přepíše stávající záznamy).
     *
     * @param employees seznam všech zaměstnanců
     * @return true při úspěchu
     */
    public static boolean saveAll(List<Employee> employees) {
        try (Connection conn = DriverManager.getConnection(URL)) {
            conn.setAutoCommit(false);

            try (Statement stmt = conn.createStatement()) {
                stmt.execute("DELETE FROM cooperations");
                stmt.execute("DELETE FROM employees");
            }

            String insertEmployee = "INSERT INTO employees(id, name, surname, birth_year, type) VALUES(?,?,?,?,?)";
            String insertCoop     = "INSERT INTO cooperations(employee_id, colleague_id, level) VALUES(?,?,?)";

            try (PreparedStatement psEmp  = conn.prepareStatement(insertEmployee);
                 PreparedStatement psCoop = conn.prepareStatement(insertCoop)) {

                for (Employee emp : employees) {
                    psEmp.setInt(1, emp.getId());
                    psEmp.setString(2, emp.getName());
                    psEmp.setString(3, emp.getSurname());
                    psEmp.setInt(4, emp.getBirthYear());
                    psEmp.setString(5, emp.getTypeName());
                    psEmp.addBatch();

                    for (Cooperation c : emp.getCooperations()) {
                        psCoop.setInt(1, emp.getId());
                        psCoop.setInt(2, c.getColleagueId());
                        psCoop.setString(3, c.getLevel().name());
                        psCoop.addBatch();
                    }
                }

                psEmp.executeBatch();
                psCoop.executeBatch();
            }

            conn.commit();
            return true;

        } catch (SQLException e) {
            System.err.println("[SQLite] Chyba při ukládání: " + e.getMessage());
            return false;
        }
    }

    // ── Načtení ────────────────────────────────────────────────────────────────

    /**
     * Načte všechna data ze SQLite databáze.
     *
     * @return seznam zaměstnanců (prázdný seznam při chybě nebo prázdné DB)
     */
    public static List<Employee> loadAll() {
        List<Employee> result = new ArrayList<>();

        try (Connection conn = DriverManager.getConnection(URL)) {

            // Načti zaměstnance
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT * FROM employees ORDER BY id")) {

                while (rs.next()) {
                    int    id        = rs.getInt("id");
                    String name      = rs.getString("name");
                    String surname   = rs.getString("surname");
                    int    birthYear = rs.getInt("birth_year");
                    String type      = rs.getString("type");

                    Employee emp = Employee.create(type, id, name, surname, birthYear);
                    result.add(emp);
                    Employee.updateNextId(id);
                }
            }

            // Načti spolupráce
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT * FROM cooperations")) {

                while (rs.next()) {
                    int empId      = rs.getInt("employee_id");
                    int colleagueId = rs.getInt("colleague_id");
                    String levelStr = rs.getString("level");

                    result.stream()
                            .filter(e -> e.getId() == empId)
                            .findFirst()
                            .ifPresent(emp -> {
                                CooperationLevel level = CooperationLevel.valueOf(levelStr);
                                emp.addCooperation(new Cooperation(colleagueId, level));
                            });
                }
            }

        } catch (SQLException e) {
            System.err.println("[SQLite] Chyba při načítání: " + e.getMessage());
        }

        return result;
    }

}
