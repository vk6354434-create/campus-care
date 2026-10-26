-- Campus Complaint Management System - database
--
-- You normally do NOT need to run this file:
-- the app creates and upgrades these tables automatically on startup
-- (see DatabaseSetup.java). It is kept here for reference and reports.

CREATE DATABASE IF NOT EXISTS campus_complaint_system;
USE campus_complaint_system;

CREATE TABLE IF NOT EXISTS students (
    student_id   INT AUTO_INCREMENT PRIMARY KEY,
    name         VARCHAR(100) NOT NULL,
    email        VARCHAR(100) NOT NULL UNIQUE,
    password     VARCHAR(255) NOT NULL,          -- salted SHA-256 hash
    phone        VARCHAR(15)  NOT NULL,
    department   VARCHAR(100) NOT NULL,
    year         INT          NOT NULL,
    last_login   TIMESTAMP NULL,                 -- for "new updates" notifications
    created_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS admins (
    admin_id         INT AUTO_INCREMENT PRIMARY KEY,
    name             VARCHAR(100) NOT NULL,
    email            VARCHAR(100) NOT NULL UNIQUE,
    password         VARCHAR(255) NOT NULL,      -- salted SHA-256 hash
    role             VARCHAR(20)  NOT NULL DEFAULT 'ADMIN',
    approval_status  VARCHAR(20)  NOT NULL DEFAULT 'APPROVED',
    created_at       TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS complaints (
    complaint_id  INT AUTO_INCREMENT PRIMARY KEY,
    student_id    INT NOT NULL,
    category      VARCHAR(50)  NOT NULL,
    title         VARCHAR(150) NOT NULL,
    description   TEXT         NOT NULL,
    room_no       VARCHAR(20)  NOT NULL,
    block         VARCHAR(50)  NOT NULL,
    status        VARCHAR(20)  NOT NULL DEFAULT 'Submitted', -- Submitted / In Progress / Resolved / Rejected
    priority      VARCHAR(10)  NOT NULL DEFAULT 'Medium',    -- Low / Medium / High / Urgent
    image         LONGBLOB NULL,                             -- photo of the problem
    image_name    VARCHAR(255) NULL,
    rating        INT NULL,                                  -- 1 to 5 stars after resolution
    feedback      TEXT NULL,
    created_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP NULL,
    resolved_at   TIMESTAMP NULL,                            -- used for resolution-time analytics
    assigned_to   VARCHAR(60) NULL,                          -- department handling it
    ai_category   VARCHAR(50) NULL,                          -- what the AI predicted
    ai_confidence INT NULL,                                  -- AI confidence (0-100)
    FOREIGN KEY (student_id) REFERENCES students(student_id)
);

CREATE TABLE IF NOT EXISTS complaint_updates (
    update_id     INT AUTO_INCREMENT PRIMARY KEY,
    complaint_id  INT NOT NULL,
    admin_id      INT NOT NULL,
    status        VARCHAR(20) NOT NULL,
    remarks       TEXT,
    updated_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (complaint_id) REFERENCES complaints(complaint_id),
    FOREIGN KEY (admin_id) REFERENCES admins(admin_id)
);
