package com.example.gitoo.school;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class NeisSchoolInfoResponse {

    @JsonProperty("schoolInfo")
    private List<SchoolInfoBlock> schoolInfo;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class SchoolInfoBlock {
        @JsonProperty("row")
        private List<Row> row;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Row {
        @JsonProperty("ATPT_OFCDC_SC_CODE")
        private String atptCode;

        @JsonProperty("SD_SCHUL_CODE")
        private String schoolCode;

        @JsonProperty("SCHUL_NM")
        private String schoolName;

        @JsonProperty("SCHUL_KND_SC_NM")
        private String schoolType;

        @JsonProperty("ORG_RDNMA")
        private String roadAddress;

        @JsonProperty("ORG_RDNDA")
        private String roadAddressDetail;

        @JsonProperty("LCTN_SC_NM")
        private String locationName;
    }
}