INSERT INTO category (name, type)
VALUES ('Sleep', 'HEALTH') ON CONFLICT (name) DO NOTHING;
INSERT INTO category (name, type)
VALUES ('Workout', 'HEALTH') ON CONFLICT (name) DO NOTHING;
INSERT INTO category (name, type)
VALUES ('Skincare', 'HEALTH') ON CONFLICT (name) DO NOTHING;
INSERT INTO category (name, type)
VALUES ('DSA', 'CAREER') ON CONFLICT (name) DO NOTHING;
INSERT INTO category (name, type)
VALUES ('Mocks', 'CAREER') ON CONFLICT (name) DO NOTHING;
INSERT INTO category (name, type)
VALUES ('System Design', 'CAREER') ON CONFLICT (name) DO NOTHING;
INSERT INTO app_user (name, email)
VALUES ('anshu', 'anshusinha57@gmail.com');