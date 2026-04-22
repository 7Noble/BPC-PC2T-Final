package bpcpc2t.model;

import java.util.List;

/**
 * Rozhraní pro přístup k databázi zaměstnanců.
 * Slouží jako abstrakce pro metody dovedností (skill), čímž se zamezuje
 * cyklické závislosti mezi modelem a servisní vrstvou.
 */
public interface EmployeeRepository {

    Employee findById(int id);

    List<Employee> getAllEmployees();
}
