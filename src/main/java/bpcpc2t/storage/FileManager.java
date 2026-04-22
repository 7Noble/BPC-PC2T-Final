package bpcpc2t.storage;

import bpcpc2t.model.*;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Správce souborů pro ukládání a načítání jednotlivých zaměstnanců.
 *
 * Formát souboru (prostý text, UTF-8):
 * <pre>
 * TYPE:DataAnalyst
 * ID:1
 * NAME:Jan
 * SURNAME:Novák
 * BIRTHYEAR:1990
 * COOPERATION:2:GOOD
 * COOPERATION:5:BAD
 * </pre>
 */
public class FileManager {

    private FileManager() {}

    /**
     * Uloží zaměstnance do textového souboru.
     *
     * @param employee zaměstnanec k uložení
     * @param filePath cesta k souboru
     * @throws IOException při chybě zápisu
     */
    public static void saveEmployee(Employee employee, String filePath) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(filePath), StandardCharsets.UTF_8))) {

            writer.write("TYPE:" + employee.getTypeName());
            writer.newLine();
            writer.write("ID:" + employee.getId());
            writer.newLine();
            writer.write("NAME:" + employee.getName());
            writer.newLine();
            writer.write("SURNAME:" + employee.getSurname());
            writer.newLine();
            writer.write("BIRTHYEAR:" + employee.getBirthYear());
            writer.newLine();

            for (Cooperation c : employee.getCooperations()) {
                writer.write("COOPERATION:" + c.getColleagueId() + ":" + c.getLevel().name());
                writer.newLine();
            }
        }
    }

    /**
     * Načte zaměstnance z textového souboru.
     *
     * @param filePath cesta k souboru
     * @return načtený zaměstnanec
     * @throws IOException          při chybě čtení
     * @throws IllegalStateException pokud soubor má neplatný formát
     */
    public static Employee loadEmployee(String filePath) throws IOException {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(filePath), StandardCharsets.UTF_8))) {

            String type = null;
            int id = -1;
            String name = null;
            String surname = null;
            int birthYear = -1;
            List<Cooperation> cooperations = new ArrayList<>();

            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;

                String[] parts = line.split(":", 2);
                if (parts.length < 2) continue;

                switch (parts[0]) {
                    case "TYPE"        -> type = parts[1];
                    case "ID"          -> id = Integer.parseInt(parts[1]);
                    case "NAME"        -> name = parts[1];
                    case "SURNAME"     -> surname = parts[1];
                    case "BIRTHYEAR"   -> birthYear = Integer.parseInt(parts[1]);
                    case "COOPERATION" -> {
                        String[] cParts = parts[1].split(":", 2);
                        if (cParts.length == 2) {
                            int colleagueId = Integer.parseInt(cParts[0]);
                            CooperationLevel level = CooperationLevel.valueOf(cParts[1]);
                            cooperations.add(new Cooperation(colleagueId, level));
                        }
                    }
                }
            }

            if (type == null || id < 0 || name == null || surname == null || birthYear < 0) {
                throw new IllegalStateException("Neplatný formát souboru: " + filePath);
            }

            Employee employee = createEmployee(type, id, name, surname, birthYear);
            for (Cooperation c : cooperations) {
                employee.addCooperation(c);
            }

            Employee.updateNextId(id);
            return employee;
        }
    }

    private static Employee createEmployee(String type, int id, String name, String surname, int birthYear) {
        return switch (type) {
            case "DataAnalyst"        -> new DataAnalyst(id, name, surname, birthYear);
            case "SecuritySpecialist" -> new SecuritySpecialist(id, name, surname, birthYear);
            default -> throw new IllegalArgumentException("Neznámý typ zaměstnance: " + type);
        };
    }
}
