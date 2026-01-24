package com.example.gitoo.service;

import com.example.gitoo.dto.response.NeisSchoolInfoResponse;
import com.example.gitoo.dto.response.SchoolSearchResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class SchoolSearchService {

    private final WebClient webClient;

    @Value("${neis.base-url:https://open.neis.go.kr/hub}")
    private String baseUrl;

    @Value("${neis.key}")
    private String apiKey;

    @Value("${neis.type:json}")
    private String type;

    @Value("${neis.pIndex:1}")
    private int pIndex;

    @Value("${neis.pSize:50}")
    private int pSize;

    public List<SchoolSearchResponse> search(String q) {
        if (q == null || q.trim().length() < 2) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "q must be at least 2 characters");
        }
        String query = q.trim();

        try {
            NeisSchoolInfoResponse neis = webClient.get()
                    .uri(baseUrl + "/schoolInfo?KEY={key}&Type={type}&pIndex={pIndex}&pSize={pSize}&SCHUL_NM={q}",
                            apiKey, type, pIndex, pSize, query)
                    .retrieve()
                    .onStatus(status -> status.isError(), resp ->
                            resp.bodyToMono(String.class).map(body ->
                                    new ResponseStatusException(resp.statusCode(), "NEIS error: " + body)
                            )
                    )
                    .bodyToMono(NeisSchoolInfoResponse.class)
                    .block();

            if (neis == null || neis.getSchoolInfo() == null) {
                return Collections.emptyList();
            }

            List<NeisSchoolInfoResponse.Row> rows = neis.getSchoolInfo().stream()
                    .filter(Objects::nonNull)
                    .map(NeisSchoolInfoResponse.SchoolInfoBlock::getRow)
                    .filter(Objects::nonNull)
                    .findFirst()
                    .orElse(Collections.emptyList());

            return rows.stream()
                    .map(r -> {
                        String schoolKey = (r.getAtptCode() != null && r.getSchoolCode() != null)
                                ? r.getAtptCode() + ":" + r.getSchoolCode()
                                : null;

                        return SchoolSearchResponse.builder()
                                .schoolName(r.getSchoolName())
                                .schoolType(r.getSchoolType())
                                .address(joinAddress(r.getRoadAddress(), r.getRoadAddressDetail()))
                                .atptCode(r.getAtptCode())
                                .schoolCode(r.getSchoolCode())
                                .schoolKey(schoolKey)
                                .build();
                    })
                    .filter(it -> it.getSchoolKey() != null)
                    .toList();

        } catch (WebClientResponseException e) {
            // NEIS가 4xx/5xx를 준 경우
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Failed to call NEIS: " + e.getStatusCode() + " " + safeBody(e.getResponseBodyAsString())
            );
        } catch (Exception e) {
            // 기타 예외 -> 원인 메시지를 노출해서 디버깅 쉽게
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "School search failed: " + e.getMessage(), e);
        }
    }

    private String safeBody(String s) {
        if (s == null) return "";
        return s.length() > 300 ? s.substring(0, 300) + "..." : s;
    }

    private String joinAddress(String road, String detail) {
        String a = road == null ? "" : road.trim();
        String b = detail == null ? "" : detail.trim();
        String merged = (a + " " + b).trim();
        return merged.isEmpty() ? null : merged;
    }
}