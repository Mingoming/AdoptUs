const {
  createMigrationPlan,
  validateCanonicalUser,
} = require("./user-migration-core");

async function executeMigration({
  documents,
  dryRun,
  serverTimestamp,
  isTimestamp = (value) => value != null,
  writeChanges,
}) {
  const plan = createMigrationPlan(
    documents,
    serverTimestamp,
    isTimestamp
  );

  if (plan.invalid.length > 0) {
    const summary = plan.invalid
      .map((item) => `${item.id}: ${item.errors.join(", ")}`)
      .join("; ");
    throw new Error(`Migration validation failed: ${summary}`);
  }

  let writeResult = {
    written: [],
    conflicts: [],
  };
  if (!dryRun && plan.changes.length > 0) {
    writeResult = await writeChanges(plan.changes) || writeResult;
  }

  return {
    ...plan,
    dryRun,
    conflicts: writeResult.conflicts || [],
    report: {
      ...plan.report,
      written: (writeResult.written || []).length,
      conflicts: (writeResult.conflicts || []).length,
    },
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
