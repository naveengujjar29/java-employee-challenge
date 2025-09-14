package com.reliaquest.api.config;

import com.reliaquest.api.dto.EmployeeDto;
import org.modelmapper.ModelMapper;
import org.modelmapper.PropertyMap;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ModelMapperConfig {

    @Bean
    public ModelMapper modelMapper() {
        ModelMapper mapper = new ModelMapper();

        // Configure mapping from ServerEmployeeDto to EmployeeDto
        // ServerEmployeeDto has employee_* fields, EmployeeDto has simple field names
        mapper.addMappings(new PropertyMap<com.reliaquest.api.model.ServerEmployeeDto, EmployeeDto>() {
            @Override
            protected void configure() {
                map().setId(source.getId());
                map().setName(source.getName());
                map().setSalary(source.getSalary());
                map().setAge(source.getAge());
                map().setTitle(source.getTitle());
                map().setEmail(source.getEmail());
            }
        });

        return mapper;

    }
}
