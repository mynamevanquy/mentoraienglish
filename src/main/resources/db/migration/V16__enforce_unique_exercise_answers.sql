ALTER TABLE exercise_answers
    ADD CONSTRAINT uk_exercise_answers_attempt_question UNIQUE (attempt_id, question_id);
