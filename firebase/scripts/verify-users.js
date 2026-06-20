const { initializeFirestore } = require("./firebase-admin-client");
const { verifyUsers } = require("./user-migration-runner");

async function main() {
  const {
    db,
    isEmulator,
    projectId,
    isTimestamp,
  } = initializeFirestore();
  const snapshot = await db.collection("users").get();
  const documents = snapshot.docs.map((document) => ({
    id: document.id,
    data: document.data(),
  }));
  const report = verifyUsers(documents, isTimestamp);

  console.log(JSON.stringify({
    projectId,
    emulator: isEmulator,
    ...report,
  }, null, 2));

  if (report.invalidCount > 0) {
    process.exitCode = 1;
  }
}

main().catch((error) => {
  console.error(error.message);
  process.exitCode = 1;
});
