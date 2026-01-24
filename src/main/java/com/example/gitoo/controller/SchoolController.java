package com.example.gitoo.controller;

import com.example.gitoo.dto.response.SchoolSearchResponse;
import com.example.gitoo.service.SchoolSearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("api/schools")
public class SchoolController {
    private final SchoolSearchService schoolSearchService;

    @GetMapping("/search")
    public List<SchoolSearchResponse> search(@RequestParam("q") String schoolName) {
        return schoolSearchService.search(schoolName);
    }
}