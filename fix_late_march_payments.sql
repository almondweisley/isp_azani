-- azani_db_dml.sql
-- Sample data for the Azani ISP database. Run azani_db_ddl.sql first.
-- Clears every table first, so you can run it more than once.
-- Every figure comes from the SCO200 brief. The data is a snapshot taken on 2026-11-30.
--
-- Timeline
--   August 2026     registration, site visits, equipment, follow-up visits, installation
--   1 Sept 2026     every subscription starts
--   Sept, Oct, Nov  three billing months. A bill falls due at the end of its month;
--                   unpaid then = 15 percent fine (once); bill + fine unpaid by the 10th
--                   of the next month = disconnection on the 11th; reconnection needs
--                   the bill, the fine and the KSh 1,000 fee.
--   30 Nov 2026     snapshot: November bills fall due today, so none is overdue yet.
--
-- Rules the rows follow (they match the Java services)
--   * A late month is ONE monthly payment of bill + fine; its fine row is settled.
--   * A month is billed if the service was live on its 1st; no charge while disconnected.
--   * Upgrade discount (10 percent of the new plan) applies in the upgrade month only.
--   * A reconnection payment carries the billing month that caused the disconnection.
--   * A currently disconnected institution has status 'suspended' and its
--     subscription status 'disconnected'.
--
-- Scenarios (everyone else pays on time every month)
--   4  Egerton           Sept unpaid -> fined, disconnected 11 Oct, paid + reconnected 14 Oct
--   6  Moi               Oct unpaid -> fined, disconnected 11 Nov, still disconnected (defaulter)
--   8  Pwani             Oct unpaid -> fined, disconnected 11 Nov, paid + reconnected 13 Nov
--   16 Maseno School     Sept paid late on 5 Oct -> fined, not disconnected
--   18 Nakuru High       Sept and Oct unpaid -> two fines, disconnected 11 Oct (defaulter)
--   21 Kenya Utalii      Oct unpaid -> disconnected 11 Nov; paid bill + fine 20 Nov,
--                        reconnection fee still unpaid, so still disconnected
--   24 Kisumu Poly       Oct paid late on 6 Nov -> fined, not disconnected
--   29 Kibera Hill       Sept unpaid -> disconnected 11 Oct, paid + reconnected 20 Oct
--   30 Lakeview          Sept paid late on 3 Oct -> fined, not disconnected
--   31 Mwatate           Oct paid late on 4 Nov; upgraded 4 -> 10 Mbps on 5 Nov
--   11 Alliance, 19 KMTC upgraded on 1 Oct (10 -> 25 and 20 -> 50 Mbps)
--   11, 19, 25, 29       failed the first visit, bought equipment, passed a follow-up visit
--   28 KIMC, 32 Ridgeway registered only: not ready, no purchase, no installation
--   9 TU-K, 27 Thika TTI November not yet paid (due today, not overdue)
--
-- Institution ids run 1 to 32 in the order of section 3; the clear step restarts
-- every AUTO_INCREMENT counter at 1, so the literal ids below are safe.

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
    ('University of Nairobi', 'college', 'University Way, Nairobi', '2026-08-03', 'active'),  -- id 1
    ('Kenyatta University', 'college', 'Kahawa, Nairobi', '2026-08-03', 'active'),  -- id 2
    ('Jomo Kenyatta University of Agriculture and Technology', 'college', 'Juja, Kiambu County', '2026-08-03', 'active'),  -- id 3
    ('Egerton University', 'college', 'Njoro, Nakuru County', '2026-08-04', 'active'),  -- id 4
    ('Maseno University', 'college', 'Maseno, Kisumu County', '2026-08-04', 'active'),  -- id 5
    ('Moi University', 'college', 'Kesses, Uasin Gishu County', '2026-08-04', 'suspended'),  -- id 6
    ('University of Eldoret', 'college', 'Kapseret, Uasin Gishu County', '2026-08-05', 'active'),  -- id 7
    ('Pwani University', 'college', 'Kilifi, Kilifi County', '2026-08-05', 'active'),  -- id 8
    ('Technical University of Kenya', 'college', 'Haile Selassie Avenue, Nairobi', '2026-08-05', 'active'),  -- id 9
    ('Strathmore University', 'college', 'Madaraka Estate, Nairobi', '2026-08-06', 'active'),  -- id 10
    ('Alliance High School', 'senior', 'Kikuyu, Kiambu County', '2026-08-06', 'active'),  -- id 11
    ('The Kenya High School', 'senior', 'Kileleshwa, Nairobi', '2026-08-06', 'active'),  -- id 12
    ('Mangu High School', 'senior', 'Thika, Kiambu County', '2026-08-07', 'active'),  -- id 13
    ('Starehe Boys Centre and School', 'senior', 'Nairobi', '2026-08-07', 'active'),  -- id 14
    ('Loreto High School Limuru', 'senior', 'Limuru, Kiambu County', '2026-08-07', 'active'),  -- id 15
    ('Maseno School', 'senior', 'Maseno, Kisumu County', '2026-08-08', 'active'),  -- id 16
    ('Kapsabet Boys High School', 'senior', 'Kapsabet, Nandi County', '2026-08-08', 'active'),  -- id 17
    ('Nakuru High School', 'senior', 'Nakuru, Nakuru County', '2026-08-08', 'suspended'),  -- id 18
    ('Kenya Medical Training College', 'college', 'Old Mbagathi Road, Nairobi', '2026-08-09', 'active'),  -- id 19
    ('Kenya Institute of Highways and Building Technology', 'college', 'Ngong Road, Nairobi', '2026-08-09', 'active'),  -- id 20
    ('Kenya Utalii College', 'college', 'Thika Road, Nairobi', '2026-08-09', 'suspended'),  -- id 21
    ('Nairobi Technical Training Institute', 'college', 'Ngara, Nairobi', '2026-08-10', 'active'),  -- id 22
    ('Rift Valley Institute of Science and Technology', 'college', 'Nakuru, Nakuru County', '2026-08-10', 'active'),  -- id 23
    ('Kisumu National Polytechnic', 'college', 'Kisumu, Kisumu County', '2026-08-10', 'active'),  -- id 24
    ('Mombasa Technical Training Institute', 'college', 'Mombasa, Mombasa County', '2026-08-11', 'active'),  -- id 25
    ('Eldoret National Polytechnic', 'college', 'Eldoret, Uasin Gishu County', '2026-08-11', 'active'),  -- id 26
    ('Thika Technical Training Institute', 'college', 'Thika, Kiambu County', '2026-08-11', 'active'),  -- id 27
    ('Kenya Institute of Mass Communication', 'college', 'South C, Nairobi', '2026-08-12', 'inactive'),  -- id 28
    ('Kibera Hill Primary School', 'primary', 'Kibera, Nairobi', '2026-08-12', 'active'),  -- id 29
    ('Lakeview Primary School', 'primary', 'Kisumu, Kisumu County', '2026-08-12', 'active'),  -- id 30
    ('Mwatate Junior School', 'junior', 'Mwatate, Taita Taveta County', '2026-08-13', 'active'),  -- id 31
    ('Ridgeway Junior School', 'junior', 'Ruiru, Kiambu County', '2026-08-13', 'inactive');  -- id 32

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

-- 5. Site visits (is_ready is true only with computers and a LAN).
--    Rows 33 to 36 are follow-up visits after the equipment purchase; the latest visit counts.
INSERT INTO readiness_assessments (institution_id, visit_date, user_count, has_computers, has_lan, is_ready) VALUES
    (1, '2026-08-17', 1200, TRUE, TRUE, TRUE),  -- University of Nairobi
    (2, '2026-08-17', 1200, TRUE, TRUE, TRUE),  -- Kenyatta University
    (3, '2026-08-17', 1200, TRUE, TRUE, TRUE),  -- Jomo Kenyatta University of Agriculture and Technology
    (4, '2026-08-17', 1200, TRUE, TRUE, TRUE),  -- Egerton University
    (5, '2026-08-17', 1200, TRUE, TRUE, TRUE),  -- Maseno University
    (6, '2026-08-17', 1200, TRUE, TRUE, TRUE),  -- Moi University
    (7, '2026-08-17', 1200, TRUE, TRUE, TRUE),  -- University of Eldoret
    (8, '2026-08-17', 1200, TRUE, TRUE, TRUE),  -- Pwani University
    (9, '2026-08-17', 1200, TRUE, TRUE, TRUE),  -- Technical University of Kenya
    (10, '2026-08-17', 1200, TRUE, TRUE, TRUE),  -- Strathmore University
    (11, '2026-08-17', 450, FALSE, FALSE, FALSE),  -- Alliance High School
    (12, '2026-08-17', 450, TRUE, TRUE, TRUE),  -- The Kenya High School
    (13, '2026-08-17', 450, TRUE, TRUE, TRUE),  -- Mangu High School
    (14, '2026-08-17', 450, TRUE, TRUE, TRUE),  -- Starehe Boys Centre and School
    (15, '2026-08-17', 450, TRUE, TRUE, TRUE),  -- Loreto High School Limuru
    (16, '2026-08-17', 450, TRUE, TRUE, TRUE),  -- Maseno School
    (17, '2026-08-17', 450, TRUE, TRUE, TRUE),  -- Kapsabet Boys High School
    (18, '2026-08-17', 450, TRUE, TRUE, TRUE),  -- Nakuru High School
    (19, '2026-08-17', 700, TRUE, FALSE, FALSE),  -- Kenya Medical Training College
    (20, '2026-08-17', 700, TRUE, TRUE, TRUE),  -- Kenya Institute of Highways and Building Technology
    (21, '2026-08-17', 700, TRUE, TRUE, TRUE),  -- Kenya Utalii College
    (22, '2026-08-17', 700, TRUE, TRUE, TRUE),  -- Nairobi Technical Training Institute
    (23, '2026-08-17', 700, TRUE, TRUE, TRUE),  -- Rift Valley Institute of Science and Technology
    (24, '2026-08-17', 700, TRUE, TRUE, TRUE),  -- Kisumu National Polytechnic
    (25, '2026-08-17', 700, FALSE, TRUE, FALSE),  -- Mombasa Technical Training Institute
    (26, '2026-08-17', 700, TRUE, TRUE, TRUE),  -- Eldoret National Polytechnic
    (27, '2026-08-17', 700, TRUE, TRUE, TRUE),  -- Thika Technical Training Institute
    (28, '2026-08-17', 700, FALSE, FALSE, FALSE),  -- Kenya Institute of Mass Communication
    (29, '2026-08-17', 300, FALSE, FALSE, FALSE),  -- Kibera Hill Primary School
    (30, '2026-08-17', 320, TRUE, TRUE, TRUE),  -- Lakeview Primary School
    (31, '2026-08-17', 350, TRUE, TRUE, TRUE),  -- Mwatate Junior School
    (32, '2026-08-17', 280, TRUE, FALSE, FALSE),  -- Ridgeway Junior School
    (11, '2026-08-26', 450, TRUE, TRUE, TRUE),  -- Alliance High School: follow-up after the equipment purchase
    (19, '2026-08-26', 700, TRUE, TRUE, TRUE),  -- Kenya Medical Training College: follow-up after the equipment purchase
    (25, '2026-08-26', 700, TRUE, TRUE, TRUE),  -- Mombasa Technical Training Institute: follow-up after the equipment purchase
    (29, '2026-08-26', 300, TRUE, TRUE, TRUE);  -- Kibera Hill Primary School: follow-up after the equipment purchase

-- 6. Equipment orders (tier_id is NULL when only computers are bought)
INSERT INTO equipment_orders (institution_id, tier_id, computer_qty, computer_cost, lan_cost, total_cost, order_date) VALUES
    (11, 2, 20, 800000.00, 20000.00, 820000.00, '2026-08-19'),  -- Alliance High School
    (19, 3, 0, 0.00, 30000.00, 30000.00, '2026-08-19'),  -- Kenya Medical Training College
    (25, NULL, 15, 600000.00, 0.00, 600000.00, '2026-08-19'),  -- Mombasa Technical Training Institute
    (29, 1, 5, 200000.00, 10000.00, 210000.00, '2026-08-19');  -- Kibera Hill Primary School

-- 7. Installations (KSh 10,000 each; equipment buyers after their follow-up visit;
--    KIMC and Ridgeway have none)
INSERT INTO installations (institution_id, fee, installed_on) VALUES
    (1, 10000.00, '2026-08-24'),  -- University of Nairobi
    (2, 10000.00, '2026-08-24'),  -- Kenyatta University
    (3, 10000.00, '2026-08-24'),  -- Jomo Kenyatta University of Agriculture and Technology
    (4, 10000.00, '2026-08-24'),  -- Egerton University
    (5, 10000.00, '2026-08-24'),  -- Maseno University
    (6, 10000.00, '2026-08-24'),  -- Moi University
    (7, 10000.00, '2026-08-24'),  -- University of Eldoret
    (8, 10000.00, '2026-08-24'),  -- Pwani University
    (9, 10000.00, '2026-08-24'),  -- Technical University of Kenya
    (10, 10000.00, '2026-08-24'),  -- Strathmore University
    (11, 10000.00, '2026-08-28'),  -- Alliance High School
    (12, 10000.00, '2026-08-24'),  -- The Kenya High School
    (13, 10000.00, '2026-08-24'),  -- Mangu High School
    (14, 10000.00, '2026-08-24'),  -- Starehe Boys Centre and School
    (15, 10000.00, '2026-08-24'),  -- Loreto High School Limuru
    (16, 10000.00, '2026-08-24'),  -- Maseno School
    (17, 10000.00, '2026-08-24'),  -- Kapsabet Boys High School
    (18, 10000.00, '2026-08-24'),  -- Nakuru High School
    (19, 10000.00, '2026-08-28'),  -- Kenya Medical Training College
    (20, 10000.00, '2026-08-24'),  -- Kenya Institute of Highways and Building Technology
    (21, 10000.00, '2026-08-24'),  -- Kenya Utalii College
    (22, 10000.00, '2026-08-24'),  -- Nairobi Technical Training Institute
    (23, 10000.00, '2026-08-24'),  -- Rift Valley Institute of Science and Technology
    (24, 10000.00, '2026-08-24'),  -- Kisumu National Polytechnic
    (25, 10000.00, '2026-08-28'),  -- Mombasa Technical Training Institute
    (26, 10000.00, '2026-08-24'),  -- Eldoret National Polytechnic
    (27, 10000.00, '2026-08-24'),  -- Thika Technical Training Institute
    (29, 10000.00, '2026-08-28'),  -- Kibera Hill Primary School
    (30, 10000.00, '2026-08-24'),  -- Lakeview Primary School
    (31, 10000.00, '2026-08-24');  -- Mwatate Junior School

-- 8. Subscriptions (plan_id 1..5 = 4, 10, 20, 25, 50 Mbps), all from 1 Sept 2026
INSERT INTO subscriptions (institution_id, plan_id, start_date, status) VALUES
    (1, 5, '2026-09-01', 'active'),  -- subscription 1: University of Nairobi, 50 Mbps
    (2, 5, '2026-09-01', 'active'),  -- subscription 2: Kenyatta University, 50 Mbps
    (3, 5, '2026-09-01', 'active'),  -- subscription 3: Jomo Kenyatta University of Agriculture and Technology, 50 Mbps
    (4, 5, '2026-09-01', 'active'),  -- subscription 4: Egerton University, 50 Mbps
    (5, 5, '2026-09-01', 'active'),  -- subscription 5: Maseno University, 50 Mbps
    (6, 5, '2026-09-01', 'disconnected'),  -- subscription 6: Moi University, 50 Mbps
    (7, 5, '2026-09-01', 'active'),  -- subscription 7: University of Eldoret, 50 Mbps
    (8, 5, '2026-09-01', 'active'),  -- subscription 8: Pwani University, 50 Mbps
    (9, 5, '2026-09-01', 'active'),  -- subscription 9: Technical University of Kenya, 50 Mbps
    (10, 5, '2026-09-01', 'active'),  -- subscription 10: Strathmore University, 50 Mbps
    (11, 2, '2026-09-01', 'active'),  -- subscription 11: Alliance High School, 10 Mbps
    (12, 2, '2026-09-01', 'active'),  -- subscription 12: The Kenya High School, 10 Mbps
    (13, 2, '2026-09-01', 'active'),  -- subscription 13: Mangu High School, 10 Mbps
    (14, 2, '2026-09-01', 'active'),  -- subscription 14: Starehe Boys Centre and School, 10 Mbps
    (15, 2, '2026-09-01', 'active'),  -- subscription 15: Loreto High School Limuru, 10 Mbps
    (16, 2, '2026-09-01', 'active'),  -- subscription 16: Maseno School, 10 Mbps
    (17, 2, '2026-09-01', 'active'),  -- subscription 17: Kapsabet Boys High School, 10 Mbps
    (18, 2, '2026-09-01', 'disconnected'),  -- subscription 18: Nakuru High School, 10 Mbps
    (19, 3, '2026-09-01', 'active'),  -- subscription 19: Kenya Medical Training College, 20 Mbps
    (20, 3, '2026-09-01', 'active'),  -- subscription 20: Kenya Institute of Highways and Building Technology, 20 Mbps
    (21, 3, '2026-09-01', 'disconnected'),  -- subscription 21: Kenya Utalii College, 20 Mbps
    (22, 3, '2026-09-01', 'active'),  -- subscription 22: Nairobi Technical Training Institute, 20 Mbps
    (23, 3, '2026-09-01', 'active'),  -- subscription 23: Rift Valley Institute of Science and Technology, 20 Mbps
    (24, 3, '2026-09-01', 'active'),  -- subscription 24: Kisumu National Polytechnic, 20 Mbps
    (25, 3, '2026-09-01', 'active'),  -- subscription 25: Mombasa Technical Training Institute, 20 Mbps
    (26, 3, '2026-09-01', 'active'),  -- subscription 26: Eldoret National Polytechnic, 20 Mbps
    (27, 3, '2026-09-01', 'active'),  -- subscription 27: Thika Technical Training Institute, 20 Mbps
    (29, 1, '2026-09-01', 'active'),  -- subscription 28: Kibera Hill Primary School, 4 Mbps
    (30, 1, '2026-09-01', 'active'),  -- subscription 29: Lakeview Primary School, 4 Mbps
    (31, 1, '2026-09-01', 'active');  -- subscription 30: Mwatate Junior School, 4 Mbps

-- 9. Upgrades (discount is 10 percent of the cost of the bandwidth upgraded to,
--    applied in the upgrade month only)
INSERT INTO upgrades (subscription_id, old_plan_id, new_plan_id, discount_rate, upgraded_on) VALUES
    (11, 2, 4, 0.10, '2026-10-01'),  -- Alliance High School: 10 to 25 Mbps
    (19, 3, 5, 0.10, '2026-10-01'),  -- Kenya Medical Training College: 20 to 50 Mbps
    (30, 1, 2, 0.10, '2026-11-05');  -- Mwatate Junior School: 4 to 10 Mbps

UPDATE subscriptions SET plan_id = 4 WHERE subscription_id = 11;  -- Alliance High School
UPDATE subscriptions SET plan_id = 5 WHERE subscription_id = 19;  -- Kenya Medical Training College
UPDATE subscriptions SET plan_id = 2 WHERE subscription_id = 30;  -- Mwatate Junior School

-- 10. Payments: registration, installation, then monthly and reconnection by month and date
INSERT INTO payments (institution_id, payment_type, amount, paid_on, billing_month) VALUES
    (1, 'registration', 8500.00, '2026-08-03', NULL),  -- registration: University of Nairobi
    (2, 'registration', 8500.00, '2026-08-03', NULL),  -- registration: Kenyatta University
    (3, 'registration', 8500.00, '2026-08-03', NULL),  -- registration: Jomo Kenyatta University of Agriculture and Technology
    (4, 'registration', 8500.00, '2026-08-04', NULL),  -- registration: Egerton University
    (5, 'registration', 8500.00, '2026-08-04', NULL),  -- registration: Maseno University
    (6, 'registration', 8500.00, '2026-08-04', NULL),  -- registration: Moi University
    (7, 'registration', 8500.00, '2026-08-05', NULL),  -- registration: University of Eldoret
    (8, 'registration', 8500.00, '2026-08-05', NULL),  -- registration: Pwani University
    (9, 'registration', 8500.00, '2026-08-05', NULL),  -- registration: Technical University of Kenya
    (10, 'registration', 8500.00, '2026-08-06', NULL),  -- registration: Strathmore University
    (11, 'registration', 8500.00, '2026-08-06', NULL),  -- registration: Alliance High School
    (12, 'registration', 8500.00, '2026-08-06', NULL),  -- registration: The Kenya High School
    (13, 'registration', 8500.00, '2026-08-07', NULL),  -- registration: Mangu High School
    (14, 'registration', 8500.00, '2026-08-07', NULL),  -- registration: Starehe Boys Centre and School
    (15, 'registration', 8500.00, '2026-08-07', NULL),  -- registration: Loreto High School Limuru
    (16, 'registration', 8500.00, '2026-08-08', NULL),  -- registration: Maseno School
    (17, 'registration', 8500.00, '2026-08-08', NULL),  -- registration: Kapsabet Boys High School
    (18, 'registration', 8500.00, '2026-08-08', NULL),  -- registration: Nakuru High School
    (19, 'registration', 8500.00, '2026-08-09', NULL),  -- registration: Kenya Medical Training College
    (20, 'registration', 8500.00, '2026-08-09', NULL),  -- registration: Kenya Institute of Highways and Building Technology
    (21, 'registration', 8500.00, '2026-08-09', NULL),  -- registration: Kenya Utalii College
    (22, 'registration', 8500.00, '2026-08-10', NULL),  -- registration: Nairobi Technical Training Institute
    (23, 'registration', 8500.00, '2026-08-10', NULL),  -- registration: Rift Valley Institute of Science and Technology
    (24, 'registration', 8500.00, '2026-08-10', NULL),  -- registration: Kisumu National Polytechnic
    (25, 'registration', 8500.00, '2026-08-11', NULL),  -- registration: Mombasa Technical Training Institute
    (26, 'registration', 8500.00, '2026-08-11', NULL),  -- registration: Eldoret National Polytechnic
    (27, 'registration', 8500.00, '2026-08-11', NULL),  -- registration: Thika Technical Training Institute
    (28, 'registration', 8500.00, '2026-08-12', NULL),  -- registration: Kenya Institute of Mass Communication
    (29, 'registration', 8500.00, '2026-08-12', NULL),  -- registration: Kibera Hill Primary School
    (30, 'registration', 8500.00, '2026-08-12', NULL),  -- registration: Lakeview Primary School
    (31, 'registration', 8500.00, '2026-08-13', NULL),  -- registration: Mwatate Junior School
    (32, 'registration', 8500.00, '2026-08-13', NULL),  -- registration: Ridgeway Junior School
    (1, 'installation', 10000.00, '2026-08-24', NULL),  -- installation: University of Nairobi
    (2, 'installation', 10000.00, '2026-08-24', NULL),  -- installation: Kenyatta University
    (3, 'installation', 10000.00, '2026-08-24', NULL),  -- installation: Jomo Kenyatta University of Agriculture and Technology
    (4, 'installation', 10000.00, '2026-08-24', NULL),  -- installation: Egerton University
    (5, 'installation', 10000.00, '2026-08-24', NULL),  -- installation: Maseno University
    (6, 'installation', 10000.00, '2026-08-24', NULL),  -- installation: Moi University
    (7, 'installation', 10000.00, '2026-08-24', NULL),  -- installation: University of Eldoret
    (8, 'installation', 10000.00, '2026-08-24', NULL),  -- installation: Pwani University
    (9, 'installation', 10000.00, '2026-08-24', NULL),  -- installation: Technical University of Kenya
    (10, 'installation', 10000.00, '2026-08-24', NULL),  -- installation: Strathmore University
    (11, 'installation', 10000.00, '2026-08-28', NULL),  -- installation: Alliance High School
    (12, 'installation', 10000.00, '2026-08-24', NULL),  -- installation: The Kenya High School
    (13, 'installation', 10000.00, '2026-08-24', NULL),  -- installation: Mangu High School
    (14, 'installation', 10000.00, '2026-08-24', NULL),  -- installation: Starehe Boys Centre and School
    (15, 'installation', 10000.00, '2026-08-24', NULL),  -- installation: Loreto High School Limuru
    (16, 'installation', 10000.00, '2026-08-24', NULL),  -- installation: Maseno School
    (17, 'installation', 10000.00, '2026-08-24', NULL),  -- installation: Kapsabet Boys High School
    (18, 'installation', 10000.00, '2026-08-24', NULL),  -- installation: Nakuru High School
    (19, 'installation', 10000.00, '2026-08-28', NULL),  -- installation: Kenya Medical Training College
    (20, 'installation', 10000.00, '2026-08-24', NULL),  -- installation: Kenya Institute of Highways and Building Technology
    (21, 'installation', 10000.00, '2026-08-24', NULL),  -- installation: Kenya Utalii College
    (22, 'installation', 10000.00, '2026-08-24', NULL),  -- installation: Nairobi Technical Training Institute
    (23, 'installation', 10000.00, '2026-08-24', NULL),  -- installation: Rift Valley Institute of Science and Technology
    (24, 'installation', 10000.00, '2026-08-24', NULL),  -- installation: Kisumu National Polytechnic
    (25, 'installation', 10000.00, '2026-08-28', NULL),  -- installation: Mombasa Technical Training Institute
    (26, 'installation', 10000.00, '2026-08-24', NULL),  -- installation: Eldoret National Polytechnic
    (27, 'installation', 10000.00, '2026-08-24', NULL),  -- installation: Thika Technical Training Institute
    (29, 'installation', 10000.00, '2026-08-28', NULL),  -- installation: Kibera Hill Primary School
    (30, 'installation', 10000.00, '2026-08-24', NULL),  -- installation: Lakeview Primary School
    (31, 'installation', 10000.00, '2026-08-24', NULL),  -- installation: Mwatate Junior School
    (20, 'monthly', 3500.00, '2026-09-05', '2026-09'),  -- 2026-09: Kenya Institute of Highways and Building Technology
    (1, 'monthly', 7000.00, '2026-09-06', '2026-09'),  -- 2026-09: University of Nairobi
    (21, 'monthly', 3500.00, '2026-09-06', '2026-09'),  -- 2026-09: Kenya Utalii College
    (2, 'monthly', 7000.00, '2026-09-07', '2026-09'),  -- 2026-09: Kenyatta University
    (22, 'monthly', 3500.00, '2026-09-07', '2026-09'),  -- 2026-09: Nairobi Technical Training Institute
    (3, 'monthly', 7000.00, '2026-09-08', '2026-09'),  -- 2026-09: Jomo Kenyatta University of Agriculture and Technology
    (23, 'monthly', 3500.00, '2026-09-08', '2026-09'),  -- 2026-09: Rift Valley Institute of Science and Technology
    (24, 'monthly', 3500.00, '2026-09-09', '2026-09'),  -- 2026-09: Kisumu National Polytechnic
    (5, 'monthly', 7000.00, '2026-09-10', '2026-09'),  -- 2026-09: Maseno University
    (25, 'monthly', 3500.00, '2026-09-10', '2026-09'),  -- 2026-09: Mombasa Technical Training Institute
    (6, 'monthly', 7000.00, '2026-09-11', '2026-09'),  -- 2026-09: Moi University
    (26, 'monthly', 3500.00, '2026-09-11', '2026-09'),  -- 2026-09: Eldoret National Polytechnic
    (7, 'monthly', 7000.00, '2026-09-12', '2026-09'),  -- 2026-09: University of Eldoret
    (27, 'monthly', 3500.00, '2026-09-12', '2026-09'),  -- 2026-09: Thika Technical Training Institute
    (8, 'monthly', 7000.00, '2026-09-13', '2026-09'),  -- 2026-09: Pwani University
    (9, 'monthly', 7000.00, '2026-09-14', '2026-09'),  -- 2026-09: Technical University of Kenya
    (10, 'monthly', 7000.00, '2026-09-15', '2026-09'),  -- 2026-09: Strathmore University
    (11, 'monthly', 2000.00, '2026-09-16', '2026-09'),  -- 2026-09: Alliance High School
    (31, 'monthly', 1200.00, '2026-09-16', '2026-09'),  -- 2026-09: Mwatate Junior School
    (12, 'monthly', 2000.00, '2026-09-17', '2026-09'),  -- 2026-09: The Kenya High School
    (13, 'monthly', 2000.00, '2026-09-18', '2026-09'),  -- 2026-09: Mangu High School
    (14, 'monthly', 2000.00, '2026-09-19', '2026-09'),  -- 2026-09: Starehe Boys Centre and School
    (15, 'monthly', 2000.00, '2026-09-20', '2026-09'),  -- 2026-09: Loreto High School Limuru
    (17, 'monthly', 2000.00, '2026-09-22', '2026-09'),  -- 2026-09: Kapsabet Boys High School
    (19, 'monthly', 3500.00, '2026-09-24', '2026-09'),  -- 2026-09: Kenya Medical Training College
    (30, 'monthly', 1380.00, '2026-10-03', '2026-09'),  -- 2026-09 late, bill 1200.00 + fine 180.00: Lakeview Primary School
    (16, 'monthly', 2300.00, '2026-10-05', '2026-09'),  -- 2026-09 late, bill 2000.00 + fine 300.00: Maseno School
    (4, 'monthly', 8050.00, '2026-10-14', '2026-09'),  -- 2026-09 late, bill 7000.00 + fine 1050.00: Egerton University
    (4, 'reconnection', 1000.00, '2026-10-14', '2026-09'),  -- reconnection: Egerton University
    (29, 'monthly', 1380.00, '2026-10-20', '2026-09'),  -- 2026-09 late, bill 1200.00 + fine 180.00: Kibera Hill Primary School
    (29, 'reconnection', 1000.00, '2026-10-20', '2026-09'),  -- reconnection: Kibera Hill Primary School
    (20, 'monthly', 3500.00, '2026-10-05', '2026-10'),  -- 2026-10: Kenya Institute of Highways and Building Technology
    (1, 'monthly', 7000.00, '2026-10-06', '2026-10'),  -- 2026-10: University of Nairobi
    (2, 'monthly', 7000.00, '2026-10-07', '2026-10'),  -- 2026-10: Kenyatta University
    (22, 'monthly', 3500.00, '2026-10-07', '2026-10'),  -- 2026-10: Nairobi Technical Training Institute
    (3, 'monthly', 7000.00, '2026-10-08', '2026-10'),  -- 2026-10: Jomo Kenyatta University of Agriculture and Technology
    (23, 'monthly', 3500.00, '2026-10-08', '2026-10'),  -- 2026-10: Rift Valley Institute of Science and Technology
    (5, 'monthly', 7000.00, '2026-10-10', '2026-10'),  -- 2026-10: Maseno University
    (25, 'monthly', 3500.00, '2026-10-10', '2026-10'),  -- 2026-10: Mombasa Technical Training Institute
    (26, 'monthly', 3500.00, '2026-10-11', '2026-10'),  -- 2026-10: Eldoret National Polytechnic
    (7, 'monthly', 7000.00, '2026-10-12', '2026-10'),  -- 2026-10: University of Eldoret
    (27, 'monthly', 3500.00, '2026-10-12', '2026-10'),  -- 2026-10: Thika Technical Training Institute
    (9, 'monthly', 7000.00, '2026-10-14', '2026-10'),  -- 2026-10: Technical University of Kenya
    (10, 'monthly', 7000.00, '2026-10-15', '2026-10'),  -- 2026-10: Strathmore University
    (30, 'monthly', 1200.00, '2026-10-15', '2026-10'),  -- 2026-10: Lakeview Primary School
    (11, 'monthly', 3600.00, '2026-10-16', '2026-10'),  -- 2026-10 at the upgrade discount: Alliance High School
    (12, 'monthly', 2000.00, '2026-10-17', '2026-10'),  -- 2026-10: The Kenya High School
    (13, 'monthly', 2000.00, '2026-10-18', '2026-10'),  -- 2026-10: Mangu High School
    (14, 'monthly', 2000.00, '2026-10-19', '2026-10'),  -- 2026-10: Starehe Boys Centre and School
    (15, 'monthly', 2000.00, '2026-10-20', '2026-10'),  -- 2026-10: Loreto High School Limuru
    (16, 'monthly', 2000.00, '2026-10-21', '2026-10'),  -- 2026-10: Maseno School
    (17, 'monthly', 2000.00, '2026-10-22', '2026-10'),  -- 2026-10: Kapsabet Boys High School
    (19, 'monthly', 6300.00, '2026-10-24', '2026-10'),  -- 2026-10 at the upgrade discount: Kenya Medical Training College
    (4, 'monthly', 7000.00, '2026-10-28', '2026-10'),  -- 2026-10: Egerton University
    (29, 'monthly', 1200.00, '2026-10-30', '2026-10'),  -- 2026-10: Kibera Hill Primary School
    (31, 'monthly', 1380.00, '2026-11-04', '2026-10'),  -- 2026-10 late, bill 1200.00 + fine 180.00: Mwatate Junior School
    (24, 'monthly', 4025.00, '2026-11-06', '2026-10'),  -- 2026-10 late, bill 3500.00 + fine 525.00: Kisumu National Polytechnic
    (8, 'monthly', 8050.00, '2026-11-13', '2026-10'),  -- 2026-10 late, bill 7000.00 + fine 1050.00: Pwani University
    (8, 'reconnection', 1000.00, '2026-11-13', '2026-10'),  -- reconnection: Pwani University
    (21, 'monthly', 4025.00, '2026-11-20', '2026-10'),  -- 2026-10 late, bill 3500.00 + fine 525.00: Kenya Utalii College
    (20, 'monthly', 3500.00, '2026-11-05', '2026-11'),  -- 2026-11: Kenya Institute of Highways and Building Technology
    (1, 'monthly', 7000.00, '2026-11-06', '2026-11'),  -- 2026-11: University of Nairobi
    (2, 'monthly', 7000.00, '2026-11-07', '2026-11'),  -- 2026-11: Kenyatta University
    (22, 'monthly', 3500.00, '2026-11-07', '2026-11'),  -- 2026-11: Nairobi Technical Training Institute
    (3, 'monthly', 7000.00, '2026-11-08', '2026-11'),  -- 2026-11: Jomo Kenyatta University of Agriculture and Technology
    (23, 'monthly', 3500.00, '2026-11-08', '2026-11'),  -- 2026-11: Rift Valley Institute of Science and Technology
    (24, 'monthly', 3500.00, '2026-11-09', '2026-11'),  -- 2026-11: Kisumu National Polytechnic
    (5, 'monthly', 7000.00, '2026-11-10', '2026-11'),  -- 2026-11: Maseno University
    (25, 'monthly', 3500.00, '2026-11-10', '2026-11'),  -- 2026-11: Mombasa Technical Training Institute
    (26, 'monthly', 3500.00, '2026-11-11', '2026-11'),  -- 2026-11: Eldoret National Polytechnic
    (7, 'monthly', 7000.00, '2026-11-12', '2026-11'),  -- 2026-11: University of Eldoret
    (10, 'monthly', 7000.00, '2026-11-15', '2026-11'),  -- 2026-11: Strathmore University
    (30, 'monthly', 1200.00, '2026-11-15', '2026-11'),  -- 2026-11: Lakeview Primary School
    (11, 'monthly', 4000.00, '2026-11-16', '2026-11'),  -- 2026-11: Alliance High School
    (12, 'monthly', 2000.00, '2026-11-17', '2026-11'),  -- 2026-11: The Kenya High School
    (13, 'monthly', 2000.00, '2026-11-18', '2026-11'),  -- 2026-11: Mangu High School
    (14, 'monthly', 2000.00, '2026-11-19', '2026-11'),  -- 2026-11: Starehe Boys Centre and School
    (15, 'monthly', 2000.00, '2026-11-20', '2026-11'),  -- 2026-11: Loreto High School Limuru
    (16, 'monthly', 2000.00, '2026-11-21', '2026-11'),  -- 2026-11: Maseno School
    (17, 'monthly', 2000.00, '2026-11-22', '2026-11'),  -- 2026-11: Kapsabet Boys High School
    (19, 'monthly', 7000.00, '2026-11-24', '2026-11'),  -- 2026-11: Kenya Medical Training College
    (31, 'monthly', 1800.00, '2026-11-24', '2026-11'),  -- 2026-11 at the upgrade discount: Mwatate Junior School
    (29, 'monthly', 1200.00, '2026-11-25', '2026-11'),  -- 2026-11: Kibera Hill Primary School
    (4, 'monthly', 7000.00, '2026-11-26', '2026-11'),  -- 2026-11: Egerton University
    (8, 'monthly', 7000.00, '2026-11-27', '2026-11');  -- 2026-11: Pwani University

-- 11. Overdue fines (15 percent of the bill, applied once per month)
INSERT INTO overdue_fines (institution_id, billing_month, fine_amount, settled) VALUES
    (4, '2026-09', 1050.00, TRUE),  -- Egerton University, settled
    (16, '2026-09', 300.00, TRUE),  -- Maseno School, settled
    (18, '2026-09', 300.00, FALSE),  -- Nakuru High School, UNPAID
    (29, '2026-09', 180.00, TRUE),  -- Kibera Hill Primary School, settled
    (30, '2026-09', 180.00, TRUE),  -- Lakeview Primary School, settled
    (6, '2026-10', 1050.00, FALSE),  -- Moi University, UNPAID
    (8, '2026-10', 1050.00, TRUE),  -- Pwani University, settled
    (18, '2026-10', 300.00, FALSE),  -- Nakuru High School, UNPAID
    (21, '2026-10', 525.00, TRUE),  -- Kenya Utalii College, settled
    (24, '2026-10', 525.00, TRUE),  -- Kisumu National Polytechnic, settled
    (31, '2026-10', 180.00, TRUE);  -- Mwatate Junior School, settled

-- 12. Disconnections (bill + fine unpaid after the 10th of the next month)
INSERT INTO disconnections (institution_id, disconnected_on, reconnected, reconnected_on) VALUES
    (4, '2026-10-11', TRUE, '2026-10-14'),  -- Egerton University, reconnected 2026-10-14
    (18, '2026-10-11', FALSE, NULL),  -- Nakuru High School, still disconnected
    (29, '2026-10-11', TRUE, '2026-10-20'),  -- Kibera Hill Primary School, reconnected 2026-10-20
    (6, '2026-11-11', FALSE, NULL),  -- Moi University, still disconnected
    (8, '2026-11-11', TRUE, '2026-11-13'),  -- Pwani University, reconnected 2026-11-13
    (21, '2026-11-11', FALSE, NULL);  -- Kenya Utalii College, still disconnected

-- 13. Self-check: the counts below should read
-- institutions 32, contact_persons 32, readiness_assessments 36, equipment_orders 4, installations 30, subscriptions 30, upgrades 3, payments 147, overdue_fines 11, disconnections 6, bandwidth_plans 5, lan_node_tiers 4.
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