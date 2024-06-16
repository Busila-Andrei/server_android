package com.example.server_android.exam;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class QuestionDTO {
    private Long id;
    private String type;
    private String questionText;
    private String correctAnswer;
    private String otherAnswers;
    private Long testId;
    private Long subcategoryId;
}
