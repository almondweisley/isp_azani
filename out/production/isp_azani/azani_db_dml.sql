-- azani_db_dml.sql
-- Sample data for the Azani ISP database. Run azani_db_ddl.sql first.
-- Clears every table first, so you can run it more than once.
-- Every figure comes from the SCO200 brief. The data is a snapshot taken on 2025-04-30.
-- Institution names are real Kenyan institutions, except the four marked fictional
-- (they use example.org addresses) so that the primary and junior categories hold rows.
-- Universities are filed under 'college', the closest category in the brief.
-- Foreign keys use literal ids. Because the clear step restarts every AUTO_INCREMENT
-- counter at 1, institution ids run 1 to 32 in the order of section 3.

USE azani_db;

-- 1. Clear every table
SET FOREIGN_KEY_CHECKS = 0;
TRUNCATE TABLE disconnections;
TRUNCATE TABLE overdue_fines;
TRUNCATE TABLE payments;
TRUNCATE TABLE upgrades;
TRUNCATE TABLE subscriptions;
TRUNCATE TABLE bandwidth_plans;
TRUNCATE TABLE installations;
TRUNCATE TABLE equipment_orders;
TRUNCATE TABLE lan_node_tiers;
TRUNCATE TABLE readiness_assessments;
TRUNCATE TABLE contact_persons;
TRUNCATE TABLE institutions;
SET FOREIGN_KEY_CHECKS = 1;

-- 2. Price lists (brief Tables 1 and 2)
INSERT INTO bandwidth_plans (mbps, monthly_cost) VALUES
    (4, 1200.00),
    (10, 2000.00),
    (20, 3500.00),
    (25, 4000.00),
    (50, 7000.00);

INSERT INTO lan_node_tiers (min_nodes, max_nodes, cost) VALUES
    (2, 10, 10000.00),
    (11, 20, 20000.00),
    (21, 40, 30000.00),
    (41, 100, 40000.00);

-- 3. Institutions (ids 1 to 32 follow this order)
INSERT INTO institutions (name, type, address, registered_on, status) VALUES
    ('University of Nairobi', 'college', 'University Way, Nairobi', '2025-01-06', 'active'),  -- id 1
    ('Kenyatta University', 'college', 'Kahawa, Nairobi', '2025-01-07', 'active'),  -- id 2
    ('Jomo Kenyatta University of Agriculture and Technology', 'college', 'Juja, Kiambu County', '2025-01-08', 'active'),  -- id 3
    ('Egerton University', 'college', 'Njoro, Nakuru County', '2025-01-09', 'active'),  -- id 4
    ('Maseno University', 'college', 'Maseno, Kisumu County', '2025-01-10', 'active'),  -- id 5
    ('Moi University', 'college', 'Kesses, Uasin Gishu County', '2025-01-11', 'active'),  -- id 6
    ('University of Eldoret', 'college', 'Kapseret, Uasin Gishu County', '2025-01-12', 'active'),  -- id 7
    ('Pwani University', 'college', 'Kilifi, Kilifi County', '2025-01-13', 'active'),  -- id 8
    ('Technical University of Kenya', 'college', 'Haile Selassie Avenue, Nairobi', '2025-01-14', 'active'),  -- id 9
    ('Strathmore University', 'college', 'Madaraka Estate, Nairobi', '2025-01-15', 'active'),  -- id 10
    ('Alliance High School', 'senior', 'Kikuyu, Kiambu County', '2025-01-16', 'active'),  -- id 11
    ('The Kenya High School', 'senior', 'Kileleshwa, Nairobi', '2025-01-17', 'active'),  -- id 12
    ('Mangu High School', 'senior', 'Thika, Kiambu County', '2025-01-18', 'active'),  -- id 13
    ('Starehe Boys Centre and School', 'senior', 'Nairobi', '2025-01-19', 'active'),  -- id 14
    ('Loreto High School Limuru', 'senior', 'Limuru, Kiambu County', '2025-01-20', 'active'),  -- id 15
    ('Maseno School', 'senior', 'Maseno, Kisumu County', '2025-01-21', 'active'),  -- id 16
    ('Kapsabet Boys High School', 'senior', 'Kapsabet, Nandi County', '2025-01-22', 'active'),  -- id 17
    ('Nakuru High School', 'senior', 'Nakuru, Nakuru County', '2025-01-23', 'suspended'),  -- id 18
    ('Kenya Medical Training College', 'college', 'Old Mbagathi Road, Nairobi', '2025-01-24', 'active'),  -- id 19
    ('Kenya Institute of Highways and Building Technology', 'college', 'Ngong Road, Nairobi', '2025-01-25', 'active'),  -- id 20
    ('Kenya Utalii College', 'college', 'Thika Road, Nairobi', '2025-01-26', 'active'),  -- id 21
    ('Nairobi Technical Training Institute', 'college', 'Ngara, Nairobi', '2025-01-27', 'active'),  -- id 22
    ('Rift Valley Institute of Science and Technology', 'college', 'Nakuru, Nakuru County', '2025-01-28', 'active'),  -- id 23
    ('Kisumu National Polytechnic', 'college', 'Kisumu, Kisumu County', '2025-01-29', 'active'),  -- id 24
    ('Mombasa Technical Training Institute', 'college', 'Mombasa, Mombasa County', '2025-01-30', 'active'),  -- id 25
    ('Eldoret National Polytechnic', 'college', 'Eldoret, Uasin Gishu County', '2025-01-31', 'active'),  -- id 26
    ('Thika Technical Training Institute', 'college', 'Thika, Kiambu County', '2025-02-01', 'active'),  -- id 27
    ('Kenya Institute of Mass Communication', 'college', 'South C, Nairobi', '2025-02-02', 'inactive'),  -- id 28
    ('Kibera Hill Primary School', 'primary', 'Kibera, Nairobi', '2025-02-03', 'active'),  -- id 29
    ('Lakeview Primary School', 'primary', 'Kisumu, Kisumu County', '2025-02-04', 'active'),  -- id 30
    ('Mwatate Junior School', 'junior', 'Mwatate, Taita Taveta County', '2025-02-05', 'active'),  -- id 31
    ('Ridgeway Junior School', 'junior', 'Ruiru, Kiambu County', '2025-02-06', 'inactive');  -- id 32

-- 4. Contact persons
INSERT INTO contact_persons (institution_id, full_name, phone, email) VALUES
    (1, 'Grace Wanjiku', '+254700100001', 'grace.wanjiku@uonbi.ac.ke'),  -- University of Nairobi
    (2, 'Peter Mwangi', '+254700100002', 'p.mwangi@ku.ac.ke'),  -- Kenyatta University
    (3, 'Faith Njeri', '+254700100003', 'faith.njeri@jkuat.ac.ke'),  -- Jomo Kenyatta University of Agriculture and Technology
    (4, 'Daniel Kiptoo', '+254700100004', 'daniel.kiptoo@egerton.ac.ke'),  -- Egerton University
    (5, 'Achieng Otieno', '+254700100005', 'a.otieno@maseno.ac.ke'),  -- Maseno University
    (6, 'Brian Kiplagat', '+254700100006', 'b.kiplagat@mu.ac.ke'),  -- Moi University
    (7, 'Janet Chebet', '+254700100007', 'janet.chebet@uoeld.ac.ke'),  -- University of Eldoret
    (8, 'Salim Juma', '+254700100008', 'salim.juma@pu.ac.ke'),  -- Pwani University
    (9, 'Mercy Akinyi', '+254700100009', 'mercy.akinyi@tukenya.ac.ke'),  -- Technical University of Kenya
    (10, 'Kevin Mutua', '+254700100010', 'kevin.mutua@strathmore.edu'),  -- Strathmore University
    (11, 'Samuel Kariuki', '+254700100011', 's.kariuki@alliancehighschool.ac.ke'),  -- Alliance High School
    (12, 'Lydia Wambui', '+254700100012', 'lydia.wambui@kenyahigh.ac.ke'),  -- The Kenya High School
    (13, 'Joseph Maina', '+254700100013', 'j.maina@manguhigh.com'),  -- Mangu High School
    (14, 'Esther Naliaka', '+254700100014', 'esther.naliaka@stareheboys.org'),  -- Starehe Boys Centre and School
    (15, 'Mary Wairimu', '+254700100015', 'mary.wairimu@loreto-limuru.sc.ke'),  -- Loreto High School Limuru
    (16, 'George Ouma', '+254700100016', 'g.ouma@masenoschool.sc.ke'),  -- Maseno School
    (17, 'David Sang', '+254700100017', 'd.sang@kapsabetboys.sc.ke'),  -- Kapsabet Boys High School
    (18, 'Alice Chepngeno', '+254700100018', 'alice.chepngeno@nakuruhigh.sc.ke'),  -- Nakuru High School
    (19, 'Rose Atieno', '+254700100019', 'rose.atieno@kmtc.ac.ke'),  -- Kenya Medical Training College
    (20, 'Martin Karanja', '+254700100020', 'm.karanja@kihbt.ac.ke'),  -- Kenya Institute of Highways and Building Technology
    (21, 'Hassan Noor', '+254700100021', 'h.noor@utalii.ac.ke'),  -- Kenya Utalii College
    (22, 'Lucy Nyambura', '+254700100022', 'lucy.nyambura@ntti.ac.ke'),  -- Nairobi Technical Training Institute
    (23, 'Peter Cheruiyot', '+254700100023', 'p.cheruiyot@rvist.ac.ke'),  -- Rift Valley Institute of Science and Technology
    (24, 'Susan Auma', '+254700100024', 'susan.auma@kisumupoly.ac.ke'),  -- Kisumu National Polytechnic
    (25, 'Ahmed Bakari', '+254700100025', 'a.bakari@mombasatti.ac.ke'),  -- Mombasa Technical Training Institute
    (26, 'Emily Jepkosgei', '+254700100026', 'emily.jepkosgei@tenp.ac.ke'),  -- Eldoret National Polytechnic
    (27, 'Patrick Kimani', '+254700100027', 'p.kimani@thikatechnical.ac.ke'),  -- Thika Technical Training Institute
    (28, 'Naomi Wekesa', '+254700100028', 'naomi.wekesa@kimc.ac.ke'),  -- Kenya Institute of Mass Communication
    (29, 'Ruth Adhiambo', '+254700100029', 'ruth.adhiambo@example.org'),  -- Kibera Hill Primary School
    (30, 'John Odhiambo', '+254700100030', 'john.odhiambo@example.org'),  -- Lakeview Primary School
    (31, 'Zawadi Mwakio', '+254700100031', 'zawadi.mwakio@example.org'),  -- Mwatate Junior School
    (32, 'Tom Kamau', '+254700100032', 'tom.kamau@example.org');  -- Ridgeway Junior School

-- 5. Site visits (is_ready is true only with computers and a LAN)
INSERT INTO readiness_assessments (institution_id, visit_date, user_count, has_computers, has_lan, is_ready) VALUES
    (1, '2025-02-10', 1200, TRUE, TRUE, TRUE),  -- University of Nairobi
    (2, '2025-02-10', 1200, TRUE, TRUE, TRUE),  -- Kenyatta University
    (3, '2025-02-10', 1200, TRUE, TRUE, TRUE),  -- Jomo Kenyatta University of Agriculture and Technology
    (4, '2025-02-10', 1200, TRUE, TRUE, TRUE),  -- Egerton University
    (5, '2025-02-10', 1200, TRUE, TRUE, TRUE),  -- Maseno University
    (6, '2025-02-10', 1200, TRUE, TRUE, TRUE),  -- Moi University
    (7, '2025-02-10', 1200, TRUE, TRUE, TRUE),  -- University of Eldoret
    (8, '2025-02-10', 1200, TRUE, TRUE, TRUE),  -- Pwani University
    (9, '2025-02-10', 1200, TRUE, TRUE, TRUE),  -- Technical University of Kenya
    (10, '2025-02-10', 1200, TRUE, TRUE, TRUE),  -- Strathmore University
    (11, '2025-02-10', 450, FALSE, FALSE, FALSE),  -- Alliance High School
    (12, '2025-02-10', 450, TRUE, TRUE, TRUE),  -- The Kenya High School
    (13, '2025-02-10', 450, TRUE, TRUE, TRUE),  -- Mangu High School
    (14, '2025-02-10', 450, TRUE, TRUE, TRUE),  -- Starehe Boys Centre and School
    (15, '2025-02-10', 450, TRUE, TRUE, TRUE),  -- Loreto High School Limuru
    (16, '2025-02-10', 450, TRUE, TRUE, TRUE),  -- Maseno School
    (17, '2025-02-10', 450, TRUE, TRUE, TRUE),  -- Kapsabet Boys High School
    (18, '2025-02-10', 450, TRUE, TRUE, TRUE),  -- Nakuru High School
    (19, '2025-02-10', 700, TRUE, FALSE, FALSE),  -- Kenya Medical Training College
    (20, '2025-02-10', 700, TRUE, TRUE, TRUE),  -- Kenya Institute of Highways and Building Technology
    (21, '2025-02-10', 700, TRUE, TRUE, TRUE),  -- Kenya Utalii College
    (22, '2025-02-10', 700, TRUE, TRUE, TRUE),  -- Nairobi Technical Training Institute
    (23, '2025-02-10', 700, TRUE, TRUE, TRUE),  -- Rift Valley Institute of Science and Technology
    (24, '2025-02-10', 700, TRUE, TRUE, TRUE),  -- Kisumu National Polytechnic
    (25, '2025-02-10', 700, FALSE, TRUE, FALSE),  -- Mombasa Technical Training Institute
    (26, '2025-02-10', 700, TRUE, TRUE, TRUE),  -- Eldoret National Polytechnic
    (27, '2025-02-10', 700, TRUE, TRUE, TRUE),  -- Thika Technical Training Institute
    (28, '2025-02-10', 700, FALSE, FALSE, FALSE),  -- Kenya Institute of Mass Communication
    (29, '2025-02-10', 300, FALSE, FALSE, FALSE),  -- Kibera Hill Primary School
    (30, '2025-02-10', 320, TRUE, TRUE, TRUE),  -- Lakeview Primary School
    (31, '2025-02-10', 350, TRUE, TRUE, TRUE),  -- Mwatate Junior School
    (32, '2025-02-10', 280, TRUE, FALSE, FALSE);  -- Ridgeway Junior School

-- 6. Equipment orders (tier_id is NULL when only computers are bought)
INSERT INTO equipment_orders (institution_id, tier_id, computer_qty, computer_cost, lan_cost, total_cost, order_date) VALUES
    (11, 2, 20, 800000.00, 20000.00, 820000.00, '2025-02-12'),  -- Alliance High School
    (19, 3, 0, 0.00, 30000.00, 30000.00, '2025-02-12'),  -- Kenya Medical Training College
    (25, NULL, 15, 600000.00, 0.00, 600000.00, '2025-02-12'),  -- Mombasa Technical Training Institute
    (29, 1, 5, 200000.00, 10000.00, 210000.00, '2025-02-12');  -- Kibera Hill Primary School

-- 7. Installations (KSh 10,000 each; the two pending institutions have none)
INSERT INTO installations (institution_id, fee, installed_on) VALUES
    (1, 10000.00, '2025-02-20'),  -- University of Nairobi
    (2, 10000.00, '2025-02-20'),  -- Kenyatta University
    (3, 10000.00, '2025-02-20'),  -- Jomo Kenyatta University of Agriculture and Technology
    (4, 10000.00, '2025-02-20'),  -- Egerton University
    (5, 10000.00, '2025-02-20'),  -- Maseno University
    (6, 10000.00, '2025-02-20'),  -- Moi University
    (7, 10000.00, '2025-02-20'),  -- University of Eldoret
    (8, 10000.00, '2025-02-20'),  -- Pwani University
    (9, 10000.00, '2025-02-20'),  -- Technical University of Kenya
    (10, 10000.00, '2025-02-20'),  -- Strathmore University
    (11, 10000.00, '2025-02-24'),  -- Alliance High School
    (12, 10000.00, '2025-02-20'),  -- The Kenya High School
    (13, 10000.00, '2025-02-20'),  -- Mangu High School
    (14, 10000.00, '2025-02-20'),  -- Starehe Boys Centre and School
    (15, 10000.00, '2025-02-20'),  -- Loreto High School Limuru
    (16, 10000.00, '2025-02-20'),  -- Maseno School
    (17, 10000.00, '2025-02-20'),  -- Kapsabet Boys High School
    (18, 10000.00, '2025-02-20'),  -- Nakuru High School
    (19, 10000.00, '2025-02-24'),  -- Kenya Medical Training College
    (20, 10000.00, '2025-02-20'),  -- Kenya Institute of Highways and Building Technology
    (21, 10000.00, '2025-02-20'),  -- Kenya Utalii College
    (22, 10000.00, '2025-02-20'),  -- Nairobi Technical Training Institute
    (23, 10000.00, '2025-02-20'),  -- Rift Valley Institute of Science and Technology
    (24, 10000.00, '2025-02-20'),  -- Kisumu National Polytechnic
    (25, 10000.00, '2025-02-24'),  -- Mombasa Technical Training Institute
    (26, 10000.00, '2025-02-20'),  -- Eldoret National Polytechnic
    (27, 10000.00, '2025-02-20'),  -- Thika Technical Training Institute
    (29, 10000.00, '2025-02-24'),  -- Kibera Hill Primary School
    (30, 10000.00, '2025-02-20'),  -- Lakeview Primary School
    (31, 10000.00, '2025-02-20');  -- Mwatate Junior School

-- 8. Subscriptions (plan_id 1..5 = 4, 10, 20, 25, 50 Mbps)
INSERT INTO subscriptions (institution_id, plan_id, start_date, status) VALUES
    (1, 5, '2025-03-01', 'active'),  -- subscription 1: University of Nairobi, 50 Mbps
    (2, 5, '2025-03-01', 'active'),  -- subscription 2: Kenyatta University, 50 Mbps
    (3, 5, '2025-03-01', 'active'),  -- subscription 3: Jomo Kenyatta University of Agriculture and Technology, 50 Mbps
    (4, 5, '2025-03-01', 'active'),  -- subscription 4: Egerton University, 50 Mbps
    (5, 5, '2025-03-01', 'active'),  -- subscription 5: Maseno University, 50 Mbps
    (6, 5, '2025-03-01', 'active'),  -- subscription 6: Moi University, 50 Mbps
    (7, 5, '2025-03-01', 'active'),  -- subscription 7: University of Eldoret, 50 Mbps
    (8, 5, '2025-03-01', 'active'),  -- subscription 8: Pwani University, 50 Mbps
    (9, 5, '2025-03-01', 'active'),  -- subscription 9: Technical University of Kenya, 50 Mbps
    (10, 5, '2025-03-01', 'active'),  -- subscription 10: Strathmore University, 50 Mbps
    (11, 2, '2025-03-01', 'active'),  -- subscription 11: Alliance High School, 10 Mbps
    (12, 2, '2025-03-01', 'active'),  -- subscription 12: The Kenya High School, 10 Mbps
    (13, 2, '2025-03-01', 'active'),  -- subscription 13: Mangu High School, 10 Mbps
    (14, 2, '2025-03-01', 'active'),  -- subscription 14: Starehe Boys Centre and School, 10 Mbps
    (15, 2, '2025-03-01', 'active'),  -- subscription 15: Loreto High School Limuru, 10 Mbps
    (16, 2, '2025-03-01', 'active'),  -- subscription 16: Maseno School, 10 Mbps
    (17, 2, '2025-03-01', 'active'),  -- subscription 17: Kapsabet Boys High School, 10 Mbps
    (18, 2, '2025-03-01', 'disconnected'),  -- subscription 18: Nakuru High School, 10 Mbps
    (19, 3, '2025-03-01', 'active'),  -- subscription 19: Kenya Medical Training College, 20 Mbps
    (20, 3, '2025-03-01', 'active'),  -- subscription 20: Kenya Institute of Highways and Building Technology, 20 Mbps
    (21, 3, '2025-03-01', 'active'),  -- subscription 21: Kenya Utalii College, 20 Mbps
    (22, 3, '2025-03-01', 'active'),  -- subscription 22: Nairobi Technical Training Institute, 20 Mbps
    (23, 3, '2025-03-01', 'active'),  -- subscription 23: Rift Valley Institute of Science and Technology, 20 Mbps
    (24, 3, '2025-03-01', 'active'),  -- subscription 24: Kisumu National Polytechnic, 20 Mbps
    (25, 3, '2025-03-01', 'active'),  -- subscription 25: Mombasa Technical Training Institute, 20 Mbps
    (26, 3, '2025-03-01', 'active'),  -- subscription 26: Eldoret National Polytechnic, 20 Mbps
    (27, 3, '2025-03-01', 'active'),  -- subscription 27: Thika Technical Training Institute, 20 Mbps
    (29, 1, '2025-03-01', 'active'),  -- subscription 28: Kibera Hill Primary School, 4 Mbps
    (30, 1, '2025-03-01', 'active'),  -- subscription 29: Lakeview Primary School, 4 Mbps
    (31, 1, '2025-03-01', 'active');  -- subscription 30: Mwatate Junior School, 4 Mbps

-- 9. Upgrades (discount is 10 percent of the cost of the bandwidth upgraded to)
INSERT INTO upgrades (subscription_id, old_plan_id, new_plan_id, discount_rate, upgraded_on) VALUES
    (19, 3, 5, 0.10, '2025-04-01'),  -- Kenya Medical Training College: 20 to 50 Mbps
    (11, 2, 4, 0.10, '2025-04-01');  -- Alliance High School: 10 to 25 Mbps

UPDATE subscriptions SET plan_id = 5 WHERE subscription_id = 19;  -- Kenya Medical Training College
UPDATE subscriptions SET plan_id = 4 WHERE subscription_id = 11;  -- Alliance High School

-- 10. Payments (registration, installation, monthly and reconnection)
INSERT INTO payments (institution_id, payment_type, amount, paid_on, billing_month) VALUES
    (1, 'registration', 8500.00, '2025-01-06', NULL),  -- registration: University of Nairobi
    (1, 'installation', 10000.00, '2025-02-20', NULL),  -- installation: University of Nairobi
    (2, 'registration', 8500.00, '2025-01-07', NULL),  -- registration: Kenyatta University
    (2, 'installation', 10000.00, '2025-02-20', NULL),  -- installation: Kenyatta University
    (3, 'registration', 8500.00, '2025-01-08', NULL),  -- registration: Jomo Kenyatta University of Agriculture and Technology
    (3, 'installation', 10000.00, '2025-02-20', NULL),  -- installation: Jomo Kenyatta University of Agriculture and Technology
    (4, 'registration', 8500.00, '2025-01-09', NULL),  -- registration: Egerton University
    (4, 'installation', 10000.00, '2025-02-20', NULL),  -- installation: Egerton University
    (5, 'registration', 8500.00, '2025-01-10', NULL),  -- registration: Maseno University
    (5, 'installation', 10000.00, '2025-02-20', NULL),  -- installation: Maseno University
    (6, 'registration', 8500.00, '2025-01-11', NULL),  -- registration: Moi University
    (6, 'installation', 10000.00, '2025-02-20', NULL),  -- installation: Moi University
    (7, 'registration', 8500.00, '2025-01-12', NULL),  -- registration: University of Eldoret
    (7, 'installation', 10000.00, '2025-02-20', NULL),  -- installation: University of Eldoret
    (8, 'registration', 8500.00, '2025-01-13', NULL),  -- registration: Pwani University
    (8, 'installation', 10000.00, '2025-02-20', NULL),  -- installation: Pwani University
    (9, 'registration', 8500.00, '2025-01-14', NULL),  -- registration: Technical University of Kenya
    (9, 'installation', 10000.00, '2025-02-20', NULL),  -- installation: Technical University of Kenya
    (10, 'registration', 8500.00, '2025-01-15', NULL),  -- registration: Strathmore University
    (10, 'installation', 10000.00, '2025-02-20', NULL),  -- installation: Strathmore University
    (11, 'registration', 8500.00, '2025-01-16', NULL),  -- registration: Alliance High School
    (11, 'installation', 10000.00, '2025-02-24', NULL),  -- installation: Alliance High School
    (12, 'registration', 8500.00, '2025-01-17', NULL),  -- registration: The Kenya High School
    (12, 'installation', 10000.00, '2025-02-20', NULL),  -- installation: The Kenya High School
    (13, 'registration', 8500.00, '2025-01-18', NULL),  -- registration: Mangu High School
    (13, 'installation', 10000.00, '2025-02-20', NULL),  -- installation: Mangu High School
    (14, 'registration', 8500.00, '2025-01-19', NULL),  -- registration: Starehe Boys Centre and School
    (14, 'installation', 10000.00, '2025-02-20', NULL),  -- installation: Starehe Boys Centre and School
    (15, 'registration', 8500.00, '2025-01-20', NULL),  -- registration: Loreto High School Limuru
    (15, 'installation', 10000.00, '2025-02-20', NULL),  -- installation: Loreto High School Limuru
    (16, 'registration', 8500.00, '2025-01-21', NULL),  -- registration: Maseno School
    (16, 'installation', 10000.00, '2025-02-20', NULL),  -- installation: Maseno School
    (17, 'registration', 8500.00, '2025-01-22', NULL),  -- registration: Kapsabet Boys High School
    (17, 'installation', 10000.00, '2025-02-20', NULL),  -- installation: Kapsabet Boys High School
    (18, 'registration', 8500.00, '2025-01-23', NULL),  -- registration: Nakuru High School
    (18, 'installation', 10000.00, '2025-02-20', NULL),  -- installation: Nakuru High School
    (19, 'registration', 8500.00, '2025-01-24', NULL),  -- registration: Kenya Medical Training College
    (19, 'installation', 10000.00, '2025-02-24', NULL),  -- installation: Kenya Medical Training College
    (20, 'registration', 8500.00, '2025-01-25', NULL),  -- registration: Kenya Institute of Highways and Building Technology
    (20, 'installation', 10000.00, '2025-02-20', NULL),  -- installation: Kenya Institute of Highways and Building Technology
    (21, 'registration', 8500.00, '2025-01-26', NULL),  -- registration: Kenya Utalii College
    (21, 'installation', 10000.00, '2025-02-20', NULL),  -- installation: Kenya Utalii College
    (22, 'registration', 8500.00, '2025-01-27', NULL),  -- registration: Nairobi Technical Training Institute
    (22, 'installation', 10000.00, '2025-02-20', NULL),  -- installation: Nairobi Technical Training Institute
    (23, 'registration', 8500.00, '2025-01-28', NULL),  -- registration: Rift Valley Institute of Science and Technology
    (23, 'installation', 10000.00, '2025-02-20', NULL),  -- installation: Rift Valley Institute of Science and Technology
    (24, 'registration', 8500.00, '2025-01-29', NULL),  -- registration: Kisumu National Polytechnic
    (24, 'installation', 10000.00, '2025-02-20', NULL),  -- installation: Kisumu National Polytechnic
    (25, 'registration', 8500.00, '2025-01-30', NULL),  -- registration: Mombasa Technical Training Institute
    (25, 'installation', 10000.00, '2025-02-24', NULL),  -- installation: Mombasa Technical Training Institute
    (26, 'registration', 8500.00, '2025-01-31', NULL),  -- registration: Eldoret National Polytechnic
    (26, 'installation', 10000.00, '2025-02-20', NULL),  -- installation: Eldoret National Polytechnic
    (27, 'registration', 8500.00, '2025-02-01', NULL),  -- registration: Thika Technical Training Institute
    (27, 'installation', 10000.00, '2025-02-20', NULL),  -- installation: Thika Technical Training Institute
    (28, 'registration', 8500.00, '2025-02-02', NULL),  -- registration: Kenya Institute of Mass Communication
    (29, 'registration', 8500.00, '2025-02-03', NULL),  -- registration: Kibera Hill Primary School
    (29, 'installation', 10000.00, '2025-02-24', NULL),  -- installation: Kibera Hill Primary School
    (30, 'registration', 8500.00, '2025-02-04', NULL),  -- registration: Lakeview Primary School
    (30, 'installation', 10000.00, '2025-02-20', NULL),  -- installation: Lakeview Primary School
    (31, 'registration', 8500.00, '2025-02-05', NULL),  -- registration: Mwatate Junior School
    (31, 'installation', 10000.00, '2025-02-20', NULL),  -- installation: Mwatate Junior School
    (32, 'registration', 8500.00, '2025-02-06', NULL),  -- registration: Ridgeway Junior School
    (1, 'monthly', 7000.00, '2025-03-05', '2025-03'),  -- March bill: University of Nairobi
    (2, 'monthly', 7000.00, '2025-03-06', '2025-03'),  -- March bill: Kenyatta University
    (3, 'monthly', 7000.00, '2025-03-07', '2025-03'),  -- March bill: Jomo Kenyatta University of Agriculture and Technology
    (4, 'monthly', 7000.00, '2025-04-14', '2025-03'),  -- March bill: Egerton University
    (5, 'monthly', 7000.00, '2025-03-09', '2025-03'),  -- March bill: Maseno University
    (6, 'monthly', 7000.00, '2025-03-10', '2025-03'),  -- March bill: Moi University
    (7, 'monthly', 7000.00, '2025-03-11', '2025-03'),  -- March bill: University of Eldoret
    (8, 'monthly', 7000.00, '2025-03-12', '2025-03'),  -- March bill: Pwani University
    (9, 'monthly', 7000.00, '2025-03-13', '2025-03'),  -- March bill: Technical University of Kenya
    (10, 'monthly', 7000.00, '2025-03-14', '2025-03'),  -- March bill: Strathmore University
    (11, 'monthly', 2000.00, '2025-03-15', '2025-03'),  -- March bill: Alliance High School
    (12, 'monthly', 2000.00, '2025-03-16', '2025-03'),  -- March bill: The Kenya High School
    (13, 'monthly', 2000.00, '2025-03-17', '2025-03'),  -- March bill: Mangu High School
    (14, 'monthly', 2000.00, '2025-03-18', '2025-03'),  -- March bill: Starehe Boys Centre and School
    (15, 'monthly', 2000.00, '2025-03-19', '2025-03'),  -- March bill: Loreto High School Limuru
    (16, 'monthly', 2000.00, '2025-04-05', '2025-03'),  -- March bill: Maseno School
    (17, 'monthly', 2000.00, '2025-03-21', '2025-03'),  -- March bill: Kapsabet Boys High School
    (19, 'monthly', 3500.00, '2025-03-23', '2025-03'),  -- March bill: Kenya Medical Training College
    (20, 'monthly', 3500.00, '2025-03-24', '2025-03'),  -- March bill: Kenya Institute of Highways and Building Technology
    (21, 'monthly', 3500.00, '2025-03-05', '2025-03'),  -- March bill: Kenya Utalii College
    (22, 'monthly', 3500.00, '2025-03-06', '2025-03'),  -- March bill: Nairobi Technical Training Institute
    (23, 'monthly', 3500.00, '2025-03-07', '2025-03'),  -- March bill: Rift Valley Institute of Science and Technology
    (24, 'monthly', 3500.00, '2025-03-08', '2025-03'),  -- March bill: Kisumu National Polytechnic
    (25, 'monthly', 3500.00, '2025-03-09', '2025-03'),  -- March bill: Mombasa Technical Training Institute
    (26, 'monthly', 3500.00, '2025-03-10', '2025-03'),  -- March bill: Eldoret National Polytechnic
    (27, 'monthly', 3500.00, '2025-03-11', '2025-03'),  -- March bill: Thika Technical Training Institute
    (29, 'monthly', 1200.00, '2025-03-12', '2025-03'),  -- March bill: Kibera Hill Primary School
    (30, 'monthly', 1200.00, '2025-03-13', '2025-03'),  -- March bill: Lakeview Primary School
    (31, 'monthly', 1200.00, '2025-03-14', '2025-03'),  -- March bill: Mwatate Junior School
    (19, 'monthly', 6300.00, '2025-04-28', '2025-04'),  -- April bill at the discounted rate: Kenya Medical Training College
    (11, 'monthly', 3600.00, '2025-04-29', '2025-04'),  -- April bill at the discounted rate: Alliance High School
    (4, 'reconnection', 1000.00, '2025-04-14', '2025-03');  -- reconnection: Egerton University

-- 11. Overdue fines (15 percent of the March bill, applied once)
INSERT INTO overdue_fines (institution_id, billing_month, fine_amount, settled) VALUES
    (4, '2025-03', 1050.00, TRUE),  -- Egerton University
    (16, '2025-03', 300.00, TRUE),  -- Maseno School
    (18, '2025-03', 300.00, FALSE);  -- Nakuru High School

-- 12. Disconnections (both unpaid after the 10th of April)
INSERT INTO disconnections (institution_id, disconnected_on, reconnected, reconnected_on) VALUES
    (4, '2025-04-11', TRUE, '2025-04-15'),  -- Egerton University, reconnected on 15 April
    (18, '2025-04-11', FALSE, NULL);  -- Nakuru High School, still disconnected

-- 13. Self-check: the counts below should read
-- institutions 32, contact_persons 32, readiness_assessments 32, equipment_orders 4,
-- installations 30, subscriptions 30, upgrades 2, payments 94, overdue_fines 3,
-- disconnections 2, bandwidth_plans 5, lan_node_tiers 4.
SELECT 'institutions' AS table_name, COUNT(*) AS row_count FROM institutions
UNION ALL SELECT 'contact_persons', COUNT(*) FROM contact_persons
UNION ALL SELECT 'readiness_assessments', COUNT(*) FROM readiness_assessments
UNION ALL SELECT 'equipment_orders', COUNT(*) FROM equipment_orders
UNION ALL SELECT 'installations', COUNT(*) FROM installations
UNION ALL SELECT 'subscriptions', COUNT(*) FROM subscriptions
UNION ALL SELECT 'upgrades', COUNT(*) FROM upgrades
UNION ALL SELECT 'payments', COUNT(*) FROM payments
UNION ALL SELECT 'overdue_fines', COUNT(*) FROM overdue_fines
UNION ALL SELECT 'disconnections', COUNT(*) FROM disconnections
UNION ALL SELECT 'bandwidth_plans', COUNT(*) FROM bandwidth_plans
UNION ALL SELECT 'lan_node_tiers', COUNT(*) FROM lan_node_tiers;
