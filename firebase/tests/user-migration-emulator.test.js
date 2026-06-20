const { execFileSync } = require("node:child_process");
const path = require("node:path");
const test = require("node:test");
const assert = require("node:assert/strict");
const { initializeApp } = require("firebase-admin/app");
const { getFirestore, Timestamp } = require("firebase-admin/firestore");

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
    full_name: "Legacy User",
    photo_url: "",
    role: "user",
    created_at: Timestamp.now(),
  });

  const dryRunOutput = runScript("scripts/migrate-users.js", ["--dry-run"]);
  assert.match(dryRunOutput, /\[DRY\] users\/u1/);

  const afterDryRun = (await db.collection("users").doc("u1").get()).data();
  assert.equal(afterDryRun.full_name, "Legacy User");
  assert.equal(afterDryRun.fullName, undefined);

  const migrationOutput = runScript("scripts/migrate-users.js");
  assert.match(migrationOutput, /\[WRITE\] users\/u1/);

  const migrated = (await db.collection("users").doc("u1").get()).data();
  assert.equal(migrated.uid, "u1");
  assert.equal(migrated.username, "legacy_user");
  assert.equal(migrated.fullName, "Legacy User");
  assert.equal(Object.hasOwn(migrated, "email"), false);
  assert.equal(Object.hasOwn(migrated, "full_name"), false);

  const verificationOutput = runScript("scripts/verify-users.js");
  assert.match(verificationOutput, /"invalidCount": 0/);
});
