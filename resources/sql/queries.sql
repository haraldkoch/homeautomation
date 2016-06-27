-- :name create-user! :! :n
-- :doc creates a new user record
INSERT INTO users
(username, first_name, last_name)
VALUES (:username, :first_name, :last_name);

-- :name update-user! :! :n
-- :doc update an existing user record
UPDATE users
SET username = :username, first_name = :first_name, last_name = :last_name
WHERE id = :id;

-- :name get-user :? :1
-- :doc retrieve a user given the id.
SELECT * FROM users
WHERE id = :id;

-- :name get-user-by-name :? :1
-- :doc retrieve a user given username.
SELECT * FROM users
WHERE username = :username;

-- :name get-users :? :*
-- :doc retrieve all users
SELECT * FROM users;

-- :name delete-user! :! :n
-- :doc delete a user given the id
DELETE FROM users
WHERE id = :id;

-- :name set-user-presence! :! :n
-- :doc set the presence status of a user
UPDATE users SET presence = :presence WHERE id = :id;

-- :name find-user-for-device :? :1
-- :doc locates the user record for a given device
SELECT users.* FROM users
JOIN devices ON (users.id = devices.owner)
WHERE devices.name = :device;

-- :name get-device :? :1
SELECT d.id, d.macaddr, d.name, users.username as owner, d.ignore, d.status, d.last_status_change, d.last_seen
FROM devices d
  LEFT JOIN users ON (d.owner = users.id)
 WHERE d.id = :id;

-- :name get-devices :? :*
-- :doc get all devices
SELECT d.id, d.macaddr, d.name, users.username as owner, d.ignore, d.status, d.last_status_change, d.last_seen
FROM devices d
LEFT JOIN users ON (d.owner = users.id);

-- :name find-device :? :1
-- :doc find a device by MAC address
SELECT d.id, d.macaddr, d.name, users.username as owner, d.ignore, d.status, d.last_status_change, d.last_seen
  FROM devices d
    LEFT JOIN users ON (d.owner = users.id)
WHERE d.macaddr = :macaddr;

-- :name get-devices-for-user :? :*
SELECT d.id, d.macaddr, d.name, users.username as owner, d.ignore, d.status, d.last_status_change, d.last_seen
FROM devices d
  LEFT JOIN users ON (d.owner = users.id)
WHERE users.username = :owner;

-- :name create-device! :! :n
INSERT INTO devices
(macaddr, name, status, last_status_change, last_seen)
VALUES (:macaddr, :name, :status, :last_status_change, :last_seen);

-- :name update-device-name! :! :n
UPDATE devices
SET name = :name
WHERE macaddr = :macaddr;

-- :name update-device-seen! :! :n
UPDATE devices
SET last_seen = :last_seen
WHERE macaddr = :macaddr;

-- :name update-device-status! :! :n
UPDATE devices
SET status = :status, last_status_change = :last_status_change
WHERE macaddr = :macaddr;

-- :name set-device-owner! :! :n
UPDATE devices
SET owner = (select id from users where username = :owner)
WHERE id = :device_id;

-- :name set-device-name! :! :n
UPDATE devices
SET name = :name
WHERE id = :device_id;

-- :name set-device-ignore! :! :n
UPDATE devices
SET `ignore` = :ignore
WHERE id = :device_id;

-- :name mark-devices-idle! :! :n
UPDATE devices
SET status = 'idle'
WHERE status = 'present' AND last_seen < TIMESTAMPADD(MINUTE,-30,NOW());
