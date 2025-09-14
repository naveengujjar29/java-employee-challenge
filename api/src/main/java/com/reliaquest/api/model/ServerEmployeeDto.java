package com.reliaquest.api.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serializable;
import java.util.UUID;

/**
 * @author Naveen Kumar
 */
public class ServerEmployeeDto implements Serializable {

    @JsonProperty("id")
    private UUID id;

    @JsonProperty("employee_name")
    private String name;

    @JsonProperty("employee_salary")
    private Integer salary;

    @JsonProperty("employee_age")
    private Integer age;

    @JsonProperty("employee_title")
    private String title;

    @JsonProperty("employee_email")
    private String email;

    // Default constructor
    public ServerEmployeeDto() {}

    // All-args constructor
    public ServerEmployeeDto(UUID id, String name, Integer salary, Integer age, String title, String email) {
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
        return "ServerEmployeeDto{" + "id="
                + id + ", name='"
                + name + '\'' + ", salary="
                + salary + ", age="
                + age + ", title='"
                + title + '\'' + ", email='"
                + email + '\'' + '}';
    }
}
