const test = require("node:test");
const assert = require("node:assert/strict");
const {
  executeMigration,
  verifyUsers,
} = require("../scripts/user-migration-runner");

const timestamp = { timestamp: true };
const legacyDocument = {
  id: "u1",
  data: {
    id: "u1",
    username: "Legacy User",
    full_name: "Legacy User",
    photo_url: "",
    role: "user",
    created_at: timestamp,
  },
};

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
