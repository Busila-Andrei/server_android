package com.example.server_android.exam;

public class QuestionMapper {

    public static QuestionDTO toQuestionDTO(Question question) {
        return new QuestionDTO(
                question.getId(),
                question.getType(),
                question.getQuestionText(),
                question.getCorrectAnswer(),
                question.getOtherAnswers(),
                question.getTestId(),
                question.getSubcategoryId()
        );
    }

    public static Question toQuestion(QuestionDTO questionDTO) {
        return Question.builder()
                .id(questionDTO.getId())
                .type(questionDTO.getType())
                .questionText(questionDTO.getQuestionText())
                .correctAnswer(questionDTO.getCorrectAnswer())
                .otherAnswers(questionDTO.getOtherAnswers())
                .testId(questionDTO.getTestId())
                .subcategoryId(questionDTO.getSubcategoryId())
                .build();
    }
}
