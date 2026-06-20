const test = require("node:test");
const assert = require("node:assert/strict");
const {
  canonicalizeUser,
  createMigrationPlan,
  validateCanonicalUser,
} = require("../scripts/user-migration-core");

const createdAt = { timestamp: "created" };
const updatedAt = { timestamp: "updated" };

test("canonicalizeUser converts legacy fields and removes private email", () => {
  const result = canonicalizeUser(
    "u12345678",
    {
      id: "u12345678",
      username: "John Doe!",
      email: "john@example.com",
      full_name: "John Doe",
      photo_url: "avatar.jpg",
      role: "user",
      created_at: createdAt,
    },
    () => updatedAt
  );

  assert.deepEqual(result, {
    uid: "u12345678",
    username: "john_doe",
    fullName: "John Doe",
    photoUrl: "avatar.jpg",
    bio: "",
    city: "",
    whatsapp: "",
    role: "user",
    createdAt,
    updatedAt,
  });
  assert.equal(Object.hasOwn(result, "email"), false);
  assert.equal(Object.hasOwn(result, "full_name"), false);
});

test("canonicalizeUser supplies safe values for malformed legacy data", () => {
  const result = canonicalizeUser(
    "u12345678",
    {
      username: "@@",
      full_name: "",
      role: "admin",
      bio: "x".repeat(400),
    },
    () => updatedAt
  );

  assert.equal(result.username, "user_u1234567");
  assert.equal(result.fullName, "user_u1234567");
  assert.equal(result.role, "user");
  assert.equal(result.bio.length, 300);
  assert.equal(result.createdAt, updatedAt);
});

test("createMigrationPlan is dry-run friendly and idempotent", () => {
  const legacy = {
    id: "u1",
    username: "legacy_user",
    full_name: "Legacy User",
    photo_url: "",
    role: "user",
    created_at: createdAt,
  };
  const canonical = canonicalizeUser("u2", {
    uid: "u2",
    username: "canonical_user",
    fullName: "Canonical User",
    photoUrl: "",
    bio: "",
    city: "",
    whatsapp: "",
    role: "user",
    createdAt,
    updatedAt,
  }, () => updatedAt);

  const plan = createMigrationPlan(
    [
      { id: "u1", data: legacy },
      { id: "u2", data: canonical },
    ],
    () => updatedAt
  );

  assert.deepEqual(plan.report, {
    scanned: 2,
    changed: 1,
    skipped: 1,
  });
  assert.deepEqual(plan.changes.map((change) => change.id), ["u1"]);

  const secondPlan = createMigrationPlan(
    plan.changes.map((change) => ({ id: change.id, data: change.data })),
    () => updatedAt
  );
  assert.equal(secondPlan.report.changed, 0);
});

test("validateCanonicalUser rejects legacy or unexpected fields", () => {
  const canonical = canonicalizeUser("u1", {
    uid: "u1",
    username: "canonical_user",
    fullName: "Canonical User",
    photoUrl: "",
    bio: "",
    city: "",
    whatsapp: "",
    role: "user",
    createdAt,
    updatedAt,
  }, () => updatedAt);

  assert.deepEqual(validateCanonicalUser("u1", canonical), []);
  assert.deepEqual(
    validateCanonicalUser("u1", { ...canonical, email: "private@example.com" }),
    ["unexpected key email"]
  );
  assert.ok(
    validateCanonicalUser("u1", { ...canonical, uid: "u2" })
      .includes("uid does not match document ID")
  );
});
