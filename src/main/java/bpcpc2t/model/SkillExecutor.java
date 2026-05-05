package bpcpc2t.model;

public interface SkillExecutor {

    void executeSkill(EmployeeRepository repository);

    String getSkillDescription();
}
