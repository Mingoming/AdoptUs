const { execFileSync } = require("node:child_process");
const path = require("node:path");
const test = require("node:test");
const assert = require("node:assert/strict");
const { initializeApp } = require("firebase-admin/app");
const {
  FieldValue,
  getFirestore,
  Timestamp,
} = require("firebase-admin/firestore");
const {
  writeWithPreconditions,
} = require("../scripts/migrate-users");

const projectId = "demo-adoptus";
const app = initializeApp({ projectId }, "migration-emulator-test");
const db = getFirestore(app);
const firebaseDirectory = path.resolve(__dirname, "..");

function runScript(script, args = []) {
  return execFileSync(
    process.execPath,
    [path.resolve(firebaseDirectory, script), ...args],
    {
      cwd: firebaseDirectory,
      encoding: "utf8",
      env: {
        ...process.env,
        GCLOUD_PROJECT: projectId,
      },
    }
  );
}

test("migration CLI dry-runs migrates and verifies emulator users", async () => {
  await db.collection("users").doc("u1").set({
    id: "u1",
    username: "Legacy User",
    email: "legacy@example.com",
    whatsapp: "08123456789",
    full_name: "Legacy User",
    photo_url: "",
    role: "user",
    created_at: Timestamp.now(),
  });

  const dryRunOutput = runScript("scripts/migrate-users.js", ["--dry-run"]);
  assert.match(dryRunOutput, /"operation":"DRY_RUN"/);
  assert.match(dryRunOutput, /"path":"users\/u1"/);
  assert.doesNotMatch(dryRunOutput, /legacy@example\.com/);
  assert.doesNotMatch(dryRunOutput, /081234/);

  const afterDryRun = (await db.collection("users").doc("u1").get()).data();
  assert.equal(afterDryRun.full_name, "Legacy User");
  assert.equal(afterDryRun.fullName, undefined);

  const migrationOutput = runScript("scripts/migrate-users.js");
  assert.match(migrationOutput, /"operation":"WRITE"/);

  const migrated = (await db.collection("users").doc("u1").get()).data();
  assert.equal(migrated.uid, "u1");
  assert.equal(migrated.username, "legacy_user");
  assert.equal(migrated.fullName, "Legacy User");
  assert.equal(Object.hasOwn(migrated, "email"), false);
  assert.equal(Object.hasOwn(migrated, "full_name"), false);

  const verificationOutput = runScript("scripts/verify-users.js");
  assert.match(verificationOutput, /"invalidCount": 0/);
});

test("stale updateTime is reported as conflict without overwriting data", async () => {
  const ref = db.collection("users").doc("conflict-user");
  await ref.set({
    id: "conflict-user",
    username: "legacy_user",
    full_name: "Before",
    photo_url: "",
    role: "user",
    created_at: Timestamp.now(),
  });
  const staleSnapshot = await ref.get();
  await ref.update({ full_name: "Concurrent Edit" });

  const result = await writeWithPreconditions(
    db,
    [{
      id: "conflict-user",
      updateTime: staleSnapshot.updateTime,
      data: {
        uid: "conflict-user",
        username: "legacy_user",
        fullName: "Before",
        photoUrl: "",
        bio: "",
        city: "",
        whatsapp: "",
        role: "user",
        createdAt: Timestamp.now(),
        updatedAt: Timestamp.now(),
      },
      removeFields: ["id", "full_name", "photo_url", "created_at"],
    }],
    () => FieldValue.delete()
  );

  assert.deepEqual(result.written, []);
  assert.equal(result.conflicts.length, 1);
  const current = (await ref.get()).data();
  assert.equal(current.full_name, "Concurrent Edit");
  assert.equal(current.fullName, undefined);
});
