package com.pado.inflow.department.command.domain.aggregate.dto;


import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
@Data
@AllArgsConstructor
@NoArgsConstructor

public class DeleteDepartmentRequestDTO {

    @JsonProperty("department_code")
    private String departmentCode;

}
