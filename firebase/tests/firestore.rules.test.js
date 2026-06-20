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
        path.resolve(__dirname, "../../firestore.rules"),
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

function canonicalUser(uid) {
  return {
    uid,
    username: "canonical_user",
    fullName: "Canonical User",
    photoUrl: "",
    bio: "",
    city: "",
    whatsapp: "",
    role: "user",
    createdAt: serverTimestamp(),
    updatedAt: serverTimestamp(),
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

async function seed(pathName, data) {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await setDoc(doc(context.firestore(), pathName), data);
  });
}

test("unauthenticated clients cannot read canonical data", async () => {
  const db = testEnv.unauthenticatedContext().firestore();

  await assertFails(getDoc(doc(db, "users/u1")));
  await assertFails(getDoc(doc(db, "posts/p1")));
});

test("authenticated clients can read profiles and posts", async () => {
  await seed("users/u1", canonicalUser("u1"));
  await seed("posts/p1", post("p1", "u1"));

  const db = testEnv.authenticatedContext("u2").firestore();
  await assertSucceeds(getDoc(doc(db, "users/u1")));
  await assertSucceeds(getDoc(doc(db, "posts/p1")));
});

test("profile owner can create a canonical profile", async () => {
  const db = testEnv.authenticatedContext("u1").firestore();

  await assertSucceeds(setDoc(doc(db, "users/u1"), canonicalUser("u1")));
});

test("profile create rejects wrong identity legacy keys and elevated role", async () => {
  const db = testEnv.authenticatedContext("u1").firestore();

  await assertFails(setDoc(doc(db, "users/u2"), canonicalUser("u2")));
  await assertFails(setDoc(doc(db, "users/u1"), {
    ...canonicalUser("u1"),
    full_name: "Legacy Name",
  }));
  await assertFails(setDoc(doc(db, "users/u1"), {
    ...canonicalUser("u1"),
    role: "admin",
  }));
});

test("profile owner can update editable canonical fields", async () => {
  await seed("users/u1", canonicalUser("u1"));
  const db = testEnv.authenticatedContext("u1").firestore();

  await assertSucceeds(updateDoc(doc(db, "users/u1"), {
    bio: "Updated bio",
    city: "Mataram",
    updatedAt: serverTimestamp(),
  }));
});

test("profile owner preserves existing admin and moderator roles", async () => {
  for (const role of ["admin", "moderator"]) {
    const uid = `${role}-1`;
    await seed(`users/${uid}`, {
      ...canonicalUser(uid),
      role,
    });
    const db = testEnv.authenticatedContext(uid).firestore();

    await assertSucceeds(updateDoc(doc(db, `users/${uid}`), {
      bio: `${role} profile update`,
      updatedAt: serverTimestamp(),
    }));
    await assertFails(updateDoc(doc(db, `users/${uid}`), {
      role: "user",
      updatedAt: serverTimestamp(),
    }));
  }
});

test("profile immutable fields and deletion are protected", async () => {
  await seed("users/u1", canonicalUser("u1"));
  const db = testEnv.authenticatedContext("u1").firestore();

  await assertFails(updateDoc(doc(db, "users/u1"), {
    uid: "u2",
    updatedAt: serverTimestamp(),
  }));
  await assertFails(updateDoc(doc(db, "users/u1"), {
    role: "admin",
    updatedAt: serverTimestamp(),
  }));
  await assertFails(updateDoc(doc(db, "users/u1"), {
    createdAt: serverTimestamp(),
    updatedAt: serverTimestamp(),
  }));
  await assertFails(deleteDoc(doc(db, "users/u1")));
});

test("another user cannot write a profile", async () => {
  await seed("users/u1", canonicalUser("u1"));
  const db = testEnv.authenticatedContext("u2").firestore();

  await assertFails(updateDoc(doc(db, "users/u1"), {
    city: "Bima",
    updatedAt: serverTimestamp(),
  }));
  await assertFails(deleteDoc(doc(db, "users/u1")));
});

test("post owner can create a valid post", async () => {
  const db = testEnv.authenticatedContext("u1").firestore();

  await assertSucceeds(setDoc(doc(db, "posts/p1"), post("p1", "u1")));
});

test("post create enforces identity and initial state", async () => {
  const db = testEnv.authenticatedContext("u1").firestore();

  await assertFails(setDoc(doc(db, "posts/p1"), post("wrong-id", "u1")));
  await assertFails(setDoc(doc(db, "posts/p1"), post("p1", "u2")));
  await assertFails(setDoc(doc(db, "posts/p1"), {
    ...post("p1", "u1"),
    likesCount: 1,
  }));
  await assertFails(setDoc(doc(db, "posts/p1"), {
    ...post("p1", "u1"),
    status: "adopted",
  }));
});

test("post create rejects invalid domain values", async () => {
  const db = testEnv.authenticatedContext("u1").firestore();

  for (const invalidPost of [
    { ...post("p1", "u1"), age: -1 },
    { ...post("p1", "u1"), adoptionFee: -1 },
    { ...post("p1", "u1"), ageUnit: "Days" },
    { ...post("p1", "u1"), mediaType: "audio" },
    { ...post("p1", "u1"), status: "hidden" },
  ]) {
    await assertFails(setDoc(doc(db, "posts/p1"), invalidPost));
  }
});

test("post owner can update editable fields only", async () => {
  await seed("posts/p1", post("p1", "u1"));
  const db = testEnv.authenticatedContext("u1").firestore();

  await assertSucceeds(updateDoc(doc(db, "posts/p1"), {
    city: "Bima",
    status: "pending",
  }));
  await assertFails(updateDoc(doc(db, "posts/p1"), { userId: "u2" }));
  await assertFails(updateDoc(doc(db, "posts/p1"), { postId: "p2" }));
  await assertFails(updateDoc(doc(db, "posts/p1"), { likesCount: 10 }));
  await assertFails(updateDoc(doc(db, "posts/p1"), {
    createdAt: serverTimestamp(),
  }));
});

test("only the post owner can delete", async () => {
  await seed("posts/p1", post("p1", "u1"));
  const otherDb = testEnv.authenticatedContext("u2").firestore();
  const ownerDb = testEnv.authenticatedContext("u1").firestore();

  await assertFails(deleteDoc(doc(otherDb, "posts/p1")));
  await assertSucceeds(deleteDoc(doc(ownerDb, "posts/p1")));
});

test("unknown collections are denied", async () => {
  const db = testEnv.authenticatedContext("u1").firestore();

  await assertFails(setDoc(doc(db, "adminSecrets/s1"), { value: "secret" }));
  await assertFails(getDoc(doc(db, "adminSecrets/s1")));
});
