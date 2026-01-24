package com.example.gitoo.repository;

import com.example.gitoo.model.School;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface SchoolRepository extends JpaRepository<School, Integer> {
    Optional<School> findBySchoolName(String schoolName);
    Optional<School> findBySchoolKey(String schoolKey);

}
