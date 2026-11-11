package com.cloudnest.academy.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "quiz_questions")
public class QuizQuestion {
    public enum Type { SINGLE, MULTI, TRUE_FALSE, FILL }

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) public Long id;
    @ManyToOne(optional = false) public Lesson lesson;
    @Column(length = 4000, nullable = false) public String prompt;
    @Enumerated(EnumType.STRING) public Type type = Type.SINGLE;
    /** Options separated by new lines. */
    @Column(length = 4000) public String options;
    /** SINGLE/TRUE_FALSE: index. MULTI: comma separated indexes. FILL: accepted answers separated by "|". */
    public String correct;
    @Column(length = 4000) public String explanation;
    public int position;
}
