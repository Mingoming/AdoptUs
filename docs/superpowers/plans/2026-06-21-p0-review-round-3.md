# P0 Review Round 3 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Close the remaining migration reporting, PII logging, privileged-role, profile recovery race, and username whitespace risks before PR #1 is merged.

**Architecture:** Migration planning remains read-only and validates the complete snapshot before writes. The writer returns structured per-document outcomes, dry-run diffs expose metadata only, privileged roles require an explicit UID allowlist, and Android profile recovery uses an atomic transaction that creates only when absent.

**Tech Stack:** Node.js test runner, Firebase Admin SDK, Firestore Emulator, Kotlin, Firebase Android SDK, JUnit 4.

---

### Task 1: Structured Migration Outcomes

**Files:**
- Modify: `firebase/scripts/migrate-users.js`
- Modify: `firebase/scripts/user-migration-runner.js`
- Test: `firebase/tests/user-migration-runner.test.js`
- Test: `firebase/tests/user-migration-emulator.test.js`

- [x] Add failing tests for success -> conflict -> success and unexpected failure after a successful write.
- [x] Make the writer return `written`, `conflicts`, and `failed` without losing completed outcomes.
- [x] Stop further writes after a non-precondition failure while retaining unattempted documents as planned.
- [x] Print `PLANNED`, `SKIPPED`, `WRITTEN`, `CONFLICT`, and `FAILED` only for documents in the corresponding result.
- [x] Return exit code 2 for conflicts and exit code 1 for failures.

### Task 2: Value-Free Migration Diff

**Files:**
- Modify: `firebase/scripts/user-migration-core.js`
- Test: `firebase/tests/user-migration-core.test.js`
- Test: `firebase/tests/user-migration-emulator.test.js`

- [x] Add failing tests containing name, URL, bio, city, email, WhatsApp, nested, and unexpected values.
- [x] Replace before/after values with field name, operation, and value type metadata.
- [x] Verify none of the seeded values appear in CLI dry-run output.

### Task 3: Privileged Role Allowlist

**Files:**
- Modify: `firebase/scripts/migrate-users.js`
- Modify: `firebase/scripts/user-migration-core.js`
- Modify: `firebase/scripts/user-migration-runner.js`
- Test: `firebase/tests/user-migration-core.test.js`
- Test: `firebase/tests/user-migration-runner.test.js`

- [x] Add failing tests showing `admin` and `moderator` enter `privilegedRoleReview`.
- [x] Accept repeated `--allow-privileged-uid=<uid>` arguments.
- [x] Allow dry-run to report review entries but prevent writes until every privileged UID is allowlisted.
- [x] Continue rejecting unknown roles.

### Task 4: Atomic Profile Recovery

**Files:**
- Create: `app/src/main/java/com/example/adoptus/data/repository/ProfileRecoveryCoordinator.kt`
- Modify: `app/src/main/java/com/example/adoptus/data/repository/AuthRepository.kt`
- Modify: `app/src/main/java/com/example/adoptus/fragment/SettingFragment.kt`
- Create: `app/src/test/java/com/example/adoptus/data/repository/ProfileRecoveryCoordinatorTest.kt`

- [x] Add failing tests for create-when-missing and preserve-when-existing.
- [x] Use one Firestore transaction for existence check and conditional create.
- [x] Reuse the helper from login, Google login, and Setting.

### Task 5: Original Username Validation

**Files:**
- Modify: `app/src/main/java/com/example/adoptus/ui/auth/RegisterActivity.kt`
- Modify: `app/src/main/java/com/example/adoptus/fragment/SettingFragment.kt`
- Test: `app/src/test/java/com/example/adoptus/data/model/UserTest.kt`

- [x] Add failing tests for leading, trailing, tab, newline, and internal whitespace.
- [x] Validate the original username input before any trim.
- [x] Pass the accepted original value unchanged because a valid username contains no whitespace.

### Task 6: Verification and Delivery

**Files:**
- Modify: `README.md`
- Modify: `CHANGELOG.md`
- Modify: `doc/adr/0006-firestore-security-user-schema.md`

- [x] Run Node migration unit tests.
- [x] Run Android unit tests and compilation.
- [x] Run transitional and strict rules emulator tests.
- [x] Run migration emulator tests.
- [x] Audit tracked files and credentials.
- [ ] Commit and push to `p0-firestore-security`.
- [ ] Re-review the new commit without merging or deploying.
