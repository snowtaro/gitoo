package com.example.gitoo.school;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SchoolSearchResponse {
    private String schoolName;
    private String address;
    private String schoolType;
    private String schoolKey;
    private String schoolCode;
    private String atptCode;

}