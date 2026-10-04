-- azani_db_ddl.sql
-- Builds all twelve tables of the Azani ISP database from nothing.
-- Safe to run more than once: it drops each table first.
-- Needs MySQL 8.0.16 or later, because earlier versions ignore CHECK constraints.
-- Structure follows Azani_ISP_Project_Structure.docx.
-- Figures and category names follow the SCO200 brief; no price lives in this file.

CREATE DATABASE IF NOT EXISTS azani_db:
USE azani_db;

DROP TABLE IF EXISTS disconnections;
DROP TABLE IF EXISTS overdue_fines;
DROP TABLE IF EXISTS payments;
DROP TABLE IF EXISTS subsciptions;
DROP TABLE IF EXISTS bandwidth_plans;
DROP TABLE IF EXISTS installations;
DROP TABLE IF EXISTS equipment_orders;
DROP TABLE IF EXISTS lan_node_tiers;
DROP TABLE IF EXISTS readiness_assesments;
DROP TABLE IF EXISTS contact_persons;
DROP TABLE IF EXISTS institutions;


CREATE TABLE institutions (
    institution_id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    type VARCHAR(20) NOT NULL,
    address VARCHAR(150),
    registered_on DATE NOT NULL DEFAULT(CURRENT_DATE),
    status VARCHAR(20) NOT NULL DEFAULT 'active',
    CONSTRAINT chk_status CHECK (status IN ('active', 'inactive', 'suspended')),
    CONSTRAINT chk_type CHECK (type IN('primary', 'junior', 'senior', 'college'))
);

CREATE TABLE contact_persons (
    contact_id INT AUTO_INCREMENT PRIMARY KEY,
    institution_id INT NOT NULL,
    full_name VARCHAR(100) NOT NULL,
    phone VARCHAR(20),
    email VARCHAR(100),
    FOREIGN KEY (institution_id) REFERENCES institutions (institution_id)
);

CREATE TABLE readiness_assesments (
    assesment_id INT AUTO_INCREMENT PRIMARY KEY,
    institution_id INT NOT NULL,
    visit_date DATE NOT NULL,
    user_count INT NOT NULL,
    has_computers BOOLEAN NOT NULL,
    has_lan BOOLEAN NOT NULL,
    is_ready BOOLEAN NOT NULL,
    CONSTRAINT chk_user_count CHECK (user_count > 0),
    FOREIGN KEY (institution_id) REFERENCES institutions (institution_id) 
);

CREATE TABLE lan_node_tiers (
    tier_id INT AUTO_INCREMENT PRIMARY KEY,
    min_nodes INT NOT NULL,
    max_nodes INT NOT NULL,
    cost DECIMAL (10,2) NOT NULL,
    CONSTRAINT chk_node_range CHECK (min_nodes <= max_nodes)
);

CREATE TABLE equipment_orders (
    order_id INT AUTO_INCREMENT PRIMARY KEY,
    institution_id INT NOT NULL,
    tier_id INT NOT NULL,
    computer_qty INT NOT NULL DEFAULT 0,
    computer_cost DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    total_cost DECIMAL(10,2) NOT NULL,
    order_date DATE NOT NULL,
    CONSTRAINT chk_computer_qty CHECK (computer_qty > 0),
    FOREIGN KEY (institution_id) REFERENCES institutions(instituion_id),
    FOREIGN KEY (tier_id) REFERENCES lan_node_tiers(tier_id) 
);

CREATE TABLE installations (
    installation_id INT AUTO_INCREMENT PRIMARY KEY,
    institution_id INT NOT NULL,
    fee DECIMAL(10,2) NOT NULL,
    installed_on DATE NOT NULL,
    FOREIGN KEY (institution_id) REFERENCES institutions (institution_is)
);

CREATE TABLE bandwidth_plans (
    plan_id INT AUTO_INCREMENT PRIMARY KEY,
    mbps INT NOT NULL UNIQUE,
    monthly_cost DECIMAL(10,2) NOT NULL
);

CREATE TABLE subscriptions (
    subscription_id INT AUTO_INCREMENT PRIMARY KEY,
    institution_id INT NOT NULL,
    plan_id INT NOT NULL,
    start_date DATE NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'active',
    CONSTRAINT chk_status CHECK (status IN ('active', 'disconnected')),
    FOREIGN KEY (institution_id) REFERENCES institutions (institution_id),
    FOREIGN KEY (plain_id) REFERENCES bandwidth_plans (plain_id)
);

CREATE TABLE upgrades (
    upgrade_id INT AUTO_INCREMENT PRIMARY KEY,
    subsciption_id INT NOT NULL,
    old_plan_id INT NOT NULL,
    new_plan_id INT NOT NULL,
    discount_rate DECIMAL (10, 2) NOT NULL,
    upgraded_on DATE NOT NULL,
    CONSTRAINT chk_discount_rate CHECK (discount_rate > 0 AND discount_rate <= 1),
    FOREIGN KEY (subscription_id) REFERENCES subscriptions (subsciption_id),
    FOREIGN KEY (old_plan_id) REFERENCES bandwidth_plans(plan_id),
    FOREIGN KEY (new_plan_id) REFERENCES bandwidth_plans(plan_id),
);

CREATE TABLE payments (
    payment_id INT AUTO_INCREMENT PRIMARY KEY,
    institution_id INT NOT NULL,
    payment_type VARCHAR(20) NOT NULL,
    amount DECIMAL(10,2) NOT NULL,
    paid_on DATE NOT NULL,
    billing_month VARCHAR(20) NULL,
    CONSTRAINT chk_payment_type
        CHECK (payment_type IN ('registration', 'installation', 'monthly', 'reconnection')),
    CONSTRAINT chk_payment_amount CHECK (amount > 0),
    CONSTRAINT chk_monthly_has_month
        CHECK (payment_type <> 'monthly' OR billing_month IS NOT NULL),
    FOREIGN KEY (institution_id) REFERENCES institutions (institution_id)
);

-- One fine per institution per bill: the 15 percent fine applies once.
CREATE TABLE overdue_fines (
    fine_id INT AUTO_INCREMENT PRIMARY KEY,
    institution_id INT NOT NULL,
    billing_month VARCHAR(20) NOT NULL,
    fine_amount DECIMAL(10,2) NOT NULL,
    settled BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT uq_fine_once UNIQUE (institution_id, billing_month),
    FOREIGN KEY (institution_id) REFERENCES institutions (institution_id)
);

CREATE TABLE disconnections (
    disconnection_id INT AUTO_INCREMENT PRIMARY KEY,
    institution_id INT NOT NULL,
    disconnected_on DATE NOT NULL,
    reconnected BOOLEAN NOT NULL DEFAULT FALSE,
    reconnected_on DATE NULL,
    FOREIGN KEY (institution_id) REFERENCES institutions (institution_id)
);

SHOW TABLES;

