package com.example.server_android.exam;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "questions")
@Getter
@Setter
@ToString
@Builder
@AllArgsConstructor
@RequiredArgsConstructor
public class Question {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String type;
    private String questionText;
    private String correctAnswer;
    private String otherAnswers;

    @Column(name = "test_id")
    private Long testId;
    @Column(name = "subcategory_id")
    private Long subcategoryId;


}


