package com.CBTServer.WebCSAT.dto;

import com.CBTServer.WebCSAT.domain.Question;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor @NoArgsConstructor @Getter @Setter
public class QuestionDTO {
    private Long questionId;
    private int num;
    private String questionTitle;
    private String questionArticle;
    private String questionContext;
    private int score;
    private String answer;
    private String option1;
    private String option2;
    private String option3;
    private String option4;
    private String option5;
    private boolean questionType;
    private String csatDate;
    private Long subjectId;
    private Long subclassId;

    public QuestionDTO(Question question) {
        this.questionId = question.getQuestionId();
        this.num = question.getNum();
        this.questionTitle = question.getQuestionTitle();

        // ImageMeta로 바뀐 필드 처리
        this.questionArticle = question.getQuestionArticle() != null ? question.getQuestionArticle().getUrl() : null;
        this.questionContext = question.getQuestionContext() != null ? question.getQuestionContext().getUrl() : null;

        this.score = question.getScore();
        this.answer = question.getAnswer();
        this.option1 = question.getOption1();
        this.option2 = question.getOption2();
        this.option3 = question.getOption3();
        this.option4 = question.getOption4();
        this.option5 = question.getOption5();
        this.questionType = question.isQuestionType();
        this.csatDate = question.getCsatDate() != null ? question.getCsatDate().getCsatDate() : null;
        this.subjectId = question.getSubject() != null ? question.getSubject().getSubjectId() : null;
        this.subclassId = question.getSubclass() != null ? question.getSubclass().getSubclassId() : null;
    }


}
