INSERT INTO categories (name) VALUES
('Концерты'),
('Выставки'),
('Театр'),
('Кино'),
('Спорт'),
('Образование'),
('Наука'),
('Технологии'),
('Искусство'),
('Музыка')
ON CONFLICT (name) DO NOTHING;

INSERT INTO users (name, email) VALUES
('Александр Иванов', 'alex.ivanov@example.com'),
('Мария Петрова', 'maria.petrova@example.com'),
('Дмитрий Сидоров', 'dmitry.sidorov@example.com'),
('Екатерина Кузнецова', 'ekaterina.kuznetsova@example.com'),
('Михаил Попов', 'mikhail.popov@example.com')
ON CONFLICT (email) DO NOTHING;

INSERT INTO events (annotation, category_id, created_on, description, event_date, initiator_id, lat, lon, paid, participant_limit, request_moderation, state, title) VALUES
('Тестовый концерт рок-группы', 1, NOW(), 'Полное описание тестового концерта рок-группы с участием известных музыкантов', NOW() + INTERVAL '3 days', 1, 55.7558, 37.6173, true, 100, true, 'PENDING', 'Тестовый рок-концерт'),
('Выставка современного искусства', 2, NOW(), 'Экспозиция современных художников и скульпторов', NOW() + INTERVAL '5 days', 2, 55.7517, 37.6178, false, 0, false, 'PUBLISHED', 'Выставка современного искусства'),
('Спортивный марафон', 5, NOW(), 'Городской марафон на 10 км для всех желающих', NOW() + INTERVAL '7 days', 3, 55.7604, 37.6184, false, 50, true, 'PUBLISHED', 'Городской марафон'),
('Научная конференция', 7, NOW(), 'Ежегодная конференция по инновациям в науке', NOW() + INTERVAL '10 days', 4, 55.7580, 37.6160, true, 200, true, 'CANCELED', 'Научная конференция 2024'),
('Театральная премьера', 3, NOW(), 'Премьерный показ новой пьесы известного драматурга', NOW() + INTERVAL '2 days', 5, 55.7539, 37.6208, true, 150, true, 'PENDING', 'Театральная премьера')
ON CONFLICT DO NOTHING;

INSERT INTO compilations (pinned, title) VALUES
(true, 'Популярные события'),
(false, 'Бесплатные мероприятия'),
(true, 'Спортивные события'),
(false, 'Культурные мероприятия')
ON CONFLICT DO NOTHING;

INSERT INTO compilation_events (compilation_id, event_id) VALUES
(1, 2),
(1, 3),
(2, 2),
(2, 3),
(3, 3),
(4, 1),
(4, 2),
(4, 5)
ON CONFLICT DO NOTHING;

INSERT INTO participation_requests (created, event_id, requester_id, status) VALUES
(NOW(), 2, 1, 'CONFIRMED'),
(NOW(), 2, 3, 'PENDING'),
(NOW(), 3, 2, 'CONFIRMED'),
(NOW(), 3, 4, 'CONFIRMED'),
(NOW(), 3, 5, 'REJECTED'),
(NOW() - INTERVAL '1 hour', 2, 4, 'CANCELED')
ON CONFLICT DO NOTHING;