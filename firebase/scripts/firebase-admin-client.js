const { applicationDefault, initializeApp } = require("firebase-admin/app");
const {
  FieldValue,
  Timestamp,
  getFirestore,
} = require("firebase-admin/firestore");

function initializeFirestore() {
  const projectId = process.env.GCLOUD_PROJECT || "adoptus-e66f1";
  const emulatorHost = process.env.FIRESTORE_EMULATOR_HOST;

  if (!emulatorHost && !process.env.GOOGLE_APPLICATION_CREDENTIALS) {
    throw new Error(
      "GOOGLE_APPLICATION_CREDENTIALS is required outside the Firestore emulator."
    );
  }

  initializeApp(
    emulatorHost
      ? { projectId }
      : {
          credential: applicationDefault(),
          projectId,
        }
  );

  return {
    db: getFirestore(),
    isEmulator: Boolean(emulatorHost),
    projectId,
    serverTimestamp: () => FieldValue.serverTimestamp(),
    isTimestamp: (value) => value instanceof Timestamp,
  };
}

module.exports = {
  initializeFirestore,
};
