# Account lifecycle and token strategy

## Current behavior

- Passwords are stored only as BCrypt hashes.
- Tenant JWTs are stateless, short-lived, and contain the user email as their subject.
- `User.tokensValidFrom` invalidates all tokens issued before the recorded instant.
- Disabled and deleted users are rejected by `JwtAuthFilter`.
- `DELETE /api/settings/account` requires explicit confirmation. Password-based accounts must also provide the current password. OAuth-only accounts may delete without a password because they do not have one stored locally.
- Account deletion is currently a soft deletion (`DELETED`) so related billing, audit, and retention records remain referentially available for operational handling.

## Password reset and email verification

These flows are intentionally not enabled yet. The application has no configured transactional email provider, delivery queue, or bounce/verification handling. Do not expose reset or verification tokens in API responses, logs, or development error messages.

When an email provider is selected, implement both flows with:

1. Cryptographically random, single-use, hashed tokens stored in dedicated tables.
2. A short expiry (for example, 30 minutes for password reset and 24 hours for email verification).
3. Generic responses for unknown email addresses to prevent account enumeration.
4. Token consumption in one transaction, with password changes updating `tokensValidFrom`.
5. Provider delivery through an outbox or durable queue; never send mail from a database transaction.
