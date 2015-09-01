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

-- name: delete-user!
-- delete a user given the id
DELETE FROM users
WHERE id = :id;

-- name: find-user-for-device
-- locates the user record for a given device
SELECT users.* FROM users
JOIN devices ON (users.id = devices.owner)
WHERE devices.name = :device;

-- name: find-device
-- find a device by name
SELECT * FROM devices
WHERE name = :device;

-- name: get-device
-- get a device by ID
SELECT * FROM devices
WHERE id = :id;

-- name: get-devices-for-user
SELECT * FROM devices
WHERE owner = :owner;

-- name: create-device<!
INSERT INTO devices
(name) VALUES (:device)