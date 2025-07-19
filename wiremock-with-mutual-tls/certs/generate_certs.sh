#!/bin/bash

set -e

# Configuration
CA_PASSWORD=$(openssl rand -base64 32)
KEYSTORE_PASSWORD=$(openssl rand -base64 32)
TRUSTSTORE_PASSWORD=$(openssl rand -base64 32)
CERT_DIR="certs"

# Clean up previous artifacts
rm -rf "$CERT_DIR"
mkdir -p "$CERT_DIR"
cd "$CERT_DIR"

echo "--- 1. Generating Certificate Authority (CA) ---"
# Generate CA private key, protected by a password
openssl genrsa -aes256 -passout pass:"${CA_PASSWORD}" -out ca.key 4096

# Create self-signed root CA certificate
openssl req -x509 -new -nodes -key ca.key -passin pass:"${CA_PASSWORD}" -sha256 -days 3650 -out ca.crt -subj "/C=ES/ST=Madrid/L=Madrid/O=Dio Corporation/OU=IT/CN=Dio Corporation Root CA"

echo "--- 2. Generating WireMock Server Certificate ---"
# Create server private key (not password protected for automated startup)
openssl genrsa -out server.key 2048
# Create OpenSSL config for server with SANs
cat > server.conf <<EOF
[req]
distinguished_name = req_distinguished_name
req_extensions = v3_req
prompt = no

[req_distinguished_name]
C = ES
ST = Madrid
L = Madrid
O = Dio Corporation
OU = IT
CN = Server

[v3_req]
keyUsage = keyEncipherment, dataEncipherment
extendedKeyUsage = serverAuth
subjectAltName = @alt_names

[alt_names]
DNS.1 = wiremock
DNS.2 = localhost
IP.1 = 127.0.0.1
EOF

# Create server Certificate Signing Request (CSR)
openssl req -new -key server.key -out server.csr -config server.conf

# Sign the server CSR with our CA
openssl x509 -req -in server.csr -CA ca.crt -CAkey ca.key -passin pass:"${CA_PASSWORD}" -CAcreateserial -out server.crt -days 3650 -sha256 -extfile server.conf -extensions v3_req

echo "--- 3. Generating Client Certificate ---"
# Create client private key
openssl genrsa -out client.key 2048

# Create client CSR
openssl req -new -key client.key -out client.csr -subj "/C=ES/ST=Madrid/L=Madrid/O=Dio Corporation/OU=IT/CN=Test Client"

# Sign the client CSR with our CA
openssl x509 -req -in client.csr -CA ca.crt -CAkey ca.key -passin pass:"${CA_PASSWORD}" -out client.crt -days 3650 -sha256

echo "--- 4. Packaging Keystores for WireMock ---"
# Create a PKCS12 bundle for the server
openssl pkcs12 -export -in server.crt -inkey server.key -out server.p12 -name wiremock-server -passout "pass:${KEYSTORE_PASSWORD}"

# Create a PKCS12 truststore and import the CA certificate
keytool -importcert -alias ca -file ca.crt -deststoretype pkcs12 -keystore wiremock.truststore.p12 -storepass "${TRUSTSTORE_PASSWORD}" -noprompt

# Clean up intermediate files
rm server.csr client.csr server.conf ca.srl

cd ..
echo "--- Certificate generation complete. Artifacts are in the '$CERT_DIR' directory. ---"
echo "---> CA password: '${CA_PASSWORD}'" | cat -v
echo "---> Keystore password: '${KEYSTORE_PASSWORD}'" | cat -v
echo "---> Truststore password: '${TRUSTSTORE_PASSWORD}'" | cat -v
