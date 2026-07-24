-- Initial Seed Data for PHC Doctor Attendance System

INSERT IGNORE INTO divisions (id, name, district_name) VALUES 
(1, 'Coimbatore North', 'Coimbatore');

INSERT IGNORE INTO phcs (id, name, location, type, latitude, longitude, division_id) VALUES 
(1, 'Thudiyalur PHC', 'Coimbatore', 'PHC', 11.0168, 76.9558, 1),
(2, 'Periyanaickenpalayam Upgraded PHC', 'Coimbatore', 'Upgraded PHC', 11.1512, 76.9410, 1);

INSERT IGNORE INTO doctors (id, name, email, password, specialization, role, phc_id) VALUES 
(1, 'Dr. Arunkumar', 'doctor@phc.gov.in', 'doc123', 'General Physician', 'DOCTOR', 1),
(2, 'Dr. Rajeswari', 'admin@phc.gov.in', 'admin123', 'Health Administrator', 'ADMIN', 1);
