package bpcpc2t.storage;

import bpcpc2t.model.*;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class FileManager {

    private FileManager() {}

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

            Employee employee = Employee.create(type, id, name, surname, birthYear);
            for (Cooperation c : cooperations) {
                employee.addCooperation(c);
            }

            Employee.updateNextId(id);
            return employee;
        }
    }

}
