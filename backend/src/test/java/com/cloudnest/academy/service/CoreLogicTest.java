package com.cloudnest.academy.service;

import com.cloudnest.academy.domain.QuizQuestion;
import com.cloudnest.academy.security.Crypto;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CoreLogicTest {

    private static QuizQuestion q(QuizQuestion.Type type, String correct) {
        QuizQuestion q = new QuizQuestion();
        q.type = type;
        q.correct = correct;
        return q;
    }

    @Test
    void gradesEveryQuestionType() {
        assertTrue(LearningService.isCorrect(q(QuizQuestion.Type.SINGLE, "1"), "1"));
        assertFalse(LearningService.isCorrect(q(QuizQuestion.Type.SINGLE, "1"), "0"));
        assertTrue(LearningService.isCorrect(q(QuizQuestion.Type.MULTI, "0,2"), "2,0"));
        assertFalse(LearningService.isCorrect(q(QuizQuestion.Type.MULTI, "0,2"), "0"));
        assertTrue(LearningService.isCorrect(q(QuizQuestion.Type.TRUE_FALSE, "1"), "1"));
        assertTrue(LearningService.isCorrect(q(QuizQuestion.Type.FILL, "Appliance|VA"), " va "));
        assertFalse(LearningService.isCorrect(q(QuizQuestion.Type.FILL, "Appliance"), null));
    }

    @Test
    void slugsAreUrlSafe() {
        assertEquals("aws-solutions-architect-associate-saa-c03", InstructorService.slugify("AWS Solutions Architect - Associate (SAA-C03)"));
        assertEquals("course", InstructorService.slugify("!!!"));
    }

    @Test
    void razorpayStyleSignatureMatches() {
        String sig = Crypto.hmacHex("secret", "order_1|pay_1");
        assertTrue(Crypto.safeEquals(sig, Crypto.hmacHex("secret", "order_1|pay_1")));
        assertFalse(Crypto.safeEquals(sig, Crypto.hmacHex("other", "order_1|pay_1")));
    }
}
