const fs = require("node:fs");
const path = require("node:path");
const test = require("node:test");
const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const {
  deleteDoc,
  doc,
  getDoc,
  serverTimestamp,
  setDoc,
  updateDoc,
} = require("firebase/firestore");

const projectId = "demo-adoptus";
let testEnv;

test.before(async () => {
  testEnv = await initializeTestEnvironment({
    projectId,
    firestore: {
      rules: fs.readFileSync(
        path.resolve(__dirname, "../../firestore.transitional.rules"),
        "utf8"
      ),
      host: "127.0.0.1",
      port: 8180,
    },
  });
});

test.beforeEach(async () => {
  await testEnv.clearFirestore();
});

test.after(async () => {
  await testEnv.cleanup();
});

function legacyUser(uid) {
  return {
    id: uid,
    username: "legacy_user",
    email: "legacy@example.com",
    full_name: "Legacy User",
    photo_url: "",
    role: "user",
    created_at: new Date(),
  };
}

function post(postId, userId) {
  return {
    postId,
    userId,
    petName: "Milo",
    petType: "Kucing",
    breed: "Domestic",
    age: 12,
    ageUnit: "Months",
    city: "Mataram",
    description: "",
    mediaUrl: "",
    mediaType: "image",
    isVaccinated: false,
    hasHealthPassport: false,
    adoptionFee: 0,
    status: "available",
    likesCount: 0,
    createdAt: serverTimestamp(),
  };
}

test("unauthenticated clients cannot read users or posts", async () => {
  const db = testEnv.unauthenticatedContext().firestore();

  await assertFails(getDoc(doc(db, "users/u1")));
  await assertFails(getDoc(doc(db, "posts/p1")));
});

test("authenticated clients can read legacy profiles and posts", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await setDoc(doc(context.firestore(), "users/u1"), legacyUser("u1"));
    await setDoc(doc(context.firestore(), "posts/p1"), post("p1", "u1"));
  });

  const db = testEnv.authenticatedContext("u2").firestore();
  await assertSucceeds(getDoc(doc(db, "users/u1")));
  await assertSucceeds(getDoc(doc(db, "posts/p1")));
});

test("users can only write their own profile", async () => {
  const otherDb = testEnv.authenticatedContext("u2").firestore();
  const ownerDb = testEnv.authenticatedContext("u1").firestore();

  await assertFails(setDoc(doc(otherDb, "users/u1"), legacyUser("u1")));
  await assertSucceeds(setDoc(doc(ownerDb, "users/u1"), legacyUser("u1")));
});

test("profile owner cannot elevate their role during transition", async () => {
  const ownerDb = testEnv.authenticatedContext("u1").firestore();
  await assertSucceeds(setDoc(doc(ownerDb, "users/u1"), legacyUser("u1")));

  await assertFails(updateDoc(doc(ownerDb, "users/u1"), { role: "admin" }));
});

test("post ownership is enforced", async () => {
  const ownerDb = testEnv.authenticatedContext("u1").firestore();
  await assertSucceeds(setDoc(doc(ownerDb, "posts/p1"), post("p1", "u1")));
  await assertSucceeds(updateDoc(doc(ownerDb, "posts/p1"), { city: "Bima" }));

  const otherDb = testEnv.authenticatedContext("u2").firestore();
  await assertFails(updateDoc(doc(otherDb, "posts/p1"), { city: "Sumbawa" }));
  await assertFails(deleteDoc(doc(otherDb, "posts/p1")));
  await assertSucceeds(deleteDoc(doc(ownerDb, "posts/p1")));
});

test("post owner cannot transfer ownership", async () => {
  const ownerDb = testEnv.authenticatedContext("u1").firestore();
  await assertSucceeds(setDoc(doc(ownerDb, "posts/p1"), post("p1", "u1")));

  await assertFails(updateDoc(doc(ownerDb, "posts/p1"), { userId: "u2" }));
  await assertFails(updateDoc(doc(ownerDb, "posts/p1"), { postId: "p2" }));
});

test("unknown collections are denied", async () => {
  const db = testEnv.authenticatedContext("u1").firestore();

  await assertFails(setDoc(doc(db, "adminSecrets/s1"), { value: "secret" }));
  await assertFails(getDoc(doc(db, "adminSecrets/s1")));
});
