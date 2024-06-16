package com.example.server_android.exam;

import org.springframework.data.jpa.repository.JpaRepository;

public interface QuestionRepository extends JpaRepository<Question, Long> {
}

// Restul claselor și interfețelor nu mai sunt necesare