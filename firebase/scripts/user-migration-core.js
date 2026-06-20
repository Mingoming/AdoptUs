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

function canonicalizeUser(documentId, data, serverTimestamp) {
  const username = normalizeUsername(data.username, documentId);
  const legacyFullName = stringValue(
    data.fullName,
    stringValue(data.full_name)
  );

  return {
    uid: documentId,
    username,
    fullName: legacyFullName.trim().slice(0, 80) || username,
    photoUrl: stringValue(
      data.photoUrl,
      stringValue(data.photo_url)
    ).slice(0, 2048),
    bio: stringValue(data.bio).slice(0, 300),
    city: stringValue(data.city).slice(0, 80),
    whatsapp: stringValue(data.whatsapp).slice(0, 30),
    role: "user",
    createdAt: data.createdAt || data.created_at || serverTimestamp(),
    updatedAt: data.updatedAt || serverTimestamp(),
  };
}

function createMigrationPlan(documents, serverTimestamp) {
  const changes = [];
  let skipped = 0;

  for (const document of documents) {
    const canonical = canonicalizeUser(
      document.id,
      document.data,
      serverTimestamp
    );

    if (isDeepStrictEqual(document.data, canonical)) {
      skipped += 1;
    } else {
      changes.push({
        id: document.id,
        data: canonical,
      });
    }
  }

  return {
    changes,
    report: {
      scanned: documents.length,
      changed: changes.length,
      skipped,
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
  if (data.role !== "user") {
    errors.push("role must be user");
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
  normalizeUsername,
  validateCanonicalUser,
};
