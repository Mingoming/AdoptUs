const test = require("node:test");
const assert = require("node:assert/strict");
const {
  executeMigration,
  verifyUsers,
} = require("../scripts/user-migration-runner");
const {
  parseAllowedPrivilegedUids,
  writeWithPreconditions,
} = require("../scripts/migrate-users");

const timestamp = { timestamp: true };
const legacyDocument = {
  id: "u1",
  updateTime: "version-1",
  data: {
    id: "u1",
    username: "Legacy User",
    full_name: "Legacy User",
    photo_url: "",
    role: "user",
    created_at: timestamp,
  },
};

test("privileged UID allowlist parser accepts repeated explicit arguments", () => {
  assert.deepEqual(
    [...parseAllowedPrivilegedUids([
      "node",
      "migrate-users.js",
      "--allow-privileged-uid=admin-1",
      "--dry-run",
      "--allow-privileged-uid=moderator-1",
    ])],
    ["admin-1", "moderator-1"]
  );
});

test("dry-run reports changes without writing", async () => {
  let writes = 0;

  const result = await executeMigration({
    documents: [legacyDocument],
    dryRun: true,
    serverTimestamp: () => timestamp,
    writeChanges: async () => {
      writes += 1;
    },
  });

  assert.equal(result.report.changed, 1);
  assert.equal(result.dryRun, true);
  assert.equal(writes, 0);
});

test("migration mode writes the canonical changes once", async () => {
  const written = [];

  const result = await executeMigration({
    documents: [legacyDocument],
    dryRun: false,
    serverTimestamp: () => timestamp,
    writeChanges: async (changes) => {
      written.push(...changes);
    },
  });

  assert.equal(result.report.changed, 1);
  assert.equal(result.dryRun, false);
  assert.deepEqual(written.map((change) => change.id), ["u1"]);
  assert.equal(written[0].updateTime, "version-1");
});

test("migration aborts before first write when any proposal is invalid", async () => {
  let writes = 0;

  await assert.rejects(
    executeMigration({
      documents: [
        legacyDocument,
        {
          id: "u2",
          updateTime: "version-2",
          data: {
            username: "owner",
            full_name: "Owner",
            role: "unknown-role",
            created_at: timestamp,
          },
        },
      ],
      dryRun: false,
      serverTimestamp: () => timestamp,
      isTimestamp: () => true,
      writeChanges: async () => {
        writes += 1;
      },
    }),
    /validation failed/
  );

  assert.equal(writes, 0);
});

test("migration reports update-time conflicts without claiming success", async () => {
  const result = await executeMigration({
    documents: [legacyDocument],
    dryRun: false,
    serverTimestamp: () => timestamp,
    isTimestamp: () => true,
    writeChanges: async () => ({
      written: [],
      conflicts: [{ id: "u1", reason: "updateTime changed" }],
    }),
  });

  assert.equal(result.report.written, 0);
  assert.equal(result.report.conflicts, 1);
  assert.equal(result.conflicts[0].id, "u1");
});

test("writer records success conflict success independently", async () => {
  const outcomes = {
    u1: "success",
    u2: "conflict",
    u3: "success",
  };
  const db = {
    collection: () => ({
      doc: (id) => ({
        update: async () => {
          if (outcomes[id] === "conflict") {
            const error = new Error("stale snapshot");
            error.code = 9;
            throw error;
          }
        },
      }),
    }),
  };
  const changes = ["u1", "u2", "u3"].map((id) => ({
    id,
    data: { uid: id },
    removeFields: [],
    updateTime: `version-${id}`,
  }));

  const result = await writeWithPreconditions(db, changes, () => null);

  assert.deepEqual(result.written, ["u1", "u3"]);
  assert.deepEqual(result.conflicts.map((item) => item.id), ["u2"]);
  assert.deepEqual(result.failed, []);
});

test("writer retains partial success when an unexpected failure aborts remaining writes", async () => {
  const attempted = [];
  const db = {
    collection: () => ({
      doc: (id) => ({
        update: async () => {
          attempted.push(id);
          if (id === "u2") {
            const error = new Error("private backend detail");
            error.code = "unavailable";
            throw error;
          }
        },
      }),
    }),
  };
  const changes = ["u1", "u2", "u3"].map((id) => ({
    id,
    data: { uid: id },
    removeFields: [],
    updateTime: `version-${id}`,
  }));

  const result = await writeWithPreconditions(db, changes, () => null);

  assert.deepEqual(attempted, ["u1", "u2"]);
  assert.deepEqual(result.written, ["u1"]);
  assert.deepEqual(result.conflicts, []);
  assert.deepEqual(result.failed, [{
    id: "u2",
    code: "unavailable",
    reason: "write failed",
  }]);
});

test("migration report retains written and failed document outcomes", async () => {
  const result = await executeMigration({
    documents: [
      legacyDocument,
      {
        ...legacyDocument,
        id: "u2",
        updateTime: "version-2",
      },
      {
        ...legacyDocument,
        id: "u3",
        updateTime: "version-3",
      },
    ],
    dryRun: false,
    serverTimestamp: () => timestamp,
    isTimestamp: () => true,
    writeChanges: async () => ({
      written: ["u1"],
      conflicts: [],
      failed: [{
        id: "u2",
        code: "unavailable",
        reason: "write failed",
      }],
    }),
  });

  assert.deepEqual(result.written, ["u1"]);
  assert.deepEqual(result.failed.map((item) => item.id), ["u2"]);
  assert.equal(result.report.planned, 3);
  assert.equal(result.report.written, 1);
  assert.equal(result.report.failed, 1);
});

test("dry-run reports privileged roles but write mode requires an allowlist", async () => {
  const privilegedDocument = {
    id: "admin-1",
    updateTime: "version-admin",
    data: {
      username: "admin_user",
      full_name: "Admin User",
      role: "admin",
      created_at: timestamp,
    },
  };

  const dryRunResult = await executeMigration({
    documents: [privilegedDocument],
    dryRun: true,
    serverTimestamp: () => timestamp,
    isTimestamp: () => true,
    writeChanges: async () => {
      throw new Error("dry-run must not write");
    },
  });
  assert.deepEqual(dryRunResult.privilegedRoleReview, [{
    id: "admin-1",
    role: "admin",
  }]);

  await assert.rejects(
    executeMigration({
      documents: [privilegedDocument],
      dryRun: false,
      serverTimestamp: () => timestamp,
      isTimestamp: () => true,
      writeChanges: async () => ({
        written: ["admin-1"],
        conflicts: [],
        failed: [],
      }),
    }),
    /privileged role review/
  );

  const allowedResult = await executeMigration({
    documents: [privilegedDocument],
    dryRun: false,
    serverTimestamp: () => timestamp,
    isTimestamp: () => true,
    allowedPrivilegedUids: new Set(["admin-1"]),
    writeChanges: async () => ({
      written: ["admin-1"],
      conflicts: [],
      failed: [],
    }),
  });
  assert.equal(allowedResult.report.written, 1);
  assert.equal(allowedResult.report.privilegedRoleReview, 0);
});

test("verification reports invalid documents without modifying them", () => {
  const report = verifyUsers(
    [
      legacyDocument,
      {
        id: "u2",
        data: {
          uid: "u2",
          username: "canonical_user",
          fullName: "Canonical User",
          photoUrl: "",
          bio: "",
          city: "",
          whatsapp: "",
          role: "user",
          createdAt: timestamp,
          updatedAt: timestamp,
        },
      },
    ],
    () => true
  );

  assert.equal(report.scanned, 2);
  assert.equal(report.invalidCount, 1);
  assert.equal(report.invalid[0].id, "u1");
});
