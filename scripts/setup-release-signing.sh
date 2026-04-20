#!/usr/bin/env bash
# One-time setup for release signing.
#
# Generates a PKCS12 keystore using OpenSSL (no JDK/keytool required) and
# stores the resulting secrets in the Arcusfoundry/pixel-pilot GitHub repo.
# The release workflow decodes the keystore and signs APKs with it.
#
# Requirements: openssl, gh (authenticated with repo admin access).
#
# CRITICAL: after this script runs, back up the generated .p12 file AND
# the password to offline storage (password manager, encrypted drive).
# Losing them means you cannot ship any more updates to this app. Android
# requires signature continuity between versions of the same package.

set -euo pipefail

REPO="${REPO:-Arcusfoundry/pixel-pilot}"
P12_PATH="pixelpilot-release.p12"
KEY_ALIAS="pixel-pilot"
SUBJECT="/CN=Pixel Pilot/O=Arcus Foundry Labs/C=US"
# 30-year validity (10950 days). Play Store requires keys valid until 2033+.
VALIDITY_DAYS=10950

if ! command -v openssl >/dev/null 2>&1; then
    echo "ERROR: openssl is not on PATH." >&2
    exit 1
fi
if ! command -v gh >/dev/null 2>&1; then
    echo "ERROR: gh (GitHub CLI) is not on PATH." >&2
    exit 1
fi

# Prefer base64 -w0 (Linux/Git Bash); fall back to plain base64 and strip newlines.
b64_encode() {
    if base64 -w0 "$1" 2>/dev/null; then return; fi
    base64 "$1" | tr -d '\r\n'
}

if [[ -f "$P12_PATH" ]]; then
    echo "ERROR: $P12_PATH already exists. Back it up and delete before re-running." >&2
    exit 1
fi

echo "Generating 32-char random password..."
STORE_PW=$(openssl rand -base64 48 | tr -d '+/=\r\n' | cut -c1-32)

echo "Generating RSA-2048 key and self-signed certificate..."
# MSYS_NO_PATHCONV prevents Git Bash from mangling the -subj slash-separated DN
# into a Windows path. No-op on native Linux/macOS shells.
MSYS_NO_PATHCONV=1 openssl req -x509 -newkey rsa:2048 -days "$VALIDITY_DAYS" -nodes \
    -keyout signing-key.pem -out signing-cert.pem \
    -subj "$SUBJECT" 2>/dev/null

echo "Bundling into PKCS12 keystore ($P12_PATH)..."
MSYS_NO_PATHCONV=1 openssl pkcs12 -export \
    -in signing-cert.pem -inkey signing-key.pem \
    -out "$P12_PATH" -name "$KEY_ALIAS" \
    -password "pass:$STORE_PW" 2>/dev/null

rm -f signing-key.pem signing-cert.pem

echo "Base64-encoding keystore for GitHub secret..."
B64=$(b64_encode "$P12_PATH")

echo "Setting GitHub secrets on $REPO..."
printf '%s' "$B64" | gh secret set SIGNING_KEYSTORE_B64 --repo "$REPO"
printf '%s' "$STORE_PW" | gh secret set SIGNING_KEYSTORE_PASSWORD --repo "$REPO"
printf '%s' "$STORE_PW" | gh secret set SIGNING_KEY_PASSWORD --repo "$REPO"
printf '%s' "$KEY_ALIAS" | gh secret set SIGNING_KEY_ALIAS --repo "$REPO"

cat <<EOF

================================================================================
Release signing is set up.

Keystore file:      $P12_PATH
Store password:    $STORE_PW
Key alias:         $KEY_ALIAS

CRITICAL — back up the .p12 file AND the password to offline storage NOW.
Suggested: password manager for the password, encrypted external drive or
two separate backups for the .p12. If you lose both, you can never ship
another update of this app under this package name.

The .p12 file is gitignored and will NOT be committed. Move it out of the
repo directory after you back it up.

Next step: tag and push v0.1.0 to fire the release workflow.
    git tag -a v0.1.0 -m "Pixel Pilot 0.1.0"
    git push origin v0.1.0
================================================================================
EOF
