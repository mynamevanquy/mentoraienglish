INSERT INTO grammar_topics (
    title,
    description_vi,
    description_en,
    level,
    examples,
    rules,
    order_index,
    is_published
) VALUES
(
    'Present Simple',
    'Thì hiện tại đơn dùng để nói về thói quen, sự thật hiển nhiên, lịch trình và trạng thái ổn định.',
    'Use the present simple for habits, facts, schedules, and stable states.',
    'BEGINNER',
    '["I study English every day.", "She works at a bank.", "The train leaves at 7:30."]'::jsonb,
    '["Dùng động từ nguyên mẫu với I/you/we/they.", "Thêm -s hoặc -es cho he/she/it.", "Dùng do/does trong câu phủ định và câu hỏi.", "Trạng từ thường gặp: always, usually, often, sometimes, never."]'::jsonb,
    10,
    TRUE
),
(
    'Present Continuous',
    'Thì hiện tại tiếp diễn mô tả hành động đang xảy ra tại thời điểm nói hoặc xu hướng tạm thời.',
    'Use the present continuous for actions happening now or temporary situations.',
    'BEGINNER',
    '["I am reading a book now.", "They are learning online this week.", "She is not sleeping."]'::jsonb,
    '["Cấu trúc: subject + am/is/are + verb-ing.", "Dùng now, right now, at the moment để nhấn mạnh thời điểm hiện tại.", "Không thường dùng với stative verbs như know, like, believe."]'::jsonb,
    20,
    TRUE
),
(
    'Past Simple',
    'Thì quá khứ đơn diễn tả hành động đã kết thúc tại một thời điểm xác định trong quá khứ.',
    'Use the past simple for completed actions at a definite time in the past.',
    'BEGINNER',
    '["I visited Da Nang last year.", "He did not call me yesterday.", "Did you finish the report?"]'::jsonb,
    '["Động từ có quy tắc thêm -ed.", "Động từ bất quy tắc cần học dạng quá khứ riêng.", "Dùng did/did not với động từ nguyên mẫu trong câu hỏi và phủ định.", "Dấu hiệu thường gặp: yesterday, last week, ago, in 2020."]'::jsonb,
    30,
    TRUE
),
(
    'Present Perfect',
    'Thì hiện tại hoàn thành liên kết trải nghiệm hoặc kết quả trong quá khứ với hiện tại.',
    'Use the present perfect when past actions connect to the present.',
    'INTERMEDIATE',
    '["I have finished my homework.", "She has lived here for five years.", "Have you ever tried sushi?"]'::jsonb,
    '["Cấu trúc: subject + have/has + past participle.", "Dùng for với khoảng thời gian và since với mốc thời gian.", "Dùng ever/never cho trải nghiệm.", "Không dùng với thời điểm quá khứ đã kết thúc như yesterday hoặc last night."]'::jsonb,
    40,
    TRUE
),
(
    'Modal Verbs',
    'Động từ khuyết thiếu diễn tả khả năng, lời khuyên, sự bắt buộc, dự đoán hoặc xin phép.',
    'Modal verbs express ability, advice, obligation, prediction, or permission.',
    'INTERMEDIATE',
    '["You should review your notes.", "I can speak English.", "She must wear a helmet.", "May I ask a question?"]'::jsonb,
    '["Sau modal verb luôn dùng động từ nguyên mẫu không to.", "Can/could nói về khả năng hoặc lời xin phép.", "Should dùng cho lời khuyên.", "Must và have to nói về sự bắt buộc nhưng sắc thái khác nhau."]'::jsonb,
    50,
    TRUE
),
(
    'Conditionals',
    'Câu điều kiện dùng để nói về kết quả phụ thuộc vào một điều kiện cụ thể.',
    'Conditionals describe results that depend on a condition.',
    'INTERMEDIATE',
    '["If it rains, I will stay home.", "If I had more time, I would learn Japanese.", "If she had studied, she would have passed."]'::jsonb,
    '["Zero conditional: if + present simple, present simple.", "First conditional: if + present simple, will + verb.", "Second conditional: if + past simple, would + verb.", "Third conditional: if + past perfect, would have + past participle."]'::jsonb,
    60,
    TRUE
),
(
    'Relative Clauses',
    'Mệnh đề quan hệ bổ sung thông tin cho danh từ và giúp câu rõ nghĩa hơn.',
    'Relative clauses add information about nouns.',
    'ADVANCED',
    '["The teacher who helped me is from Canada.", "This is the book that I mentioned.", "Hanoi, which is the capital of Vietnam, is very busy."]'::jsonb,
    '["Who dùng cho người, which dùng cho vật, that có thể dùng cho cả người và vật trong defining clauses.", "Defining relative clauses không dùng dấu phẩy.", "Non-defining relative clauses dùng dấu phẩy và không dùng that.", "Có thể lược bỏ đại từ quan hệ khi nó là tân ngữ."]'::jsonb,
    70,
    TRUE
),
(
    'Passive Voice',
    'Câu bị động nhấn mạnh hành động hoặc đối tượng chịu tác động hơn là người thực hiện.',
    'Use passive voice to emphasize the action or receiver rather than the doer.',
    'ADVANCED',
    '["The email was sent yesterday.", "English is spoken in many countries.", "The project will be completed next month."]'::jsonb,
    '["Cấu trúc chung: be + past participle.", "Chia động từ be theo thì của câu chủ động.", "Dùng by + agent khi người thực hiện quan trọng hoặc cần làm rõ.", "Tránh lạm dụng bị động khi câu chủ động rõ ràng hơn."]'::jsonb,
    80,
    TRUE
)
ON CONFLICT DO NOTHING;
