insert into "fee"
(id, user_id, type, comment,total_amount,  remaining_amount, status,creation_datetime, updated_at, due_datetime)
values
    ('fee1_id', 'student2_id', 'TUITION','Comment', 5000, 0, 'PAID','2021-11-08T08:25:24.00Z','2023-02-08T08:30:24.00Z','2021-12-08T08:25:24.00Z'),
    ('fee2_id', 'student2_id', 'HARDWARE','Comment', 5000, 0, 'PAID','2021-11-10T08:25:24.00Z','2023-02-08T08:30:24.00Z','2021-12-10T08:25:24.00Z'),
    ('fee3_id', 'student2_id', 'TUITION','Comment', 5000, 5000, 'LATE','2022-12-08T08:25:24.00Z','2023-02-08T08:30:24.00Z','2023-03-10T08:25:24.00Z'),
    ('fee4_id', 'student2_id', 'TUITION','Comment', 5000, 5000, 'LATE','2021-11-08T08:25:24.00Z','2023-02-08T08:30:24.00Z','2023-03-10T08:25:25.00Z'),
    ('fee5_id', 'student2_id', 'HARDWARE','Comment', 5000, 0, 'PAID','2021-11-08T08:25:24.00Z','2023-02-08T08:30:24.00Z','2021-12-08T08:25:25.00Z'),
    ('fee6_id', 'student2_id', 'TUITION','Comment', 5000, 0, 'PAID','2021-11-08T08:25:24.00Z','2023-02-08T08:30:24.00Z','2021-12-09T08:25:25.00Z'),
    ('fee7_id', 'student2_id', 'TUITION','Comment', 5000, 5000, 'LATE','2022-12-08T08:25:24.00Z','2023-02-08T08:30:24.00Z','2023-03-10T08:25:24.00Z'),
    ('fee8_id', 'student1_id', 'TUITION','Comment', 100000, 100000, 'LATE','2022-12-08T08:25:24.00Z','2023-02-08T08:30:24.00Z','2023-04-01T08:25:24.00Z'),
    ('fee9_id', 'student1_id', 'TUITION','Comment', 200000, 200000, 'LATE','2022-12-08T08:25:24.00Z','2023-02-08T08:30:24.00Z','2023-0-03T08:25:24.00Z');