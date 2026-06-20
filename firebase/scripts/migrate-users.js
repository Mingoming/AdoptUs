const { initializeFirestore } = require("./firebase-admin-client");
const { executeMigration } = require("./user-migration-runner");

function parseAllowedPrivilegedUids(argumentsList) {
  return new Set(
    argumentsList
      .filter((argument) => argument.startsWith("--allow-privileged-uid="))
      .map((argument) => argument.slice("--allow-privileged-uid=".length))
      .filter(Boolean)
  );
}

const dryRun = process.argv.includes("--dry-run");
const confirmedProduction = process.argv.includes("--confirm-production");
const allowedPrivilegedUids = parseAllowedPrivilegedUids(process.argv);

async function loadUsers(db) {
  const snapshot = await db.collection("users").get();
  return snapshot.docs.map((document) => ({
    id: document.id,
    data: document.data(),
    updateTime: document.updateTime,
  }));
}

async function writeWithPreconditions(db, changes, deleteField) {
  const written = [];
  const conflicts = [];
  const failed = [];

  for (const change of changes) {
    const updateData = { ...change.data };
    for (const field of change.removeFields) {
      updateData[field] = deleteField();
    }

    try {
      await db.collection("users").doc(change.id).update(
        updateData,
        { lastUpdateTime: change.updateTime }
      );
      written.push(change.id);
    } catch (error) {
      if (error.code === 9 || error.code === "failed-precondition") {
        conflicts.push({
          id: change.id,
          reason: "document changed after migration snapshot",
        });
        continue;
      }
      failed.push({
        id: change.id,
        code: error.code == null ? "unknown" : String(error.code),
        reason: "write failed",
      });
      break;
    }
  }

  return { written, conflicts, failed };
}

async function main() {
  const {
    db,
    isEmulator,
    projectId,
    serverTimestamp,
    deleteField,
    isTimestamp,
  } = initializeFirestore();

  if (!dryRun && !isEmulator && !confirmedProduction) {
    throw new Error(
      "Production migration requires --confirm-production after backup and dry-run review."
    );
  }

  const documents = await loadUsers(db);
  const result = await executeMigration({
    documents,
    dryRun,
    serverTimestamp,
    isTimestamp,
    allowedPrivilegedUids,
    writeChanges: (changes) => writeWithPreconditions(
      db,
      changes,
      deleteField
    ),
  });

  for (const change of result.changes) {
    console.log(JSON.stringify({
      operation: "PLANNED",
      path: `users/${change.id}`,
      diff: change.diff,
    }));
  }
  for (const id of result.skippedIds) {
    console.log(JSON.stringify({
      operation: "SKIPPED",
      path: `users/${id}`,
    }));
  }
  for (const id of result.written) {
    console.log(JSON.stringify({
      operation: "WRITTEN",
      path: `users/${id}`,
    }));
  }
  for (const conflict of result.conflicts) {
    console.error(JSON.stringify({
      operation: "CONFLICT",
      path: `users/${conflict.id}`,
      reason: conflict.reason,
    }));
  }
  for (const failure of result.failed) {
    console.error(JSON.stringify({
      operation: "FAILED",
      path: `users/${failure.id}`,
      code: failure.code,
      reason: failure.reason,
    }));
  }
  for (const review of result.privilegedRoleReview) {
    console.warn(JSON.stringify({
      operation: "PRIVILEGED_ROLE_REVIEW",
      path: `users/${review.id}`,
      role: review.role,
    }));
  }

  console.log(JSON.stringify({
    projectId,
    emulator: isEmulator,
    dryRun: result.dryRun,
    ...result.report,
  }, null, 2));

  if (result.failed.length > 0) {
    process.exitCode = 1;
  } else if (result.conflicts.length > 0) {
    process.exitCode = 2;
  }
}

if (require.main === module) {
  main().catch((error) => {
    console.error(error.message);
    process.exitCode = 1;
  });
}

module.exports = {
  loadUsers,
  parseAllowedPrivilegedUids,
  writeWithPreconditions,
};
