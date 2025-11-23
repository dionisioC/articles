#!/bin/bash
set -e

echo "--- Cleaning up old certificates ---"
rm -f ./*.crt ./*.key ./*.csr ./*.p12 ./*.srl

PASSWORD="changeit"

echo "--- 1. Generate CA (Root of Trust) ---"
openssl req -new -newkey rsa:2048 -days 365 -nodes -x509 -subj "/CN=KingdomRootCA" -keyout ca.key -out ca.crt

echo "--- 2. Generate Server Cert (localhost) ---"
openssl req -new -newkey rsa:2048 -days 365 -nodes -subj "/CN=localhost" -keyout server.key -out server.csr
openssl x509 -req -in server.csr -CA ca.crt -CAkey ca.key -CAcreateserial -out server.crt -sha256
openssl pkcs12 -export -in server.crt -inkey server.key -out server.p12 -name server -passout pass:$PASSWORD

echo "--- 3. Generate Client Cert (The Passport) ---"
openssl req -new -newkey rsa:2048 -days 365 -nodes -subj "/CN=LoyalSubject" -keyout client.key -out client.csr
openssl x509 -req -in client.csr -CA ca.crt -CAkey ca.key -CAcreateserial -out client.crt -sha256
openssl pkcs12 -export -in client.crt -inkey client.key -out client.p12 -name client -passout pass:$PASSWORD

echo "--- 4. Create Truststore ---"
keytool -import -file ca.crt -alias kingdomCa -keystore truststore.p12 -storepass $PASSWORD -noprompt

echo "--- Done! ---"
echo "Generated: server.p12, client.p12, truststore.p12"