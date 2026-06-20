const {
  createMigrationPlan,
  validateCanonicalUser,
} = require("./user-migration-core");

async function executeMigration({
  documents,
  dryRun,
  serverTimestamp,
  writeChanges,
}) {
  const plan = createMigrationPlan(documents, serverTimestamp);

  if (!dryRun && plan.changes.length > 0) {
    await writeChanges(plan.changes);
  }

  return {
    ...plan,
    dryRun,
  };
}

function verifyUsers(documents, isTimestamp) {
  const invalid = [];

  for (const document of documents) {
    const errors = validateCanonicalUser(
      document.id,
      document.data,
      isTimestamp
    );
    if (errors.length > 0) {
      invalid.push({
        id: document.id,
        errors,
      });
    }
  }

  return {
    scanned: documents.length,
    invalidCount: invalid.length,
    invalid,
  };
}

module.exports = {
  executeMigration,
  verifyUsers,
};
