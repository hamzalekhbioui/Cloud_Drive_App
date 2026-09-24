# Cloud Drive production-readiness audit

**Scope:** static inspection of the repository on 2026-09-24 plus the existing automated checks. This is an audit only; no application code or user-owned changes were modified. Statuses reflect the implementation, not labels or UI copy.

## A. Executive summary

This is a substantial portfolio project, not a basic CRUD prototype. It has a Spring Boot 4 / Java 17 REST API, React + TypeScript/Vite frontend, PostgreSQL migrations, Azure Blob direct upload design, BCrypt/JWT authentication, Google sign-in, Stripe webhooks and billing, per-plan quotas/rate limits, teams, public shares, an admin identity domain, structured logs, OpenAPI, and a useful set of service tests.

It is **not safe to deploy as a real file-sharing SaaS yet**. The immediate blockers are in sharing and data lifecycle:

1. A `VIEW` public share returns a direct Azure read SAS URL, bypassing its own download policy.
2. A recipient’s `shared-with-me` response exposes the public-link secret despite comments promising the opposite.
3. There is no automated trash expiry despite the frontend promising 30-day deletion, and abandoned pending uploads permanently reserve quota until reconciliation.
4. Team authorization makes every active member capable of destructive operations on a team file; the intended owner/admin/member permission model is not enforced for files.
5. Backend checks do not run reliably in the current toolchain, and no CI workflow exists to catch regressions.

The recommended path is focused: repair sharing and authorization first, make upload/trash cleanup explicit and tested, establish a PostgreSQL/Azure integration test lane and CI, then add only the core file-manager gaps (folders, server-side search/sort/page, rename/move).

### What is already strong

- JWT secrets are rejected when blank, weak, or known defaults; user status and token invalidation are checked in `JwtAuthFilter`.
- Passwords use BCrypt and login errors are normalized.
- Stripe webhook signatures and idempotency records are implemented; plan and quota records use row locks.
- Upload filenames and extensions are sanitized/allow-listed, legacy multipart detects MIME from bytes, and direct uploads use a short-lived write SAS followed by length verification.
- Public share revocation and fixed 24-hour expiry are implemented.
- The client has loading/empty/error states across main file views and a responsive grid/list UI.
- Flyway migrations, health/Actuator, JSON logs, Docker, OpenAPI, admin audit records, and a frontend test/build setup exist.

### Architecture observed

| Area | Implementation |
| --- | --- |
| Frontend | React 19, TypeScript, Vite, Axios, React Router, Vitest; client bundle in `frontend/` |
| Backend | Spring Boot 4.0.6, Java 17 target, Spring MVC/Security/JPA/Validation/Actuator |
| Persistence | PostgreSQL in production configuration, H2 for tests, Flyway V1–V15 |
| Storage | Azure Blob Storage; preferred two-phase direct PUT with 10-minute write SAS; legacy backend multipart path remains |
| Identity | Local BCrypt accounts + Google access-token userinfo lookup; bearer JWT stored in browser `localStorage` |
| Billing | Stripe Checkout/Portal/webhooks, plans/subscriptions/payments/usage |
| Operations | Dockerfile, Compose development database, JSON Logback, health and OpenAPI; no checked-in CI workflow or production deployment manifest |

## Feature inventory

| Feature | Status | Evidence | Quality | Missing pieces |
| --- | --- | --- | --- | --- |
| Registration/login | ✅ | `AuthController`, `AuthService`, `JwtUtil` | Medium | No verification/reset/session list; browser stores bearer token in `localStorage` |
| Google sign-in | 🟡 | `AuthService.googleAuth`, login/register pages | Medium | Access token is accepted directly; no OIDC ID-token audience/issuer/nonce validation |
| Logout/session revocation | 🟡 | client-only logout; `tokensValidFrom` filter support | Medium | No logout endpoint, refresh tokens, session/device management, or user-facing global sign-out |
| File upload | 🟡 | `FileService.beginUpload/commitUpload`; legacy `/upload` | Medium | Direct commit trusts declared extension/MIME and length only; pending cleanup/AV scan absent |
| Download/stream/preview | 🟡 | range stream endpoint; `FilePreviewModal` | Medium | Owner/team access only; no shared authenticated stream; direct SAS leaks around share policy |
| File deletion/trash | 🟡 | soft delete/restore/permanent endpoints | Medium | 30-day deletion is UI text only; no scheduled purge or retention policy |
| File organization | ❌ | no folder entity or routes | N/A | No folders, nesting, breadcrumbs, rename, move, copy, server-side sort/filter/page |
| Search/filter | 🟡 | client filters currently loaded `/files/me` list | Low | No backend search/index/pagination; unsuitable as file count grows |
| Sharing/public links | ⚠️ | `ShareService`, `ShareController` | Low | View-only bypass, token leak, recipients can use public token rather than authenticated share path |
| Teams | 🟡 | `TeamService`, team UI | Medium | Files have no enforceable team ownership model; no email delivery, role policy for file mutations, transfer/closure handling |
| Quotas/subscriptions | 🟡 | `SubscriptionService`, `BillingService` | Medium | Pending reservation cleanup and proper active/trash accounting missing; recurring reconciliation scans all subscriptions |
| Billing | 🟡 | Stripe service/controller/webhooks | Medium | No live integration test, asynchronous reconciliation/alerting, or documented Stripe setup |
| Settings/account | 🟡 | `SettingsService`, Settings UI | Low | “Delete account” is nonfunctional UI; API token is generated/stored/returned but never authenticated by API |
| Analytics/AI chat | 🟡 | `AnalyticsService`, AI services/pages | Medium | Analytics count storage including trash inconsistently; AI lifecycle/retention and data-disclosure policy absent |
| Admin surface | 🟡 | isolated admin JWT chain/controllers | Medium | Pageable sorting is user-controlled without an allow-list; admin authorization test currently fails 401/403 expectation |
| Tests/CI/docs | ⚠️ | 73 backend, 13 frontend tests; two short READMEs | Low | Backend tests blocked by toolchain, no CI config, no architecture/setup/deployment/API docs |

## B. Critical issues

| Priority | Issue | Location | Why it matters | Recommended fix |
| --- | --- | --- | --- | --- |
| P0 | View-only public share can be downloaded | `ShareService.resolvePublicToken` creates a one-hour Azure read SAS; `ShareController` only enforces permission on `/public/{token}/stream` | A caller can retrieve `url` from the metadata endpoint and download directly, bypassing `VIEW` restrictions and revocation semantics for the SAS lifetime | Never return blob/SAS URLs from public metadata. Return only metadata and serve bytes through the permission-aware stream endpoint. Add regression tests for both `VIEW` and `DOWNLOAD`. |
| P0 | Recipient share response leaks public secret | `SharedFileResponse` contains `token`; `ShareService.toRecipientResponse` sets it despite its own comment | Any recipient can turn an intended account-bound share into an unauthenticated public link and redistribute it | Remove token from recipient DTO/JSON completely; create an authenticated recipient stream endpoint that checks `sharedWithEmail`, expiry, revoked state, and permission. |
| P1 | Team member can delete/restore/permanently delete/star another owner’s team file | `FileService.findOwned` treats any active team member as authorized and all mutation methods call it | An ordinary member can destroy data and release the wrong user’s quota (`releaseQuota(userId, ...)`) | Define an explicit action matrix. Typically owner/admin: delete/restore/purge; member: read/upload; file creator: own-file actions. Centralize checks and test every action/role combination. |
| P1 | Direct upload accepts arbitrary bytes for an allowed filename | `beginUpload` validates extension; `commitUpload` checks only length | A `.pdf` SAS target may contain arbitrary content and Azure headers are client-controlled; the legacy path’s byte sniffing is not equivalent | On commit, inspect blob bytes/server-side type and compare to expected MIME; set headers server-side; consider malware scanning/quarantine before `ACTIVE`. |
| P1 | Pending uploads leak quota and orphan blobs | `FileService.beginUpload` reserves quota before client PUT; no expiry worker; `SubscriptionService.reconcileUsedBytes` later counts all files including PENDING/trash | Client/network interruption makes usable quota disappear; blob/database state can drift | Store `uploadExpiresAt`, regularly delete stale PENDING rows/blobs and release their reservation. Reconcile only the defined billable states and alert on failures. |
| P1 | No automatic trash purge despite user promise | `TrashPage` says files are deleted after 30 days; backend has no scheduler/query for it | Retention statement is false, blobs and quota accumulate, and privacy/data-retention requirements are unmet | Implement and document a retention job. Decide whether trash consumes quota, then use that consistently. |
| P1 | Azure storage key is present in local frontend environment file | ignored `frontend/.env` contains a storage connection string (not tracked in current index) | The credential may already be in shell history/backups/shared folders; a frontend environment file is an unsafe place for a storage account key | Rotate the key now; keep secrets only in local ignored backend env/secret manager. Use scoped identity/SAS issuance in production. Do not commit it. |
| P1 | Backend test gate is broken | `./mvnw test`: 60 errors caused by Mockito inline mock maker on Java 25; one `AdminSecurityIsolationTest` failure expects 401 but receives 403 | Security and business regressions will not be reliably caught | Pin supported JDK (17 or 21), configure Mockito agent or use a non-inline mock maker as appropriate, repair expected unauthenticated semantics, and run in CI. |
| P2 | Rate limit can be bypassed/spoofed behind an untrusted proxy | `RateLimitFilter.clientIp` trusts `X-Forwarded-For` from any caller | Attackers can select a new bucket/IP; buckets are unbounded in-memory and reset on scale-out | Only trust forwarded headers from configured proxy infrastructure; bound/expire bucket cache; use a distributed gateway/Redis limiter for multi-instance production. |
| P2 | Public/share deleted state is not consistently checked | public share resolvers load file but do not reject `deletedAt`/non-`ACTIVE` | A trashed file can remain publicly accessible until later removal | Require `ACTIVE` and `deletedAt == null` at every content/metadata resolution; revoke shares on deletion or make policy explicit. |
| P2 | Team deletion leaves file records pointing at a non-existent team | `TeamService.deleteTeam` deletes members/team only; `files.team_id` has no FK | Stale team association and unclear post-delete access/data ownership | Decide: forbid deletion with files, transfer files to owner, or soft-delete team. Add FK plus migration and transactional cleanup. |
| P2 | API token feature is a misleading dead end | `SettingsService.regenerateApiToken` persists and returns a plaintext UUID; no authentication filter consumes it | Users are issued a long-lived secret that has no documented purpose and is unnecessarily returned in every settings response | Remove until supported, or store a hash and implement scoped, revocable API-key authentication and last-used metadata. |

## E. Security findings

- 🔴 **Public-link authorization bypass:** direct SAS in `resolvePublicToken` defeats `VIEW` permissions. This is exploitable without authentication by anyone with the share token.
- 🔴 **Share-token disclosure:** `SharedFileResponse` and `toRecipientResponse` expose the token contrary to the documented intent. Recipient-only sharing becomes public sharing.
- 🟠 **Team BOLA/destructive authorization gap:** a membership check grants all file mutations. It is not an IDOR for unrelated teams, but it is broken object-level authorization within each team and can cause data loss/quota corruption.
- 🟠 **Unverified direct-upload content:** extension/declared size is enforced, but direct upload bypasses the legacy magic-byte validation. Treat uploaded bytes as untrusted until inspected/scanned.
- 🟠 **Local cloud credential exposure:** a real-looking Azure account key exists in an ignored file. It is not currently tracked, but rotation is required because local exposure is still exposure.
- 🟡 **Bearer token XSS exposure:** tokens are intentionally stored in `localStorage` in `frontend/src/context/AuthContext.tsx`. CSP helps, but any successful same-origin XSS can exfiltrate them. Use short-lived access tokens with an HttpOnly/SameSite refresh cookie or document/accept the tradeoff for a portfolio demo.
- 🟡 **OAuth token validation is limited:** backend calls Google `userinfo` with a browser-provided access token and checks `email_verified`; it does not validate OIDC ID-token issuer/audience/nonce. Use Google ID tokens with server-side audience/issuer verification or an authorization-code flow.
- 🟡 **CORS/CSRF policy needs production ownership:** stateless bearer auth permits CSRF disablement, but `allowCredentials(true)` is unnecessary with authorization headers. Enforce exact HTTPS origins from deployment config; never use wildcard origins with credentials.
- 🟡 **No account recovery/verification/rate-limit specialization:** generic rate limiting exists, but no separate brute-force protections, verification, reset-token hashing/expiry, or alerting on auth abuse.
- 🔵 **Response/status inconsistency:** invalid/unrecognized bearer tokens end as Spring Security 403 for protected routes in current tests, while expected unauthenticated behavior is 401. Standardize `AuthenticationEntryPoint`/`AccessDeniedHandler` JSON responses.

### Authorization review

Direct user file IDs are checked in `findOwned`, which protects unrelated users. Owner-only share creation/list/revocation is also correctly checked. Team invitation acceptance compares invite email with the authenticated user. Admin routes are guarded by a separate filter chain and role.

The exceptions above matter: team membership over-grants mutations, recipient sharing reveals a public credential, and public metadata turns a policy-protected share into a raw storage URL. Security must be tested at each sensitive endpoint, not inferred from global authentication.

## C. Missing features

### Functional and frontend gaps

### Must have before real SaaS use

- Secure share recipient download/preview and repair public `VIEW` permission.
- Folder model or remove folder-oriented UI/settings claims. Add rename, move, and server-side list/search/sort/pagination before calling it a drive.
- Account deletion/export policy, password reset, email verification, and a documented session/token lifecycle.
- Upload and trash retention jobs with retry/observability and an explicit billing rule for trashed/pending content.
- Transactional/compensating handling for blob vs database failures: upload success + DB failure should queue blob cleanup; blob deletion failure should retain a retryable deletion record rather than silently leaving orphan storage.

### Should have

- Multiple-file drag/drop queue with per-file progress/cancel/retry. Current `FilesPage` takes only `files[0]`; dashboard has a separate drop path, so behavior is inconsistent.
- Server-side filename search, MIME/date/size filters, stable sorting, opaque/capped pagination, and indexing.
- Email delivery for team invitations, share notifications, and storage warnings (preferences currently only persist booleans).
- Versioning for documents and audit/activity events for user actions.
- Better team lifecycle: membership removal effect on files, ownership transfer, and team closure behavior.

### Nice to have

- Recent files, shared-by-me, thumbnail service, richer document previews, batch operations, webhooks/API keys once safely designed, client telemetry/error tracking.
- Malware scanning, content moderation/DLP, object lifecycle rules, resumable multipart/block upload for very large files.

### Not necessary yet

- Full collaborative editing, real-time presence, enterprise SSO/SCIM, legal hold/eDiscovery, regional replication, and an Elasticsearch cluster. They add operational cost without resolving current core correctness issues.

## D. Technical debt

### Database and backend architecture audit

### Good decisions

- Controllers mostly delegate to services; DTOs are used for API responses and Bean Validation is applied to many request objects.
- Flyway is the right migration tool and tables have useful core indexes (`files(user_id, deleted_at)`, share token, team member lookups, subscription plan, usage period).
- Pessimistic locks around quota/subscription mutations and webhook idempotency are appropriate for this product size.
- Separate admin users/JWT secret/filter chain is a sound isolation decision.
- Global exception handling avoids raw error messages to clients and logs unexpected exceptions.

### Technical debt and data-model risks

- `files.user_id`, `file_shares.owner_email/shared_with_email`, subscriptions, teams, and settings use email strings instead of `users.id` foreign keys. Email cannot be safely changed and referential integrity/cascade semantics are absent.
- `files.team_id` is an unconstrained scalar, not a relationship. It permits stale records after team deletion.
- Entity relationships are largely manually assembled rather than mapped. This avoids accidental eager loads but shifts consistency obligations into services.
- The stored `files.url` is a temporary SAS URL; persisting credentials/tokens is unnecessary and risks stale access URLs. Store immutable blob key only and mint URLs at response/stream time.
- `FileResponseDto` exposes `userId` for ordinary user responses, which is unnecessary personal information in a multi-user product.
- `ShareService.getFilesSharedWithMe` performs one file lookup per share (N+1). `AdminTeamController` does a member lookup per page row. Batch-fetch/join at scale.
- File list endpoints are unbounded `List`s and mint a SAS per entity. At 100,000 files this becomes a database/memory/network bottleneck.
- `SubscriptionService.reconcileUsedBytes` loads every subscription and runs a SUM per one every six hours. It is a recovery mechanism, not a scalable accounting architecture; limit it to flagged records or batch aggregate when growth warrants it.
- `open-in-view` is enabled by default (startup warning). Disable it after ensuring mappings happen in services/transactional reads.
- User-controlled `sort` names in admin `PageRequest` need an explicit allow-list; invalid or expensive property paths should be rejected and page size capped.
- The legacy multipart upload doubles code paths and is explicitly memory/bandwidth inefficient. Keep only a deliberate compatibility window, with a sunset date and tested behavior.

### Schema gaps / indexes

- Add FK/constraints for `files.user_id` and `files.team_id` after moving to IDs; add `ON DELETE` behavior that matches retention policy.
- Enforce allowed status/role/permission values with checks/enums at persistence layer (not only request DTO patterns).
- Add indexes for active team file listing (`team_id, status, deleted_at`), pending-upload cleanup (`status, created_at` or expiry), and the future paged user list sort (`user_id, deleted_at, created_at DESC, id DESC`).
- Add unique/conditional business rules for shares if multiple active recipient shares should be disallowed; currently duplicate recipient shares are possible.
- Do not renumber historical migrations. The worktree has a pre-existing deletion of an old `V11__admin_panel.sql`, while the active files include `V11__billing_schema.sql` and `V15__admin_panel.sql`; validate each deployed Flyway history before changing migrations.

## API inventory

Authentication below means the route is protected by the normal JWT chain, `Public` is intentionally unauthenticated, and `Admin` means the isolated admin JWT + `ROLE_ADMIN`. Validation refers to request binding only; it is not a substitute for service authorization.

| Method | Endpoint | Purpose | Authentication | Authorization | Validation | Status |
| --- | --- | --- | --- | --- | --- | --- |
| POST | `/api/auth/register` | Register | Public | n/a | `@Valid` | 🟡 |
| POST | `/api/auth/login` | Login | Public | n/a | `@Valid` | 🟡 |
| POST | `/api/auth/google` | Google sign-in | Public | Google token only | Map, no bean validation | 🟡 |
| GET | `/api/plans` | Active plans | Public | n/a | n/a | ✅ |
| POST | `/api/files/upload` | Legacy multipart upload | JWT | owner/team member | multipart; service policy | 🟡 legacy |
| POST | `/api/files/upload/begin` | Mint upload target | JWT | team membership if supplied | untyped `Map` | ⚠️ |
| POST | `/api/files/upload/{id}/commit` | Activate upload | JWT | owner or team admin for pending | path only | 🟡 |
| GET | `/api/files/me` | Own files | JWT | caller-scoped query | n/a | 🟡 unpaged |
| GET | `/api/files/starred` | Own starred | JWT | caller-scoped query | n/a | 🟡 unpaged |
| GET | `/api/files/trash` | Own trash | JWT | caller-scoped query | n/a | 🟡 unpaged |
| GET | `/api/files/team/{id}` | Team files | JWT | active member | path | 🟡 unpaged |
| GET | `/api/files/{id}/stream` | Owner/team stream | JWT | `findOwned` | range parser | 🟡 role gap |
| DELETE | `/api/files/{id}` | Trash | JWT | `findOwned` | path | ⚠️ team mutation |
| POST | `/api/files/{id}/restore` | Restore | JWT | `findOwned` | path | ⚠️ team mutation |
| DELETE | `/api/files/{id}/permanent` | Purge | JWT | `findOwned` | path | ⚠️ team mutation/quota |
| PATCH | `/api/files/{id}/star` | Toggle star | JWT | `findOwned` | path | ⚠️ team mutation |
| GET/POST | `/api/files/{id}/ai-status[ /retry]` | AI state/retry | JWT | `findOwned` | path | 🟡 |
| POST | `/api/files/{id}/chat` | Ask AI | JWT | `findOwned` | `@Valid` | 🟡 |
| POST/GET/DELETE | `/api/documents/{id}/shares` | Create/list/revoke share | JWT | owner | create `@Valid` | 🟡 revoke only first active share |
| GET | `/api/shares/shared-with-me` | Recipient shares | JWT | recipient email | n/a | 🔴 token leaks |
| GET | `/public/{token}` | Share metadata | Public | token/expiry/revocation | path | 🔴 SAS bypass |
| GET | `/public/{token}/stream` | Share content | Public | token + permission | query | 🟡 unsafe around metadata endpoint |
| POST/GET | `/api/teams` | Create/list teams | JWT | caller-scoped | create `@Valid` | ✅/🟡 |
| GET/POST | `/api/teams/{id}/members` | Members/invite | JWT | member/admin | invite `@Valid` | 🟡 no delivery |
| POST | `/api/teams/invites/{token}/accept|decline` | Respond invitation | JWT | invited email | path | ✅ |
| DELETE | `/api/teams/{id}/members/{memberId}` | Remove member | JWT | self or admin | path | 🟡 |
| DELETE | `/api/teams/{id}` | Delete team | JWT | team owner | path | ⚠️ orphan association |
| GET | `/api/teams/pending-invites` | Pending invites | JWT | caller-scoped | n/a | ✅ |
| GET/PUT/PUT | `/api/settings`, `/preferences`, `/profile` | Settings/profile | JWT | caller-scoped | profile only `@Valid` | 🟡 preferences unrestricted strings |
| PUT | `/api/settings/password` | Change password | JWT | caller/current password | `@Valid` | ✅/🟡 |
| POST | `/api/settings/api-token` | Regenerate token | JWT | caller | n/a | ⚠️ unused secret |
| GET x5 | `/api/analytics/*` | Personal analytics | JWT | caller-scoped | n/a | 🟡 |
| GET | `/api/subscriptions` | Subscription | JWT | caller-scoped | n/a | ✅ |
| POST x4 / GET x2 | `/api/billing/*` | Checkout/cancel/reactivate/portal/usage/subscription | JWT | caller-scoped | checkout `@Valid` | 🟡 |
| POST | `/api/webhooks/stripe` | Stripe webhook | Public | Stripe signature | raw body | ✅/🟡 |
| GET | `/health`, `/actuator/health`, `/actuator/info` | Health | Public | n/a | n/a | ✅ |
| POST/GET | `/api/admin/auth/login`, `/me` | Admin auth | Public/Admin | isolated admin chain | login `@Valid` | 🟡 |
| GET | `/api/admin/overview[/*]`, `/audit`, `/teams`, `/files`, `/shares`, `/billing/*`, `/webhooks` | Admin reads | Admin | role only | pageable/query binding | 🟡 page/sort caps |
| POST/DELETE | `/api/admin/files/*`, `/shares/{id}/revoke`, `/billing/*`, `/webhooks/{id}/replay` | Admin mutations | Admin | role + audit service | some `@Valid` | 🟡 |

Note: the frontend’s `resolvePublicLink` helper calls `/api/shares/public/{token}`, while the real metadata route is `/public/{token}`. The active public page uses the correct `/api` Axios base plus `/public/{token}`. Remove the stale helper or align the route to avoid future regressions.

## Error handling and reliability

| Scenario | Current result | Gap / required outcome |
| --- | --- | --- |
| Invalid filename/type via legacy upload | Sanitized and magic-byte checked; controlled 4xx | Good baseline; direct path needs equivalent content inspection |
| Direct upload size mismatch | Blob is deleted and 400 returned | Good, but failed deletion needs logging/retry; no content check |
| Azure unavailable | service returns 503 for configured paths | Good client message; health readiness does not verify storage availability |
| DB save after blob upload fails | Transaction rolls back DB but blob may survive | Implement outbox/cleanup job and alert orphan candidate |
| Blob purge fails | exception prevents DB delete for non-404 | Avoids silent orphan but user gets failure; retryable deletion state and operations alert needed |
| Public link revoked/expired | 410 from resolvers | Correct, except already issued SAS remains valid until expiry |
| Invalid request map/direct upload fields | casts in controller can cause generic 500 | Replace `Map<String,Object>` with a validated DTO and error mapping |
| Unauthenticated/invalid JWT | protected route behavior is inconsistent (test sees 403) | Standardize 401 vs 403 JSON response and test it |
| User deletes file | soft deletion succeeds | Shares/AI/URLs not revoked; trash retention job missing |
| Network interruption during direct upload | PENDING row and reserved quota remain | expiry/retry/cancel endpoint and cleanup worker needed |

## F. Missing tests

### Testing audit and prioritized plan

### Evidence from this audit

- `npm test -- --run` passed: **4 files / 13 tests**.
- `npm run build` passed.
- `./mvnw test` did **not** pass: 73 tests discovered; 60 error before test execution because Mockito’s inline Byte Buddy mock maker cannot self-attach under installed Java 25. One real failure in `AdminSecurityIsolationTest` expects 401 but gets 403. This makes backend coverage untrustworthy until toolchain is fixed.
- Existing backend tests are mostly unit/service tests with mocks, plus a few Spring/H2 mappings/security checks. They do not exercise PostgreSQL Flyway, Azure Blob, live Stripe contract behavior, browser-to-Azure CORS, or end-to-end authorization flows.

### Missing tests, in priority order

1. **P0 share tests:** a `VIEW` metadata response contains no SAS URL; `VIEW` cannot download; `DOWNLOAD` can; recipient JSON has no token; recipient access stops immediately when revoked/expired/deleted.
2. **P0 BOLA matrix:** two users + owner/admin/member/non-member for every file stream/delete/restore/purge/star/AI endpoint. Test cross-team IDs and team deletion behavior.
3. **P1 upload lifecycle:** malformed JSON 400, negative/zero/oversize values, direct blob MIME mismatch, stale PENDING cleanup, blob write/DB failure compensation, quota release exactly once, concurrent reservations.
4. **P1 retention/consistency:** 30-day trash job, share revocation on delete policy, storage deletion retry, subscription accounting for active/trash/pending states.
5. **P1 security edge tests:** JWT expired/malformed/wrong-admin tokens, password change invalidates existing tokens if that is policy, OAuth audience/issuer checks, CORS allowed and denied origins, rate-limit forwarded-header trust.
6. **P2 integration tests:** Testcontainers PostgreSQL + Azurite, real Flyway validation, range streaming, storage headers, Stripe webhook fixture replay/idempotency.
7. **Frontend tests:** Files multi-upload/progress/error/retry; public view/download permission states; share revocation; settings delete-account unavailable state; keyboard/accessibility focus/modal interactions.

## G. Production readiness checklist

### Required before production

- [ ] Rotate the local Azure key and ensure `.env`, `application.properties`, Compose overrides, build logs, artifacts, and Git history contain no real credentials. Never commit Azure connection strings, JWT/admin JWT secrets, Stripe secret/webhook keys, database passwords, or Google OAuth client secrets.
- [ ] Repair both P0 share flaws and add tests that prove `VIEW` cannot yield downloadable bytes or a storage URL.
- [ ] Define and enforce file/team role policy for every action.
- [ ] Add validated DTO for upload start; server-side content verification/quarantine; pending upload expiry/cancel/cleanup.
- [ ] Implement trash retention and DB/blob retry/compensation workflows; document storage/quota semantics.
- [ ] Pin JDK 17/21 and make backend tests green; create CI that runs backend tests, frontend test/build, dependency scan, and secret scan on each PR.
- [ ] Test Flyway against PostgreSQL, not only H2; perform backup/restore and migration rollback rehearsal (forward-only repair migration, not destructive reset).
- [ ] Configure exact production HTTPS CORS origins, proxy trust, TLS, security headers, request-size/time limits, and production-safe Actuator exposure.
- [ ] Deploy secrets through a secret manager/workload identity; avoid account keys when managed identity can be used.
- [ ] Add readiness checks for database and Azure storage, logs/metrics/alerts for auth failures, storage cleanup failures, webhook failures, quota anomalies, and 5xx rate.
- [ ] Document incident actions: rotate secret, revoke shares, restore backup, replay webhook, purge a blob, and handle a failed migration.

### Useful later

- [ ] Error tracking/tracing with correlation IDs, distributed rate limiting, durable job queue/outbox, object lifecycle rules, CDN/thumbnailling, and performance load tests.

## H. Portfolio readiness checklist

### Documentation and portfolio readiness

The root `README.md` is currently only a title, badge, and short description. `frontend/README.md` is the Vite starter. There is no architecture diagram, supported local setup, safe environment-variable table, API/auth guide, Azure container/CORS setup, Stripe setup, migration guide, deployment runbook, screenshots/demo link, test guide, or known-limitations section. The README badge points to a GitHub Actions workflow that is not present in this checkout.

### Portfolio checklist

- [ ] Publish a 1–2 page README that explains the system boundary and trust model, with a diagram showing browser → API → PostgreSQL/Azure/Stripe/Google.
- [ ] Add scrubbed screenshots or a short demo, plus seeded demo credentials/data that contain no cloud credentials.
- [ ] Explain the direct-upload protocol, SAS TTL and authorization decisions, quota locking, webhook idempotency, and what is intentionally not implemented.
- [ ] Show an actual green CI workflow and a reproducible `docker compose` development path.
- [ ] Include security regression tests for the sharing/authorization matrix; this is more convincing than adding another dashboard.
- [ ] Add a short ADR/engineering notes page: why direct Azure upload, why Pessimistic locking, and how blob/database eventual consistency is handled.
- [ ] Keep the scope credible: do not advertise folders, auto-organization, 30-day trash deletion, or API tokens until their backend behavior exists.

## Production decision

**Current decision: not production-ready.** The application is a strong foundation and a promising portfolio piece, but P0/P1 fixes above are required before public users can store or share real files. The highest-value next implementation slice is narrowly scoped: secure sharing, team file authorization, and reliable file lifecycle cleanup—backed by a green, repeatable test gate.
