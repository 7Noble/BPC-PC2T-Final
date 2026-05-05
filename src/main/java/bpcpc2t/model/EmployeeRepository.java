package bpcpc2t.model;

import java.util.List;

public interface EmployeeRepository {

    Employee findById(int id);

    List<Employee> getAllEmployees();
}
