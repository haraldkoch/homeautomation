-- name: create-user!
-- creates a new user record
INSERT INTO users
(username, first_name, last_name)
VALUES (:username, :first_name, :last_name);

-- name: update-user!
-- update an existing user record
UPDATE users
SET username = :username, first_name = :first_name, last_name = :last_name
WHERE id = :id;

-- name: get-user
-- retrieve a user given the id.
SELECT * FROM users
WHERE id = :id;

-- name: get-users
-- retrieve all users
SELECT * FROM users;

-- name: delete-user!
-- delete a user given the id
DELETE FROM users
WHERE id = :id;

-- name: find-user-for-device
-- locates the user record for a given device
SELECT users.* FROM users
JOIN devices ON (users.id = devices.owner)
WHERE devices.name = :device;

-- name: get-devices
-- get all devices
SELECT d.id, d.macaddr, d.name, users.username as owner, d.ignore, d.status, d.last_status_change, d.last_seen
FROM devices d
LEFT JOIN users ON (d.owner = users.id);

-- name: find-device
-- find a device by MAC address
SELECT * FROM devices
WHERE macaddr = :macaddr;

-- name: get-devices-for-user
SELECT * FROM devices
WHERE owner = :owner;

-- name: create-device!
INSERT INTO devices
(macaddr, name, status, last_status_change, last_seen)
VALUES (:macaddr, :name, :status, :last_status_change, :last_seen);

-- name: update-device-name!
UPDATE devices
SET name = :name
WHERE macaddr = :macaddr;

-- name: update-device-seen!
UPDATE devices
SET last_seen = :last_seen
WHERE macaddr = :macaddr;

-- name: update-device-status!
UPDATE devices
SET status = :status, last_status_change = :last_status_change
WHERE macaddr = :macaddr;

-- name: set-device-owner!
UPDATE devices
SET owner = (select id from users where username = :owner)
WHERE id = :device_id;

-- name: set-device-name!
UPDATE devices
SET name = :name
WHERE id = :device_id;

-- name: set-device-ignore!
UPDATE devices
SET `ignore` = :ignore
WHERE id = :device_id;

-- name: mark-devices-idle!
UPDATE devices
SET status = 'idle'
WHERE status = 'present' AND last_seen < TIMESTAMPADD(MINUTE,-30,NOW());
