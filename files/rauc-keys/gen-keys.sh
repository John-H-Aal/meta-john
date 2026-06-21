#!/bin/bash
# Generate a self-signed RAUC CA and development signing keypair using host OpenSSL.
# Equivalent to meta-rauc-community/create-example-keys.sh but without the
# oe-run-native wrapper (avoids needing a pre-built openssl-native).
#
# Run once from this directory:
#   cd meta-john/files/rauc-keys && bash gen-keys.sh

set -e
cd "$(dirname "$0")"

ORG="Test Org"
CA="rauc CA"

cat > openssl.cnf <<'EOF'
[ ca ]
default_ca = CA_default

[ CA_default ]
dir            = .
database       = $dir/index.txt
new_certs_dir  = $dir/certs
certificate    = $dir/ca.cert.pem
serial         = $dir/serial
private_key    = $dir/ca.key.pem
RANDFILE       = $dir/.rand
default_startdate = 19700101000000Z
default_enddate   = 99991231235959Z
default_crl_days  = 30
default_md     = sha256
policy         = policy_any
email_in_dn    = no
copy_extensions = none

[ policy_any ]
organizationName = match
commonName       = supplied

[ req ]
default_bits        = 4096
distinguished_name  = req_distinguished_name
x509_extensions     = v3_leaf
encrypt_key         = no
default_md          = sha256
prompt              = no

[ req_distinguished_name ]
commonName     = Common Name

[ v3_ca ]
subjectKeyIdentifier   = hash
authorityKeyIdentifier = keyid:always,issuer:always
basicConstraints       = CA:TRUE

[ v3_leaf ]
subjectKeyIdentifier   = hash
authorityKeyIdentifier = keyid:always,issuer:always
basicConstraints       = CA:FALSE
EOF

export OPENSSL_CONF=openssl.cnf
mkdir -p certs
touch index.txt
echo 01 > serial

echo "==> Generating CA key and self-signed certificate..."
openssl req -newkey rsa:4096 -keyout ca.key.pem -out ca.csr.pem \
    -subj "/O=$ORG/CN=$ORG $CA Development"
openssl ca -batch -selfsign -extensions v3_ca \
    -in ca.csr.pem -out ca.cert.pem -keyfile ca.key.pem

echo "==> Generating development signing key and certificate..."
openssl req -newkey rsa:4096 -keyout development-1.key.pem \
    -out development-1.csr.pem \
    -subj "/O=$ORG/CN=$ORG Development-1"
openssl ca -batch -extensions v3_leaf \
    -in development-1.csr.pem -out development-1.cert.pem

echo ""
echo "Done. Files generated:"
ls -1 *.pem
echo ""
echo "Add these to build-rpi5/conf/site.conf (or local.conf):"
KEYDIR="$(cd "$(dirname "$0")" && pwd)"
echo "  RAUC_KEYRING_FILE = \"${KEYDIR}/ca.cert.pem\""
echo "  RAUC_KEY_FILE     = \"${KEYDIR}/development-1.key.pem\""
echo "  RAUC_CERT_FILE    = \"${KEYDIR}/development-1.cert.pem\""
