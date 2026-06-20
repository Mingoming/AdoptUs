const test = require("node:test");
const assert = require("node:assert/strict");
const {
  canonicalizeUser,
  createMigrationPlan,
  formatMigrationDiff,
  validateCanonicalUser,
} = require("../scripts/user-migration-core");

const createdAt = { timestamp: "created" };
const updatedAt = { timestamp: "updated" };
const isTimestamp = (value) => value?.timestamp != null;

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
  const result = canonicalizeUser("u12345678", {
    username: "@@",
    full_name: "",
    role: "user",
    bio: "x".repeat(400),
  }, () => updatedAt);

  assert.equal(result.username, "user_u1234567");
  assert.equal(result.fullName, "user_u1234567");
  assert.equal(result.role, "user");
  assert.equal(result.bio.length, 300);
  assert.equal(result.createdAt, updatedAt);
});

test("canonicalizeUser prefers nonblank legacy values when canonical fields are blank", () => {
  const result = canonicalizeUser("u1", {
    username: "legacy_owner",
    fullName: "   ",
    full_name: "Legacy Owner",
    photoUrl: "",
    photo_url: "legacy-avatar.jpg",
    role: "user",
    createdAt,
    updatedAt,
  }, () => updatedAt);

  assert.equal(result.fullName, "Legacy Owner");
  assert.equal(result.photoUrl, "legacy-avatar.jpg");
});

test("canonicalizeUser preserves recognized privileged roles", () => {
  const result = canonicalizeUser("u1", {
    username: "admin_user",
    fullName: "Admin User",
    photoUrl: "",
    bio: "",
    city: "",
    whatsapp: "",
    role: "admin",
    createdAt,
    updatedAt,
  }, () => updatedAt);

  assert.equal(result.role, "admin");
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
      { id: "u1", data: legacy, updateTime: "v1" },
      { id: "u2", data: canonical, updateTime: "v2" },
    ],
    () => updatedAt,
    isTimestamp
  );

  assert.deepEqual(plan.report, {
    scanned: 2,
    changed: 1,
    skipped: 1,
    invalid: 0,
  });
  assert.deepEqual(plan.changes.map((change) => change.id), ["u1"]);

  const secondPlan = createMigrationPlan(
    plan.changes.map((change) => ({
      id: change.id,
      data: change.data,
      updateTime: change.updateTime,
    })),
    () => updatedAt,
    isTimestamp
  );
  assert.equal(secondPlan.report.changed, 0);
});

test("createMigrationPlan rejects malformed timestamps before writes", () => {
  const plan = createMigrationPlan([
    {
      id: "u1",
      updateTime: "v1",
      data: {
        uid: "u1",
        username: "owner",
        fullName: "Owner",
        photoUrl: "",
        bio: "",
        city: "",
        whatsapp: "",
        role: "user",
        createdAt: "not-a-timestamp",
        updatedAt,
      },
    },
  ], () => updatedAt, isTimestamp);

  assert.equal(plan.invalid.length, 1);
  assert.ok(plan.invalid[0].errors.includes("createdAt is not Timestamp"));
  assert.equal(plan.changes.length, 0);
});

test("createMigrationPlan sends unknown roles to manual review", () => {
  const plan = createMigrationPlan([
    {
      id: "u1",
      updateTime: "v1",
      data: {
        username: "owner",
        full_name: "Owner",
        role: "superuser",
        created_at: createdAt,
      },
    },
  ], () => updatedAt, isTimestamp);

  assert.equal(plan.invalid.length, 1);
  assert.ok(plan.invalid[0].errors.includes("role is not recognized"));
});

test("formatMigrationDiff redacts PII and reports added changed removed fields", () => {
  const current = {
    email: "owner@example.com",
    whatsapp: "08123456789",
    full_name: "Legacy Owner",
    role: "user",
  };
  const next = {
    uid: "u1",
    whatsapp: "628123456789",
    fullName: "Legacy Owner",
    role: "user",
  };

  const diff = formatMigrationDiff(current, next);

  assert.deepEqual(diff.added.map((entry) => entry.field), ["fullName", "uid"]);
  assert.deepEqual(diff.removed.map((entry) => entry.field), ["email", "full_name"]);
  assert.equal(
    diff.removed.find((entry) => entry.field === "email").before,
    "[REDACTED]"
  );
  assert.deepEqual(diff.changed, [{
    field: "whatsapp",
    before: "[REDACTED]",
    after: "[REDACTED]",
  }]);
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

  assert.deepEqual(validateCanonicalUser("u1", canonical, isTimestamp), []);
  assert.deepEqual(
    validateCanonicalUser("u1", { ...canonical, email: "private@example.com" }),
    ["unexpected key email"]
  );
  assert.ok(
    validateCanonicalUser("u1", { ...canonical, uid: "u2" })
      .includes("uid does not match document ID")
  );
});
