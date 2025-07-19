# WireMock with mutual TLS

In modern microservices architectures, securing inter-service communication is important. Mutual TLS (mTLS) has emerged
as a standard for establishing strong, two-way authenticated communication channels, ensuring that both the client and
the server verify each other's identity before any application data is exchanged. While mTLS significantly enhances
security by moving beyond simple server-side authentication, it introduces considerable complexity into the development
and testing lifecycle. Teams often struggle to create testing environments that can correctly simulate mTLS clients,
manage the required cryptographic artifacts, and validate the performance of these secure endpoints under realistic load
conditions. This can lead to inadequate testing, performance bottlenecks discovered late in the cycle, or security
misconfigurations that ruin the protection mTLS is meant to provide.

We are going to explore a solution to have a mTLS environment in a fast an easy way. We are going to use:

- `OpenSSL`: Utilized to build a private Public Key Infrastructure (PKI), including a custom Certificate Authority (CA)
  for issuing all necessary server and client certificates. This provides a solid cryptographic foundation for the
  entire mTLS setup.

- `WireMock`: Deployed as a highly configurable mock server that not only simulates the target service's API but also
  acts as a strict mTLS server, enforcing client certificate authentication.

- `Docker Compose`: Used to orchestrate the testing environment, defining and running the WireMock service in a
  containerized, isolated, and reproducible manner.

## The Cryptographic Foundation: A Private PKI for Mutual TLS

### The Certificate Chain of Trust: CA, Server, and Client Roles

The entire security model of TLS and mTLS is built upon a Public Key Infrastructure (PKI), which establishes a chain of
trust through digital certificates. Understanding the distinct roles within this chain is fundamental to a successful
implementation.

- `Certificate Authority (CA)`: The CA is the ultimate root of trust for a given environment. Its primary function is to
  digitally sign, and therefore verify the identity of other entities (servers and clients) by issuing them
  certificates. For internal systems, creating a private CA is a standard and highly recommended practice. It provides
  complete control over the trust domain without the cost and overhead of a public CA, which is often unnecessary and
  inappropriate for non-public-facing services.

- `Server Certificate`: This certificate is presented by the server (WireMock in our case) to any connecting client. It
  proves the server's identity (e.g., that it is genuinely wiremock.local). For the client to trust this certificate, it
  must have been signed by a CA that the client also trusts.

- `Client Certificate`: This is the essential part of mutual authentication. The client presents this certificate to
  the server to prove its own identity. The server, in turn, will only accept the connection if the client's certificate
  was signed by a CA that the server has been configured to trust.

## Creating a Self-Signed Certificate Authority (CA) with OpenSSL

The following steps detail the creation of private Certificate Authority using OpenSSL commands.

### Generate the CA Private Key

The CA's private key is the most sensitive asset in the PKI. Its compromise would allow an attacker to issue fraudulent
certificates and impersonate any entity within the trust domain. Therefore, it must be generated with a strong algorithm
and protected with a passphrase.

The command below generates a 4096-bit RSA private key and encrypts it using AES-256.

```shell
openssl genrsa -aes256 -passout pass:password -out ca.key 4096
```

- `genrsa`: The OpenSSL command for generating RSA private keys.
- `-aes256`: This flag instructs OpenSSL to encrypt the output key file with the AES-256 cipher.
- `-passout pass:password`: This flag puts the password `password`. This is an insecure password, it is advised to use a
  env variable with a strong password or not use this flag and fill the password when prompted.
- `-out ca.key`: Specifies the output file for the private key.
- `4096`: Defines the key length in bits, providing a high level of cryptographic strength.

### Generate the Root CA Certificate

Using the newly created private key, we now generate the public root certificate. This certificate is self-signed,
meaning it is signed by its own private key, establishing it as the root of our trust chain.

```shell
openssl req -x509 -new -nodes -key ca.key -passin pass:password -sha256 -days 3650 -out ca.crt -subj "/C=ES/ST=Madrid/L=Madrid/O=Dio Corporation/OU=IT/CN=Dio Corporation Root CA"
```

- `req`: The OpenSSL command for creating and processing certificate requests.
- `-x509`: This option specifies that we want to output a self-signed certificate instead of a certificate signing
  request (CSR).
- `-new`: Creates a new certificate request (though in this case, it's being self-signed immediately).
- `-nodes`: This flag stands for "no DES" and ensures the private key is not encrypted within the context of this
  command. When using this flag, the private key will be stored unencrypted in PEM format.
  This is safe because we are only reading the already-encrypted ca.key file and not outputting a new key.
- `-key ca.key`: Specifies the private key to use for signing.
- `-passin pass:password`: The `ca.key` password.
- `-sha256`: Uses the SHA-256 hashing algorithm for the signature.
- `-days 3650`: Sets the certificate's validity period to 10 years, a common practice for long-lived root CAs.
- `-out ca.crt`: Specifies the output file for the CA certificate.
- `-subj "/C=ES/ST=Madrid/L=Madrid/O=Dio Corporation/OU=IT/CN=Dio Corporation Root CA"`: a collection of attributes that
  identify an entity (person, organization, or system) in a certificate.

## Issuing the WireMock Server Certificate

With the CA established, we can now issue a certificate for our WireMock server.

## Generate the Server's Private Key

This key will be used by the WireMock server.

```shell
openssl genrsa -out server.key 2048
```

## Create a Certificate Signing Request (CSR) with Subject Alternative Names (SANs)

This is a critical step for modern TLS compliance. According to RFC 2818, clients should validate the server's identity
against the Subject Alternative Name (SAN) extension, not the legacy Common Name (CN) field. Failure to include correct
SANs is a common cause of TLS handshake failures.

We create an OpenSSL configuration file named `server.conf`.

```text
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
```

This configuration defines the server's identity and, crucially, specifies that the certificate is valid for the
hostnames wiremock (the service name within Docker Compose) and localhost, as well as the IP address 127.0.0.1.

Now, we are going to generate the CSR using this configuration file:

```shell
openssl req -new -key server.key -out server.csr -config server.conf
```

## Sign the Server CSR with the CA

The final step is to use our CA to sign the server's CSR, which officially issues the certificate.

```shell
openssl x509 -req -in server.csr -CA ca.crt -CAkey ca.key -passin pass:password -CAcreateserial -out server.crt -days 3650 -sha256 -extfile server.conf -extensions v3_req
```

- `-in server.csr`: The input CSR file.
- `-CA ca.crt -CAkey ca.key`: The CA certificate and private key used for signing.
- `-passin pass:password`: The password for CA key's.
- `- CAcreateserial`: Creates a serial number file (ca.srl) to track issued certificates, preventing serial number
  reuse, which is a PKI best practice.
- `-out server.crt`: The final, signed server certificate.
- `-extfile server.conf -extensions v3_req`: This part tells OpenSSL to copy the extensions (including SANs) from the
  configuration file into the final certificate.

Note here we are using 10 years as well, but it should be a shorter period of time.

## Issuing the Client Certificate

The process for the client certificate is similar but simpler, as SANs are generally not required for client
authentication.

### Generate the Client's Private Key

```shell
openssl genrsa -out client.key 2048
```

### Generate the Client CSR

We will provide the information for the client. The Common Name should identify the client, for example, "Test Client".

```shell
openssl req -new -key client.key -out client.csr -subj "/C=ES/ST=Madrid/L=Madrid/O=Dio Corporation/OU=IT/CN=Test Client"
```

### Sign the Client CSR with the CA

Use the CA to sign the client's request, issuing its certificate.

```shell
openssl x509 -req -in client.csr -CA ca.crt -CAkey ca.key -passin pass:password -out client.crt -days 3650 -sha256
```

This command uses the CA to validate and sign the `client.csr`, producing the final `client.crt` file, which will be
used by
the Client to authenticate itself to WireMock.

### File summary

The process of generating certificates creates numerous files. The following table tries to summarise all the files as
it is crucial in order to avoid confusions and be sure we are using the correct file in the correct place.

| File Name               | Description                                                                | Used By                       | Format |
|-------------------------|----------------------------------------------------------------------------|-------------------------------|--------|
| ca.key                  | The private key of the Certificate Authority.                              | OpenSSL (for signing)         | PEM    |
| ca.crt                  | The public certificate of the Certificate Authority.                       | WireMock (Truststore), Client | PEM    |
| server.key              | The private key for the WireMock server.                                   | WireMock (Keystore)           | PEM    |
| server.crt              | The public certificate for the WireMock server, signed by the CA.          | WireMock (Keystore)           | PEM    |
| client.key              | The private key for the Client.                                            | Client                        | PEM    |
| client.crt              | The public certificate for the Client, signed by the CA.                   | Client                        | PEM    |
| server.p12              | A portable bundle containing the server key and certificate.               | WireMock Server               | PKCS12 |
| wiremock.truststore.p12 | The PKCS12 Truststore telling WireMock which CAs to trust for client auth. | WireMock Server               | PKCS12 |

## Preparing Keystores for the JVM

A common point of failure when integrating non-Java tools like OpenSSL with Java-based applications like WireMock is the
mismatch in expected file formats. OpenSSL produces standard PEM-encoded files, but the Java Virtual Machine (JVM)
ecosystem typically uses binary keystore formats like JKS or PKCS12.

### JKS vs. PKCS12

- `JKS (Java KeyStore)`: This is the original, proprietary keystore format native to Java. It works perfectly well,
  which is why it's used in many tutorials and older systems.
- `PKCS12 (.p12)`: This is an industry-standard, language-neutral format for storing cryptographic keys and
  certificates. Because it's a standard, it is more portable and is now the recommended format for Java applications as
  well.

For new projects, PKCS12 is strongly recommended as it offers:

- Better security features
- Wider platform compatibility
- Future-proof design
- Support for all key types (secret keys, private keys, and certificates)

| Feature                | JKS                                            | PKCS12                                               |
|------------------------|------------------------------------------------|------------------------------------------------------|
| Platform Compatibility | Java-only                                      | Cross-platform                                       |
| Security               | Less secure, vulnerable to brute-force attacks | More secure, supports stronger encryption algorithms |
| Default in Java        | Versions prior to Java 9                       | Java 9 and later                                     |
| Key Types Supported    | Private keys and certificates                  | Secret keys, private keys, and certificates          |
| File Extensions        | .jks                                           | .p12, .pfx                                           |
| Industry Standard      | No                                             | Yes (Public-Key Cryptography Standards #12)          |

We should only maintain existing JKS keystores if we have specific requirements that prevent migration to PKCS12, such
as strict legacy system dependencies or backward compatibility requirements. We will use the standard.

### Create the Server Keystore

The server keystore is a single, password-protected file that bundles the WireMock server's private key (`server.key`)
and
its corresponding public certificate (`server.crt`).

First, we convert the PEM files into the standardized and portable PKCS12 format using openssl.

```shell
openssl pkcs12 -export -in server.crt -inkey server.key -out server.p12 -name wiremock-server -passout pass:password
```

- `pkcs12 -export`: The command to create a PKCS12 file.
- `-in server.crt -inkey server.key`: Specifies the input certificate and private key.
- `-out server.p12`: The output PKCS12 file.
- `-name wiremock-server`: Assigns an "alias" or friendly name to the entry inside the keystore.
- `-passout pass:password`: Sets the password for the PKCS12 file non-interactively.

## Create the Server Truststore

The truststore is conceptually different from the keystore. It does not contain private keys. Instead, it holds the
certificates of the Certificate Authorities that the server should trust. For mTLS, this file must contain our `ca.crt`
so
that WireMock can validate the client certificate presented by the client.

```shell
keytool -importcert -alias ca -file ca.crt -deststoretype pkcs12 -keystore wiremock.truststore.p12 -storepass password -noprompt
```

- `-importcert`: The keytool command to import a trusted certificate.
- `-alias ca`: A friendly name for the CA certificate within the truststore.
- `-file ca.crt`: The path to our CA's public certificate.
- `-deststoretype pkcs12`: The destination format
- `-keystore wiremock.truststore.p12`: The output truststore file.
- `-noprompt`: Accepts the trust confirmation automatically.

Without a truststore containing the CA certificate, WireMock would have no basis for trusting the client certificate and
would reject the mTLS handshake.

## Docker Compose with WireMock

Docker Compose allows us to define and run our container application with a single configuration file, ensuring a
consistent and reproducible environment.

Create a `docker-compose.yml` file with the following structure (command will be explained later):

```yaml
services:
  wiremock:
    image: wiremock/wiremock:3.13.1
    container_name: wiremock-mtls
    ports:
      - "8443:8443"
    volumes:
      - ./certs:/etc/wiremock/certs
      - ./mappings:/home/wiremock/mappings
    command:
      - "--https-port"
      - "8443"
      - "--https-keystore"
      - "/etc/wiremock/certs/server.p12"
      - "--keystore-password"
      - "password"
      - "--key-manager-password"
      - "password"
      - "--keystore-type"
      - "pkcs12"
      - "--https-truststore"
      - "/etc/wiremock/certs/wiremock.truststore.p12"
      - "--truststore-password"
      - "password"
      - "--truststore-type"
      - "pkcs12"
      - "--https-require-client-cert"
      - "--verbose"
```

- `image`: wiremock/wiremock:3.13.1: Uses the official WireMock Docker image.
- `ports`: Maps port 8443 on the host to port 8443 in the container, exposing the HTTPS endpoint.
- `volumes`:
    - `./certs:/etc/wiremock/certs`: Mounts our local certs directory (containing the p12 files) into the container.
      This makes the keystores available to WireMock.
    - .`/mappings:/home/wiremock/mappings`: Mounts our stub definitions into the container's default mapping directory,
      allowing for easy updates to mock responses.

### Configuring WireMock for mTLS via Command-Line Flags

The mTLS behavior of the WireMock Docker container is controlled entirely through command-line arguments. These are
passed using the command directive in the docker-compose.yml file.

| Argument                    | Description                                                                                           | Example Value                               |
|-----------------------------|-------------------------------------------------------------------------------------------------------|---------------------------------------------|
| --https-port                | Enables HTTPS on the specified port.                                                                  | 8443                                        |
| --https-keystore            | Path inside the container to the PKCS12 keystore containing the server's certificate and private key. | /etc/wiremock/certs/server.p12              |
| --keystore-password         | The password for the keystore file.                                                                   | password                                    |
| --key-manager-password      | The password used by Jetty to access individual keys.                                                 | password                                    |
| --keystore-type             | The type for the keystore file.                                                                       | pkcs12                                      |
| --https-truststore          | Path inside the container to the JKS truststore containing trusted CAs for client authentication.     | /etc/wiremock/certs/wiremock.truststore.p12 |
| --truststore-password       | The password for the truststore file.                                                                 | password                                    |
| --truststore-type           | The type for the truststore file.                                                                     | pkcs12                                      |
| --https-require-client-cert | Enforces mTLS by requiring all clients to present a valid certificate signed by a trusted CA.         | N/A (flag)                                  |
| --verbose                   | Verbose mode for debugging.                                                                           | N/A (flag)                                  |

The combination of these flags, particularly `--https-require-client-cert`, transforms WireMock from a simple HTTPS
server into a strict mTLS endpoint.

### Defining a Sample Mock Endpoint

To verify our setup, we need a simple API stub. We create a `mappings` directory in the project root. Inside it, we will
create a file named `hello-mtls.json`:

```json
{
  "request": {
    "method": "GET",
    "url": "/mtls"
  },
  "response": {
    "status": 200,
    "body": "Hello, mTLS World!",
    "headers": {
      "Content-Type": "text/plain"
    }
  }
}
```

When WireMock starts, it will automatically load this mapping file.

### Local Verification: Testing the mTLS Handshake with curl

With the `docker-compose.yml` and `mappings` folder with `hello-mtls.json` file in place, we start the environment:

```shell
docker compose up
```

Now, we are going to perform a series of curl requests to validate the mTLS configuration.

### Attempt connection without a client certificate:

```shell
curl --insecure -v https://localhost:8443/mtls
```

This command must fail. The verbose output (-v) will show a TLS handshake error, like alert bad certificate,
proving that the server is correctly enforcing the `--https-require-client-cert` flag. We have to use the `--insecure`
flag to allow curl continue with the handshake even it is not able to verify the server certificate.

```text
* Host localhost:8443 was resolved.
* IPv6: ::1
* IPv4: 127.0.0.1
*   Trying [::1]:8443...
* Connected to localhost (::1) port 8443
* ALPN: curl offers h2,http/1.1
* TLSv1.3 (OUT), TLS handshake, Client hello (1):
* TLSv1.3 (IN), TLS handshake, Server hello (2):
* TLSv1.3 (IN), TLS handshake, Encrypted Extensions (8):
* TLSv1.3 (IN), TLS handshake, Request CERT (13):
* TLSv1.3 (IN), TLS handshake, Certificate (11):
* TLSv1.3 (IN), TLS handshake, CERT verify (15):
* TLSv1.3 (IN), TLS handshake, Finished (20):
* TLSv1.3 (OUT), TLS change cipher, Change cipher spec (1):
* TLSv1.3 (OUT), TLS handshake, Certificate (11):
* TLSv1.3 (OUT), TLS handshake, Finished (20):
* SSL connection using TLSv1.3 / TLS_AES_256_GCM_SHA384 / X25519 / RSASSA-PSS
* ALPN: server accepted h2
* Server certificate:
*  subject: C=ES; ST=Madrid; L=Madrid; O=Dio Corporation; OU=IT; CN=Server
*  start date: Jul 19 16:42:23 2025 GMT
*  expire date: Jul 17 16:42:23 2035 GMT
*  issuer: C=ES; ST=Madrid; L=Madrid; O=Dio Corporation; OU=IT; CN=Dio Corporation Root CA
*  SSL certificate verify result: unable to get local issuer certificate (20), continuing anyway.
*   Certificate level 0: Public key type RSA (2048/112 Bits/secBits), signed using sha256WithRSAEncryption
* using HTTP/2
* [HTTP/2] [1] OPENED stream for https://localhost:8443/mtls
* [HTTP/2] [1] [:method: GET]
* [HTTP/2] [1] [:scheme: https]
* [HTTP/2] [1] [:authority: localhost:8443]
* [HTTP/2] [1] [:path: /mtls]
* [HTTP/2] [1] [user-agent: curl/8.5.0]
* [HTTP/2] [1] [accept: */*]
> GET /mtls HTTP/2
> Host: localhost:8443
> User-Agent: curl/8.5.0
> Accept: */*
> 
* TLSv1.3 (IN), TLS alert, bad certificate (554):
* OpenSSL SSL_read: OpenSSL/3.0.13: error:0A000412:SSL routines::sslv3 alert bad certificate, errno 0
* Failed receiving HTTP2 data: 56(Failure when receiving data from the peer)
* Connection #0 to host localhost left intact
curl: (56) OpenSSL SSL_read: OpenSSL/3.0.13: error:0A000412:SSL routines::sslv3 alert bad certificate, errno 0
```

### Attempt connection with a client certificate but without trusting the server's CA:

```shell
curl -v --cert certs/client.crt --key certs/client.key https://localhost:8443/mtls 
```

This command must also fail. The error will be different, something like "failed to verify the legitimacy of the
server". This proves that the client (curl) is correctly rejecting the server's certificate because it doesn't trust our
private CA.

```text
* Host localhost:8443 was resolved.
* IPv6: ::1
* IPv4: 127.0.0.1
*   Trying [::1]:8443...
* Connected to localhost (::1) port 8443
* ALPN: curl offers h2,http/1.1
* TLSv1.3 (OUT), TLS handshake, Client hello (1):
*  CAfile: /etc/ssl/certs/ca-certificates.crt
*  CApath: /etc/ssl/certs
* TLSv1.3 (IN), TLS handshake, Server hello (2):
* TLSv1.3 (IN), TLS handshake, Encrypted Extensions (8):
* TLSv1.3 (IN), TLS handshake, Request CERT (13):
* TLSv1.3 (IN), TLS handshake, Certificate (11):
* TLSv1.3 (OUT), TLS alert, unknown CA (560):
* SSL certificate problem: unable to get local issuer certificate
* Closing connection
curl: (60) SSL certificate problem: unable to get local issuer certificate
More details here: https://curl.se/docs/sslcerts.html

curl failed to verify the legitimacy of the server and therefore could not
establish a secure connection to it. To learn more about this situation and
how to fix it, please visit the web page mentioned above.
```

### Perform a full, successful mTLS connection:

```shell
curl -v --cacert certs/ca.crt --cert certs/client.crt --key certs/client.key https://localhost:8443/mtls
```

```text
* Host localhost:8443 was resolved.
* IPv6: ::1
* IPv4: 127.0.0.1
*   Trying [::1]:8443...
* Connected to localhost (::1) port 8443
* ALPN: curl offers h2,http/1.1
* TLSv1.3 (OUT), TLS handshake, Client hello (1):
*  CAfile: certs/ca.crt
*  CApath: /etc/ssl/certs
* TLSv1.3 (IN), TLS handshake, Server hello (2):
* TLSv1.3 (IN), TLS handshake, Encrypted Extensions (8):
* TLSv1.3 (IN), TLS handshake, Request CERT (13):
* TLSv1.3 (IN), TLS handshake, Certificate (11):
* TLSv1.3 (IN), TLS handshake, CERT verify (15):
* TLSv1.3 (IN), TLS handshake, Finished (20):
* TLSv1.3 (OUT), TLS change cipher, Change cipher spec (1):
* TLSv1.3 (OUT), TLS handshake, Certificate (11):
* TLSv1.3 (OUT), TLS handshake, CERT verify (15):
* TLSv1.3 (OUT), TLS handshake, Finished (20):
* SSL connection using TLSv1.3 / TLS_AES_256_GCM_SHA384 / X25519 / RSASSA-PSS
* ALPN: server accepted h2
* Server certificate:
*  subject: C=ES; ST=Madrid; L=Madrid; O=Dio Corporation; OU=IT; CN=Server
*  start date: Jul 19 16:42:23 2025 GMT
*  expire date: Jul 17 16:42:23 2035 GMT
*  subjectAltName: host "localhost" matched cert's "localhost"
*  issuer: C=ES; ST=Madrid; L=Madrid; O=Dio Corporation; OU=IT; CN=Dio Corporation Root CA
*  SSL certificate verify ok.
*   Certificate level 0: Public key type RSA (2048/112 Bits/secBits), signed using sha256WithRSAEncryption
*   Certificate level 1: Public key type RSA (4096/152 Bits/secBits), signed using sha256WithRSAEncryption
* using HTTP/2
* [HTTP/2] [1] OPENED stream for https://localhost:8443/mtls
* [HTTP/2] [1] [:method: GET]
* [HTTP/2] [1] [:scheme: https]
* [HTTP/2] [1] [:authority: localhost:8443]
* [HTTP/2] [1] [:path: /mtls]
* [HTTP/2] [1] [user-agent: curl/8.5.0]
* [HTTP/2] [1] [accept: */*]
> GET /mtls HTTP/2
> Host: localhost:8443
> User-Agent: curl/8.5.0
> Accept: */*
> 
* TLSv1.3 (IN), TLS handshake, Newsession Ticket (4):
< HTTP/2 200 
< content-type: text/plain
< matched-stub-id: a94046b2-7e91-4b6d-9a21-2e358e8149c0
< 
* Connection #0 to host localhost left intact
Hello, mTLS World!
```
