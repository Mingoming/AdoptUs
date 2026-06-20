const { initializeFirestore } = require("./firebase-admin-client");
const { executeMigration } = require("./user-migration-runner");

const dryRun = process.argv.includes("--dry-run");
const confirmedProduction = process.argv.includes("--confirm-production");

async function loadUsers(db) {
  const snapshot = await db.collection("users").get();
  return snapshot.docs.map((document) => ({
    id: document.id,
    data: document.data(),
  }));
}

async function writeInBatches(db, changes) {
  for (let offset = 0; offset < changes.length; offset += 400) {
    const batch = db.batch();
    const chunk = changes.slice(offset, offset + 400);

    for (const change of chunk) {
      batch.set(db.collection("users").doc(change.id), change.data);
    }

    await batch.commit();
  }
}

async function main() {
  const {
    db,
    isEmulator,
    projectId,
    serverTimestamp,
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
    writeChanges: (changes) => writeInBatches(db, changes),
  });

  for (const change of result.changes) {
    console.log(`${dryRun ? "[DRY]" : "[WRITE]"} users/${change.id}`);
  }

  console.log(JSON.stringify({
    projectId,
    emulator: isEmulator,
    dryRun: result.dryRun,
    ...result.report,
  }, null, 2));
}

main().catch((error) => {
  console.error(error.message);
  process.exitCode = 1;
});
