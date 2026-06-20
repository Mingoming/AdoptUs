# P0 Firestore Security and User Schema Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Close unsafe Firestore access, establish one canonical `users` schema, migrate existing user documents without breaking login/profile flows, and prove the result with emulator tests.

**Architecture:** Use a staged rollout. First deploy transitional ownership rules that immediately remove open access while accepting both legacy and canonical user documents. Then update the Android app to write canonical fields and read both formats, migrate production documents with the Firebase Admin SDK, verify migration, and finally deploy strict schema-validating rules.

**Tech Stack:** Kotlin, Firebase Authentication, Cloud Firestore, Firebase CLI, Firebase Emulator Suite, Node.js, Firebase Admin SDK, `@firebase/rules-unit-testing`, JUnit 4.

## Post-review Amendment

The reviewed implementation supersedes older examples later in this plan:

- Migration validates every proposed canonical document before the first write.
- Writes use each snapshot's `updateTime` as a precondition and report conflicts without overwriting concurrent changes.
- Migration preserves recognized roles (`user`, `admin`, `moderator`) and rejects unknown roles for manual review.
- Dry-run output contains field-level diffs with email and WhatsApp values redacted.
- Transitional `users` reads are owner-only while legacy email may still exist.
- Registration rolls back the new Auth user if profile creation fails; login and Setting recover a missing profile.

---

## Operational Urgency

ADR 0004 states that the development Firestore rules were open only until June 20, 2026. June 20, 2026 is the current date for this plan, so Task 2 is an immediate security action. If production still has open or date-expiring rules, deploy the transitional ownership rules before feature work continues.

## Scope

### Included

- Secure `users` and `posts` against unauthenticated access.
- Restrict profile writes to the authenticated owner.
- Restrict post create/update/delete operations to the authenticated owner.
- Validate post status, required keys, primitive types, and immutable fields.
- Normalize `users` documents into a canonical camelCase schema.
- Preserve compatibility with existing snake_case user documents during rollout.
- Migrate existing production `users` documents.
- Add emulator tests for allowed and denied operations.
- Document deployment, verification, and rollback procedures.

### Excluded

- Username global uniqueness.
- Like implementation and like subcollections.
- Adoption workflow and `adoptions` collection.
- Firebase Storage and Storage Rules.
- Moving all Firebase calls into repositories.
- Search, Profile, and Pet Detail feature implementation.
- Cloud Functions.

These remain later priorities. P0 only creates a secure and consistent base for them.

## Canonical Firestore Schema

### `users/{uid}`

```text
uid         String     required, equals document ID and Firebase Auth UID
username    String     required, 3..30 chars, no spaces
fullName    String     required, 1..80 chars
photoUrl    String     required, may be empty until Storage is enabled
bio         String     required, may be empty, max 300 chars
city        String     required, may be empty, max 80 chars
whatsapp    String     required, may be empty, max 30 chars
role        String     required, currently always "user"
createdAt   Timestamp  required, immutable
updatedAt   Timestamp  required
```

Do not store `email` in the public profile document. Firebase Authentication remains the source of truth for email. Existing `email` fields are removed by the migration.

### `posts/{postId}`

The existing post schema remains canonical:

```text
postId, userId, petName, petType, breed, age, ageUnit, city,
description, mediaUrl, mediaType, isVaccinated, hasHealthPassport,
adoptionFee, status, likesCount, createdAt
```

P0 rules enforce:

- `postId` equals document ID.
- `userId` equals authenticated UID on create.
- `age`, `adoptionFee`, and `likesCount` are non-negative integers.
- `ageUnit` is `Months` or `Years`.
- `mediaType` is `image` or `video`.
- `status` is `available`, `pending`, or `adopted`.
- `postId`, `userId`, `createdAt`, and `likesCount` cannot be changed by normal owner updates.
- New posts begin with `status == "available"` and `likesCount == 0`.

## File Map

### Create

- `.firebaserc`: Firebase project alias for `adoptus-e66f1`.
- `firebase.json`: Emulator and Firestore deployment configuration.
- `firebase.transitional.json`: Deployment configuration for temporary transitional rules.
- `firestore.rules`: Final strict rules.
- `firestore.transitional.rules`: Temporary secure rules used during migration.
- `firestore.indexes.json`: Version-controlled index configuration.
- `firebase/package.json`: Rules-test and migration dependencies/scripts.
- `firebase/tests/firestore.rules.test.js`: Emulator security tests.
- `firebase/scripts/migrate-users.js`: Idempotent Admin SDK migration.
- `firebase/scripts/verify-users.js`: Read-only canonical-schema verification.
- `app/src/test/java/com/example/adoptus/data/model/UserTest.kt`: Mapping compatibility tests.
- `doc/adr/0006-firestore-security-user-schema.md`: Architecture decision and rollout record.

### Modify

- `.gitignore`: Ignore Admin SDK credentials, emulator output, and Node dependencies.
- `app/src/main/java/com/example/adoptus/data/model/User.kt`: Canonical fields and legacy-compatible mapper.
- `app/src/main/java/com/example/adoptus/data/repository/AuthRepository.kt`: Canonical profile creation.
- `app/src/main/java/com/example/adoptus/ui/auth/RegisterActivity.kt`: Client validation aligned with rules.
- `app/src/main/java/com/example/adoptus/fragment/SettingFragment.kt`: Canonical reads/writes and `updatedAt`.
- `README.md`: Canonical schema and Firebase emulator commands.
- `CHANGELOG.md`: P0 security/schema changes and verification results.

## Rollout Invariants

1. Never commit a service-account JSON file.
2. Never run the migration before exporting/backing up production Firestore.
3. Never deploy strict rules until the app compatibility release is available and migration verification reports zero invalid documents.
4. Migration must be idempotent: rerunning it produces no destructive changes.
5. Do not delete legacy fields until their canonical replacements are populated.
6. Use `--dry-run` before every real migration.

---

### Task 1: Add Firebase Configuration and Tooling

**Files:**
- Create: `.firebaserc`
- Create: `firebase.json`
- Create: `firebase.transitional.json`
- Create: `firestore.indexes.json`
- Create: `firebase/package.json`
- Modify: `.gitignore`

- [ ] **Step 1: Add the Firebase project alias**

Create `.firebaserc`:

```json
{
  "projects": {
    "default": "adoptus-e66f1"
  }
}
```

- [ ] **Step 2: Add Firestore and emulator configuration**

Create `firebase.json`:

```json
{
  "firestore": {
    "rules": "firestore.rules",
    "indexes": "firestore.indexes.json"
  },
  "emulators": {
    "auth": {
      "port": 9099
    },
    "firestore": {
      "port": 8080
    },
    "ui": {
      "enabled": true,
      "port": 4000
    },
    "singleProjectMode": true
  }
}
```

- [ ] **Step 3: Add a dedicated transitional-rules deployment config**

Create `firebase.transitional.json`:

```json
{
  "firestore": {
    "rules": "firestore.transitional.rules",
    "indexes": "firestore.indexes.json"
  }
}
```

- [ ] **Step 4: Add an empty version-controlled index configuration**

Create `firestore.indexes.json`:

```json
{
  "indexes": [],
  "fieldOverrides": []
}
```

- [ ] **Step 5: Add isolated Firebase tooling dependencies**

Create `firebase/package.json`:

```json
{
  "name": "adoptus-firebase-tools",
  "private": true,
  "type": "commonjs",
  "scripts": {
    "test:rules": "firebase emulators:exec --config ../firebase.transitional.json --project demo-adoptus --only auth,firestore \"node --test tests/firestore.rules.test.js\"",
    "migrate:users:dry": "node scripts/migrate-users.js --dry-run",
    "migrate:users": "node scripts/migrate-users.js",
    "verify:users": "node scripts/verify-users.js"
  },
  "devDependencies": {
    "@firebase/rules-unit-testing": "^4.0.1",
    "firebase": "^11.0.0",
    "firebase-admin": "^13.0.0",
    "firebase-tools": "^14.0.0"
  }
}
```

- [ ] **Step 6: Protect local credentials and generated files**

Append to `.gitignore`:

```gitignore
# Firebase local tooling
firebase/node_modules/
firebase-debug.log
firestore-debug.log
ui-debug.log
.firebase/
firebase/service-account*.json
service-account*.json
```

- [ ] **Step 7: Install tooling**

Run:

```powershell
Set-Location firebase
npm install
```

Expected: `npm` completes successfully and creates `firebase/package-lock.json`.

- [ ] **Step 8: Commit tooling configuration**

```powershell
git add .firebaserc firebase.json firebase.transitional.json firestore.indexes.json firebase/package.json firebase/package-lock.json .gitignore
git commit -m "chore: add firebase emulator tooling"
```

---

### Task 2: Write Transitional Security Rules

**Files:**
- Create: `firestore.transitional.rules`
- Test: `firebase/tests/firestore.rules.test.js`

The transitional rules close unauthenticated access and enforce ownership, but temporarily tolerate legacy user field names. They are deployed before the migration.

- [ ] **Step 1: Write failing tests for authentication and ownership**

Create `firebase/tests/firestore.rules.test.js` with test setup and these initial cases:

```javascript
const fs = require("node:fs");
const path = require("node:path");
const test = require("node:test");
const assert = require("node:assert/strict");
const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const {
  doc,
  getDoc,
  setDoc,
  updateDoc,
  deleteDoc,
  Timestamp,
  serverTimestamp,
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
      port: 8080,
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
    created_at: Timestamp.now(),
  };
}

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

test("unauthenticated clients cannot read users or posts", async () => {
  const db = testEnv.unauthenticatedContext().firestore();
  await assertFails(getDoc(doc(db, "users/u1")));
  await assertFails(getDoc(doc(db, "posts/p1")));
});

test("authenticated clients can read profiles and posts", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await setDoc(doc(context.firestore(), "users/u1"), legacyUser("u1"));
    await setDoc(doc(context.firestore(), "posts/p1"), post("p1", "u1"));
  });

  const db = testEnv.authenticatedContext("u2").firestore();
  await assertSucceeds(getDoc(doc(db, "users/u1")));
  await assertSucceeds(getDoc(doc(db, "posts/p1")));
});

test("a user cannot write another user's profile", async () => {
  const db = testEnv.authenticatedContext("u2").firestore();
  await assertFails(setDoc(doc(db, "users/u1"), canonicalUser("u1")));
});

test("a post owner can create and delete their post", async () => {
  const db = testEnv.authenticatedContext("u1").firestore();
  await assertSucceeds(setDoc(doc(db, "posts/p1"), post("p1", "u1")));
  await assertSucceeds(deleteDoc(doc(db, "posts/p1")));
});

test("a non-owner cannot update or delete a post", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await setDoc(doc(context.firestore(), "posts/p1"), post("p1", "u1"));
  });

  const db = testEnv.authenticatedContext("u2").firestore();
  await assertFails(updateDoc(doc(db, "posts/p1"), { city: "Bima" }));
  await assertFails(deleteDoc(doc(db, "posts/p1")));
});
```

- [ ] **Step 2: Run tests and confirm rules are missing**

Run from `firebase/`:

```powershell
npm run test:rules
```

Expected: FAIL because `firestore.transitional.rules` does not exist.

- [ ] **Step 3: Implement transitional rules**

Create `firestore.transitional.rules`:

```text
rules_version = '2';

service cloud.firestore {
  match /databases/{database}/documents {
    function signedIn() {
      return request.auth != null;
    }

    function isOwner(uid) {
      return signedIn() && request.auth.uid == uid;
    }

    function ownsExistingPost() {
      return signedIn() && resource.data.userId == request.auth.uid;
    }

    function createsOwnPost(postId) {
      return signedIn()
        && request.resource.data.postId == postId
        && request.resource.data.userId == request.auth.uid;
    }

    match /users/{uid} {
      allow read: if signedIn();
      allow create, update, delete: if isOwner(uid);
    }

    match /posts/{postId} {
      allow read: if signedIn();
      allow create: if createsOwnPost(postId);
      allow update: if ownsExistingPost()
        && request.resource.data.userId == resource.data.userId
        && request.resource.data.postId == resource.data.postId;
      allow delete: if ownsExistingPost();
    }

    match /{document=**} {
      allow read, write: if false;
    }
  }
}
```

- [ ] **Step 4: Run transitional rules tests**

```powershell
Set-Location firebase
npm run test:rules
```

Expected: all initial tests PASS.

- [ ] **Step 5: Review currently deployed rules before changing production**

Open Firebase Console, select Firestore Database, then open Rules. Save the currently published rules outside Git as a dated rollback artifact. Do not place production rules containing temporary open access into the repository.

- [ ] **Step 6: Deploy transitional rules only**

Before deployment, confirm the Android app always requires authentication before opening Firestore-backed pages.

Run:

```powershell
npx firebase-tools deploy --config firebase.transitional.json --only firestore:rules --project adoptus-e66f1
```

Expected: deployment succeeds and unauthenticated Firestore access is denied.

- [ ] **Step 7: Smoke-test the existing application**

Verify manually:

- Existing email/password login still works.
- Google Sign-In still works.
- Feed loads for an authenticated user.
- Add Post succeeds for the signed-in owner.
- Setting loads and updates the signed-in user's own document.
- A signed-out client cannot read `users` or `posts`.

- [ ] **Step 8: Commit transitional rules and tests**

```powershell
git add firestore.transitional.rules firebase/tests/firestore.rules.test.js
git commit -m "security: add transitional firestore rules"
```

---

### Task 3: Normalize the Android User Model

**Files:**
- Modify: `app/src/main/java/com/example/adoptus/data/model/User.kt`
- Create: `app/src/test/java/com/example/adoptus/data/model/UserTest.kt`

- [ ] **Step 1: Write failing compatibility tests**

Create `UserTest.kt`:

```kotlin
package com.example.adoptus.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UserTest {

    @Test
    fun fromMap_readsCanonicalFields() {
        val user = User.fromMap(
            documentId = "u1",
            map = mapOf(
                "uid" to "u1",
                "username" to "milo_owner",
                "fullName" to "Milo Owner",
                "photoUrl" to "https://example.com/avatar.jpg",
                "bio" to "Pet foster",
                "city" to "Mataram",
                "whatsapp" to "628123456789",
                "role" to "user"
            )
        )

        assertEquals("u1", user.uid)
        assertEquals("Milo Owner", user.fullName)
        assertEquals("https://example.com/avatar.jpg", user.photoUrl)
        assertEquals("Mataram", user.city)
        assertFalse(user.needsMigration)
    }

    @Test
    fun fromMap_fallsBackToLegacyFields() {
        val user = User.fromMap(
            documentId = "u1",
            map = mapOf(
                "id" to "u1",
                "username" to "legacy_owner",
                "full_name" to "Legacy Owner",
                "photo_url" to "legacy.jpg",
                "role" to "user"
            )
        )

        assertEquals("u1", user.uid)
        assertEquals("Legacy Owner", user.fullName)
        assertEquals("legacy.jpg", user.photoUrl)
        assertTrue(user.needsMigration)
    }

    @Test
    fun toProfileMap_writesOnlyCanonicalEditableFields() {
        val user = User(
            uid = "u1",
            username = "owner",
            fullName = "Owner Name",
            photoUrl = "",
            bio = "Bio",
            city = "Mataram",
            whatsapp = "628123"
        )

        assertEquals(
            setOf("username", "fullName", "photoUrl", "bio", "city", "whatsapp"),
            user.toEditableProfileMap().keys
        )
    }
}
```

- [ ] **Step 2: Run the model tests and confirm failure**

```powershell
.\gradlew.bat :app:testDebugUnitTest --tests "com.example.adoptus.data.model.UserTest" --no-daemon
```

Expected: FAIL because the canonical fields and mapping functions do not exist.

- [ ] **Step 3: Implement the canonical model with legacy fallback**

Replace `User.kt` with:

```kotlin
package com.example.adoptus.data.model

import com.google.firebase.Timestamp

data class User(
    val uid: String = "",
    val username: String = "",
    val fullName: String = "",
    val photoUrl: String = "",
    val bio: String = "",
    val city: String = "",
    val whatsapp: String = "",
    val role: String = "user",
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null,
    val needsMigration: Boolean = false
) {
    fun toEditableProfileMap(): Map<String, Any> = mapOf(
        "username" to username,
        "fullName" to fullName,
        "photoUrl" to photoUrl,
        "bio" to bio,
        "city" to city,
        "whatsapp" to whatsapp
    )

    companion object {
        fun fromMap(documentId: String, map: Map<String, Any?>): User {
            val hasLegacyFields = map.containsKey("id")
                || map.containsKey("full_name")
                || map.containsKey("photo_url")
                || map.containsKey("created_at")

            return User(
                uid = map["uid"] as? String
                    ?: map["id"] as? String
                    ?: documentId,
                username = map["username"] as? String ?: "",
                fullName = map["fullName"] as? String
                    ?: map["full_name"] as? String
                    ?: "",
                photoUrl = map["photoUrl"] as? String
                    ?: map["photo_url"] as? String
                    ?: "",
                bio = map["bio"] as? String ?: "",
                city = map["city"] as? String ?: "",
                whatsapp = map["whatsapp"] as? String ?: "",
                role = map["role"] as? String ?: "user",
                createdAt = map["createdAt"] as? Timestamp
                    ?: map["created_at"] as? Timestamp,
                updatedAt = map["updatedAt"] as? Timestamp,
                needsMigration = hasLegacyFields
            )
        }
    }
}
```

- [ ] **Step 4: Run model tests**

```powershell
.\gradlew.bat :app:testDebugUnitTest --tests "com.example.adoptus.data.model.UserTest" --no-daemon
```

Expected: all `UserTest` tests PASS.

- [ ] **Step 5: Commit the model normalization**

```powershell
git add app/src/main/java/com/example/adoptus/data/model/User.kt app/src/test/java/com/example/adoptus/data/model/UserTest.kt
git commit -m "refactor: normalize user data model"
```

---

### Task 4: Make Registration Write the Canonical Schema

**Files:**
- Modify: `app/src/main/java/com/example/adoptus/data/repository/AuthRepository.kt`
- Modify: `app/src/main/java/com/example/adoptus/ui/auth/RegisterActivity.kt`

- [ ] **Step 1: Replace legacy registration fields**

Change both email/password registration and first-time Google Sign-In creation to write:

```kotlin
val userData = hashMapOf(
    "uid" to uid,
    "username" to username,
    "fullName" to fullName,
    "photoUrl" to photoUrl,
    "bio" to "",
    "city" to "",
    "whatsapp" to "",
    "role" to "user",
    "createdAt" to FieldValue.serverTimestamp(),
    "updatedAt" to FieldValue.serverTimestamp()
)
```

For email/password registration:

- `uid = result.user!!.uid`
- `username` and `fullName` come from the form
- `photoUrl = ""`

For Google Sign-In:

- `uid = user.uid`
- `username` is normalized with the helper below
- `fullName = user.displayName?.trim().orEmpty().ifBlank { username }`
- `photoUrl = user.photoUrl?.toString() ?: ""`

Add a private helper so Google display names satisfy the final username rule:

```kotlin
private fun googleUsername(user: FirebaseUser): String {
    val source = user.displayName
        ?: user.email?.substringBefore("@")
        ?: "user_${user.uid.take(8)}"
    val normalized = source
        .trim()
        .lowercase()
        .replace(Regex("\\s+"), "_")
        .replace(Regex("[^a-z0-9_]"), "")
        .take(30)
    return normalized.takeIf { it.length >= 3 }
        ?: "user_${user.uid.take(8)}"
}
```

Do not write `email`, `id`, `full_name`, `photo_url`, or `created_at`.

- [ ] **Step 2: Ensure existing Google users are not overwritten**

Keep the current `if (!doc.exists())` guard. Existing documents are migrated separately, so Google login must not replace profile edits.

- [ ] **Step 3: Align registration validation with Firestore rules**

In `RegisterActivity`, reject:

```kotlin
if (fullName.length > 80) {
    binding.tilFullName.error = "Full name must be 80 characters or fewer."
    return@setOnClickListener
}

if (username.length > 30) {
    binding.tilUsername.error = "Username must be 30 characters or fewer."
    return@setOnClickListener
}
```

Keep the existing non-empty, minimum-three-character, and no-space checks.

- [ ] **Step 4: Compile Kotlin**

```powershell
.\gradlew.bat :app:compileDebugKotlin --no-daemon
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 5: Test new-user registration against the emulator**

Manually create:

- One email/password account.
- One Google account on the Firebase development project using an Android test device.

Confirm new documents contain only canonical fields.

- [ ] **Step 6: Commit canonical registration**

```powershell
git add app/src/main/java/com/example/adoptus/data/repository/AuthRepository.kt app/src/main/java/com/example/adoptus/ui/auth/RegisterActivity.kt
git commit -m "fix: write canonical user schema during registration"
```

---

### Task 5: Make Setting Read Legacy and Canonical Documents

**Files:**
- Modify: `app/src/main/java/com/example/adoptus/fragment/SettingFragment.kt`

This task deliberately avoids the larger `UserRepository` refactor, which belongs to P1. P0 only removes schema inconsistency without broad architecture churn.

- [ ] **Step 1: Parse profile documents through `User.fromMap`**

In `loadUserData`, replace individual `getString()` calls with:

```kotlin
val doc = db.collection("users").document(uid).get().await()
val user = User.fromMap(doc.id, doc.data.orEmpty())

etFullName.setText(user.fullName)
etUsername.setText(user.username)
etBio.setText(user.bio)
etCity.setText(user.city)
etWhatsapp.setText(user.whatsapp)
```

Add:

```kotlin
import com.example.adoptus.data.model.User
```

- [ ] **Step 2: Write canonical profile updates**

Build updates with:

```kotlin
val updates = mutableMapOf<String, Any>(
    "fullName" to fullName,
    "username" to username,
    "bio" to bio,
    "city" to city,
    "whatsapp" to whatsapp,
    "updatedAt" to FieldValue.serverTimestamp()
)
```

Add:

```kotlin
import com.google.firebase.firestore.FieldValue
```

Do not delete legacy fields from the Android client. The Admin migration performs controlled cleanup.

- [ ] **Step 3: Align Setting validation with Firestore rules**

Before saving, enforce:

```kotlin
if (fullName.isEmpty()) {
    Toast.makeText(context, "Full name is required", Toast.LENGTH_SHORT).show()
    return@setOnClickListener
}
if (fullName.length > 80) {
    Toast.makeText(context, "Full name must be 80 characters or fewer", Toast.LENGTH_SHORT).show()
    return@setOnClickListener
}
if (username.length !in 3..30) {
    tilUsername.error = "Username must be 3 to 30 characters"
    return@setOnClickListener
}
if (bio.length > 300 || city.length > 80 || whatsapp.length > 30) {
    Toast.makeText(context, "Profile field is too long", Toast.LENGTH_SHORT).show()
    return@setOnClickListener
}
```

- [ ] **Step 4: Compile and run unit tests**

```powershell
.\gradlew.bat :app:testDebugUnitTest :app:compileDebugKotlin --no-daemon
```

Expected: BUILD SUCCESSFUL and all tests PASS.

- [ ] **Step 5: Manually test both document formats**

Using the emulator:

- Seed one legacy user document.
- Seed one canonical user document.
- Log in as each user.
- Confirm Setting loads the same visible values.
- Save changes and confirm canonical fields plus `updatedAt` are written.

- [ ] **Step 6: Commit compatibility changes**

```powershell
git add app/src/main/java/com/example/adoptus/fragment/SettingFragment.kt
git commit -m "fix: support canonical and legacy user profiles"
```

---

### Task 6: Write the Idempotent User Migration

**Files:**
- Create: `firebase/scripts/migrate-users.js`
- Create: `firebase/scripts/verify-users.js`

- [ ] **Step 1: Implement migration argument and credential guards**

The scripts must initialize Admin SDK with Application Default Credentials:

```javascript
const { applicationDefault, initializeApp } = require("firebase-admin/app");
const { getFirestore } = require("firebase-admin/firestore");

const projectId = process.env.GCLOUD_PROJECT || "adoptus-e66f1";
initializeApp(
  process.env.FIRESTORE_EMULATOR_HOST
    ? { projectId }
    : { credential: applicationDefault(), projectId }
);
```

Operators provide credentials through:

```powershell
$env:GOOGLE_APPLICATION_CREDENTIALS="C:\secure\adoptus-service-account.json"
```

The credential file must remain outside the repository.

- [ ] **Step 2: Implement canonical transformation**

Create `firebase/scripts/migrate-users.js`:

```javascript
const { applicationDefault, initializeApp } = require("firebase-admin/app");
const { getFirestore, FieldValue } = require("firebase-admin/firestore");

const projectId = process.env.GCLOUD_PROJECT || "adoptus-e66f1";
initializeApp(
  process.env.FIRESTORE_EMULATOR_HOST
    ? { projectId }
    : { credential: applicationDefault(), projectId }
);

const db = getFirestore();
const dryRun = process.argv.includes("--dry-run");

function stringValue(value, fallback = "") {
  return typeof value === "string" ? value : fallback;
}

function normalizeUsername(value, uid) {
  const normalized = stringValue(value)
    .trim()
    .toLowerCase()
    .replace(/\s+/g, "_")
    .replace(/[^a-z0-9_]/g, "")
    .slice(0, 30);
  return normalized.length >= 3 ? normalized : `user_${uid.slice(0, 8)}`;
}

function canonicalData(documentId, data) {
  const username = normalizeUsername(data.username, documentId);
  const legacyFullName = stringValue(data.fullName, stringValue(data.full_name));
  return {
    uid: documentId,
    username,
    fullName: legacyFullName.trim().slice(0, 80) || username,
    photoUrl: stringValue(data.photoUrl, stringValue(data.photo_url)).slice(0, 2048),
    bio: stringValue(data.bio).slice(0, 300),
    city: stringValue(data.city).slice(0, 80),
    whatsapp: stringValue(data.whatsapp).slice(0, 30),
    role: data.role === "user" ? "user" : "user",
    createdAt: data.createdAt || data.created_at || FieldValue.serverTimestamp(),
    updatedAt: data.updatedAt || FieldValue.serverTimestamp(),
  };
}

async function main() {
  const snapshot = await db.collection("users").get();
  let scanned = 0;
  let changed = 0;
  let skipped = 0;
  let batch = db.batch();
  let batchSize = 0;

  for (const document of snapshot.docs) {
    scanned += 1;
    const current = document.data();
    const next = canonicalData(document.id, current);
    const legacyKeys = ["id", "email", "full_name", "photo_url", "created_at"];
    const needsChange =
      legacyKeys.some((key) => Object.hasOwn(current, key)) ||
      !Object.hasOwn(current, "uid") ||
      !Object.hasOwn(current, "fullName") ||
      !Object.hasOwn(current, "photoUrl") ||
      !Object.hasOwn(current, "bio") ||
      !Object.hasOwn(current, "city") ||
      !Object.hasOwn(current, "whatsapp") ||
      !Object.hasOwn(current, "createdAt") ||
      !Object.hasOwn(current, "updatedAt") ||
      current.uid !== next.uid ||
      current.username !== next.username ||
      current.fullName !== next.fullName ||
      current.photoUrl !== next.photoUrl ||
      current.bio !== next.bio ||
      current.city !== next.city ||
      current.whatsapp !== next.whatsapp ||
      current.role !== "user";

    if (!needsChange) {
      skipped += 1;
      continue;
    }

    changed += 1;
    console.log(`${dryRun ? "[DRY]" : "[WRITE]"} users/${document.id}`);

    if (!dryRun) {
      batch.set(document.ref, next, { merge: false });
      batchSize += 1;

      if (batchSize === 400) {
        await batch.commit();
        batch = db.batch();
        batchSize = 0;
      }
    }
  }

  if (!dryRun && batchSize > 0) {
    await batch.commit();
  }

  console.log(JSON.stringify({ scanned, changed, skipped, dryRun }, null, 2));
}

main().catch((error) => {
  console.error(error);
  process.exitCode = 1;
});
```

The script uses full document replacement after constructing every canonical field. This deliberately removes `email` and all legacy keys.

- [ ] **Step 3: Implement read-only verification**

Create `firebase/scripts/verify-users.js`:

```javascript
const { applicationDefault, initializeApp } = require("firebase-admin/app");
const { getFirestore, Timestamp } = require("firebase-admin/firestore");

const projectId = process.env.GCLOUD_PROJECT || "adoptus-e66f1";
initializeApp(
  process.env.FIRESTORE_EMULATOR_HOST
    ? { projectId }
    : { credential: applicationDefault(), projectId }
);

const db = getFirestore();
const requiredKeys = [
  "uid",
  "username",
  "fullName",
  "photoUrl",
  "bio",
  "city",
  "whatsapp",
  "role",
  "createdAt",
  "updatedAt",
];
const legacyKeys = ["id", "email", "full_name", "photo_url", "created_at"];
const allowedKeys = new Set(requiredKeys);

function validate(document) {
  const data = document.data();
  const errors = [];

  for (const key of requiredKeys) {
    if (!Object.hasOwn(data, key)) errors.push(`missing ${key}`);
  }
  for (const key of legacyKeys) {
    if (Object.hasOwn(data, key)) errors.push(`legacy key ${key}`);
  }
  for (const key of Object.keys(data)) {
    if (!allowedKeys.has(key)) errors.push(`unexpected key ${key}`);
  }
  if (data.uid !== document.id) errors.push("uid does not match document ID");
  if (data.role !== "user") errors.push("role must be user");
  if (typeof data.username !== "string" || data.username.length < 3 || data.username.length > 30 || /\s/.test(data.username)) {
    errors.push("username is invalid");
  }
  if (typeof data.fullName !== "string" || data.fullName.length < 1 || data.fullName.length > 80) {
    errors.push("fullName is invalid");
  }
  if (typeof data.photoUrl !== "string" || data.photoUrl.length > 2048) errors.push("photoUrl is invalid");
  if (typeof data.bio !== "string" || data.bio.length > 300) errors.push("bio is invalid");
  if (typeof data.city !== "string" || data.city.length > 80) errors.push("city is invalid");
  if (typeof data.whatsapp !== "string" || data.whatsapp.length > 30) errors.push("whatsapp is invalid");
  if (!(data.createdAt instanceof Timestamp)) errors.push("createdAt is not Timestamp");
  if (!(data.updatedAt instanceof Timestamp)) errors.push("updatedAt is not Timestamp");

  return errors;
}

async function main() {
  const snapshot = await db.collection("users").get();
  const invalid = [];

  for (const document of snapshot.docs) {
    const errors = validate(document);
    if (errors.length > 0) invalid.push({ path: document.ref.path, errors });
  }

  console.log(JSON.stringify({
    scanned: snapshot.size,
    invalidCount: invalid.length,
    invalid,
  }, null, 2));

  if (invalid.length > 0) process.exitCode = 1;
}

main().catch((error) => {
  console.error(error);
  process.exitCode = 1;
});
```

- [ ] **Step 4: Test migration on emulator data**

Start the emulator with import/export support:

```powershell
npx firebase-tools emulators:start --config ..\firebase.json --project demo-adoptus --only auth,firestore
```

In another terminal, point Admin SDK to the emulator:

```powershell
$env:FIRESTORE_EMULATOR_HOST="127.0.0.1:8080"
$env:GCLOUD_PROJECT="demo-adoptus"
npm run migrate:users:dry
npm run migrate:users
npm run verify:users
```

Expected:

- Dry run reports legacy documents without writing.
- Real migration converts them.
- Verification exits with code 0 and `invalidCount: 0`.
- Running migration again reports `changed: 0`.

- [ ] **Step 5: Commit migration tooling**

```powershell
git add firebase/scripts/migrate-users.js firebase/scripts/verify-users.js
git commit -m "chore: add user schema migration tooling"
```

---

### Task 7: Back Up and Migrate Production Users

**Files:**
- No repository file changes.

This task changes external production state and requires explicit operator approval.

- [ ] **Step 1: Confirm the compatible app build is ready**

Before migration:

- Registration writes canonical fields.
- Setting reads legacy and canonical fields.
- Existing authenticated flows pass smoke tests.
- Transitional rules are deployed.

- [ ] **Step 2: Export production Firestore**

Use Firebase Console export or Google Cloud CLI to create a dated backup. Record:

- Project: `adoptus-e66f1`
- Export timestamp
- Export location
- Total `users` document count

Do not continue without a recoverable backup.

- [ ] **Step 3: Run read-only verification before migration**

```powershell
Set-Location firebase
$env:GOOGLE_APPLICATION_CREDENTIALS="C:\secure\adoptus-service-account.json"
$env:GCLOUD_PROJECT="adoptus-e66f1"
npm run verify:users
```

Expected before migration: non-zero exit is acceptable and identifies legacy documents.

- [ ] **Step 4: Run production dry run**

```powershell
npm run migrate:users:dry
```

Review:

- `scanned` equals the expected user count.
- Every proposed path is under `users/`.
- No unrelated collection appears.
- `changed` count is plausible.

- [ ] **Step 5: Run production migration**

```powershell
npm run migrate:users
```

Expected: command exits successfully and prints final counts.

- [ ] **Step 6: Verify production schema**

```powershell
npm run verify:users
```

Expected: exit code 0 and `invalidCount: 0`.

- [ ] **Step 7: Smoke-test production**

Test with at least:

- One migrated email/password account.
- One migrated Google account.
- One newly registered account.

Verify login, feed, Add Post, Setting load, and Setting save.

- [ ] **Step 8: Preserve migration evidence**

Store the backup location and command summaries in the team release record. Do not commit user data or service-account details.

---

### Task 8: Write Strict Final Firestore Rules

**Files:**
- Create: `firestore.rules`
- Modify: `firebase/tests/firestore.rules.test.js`

- [ ] **Step 1: Extend tests for strict user validation**

Add tests covering:

- Owner can create a canonical user document.
- Owner cannot create a document with legacy keys.
- Owner cannot set `role` to `admin`.
- Owner cannot change `uid`, `role`, or `createdAt`.
- Owner can update editable profile fields and `updatedAt`.
- Owner cannot delete the profile document directly.
- Another user cannot update/delete the profile.
- Unauthenticated client cannot read profiles.

Example:

```javascript
test("profile owner can create a canonical profile", async () => {
  const db = testEnv.authenticatedContext("u1").firestore();
  await assertSucceeds(setDoc(doc(db, "users/u1"), canonicalUser("u1")));
});

test("profile create rejects legacy keys and elevated role", async () => {
  const db = testEnv.authenticatedContext("u1").firestore();
  await assertFails(setDoc(doc(db, "users/u1"), {
    ...canonicalUser("u1"),
    full_name: "Legacy Name",
  }));
  await assertFails(setDoc(doc(db, "users/u1"), {
    ...canonicalUser("u1"),
    role: "admin",
  }));
});

test("profile owner can update editable fields with server timestamp", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await setDoc(doc(context.firestore(), "users/u1"), canonicalUser("u1"));
  });

  const db = testEnv.authenticatedContext("u1").firestore();
  await assertSucceeds(updateDoc(doc(db, "users/u1"), {
    bio: "Updated bio",
    city: "Mataram",
    updatedAt: serverTimestamp(),
  }));
});

test("profile owner cannot change immutable fields or delete profile", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await setDoc(doc(context.firestore(), "users/u1"), canonicalUser("u1"));
  });

  const db = testEnv.authenticatedContext("u1").firestore();
  await assertFails(updateDoc(doc(db, "users/u1"), {
    role: "admin",
    updatedAt: serverTimestamp(),
  }));
  await assertFails(updateDoc(doc(db, "users/u1"), {
    uid: "u2",
    updatedAt: serverTimestamp(),
  }));
  await assertFails(updateDoc(doc(db, "users/u1"), {
    createdAt: serverTimestamp(),
    updatedAt: serverTimestamp(),
  }));
  await assertFails(deleteDoc(doc(db, "users/u1")));
});
```

- [ ] **Step 2: Extend tests for strict post validation**

Add cases covering:

- Create is rejected when `postId` differs from document ID.
- Create is rejected when `userId` differs from auth UID.
- Create is rejected for negative age or adoption fee.
- Create is rejected for unsupported status, media type, or age unit.
- Create is rejected when `likesCount` is not zero.
- Owner can update editable post fields.
- Owner cannot change `postId`, `userId`, `createdAt`, or `likesCount`.
- Non-owner cannot update/delete.
- Authenticated users can read posts.
- Unknown collections are denied.

Add concrete tests:

```javascript
test("post create enforces identity and initial counters", async () => {
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

  await assertFails(setDoc(doc(db, "posts/p1"), {
    ...post("p1", "u1"),
    age: -1,
  }));
  await assertFails(setDoc(doc(db, "posts/p1"), {
    ...post("p1", "u1"),
    adoptionFee: -1,
  }));
  await assertFails(setDoc(doc(db, "posts/p1"), {
    ...post("p1", "u1"),
    ageUnit: "Days",
  }));
  await assertFails(setDoc(doc(db, "posts/p1"), {
    ...post("p1", "u1"),
    mediaType: "audio",
  }));
  await assertFails(setDoc(doc(db, "posts/p1"), {
    ...post("p1", "u1"),
    status: "hidden",
  }));
});

test("post owner can update editable fields but not immutable fields", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await setDoc(doc(context.firestore(), "posts/p1"), post("p1", "u1"));
  });

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

test("unknown collections are denied", async () => {
  const db = testEnv.authenticatedContext("u1").firestore();
  await assertFails(setDoc(doc(db, "adminSecrets/s1"), { value: "secret" }));
  await assertFails(getDoc(doc(db, "adminSecrets/s1")));
});
```

- [ ] **Step 3: Implement strict final rules**

Create `firestore.rules`:

```text
rules_version = '2';

service cloud.firestore {
  match /databases/{database}/documents {
    function signedIn() {
      return request.auth != null;
    }

    function isOwner(uid) {
      return signedIn() && request.auth.uid == uid;
    }

    function validString(value, maxLength) {
      return value is string && value.size() <= maxLength;
    }

    function validUsername(value) {
      return value is string
        && value.size() >= 3
        && value.size() <= 30
        && !value.matches('.*\\s+.*');
    }

    function validUserCreate(uid) {
      let data = request.resource.data;
      return data.keys().hasAll([
          'uid', 'username', 'fullName', 'photoUrl', 'bio', 'city',
          'whatsapp', 'role', 'createdAt', 'updatedAt'
        ])
        && data.keys().hasOnly([
          'uid', 'username', 'fullName', 'photoUrl', 'bio', 'city',
          'whatsapp', 'role', 'createdAt', 'updatedAt'
        ])
        && data.uid == uid
        && validUsername(data.username)
        && validString(data.fullName, 80)
        && data.fullName.size() >= 1
        && validString(data.photoUrl, 2048)
        && validString(data.bio, 300)
        && validString(data.city, 80)
        && validString(data.whatsapp, 30)
        && data.role == 'user'
        && data.createdAt == request.time
        && data.updatedAt == request.time;
    }

    function validUserUpdate() {
      let data = request.resource.data;
      return data.keys().hasOnly([
          'uid', 'username', 'fullName', 'photoUrl', 'bio', 'city',
          'whatsapp', 'role', 'createdAt', 'updatedAt'
        ])
        && data.diff(resource.data).affectedKeys().hasOnly([
          'username', 'fullName', 'photoUrl', 'bio', 'city',
          'whatsapp', 'updatedAt'
        ])
        && validUsername(data.username)
        && validString(data.fullName, 80)
        && data.fullName.size() >= 1
        && validString(data.photoUrl, 2048)
        && validString(data.bio, 300)
        && validString(data.city, 80)
        && validString(data.whatsapp, 30)
        && data.updatedAt == request.time;
    }

    function validPostData(postId) {
      let data = request.resource.data;
      return data.keys().hasAll([
          'postId', 'userId', 'petName', 'petType', 'breed', 'age',
          'ageUnit', 'city', 'description', 'mediaUrl', 'mediaType',
          'isVaccinated', 'hasHealthPassport', 'adoptionFee', 'status',
          'likesCount', 'createdAt'
        ])
        && data.keys().hasOnly([
          'postId', 'userId', 'petName', 'petType', 'breed', 'age',
          'ageUnit', 'city', 'description', 'mediaUrl', 'mediaType',
          'isVaccinated', 'hasHealthPassport', 'adoptionFee', 'status',
          'likesCount', 'createdAt'
        ])
        && data.postId == postId
        && validString(data.petName, 80)
        && data.petName.size() >= 1
        && validString(data.petType, 40)
        && data.petType.size() >= 1
        && validString(data.breed, 80)
        && validString(data.city, 80)
        && validString(data.description, 2000)
        && validString(data.mediaUrl, 2048)
        && data.age is int
        && data.age >= 0
        && data.ageUnit in ['Months', 'Years']
        && data.mediaType in ['image', 'video']
        && data.isVaccinated is bool
        && data.hasHealthPassport is bool
        && data.adoptionFee is int
        && data.adoptionFee >= 0
        && data.status in ['available', 'pending', 'adopted']
        && data.likesCount is int
        && data.likesCount >= 0
        && data.createdAt is timestamp;
    }

    function validPostCreate(postId) {
      return validPostData(postId)
        && request.resource.data.userId == request.auth.uid
        && request.resource.data.status == 'available'
        && request.resource.data.likesCount == 0;
    }

    function validPostUpdate(postId) {
      return validPostData(postId)
        && request.resource.data.diff(resource.data).affectedKeys().hasOnly([
          'petName', 'petType', 'breed', 'age', 'ageUnit', 'city',
          'description', 'mediaUrl', 'mediaType', 'isVaccinated',
          'hasHealthPassport', 'adoptionFee', 'status'
        ]);
    }

    match /users/{uid} {
      allow read: if signedIn();
      allow create: if isOwner(uid) && validUserCreate(uid);
      allow update: if isOwner(uid) && validUserUpdate();
      allow delete: if false;
    }

    match /posts/{postId} {
      allow read: if signedIn();
      allow create: if signedIn() && validPostCreate(postId);
      allow update: if signedIn()
        && resource.data.userId == request.auth.uid
        && validPostUpdate(postId);
      allow delete: if signedIn() && resource.data.userId == request.auth.uid;
    }

    match /{document=**} {
      allow read, write: if false;
    }
  }
}
```

- [ ] **Step 4: Point tests to final rules**

Change the rules path in test setup:

```javascript
path.resolve(__dirname, "../../firestore.rules")
```

- [ ] **Step 5: Run the complete rules test suite**

```powershell
Set-Location firebase
npm run test:rules
```

Expected: all allow/deny tests PASS.

- [ ] **Step 6: Commit strict rules**

```powershell
git add firestore.rules firebase/tests/firestore.rules.test.js
git commit -m "security: enforce firestore ownership and schema rules"
```

---

### Task 9: Deploy Strict Rules and Verify Production

**Files:**
- No code changes before deployment.

- [ ] **Step 1: Confirm final deployment gates**

All must be true:

- Production backup exists.
- Compatible Android app is available.
- Production migration completed.
- `npm run verify:users` reports `invalidCount: 0`.
- Complete emulator rules suite passes.
- Android unit tests and compile pass.

- [ ] **Step 2: Deploy strict rules**

```powershell
Set-Location firebase
npx firebase-tools deploy --config ..\firebase.json --only firestore:rules --project adoptus-e66f1
```

Expected: deployment succeeds.

- [ ] **Step 3: Run post-deploy smoke tests**

Verify:

- Signed-out reads fail.
- Signed-in feed reads succeed.
- User can read another authenticated user's public profile.
- User can update only their own profile.
- User can create a post only with their own UID.
- Owner can update/delete their post.
- Non-owner update/delete fails.
- Unknown collections remain inaccessible.

- [ ] **Step 4: Monitor failures**

For the first release window, inspect:

- Android logs for `PERMISSION_DENIED`.
- Firestore usage and denied request patterns.
- Registration failures.
- Setting save failures.
- Add Post failures.

If a valid client operation is denied, fix the smallest rule or client-schema mismatch and add a regression test before redeploying.

---

### Task 10: Document the P0 Decision and Verification

**Files:**
- Create: `doc/adr/0006-firestore-security-user-schema.md`
- Modify: `README.md`
- Modify: `CHANGELOG.md`

- [ ] **Step 1: Write ADR 0006**

Document:

- Why open/time-limited rules were unsafe.
- Why authenticated profile reads are allowed.
- Why profile writes are owner-only.
- Canonical `users` fields.
- Why email is removed from Firestore.
- Transitional rules → compatible app → migration → strict rules rollout.
- Why username uniqueness, likes, adoption, and Storage are deferred.

- [ ] **Step 2: Update README**

Replace the mixed legacy/canonical `users` schema table with the canonical schema. Add commands:

```powershell
Set-Location firebase
npm run test:rules
npm run migrate:users:dry
npm run verify:users
```

State clearly that service-account credentials must not be committed.

- [ ] **Step 3: Update CHANGELOG**

Under `Unreleased`, add:

- Firestore ownership and schema rules.
- Firebase Emulator rules tests.
- Canonical user schema.
- Backward-compatible Android reads.
- Migration and verification scripts.
- Production migration/deployment date after execution.

- [ ] **Step 4: Run final repository verification**

```powershell
.\gradlew.bat :app:testDebugUnitTest :app:compileDebugKotlin --no-daemon
Set-Location firebase
npm run test:rules
npm run verify:users
```

Expected:

- Android tests PASS.
- Kotlin compile succeeds.
- Firestore rules tests PASS.
- Production user verification reports zero invalid documents.

- [ ] **Step 5: Confirm no credentials are staged**

```powershell
git status --short
git diff --cached --name-only
```

Expected: no service-account JSON, emulator export, debug log, or user-data file appears.

- [ ] **Step 6: Commit documentation**

```powershell
git add doc/adr/0006-firestore-security-user-schema.md README.md CHANGELOG.md
git commit -m "docs: record firestore security baseline"
```

---

## Definition of Done

P0 is complete only when all conditions below are met:

- [ ] Production Firestore no longer uses open or date-expiring access rules.
- [ ] Unauthenticated clients cannot read or write `users` or `posts`.
- [ ] A user cannot write another user's profile.
- [ ] A non-owner cannot update or delete a post.
- [ ] Invalid user/post shapes are rejected by final rules.
- [ ] All production `users` documents use only canonical fields.
- [ ] Production `users` documents contain no `email` or legacy snake_case fields.
- [ ] Registration creates canonical documents.
- [ ] Setting reads migrated and newly created profiles correctly.
- [ ] Rules tests pass in Firebase Emulator Suite.
- [ ] Android unit tests and Kotlin compilation pass.
- [ ] Production migration has a backup and a recorded verification result.
- [ ] README, CHANGELOG, and ADR reflect the final state.

## Rollback Plan

### If migration fails before strict rules deployment

1. Stop the migration.
2. Keep transitional ownership rules active.
3. Restore the Firestore export if documents were corrupted.
4. Fix the migration against emulator data.
5. Repeat dry run and verification before retrying.

### If strict rules deny valid app operations

1. Do not restore open rules.
2. Re-deploy the transitional ownership rules as the temporary fallback.
3. Capture the denied operation and required document shape.
4. Add a failing emulator test reproducing it.
5. Fix and redeploy strict rules.

### If the compatible app release is delayed

Keep transitional rules active. Do not migrate/delete legacy fields or deploy strict schema rules until the compatible build is available.

## Recommended Execution Order

```text
Firebase tooling
→ transitional ownership rules
→ Android legacy/canonical compatibility
→ canonical registration writes
→ migration scripts
→ production backup and migration
→ strict rules tests
→ strict rules deployment
→ documentation and monitoring
```
