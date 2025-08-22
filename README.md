# CockroachDB Dynamic Certs Client

A Spring Boot utility container that dynamically generates certificates for secure CockroachDB deployments. This application combines a Spring Boot service with the CockroachDB binary to create a complete certificate infrastructure at startup:

- A Certificate Authority (CA) with private key
- Client certificates for specified users (with PKCS#8 keys)
- Node certificates with configurable alternative names

The application runs once at container startup, generates all necessary certificates, then provides a health endpoint for monitoring.

## Features

- **Certificate Generation**: Creates CA, client, and node certificates using CockroachDB's cert commands
- **Multiple Users**: Supports custom client usernames (always includes 'root' user)
- **Flexible Node Names**: Configurable alternative names for node certificates
- **Container Ready**: Health endpoint for monitoring in orchestration environments
- **Multi-stage Build**: Optimized Docker image using official CockroachDB and Eclipse Temurin JDK 24 base images
- **PKCS#8 Support**: Generates PKCS#8 private keys for client certificates
- **Security Scanning**: Integrated Trivy scanning in CI/CD pipeline
- **Multi-platform Support**: Builds for both linux/amd64 and linux/arm64

## Environment Variables

| Variable | Required | Default | Description |
|----------|----------|---------|-------------|
| NODE_ALTERNATIVE_NAMES | Yes | - | Space-separated list of alternative names for the node certificate |
| CLIENT_USERNAME | No | root | Username for client certificate generation |

## Building the Image
```bash
docker build --no-cache -t timveil/cockroachdb-dynamic-certs:latest .
```

For specific platforms:
```bash
docker build --no-cache --platform linux/arm64 -t timveil/cockroachdb-dynamic-certs:latest .
```

## Publishing the Image
```bash
docker push timveil/cockroachdb-dynamic-certs:latest
```

## Running the Image

Basic usage:
```bash
docker run -p 9999:9999 \
    --env NODE_ALTERNATIVE_NAMES=localhost \
    -it timveil/cockroachdb-dynamic-certs:latest
```

With custom client username:
```bash
docker run -p 9999:9999 \
    --env NODE_ALTERNATIVE_NAMES="localhost node1.example.com" \
    --env CLIENT_USERNAME=myuser \
    -it timveil/cockroachdb-dynamic-certs:latest
```

## Health Monitoring

The application exposes a health endpoint at:
```
http://localhost:9999/actuator/health
```

This can be used to monitor the application's status in container orchestration environments.

## Certificate Locations

Inside the container, certificates are generated in:
- `/.cockroach-certs/` - Contains all generated certificates (CA, client, and node)
- `/.cockroach-key/` - Contains the CA private key (`ca.key`)

## Development

### Local Development
```bash
# Build with Maven
mvn clean package

# Run locally (requires CockroachDB binary in PATH)
mvn spring-boot:run
```

### Testing the Health Endpoint
```bash
# Check application health
curl http://localhost:9999/actuator/health
```

## Architecture

The application is built using:
- **Spring Boot 3.5.5** with Java 21 (runs on JDK 24 in Docker)
- **CockroachDB binary** for certificate generation
- **Multi-stage Docker build** for optimized image size
- **ApplicationRunner** interface for startup certificate generation

Certificate generation follows this sequence:
1. Create Certificate Authority (CA)
2. Generate client certificates for specified users
3. Create node certificates with alternative names
4. Start Spring Boot application with health monitoring

## CI/CD Pipeline

### Continuous Integration (`ci.yml`)
- **Triggers**: Every push to master and pull requests
- **Actions**: Maven build, Docker image creation, health endpoint testing
- **Security**: Trivy scans for CRITICAL and HIGH vulnerabilities

### Automated Release Process (`release.yml`)
- **Triggers**: Automatically on pushes to master that modify:
  - Source code (`src/**`)
  - Maven config (`pom.xml`)
  - Dockerfile
  - Release workflow itself
- **Versioning**: Date-based tags (YYYY.MM.DD and YYYY.MM.DD-sha)
- **Multi-platform**: Builds for linux/amd64 and linux/arm64
- **Docker Hub**: Pushes images with:
  - `latest` - Always points to newest build
  - `YYYY.MM.DD` - Date of release
  - `YYYY.MM.DD-sha` - Date plus commit SHA for uniqueness
- **Documentation**: Automatically syncs README to Docker Hub description

### Dependency Management
- **Dependabot**: Monitors Maven (daily), Docker (weekly), and GitHub Actions (weekly)
