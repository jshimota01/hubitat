# Changelog

## 0.2.1 - 2026-11-04
- Bound access and refresh tokens to the OAuth client ID that obtained them.
- Require the same client ID during refresh-token rotation and access-token validation.
- Reject refresh attempts that omit or mismatch the client ID.
- Expanded the source syntax check to cover all application JavaScript files.


## 0.2.0 - 2026-11-04
- Added a NAS-hosted OAuth authorization-code foundation with PKCE S256.
- Added single-use, five-minute authorization codes and rotating refresh tokens.
- Added hashed token storage in the persistent data volume.
- Added a mobile-friendly sign-in page and configurable login credentials.
- Added fail-closed redirect-host validation; no Amazon redirect hosts are enabled by default.
- Kept the Alexa directive endpoint unimplemented and the service LAN-bound.

## 0.1.0
- Initial isolated NAS service foundation, health endpoint, and API-key protected status endpoint.
