package com.reliaquest.api.dto;

import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.util.UUID;

/**
 * @author Naveen Kumar
 */
public class EmployeeDto implements Serializable {

    private UUID id;

    @NotBlank(message = "Employee name cannot be blank")
    private String name;

    @NotNull(message = "Employee salary cannot be null") @Positive(message = "Employee salary must be greater than zero") private Integer salary;

    @NotNull(message = "Employee age cannot be null") @Min(value = 16, message = "Employee age must be at least 16")
    @Max(value = 75, message = "Employee age must be at most 75")
    private Integer age;

    @NotBlank(message = "Employee title cannot be blank")
    private String title;

    private String email;

    // Default constructor
    public EmployeeDto() {}

    // All-args constructor
    public EmployeeDto(UUID id, String name, Integer salary, Integer age, String title, String email) {
        this.id = id;
        this.name = name;
        this.salary = salary;
        this.age = age;
        this.title = title;
        this.email = email;
    }

    // Getters and Setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getSalary() {
        return salary;
    }

    public void setSalary(Integer salary) {
        this.salary = salary;
    }

    public Integer getAge() {
        return age;
    }

    public void setAge(Integer age) {
        this.age = age;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    @Override
    public String toString() {
        return "EmployeeDto{" + "id="
                + id + ", name='"
                + name + '\'' + ", salary="
                + salary + ", age="
                + age + ", title='"
                + title + '\'' + ", email='"
                + email + '\'' + '}';
    }
}
