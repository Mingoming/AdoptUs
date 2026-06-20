const { isDeepStrictEqual } = require("node:util");

const REQUIRED_KEYS = [
  "uid",
  "username",
  "fullName",
  "photoUrl",
  "bio",
  "city",
  "whatsapp",
  "role",
  "createdAt",
  "updatedAt",
];

const ALLOWED_KEYS = new Set(REQUIRED_KEYS);
const VALID_ROLES = new Set(["user", "admin", "moderator"]);
const REDACTED_FIELDS = new Set(["email", "whatsapp"]);

function stringValue(value, fallback = "") {
  return typeof value === "string" ? value : fallback;
}

function normalizeUsername(value, uid) {
  const normalized = stringValue(value)
    .trim()
    .toLowerCase()
    .replace(/\s+/g, "_")
    .replace(/[^a-z0-9_]/g, "")
    .slice(0, 30);

  return normalized.length >= 3
    ? normalized
    : `user_${uid.slice(0, 8)}`;
}

function nonBlankString(...values) {
  for (const value of values) {
    if (typeof value === "string" && value.trim().length > 0) {
      return value.trim();
    }
  }
  return "";
}

function canonicalizeUser(documentId, data, serverTimestamp) {
  const username = normalizeUsername(data.username, documentId);
  const fullName = nonBlankString(data.fullName, data.full_name);
  const photoUrl = nonBlankString(data.photoUrl, data.photo_url);
  const role = nonBlankString(data.role) || "user";

  return {
    uid: documentId,
    username,
    fullName: fullName.slice(0, 80) || username,
    photoUrl: photoUrl.slice(0, 2048),
    bio: stringValue(data.bio).slice(0, 300),
    city: stringValue(data.city).slice(0, 80),
    whatsapp: stringValue(data.whatsapp).slice(0, 30),
    role,
    createdAt: data.createdAt || data.created_at || serverTimestamp(),
    updatedAt: data.updatedAt || serverTimestamp(),
  };
}

function redactedValue(field, value) {
  if (REDACTED_FIELDS.has(field) && value !== undefined && value !== "") {
    return "[REDACTED]";
  }
  return value;
}

function formatMigrationDiff(current, next) {
  const currentKeys = new Set(Object.keys(current));
  const nextKeys = new Set(Object.keys(next));
  const added = [];
  const changed = [];
  const removed = [];

  for (const field of [...nextKeys].sort()) {
    if (!currentKeys.has(field)) {
      added.push({
        field,
        after: redactedValue(field, next[field]),
      });
    } else if (!isDeepStrictEqual(current[field], next[field])) {
      changed.push({
        field,
        before: redactedValue(field, current[field]),
        after: redactedValue(field, next[field]),
      });
    }
  }

  for (const field of [...currentKeys].sort()) {
    if (!nextKeys.has(field)) {
      removed.push({
        field,
        before: redactedValue(field, current[field]),
      });
    }
  }

  return { added, changed, removed };
}

function createMigrationPlan(
  documents,
  serverTimestamp,
  isTimestamp = (value) => value != null
) {
  const changes = [];
  const invalid = [];
  let skipped = 0;

  for (const document of documents) {
    const canonical = canonicalizeUser(
      document.id,
      document.data,
      serverTimestamp
    );
    const errors = validateCanonicalUser(
      document.id,
      canonical,
      isTimestamp
    );

    if (errors.length > 0) {
      invalid.push({
        id: document.id,
        errors,
      });
      continue;
    }

    if (isDeepStrictEqual(document.data, canonical)) {
      skipped += 1;
    } else {
      if (!document.updateTime) {
        invalid.push({
          id: document.id,
          errors: ["missing snapshot updateTime"],
        });
        continue;
      }
      changes.push({
        id: document.id,
        data: canonical,
        updateTime: document.updateTime,
        removeFields: Object.keys(document.data)
          .filter((field) => !ALLOWED_KEYS.has(field)),
        diff: formatMigrationDiff(document.data, canonical),
      });
    }
  }

  return {
    changes,
    invalid,
    report: {
      scanned: documents.length,
      changed: changes.length,
      skipped,
      invalid: invalid.length,
    },
  };
}

function validateCanonicalUser(
  documentId,
  data,
  isTimestamp = (value) => value != null
) {
  const errors = [];

  for (const key of REQUIRED_KEYS) {
    if (!Object.hasOwn(data, key)) {
      errors.push(`missing ${key}`);
    }
  }

  for (const key of Object.keys(data)) {
    if (!ALLOWED_KEYS.has(key)) {
      errors.push(`unexpected key ${key}`);
    }
  }

  if (data.uid !== documentId) {
    errors.push("uid does not match document ID");
  }
  if (
    typeof data.username !== "string"
    || data.username.length < 3
    || data.username.length > 30
    || /\s/.test(data.username)
  ) {
    errors.push("username is invalid");
  }
  if (
    typeof data.fullName !== "string"
    || data.fullName.length < 1
    || data.fullName.length > 80
  ) {
    errors.push("fullName is invalid");
  }
  if (typeof data.photoUrl !== "string" || data.photoUrl.length > 2048) {
    errors.push("photoUrl is invalid");
  }
  if (typeof data.bio !== "string" || data.bio.length > 300) {
    errors.push("bio is invalid");
  }
  if (typeof data.city !== "string" || data.city.length > 80) {
    errors.push("city is invalid");
  }
  if (typeof data.whatsapp !== "string" || data.whatsapp.length > 30) {
    errors.push("whatsapp is invalid");
  }
  if (!VALID_ROLES.has(data.role)) {
    errors.push("role is not recognized");
  }
  if (!isTimestamp(data.createdAt)) {
    errors.push("createdAt is not Timestamp");
  }
  if (!isTimestamp(data.updatedAt)) {
    errors.push("updatedAt is not Timestamp");
  }

  return errors;
}

module.exports = {
  canonicalizeUser,
  createMigrationPlan,
  formatMigrationDiff,
  normalizeUsername,
  validateCanonicalUser,
  VALID_ROLES,
};
