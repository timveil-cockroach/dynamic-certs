# CockroachDB Dynamic Certs Client

[![CI Build and Test](https://github.com/timveil/dynamic-certs/actions/workflows/ci.yml/badge.svg)](https://github.com/timveil/dynamic-certs/actions/workflows/ci.yml) [![Automated Release](https://github.com/timveil/dynamic-certs/actions/workflows/release.yml/badge.svg)](https://github.com/timveil/dynamic-certs/actions/workflows/release.yml) [![Docker Hub](https://img.shields.io/docker/pulls/timveil/cockroachdb-dynamic-certs)](https://hub.docker.com/repository/docker/timveil/cockroachdb-dynamic-certs)

A production-ready Spring Boot utility container that dynamically generates certificates for secure CockroachDB deployments. This application addresses the common challenge of certificate management in containerized CockroachDB environments by automating the entire certificate lifecycle at container startup.

## What It Does

This container combines a Spring Boot service with the official CockroachDB binary to create a complete PKI infrastructure:

- **Certificate Authority (CA)**: Creates a root CA certificate and private key for your CockroachDB cluster
- **Client Certificates**: Generates user certificates with PKCS#8 private keys for secure client connections
- **Node Certificates**: Creates node certificates with configurable Subject Alternative Names (SANs) for cluster communication

## Use Cases

- **Kubernetes Deployments**: Generate certificates dynamically in init containers or sidecar patterns
- **Docker Compose**: Bootstrap secure CockroachDB clusters with automated certificate generation
- **CI/CD Pipelines**: Create ephemeral test environments with proper certificate security
- **Development**: Quickly spin up secure CockroachDB instances for local development
- **Multi-Node Clusters**: Generate certificates with proper SANs for distributed deployments

The application runs once at container startup, generates all necessary certificates, then provides a health endpoint for monitoring container readiness.

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

| Variable | Required | Default | Description | Example |
|----------|----------|---------|-------------|----------|
| `NODE_ALTERNATIVE_NAMES` | Yes | - | Space-separated list of alternative names for the node certificate | `localhost node1.example.com 10.0.1.5` |
| `CLIENT_USERNAME` | No | `root` | Username for client certificate generation | `myuser` |

### NODE_ALTERNATIVE_NAMES Explained

This variable is critical for proper certificate validation. Include all possible ways your CockroachDB node might be accessed:

- **Hostnames**: `cockroach-node-1`, `db.example.com`
- **IP Addresses**: `192.168.1.100`, `10.0.0.5`
- **Service Names**: `cockroachdb-service` (for Kubernetes)
- **Localhost**: `localhost`, `127.0.0.1` (for local development)

**Example for Kubernetes:**
```bash
NODE_ALTERNATIVE_NAMES="cockroachdb cockroachdb.default cockroachdb.default.svc.cluster.local localhost"
```

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

### Basic Usage
```bash
docker run -p 9999:9999 \
    --env NODE_ALTERNATIVE_NAMES=localhost \
    timveil/cockroachdb-dynamic-certs:latest
```

### Production Example
```bash
docker run -d \
    --name cert-generator \
    --env NODE_ALTERNATIVE_NAMES="cockroach-node1 cockroach-node1.internal 10.0.1.5 localhost" \
    --env CLIENT_USERNAME=appuser \
    -v /host/certs:/output \
    timveil/cockroachdb-dynamic-certs:latest
```

### Kubernetes Init Container
```yaml
initContainers:
- name: cert-generator
  image: timveil/cockroachdb-dynamic-certs:latest
  env:
  - name: NODE_ALTERNATIVE_NAMES
    value: "cockroachdb cockroachdb.default cockroachdb.default.svc.cluster.local"
  - name: CLIENT_USERNAME
    value: "root"
  volumeMounts:
  - name: certs
    mountPath: /.cockroach-certs
  - name: ca-key
    mountPath: /.cockroach-key
```

### Docker Compose Integration
```yaml
version: '3.8'
services:
  cert-generator:
    image: timveil/cockroachdb-dynamic-certs:latest
    environment:
      NODE_ALTERNATIVE_NAMES: "cockroachdb localhost 127.0.0.1"
      CLIENT_USERNAME: "root"
    volumes:
      - certs-volume:/output
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:9999/actuator/health"]
      interval: 10s
      timeout: 5s
      retries: 3

  cockroachdb:
    image: cockroachdb/cockroach:latest
    depends_on:
      cert-generator:
        condition: service_healthy
    volumes:
      - certs-volume:/.cockroach-certs:ro
    command: start --certs-dir=/.cockroach-certs --host=cockroachdb

volumes:
  certs-volume:
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

### Contributing

Contributions welcome! Please:

1. Fork the repository
2. Create a feature branch: `git checkout -b feature/amazing-feature`
3. Make your changes and test locally
4. Run the build: `mvn clean package`
5. Test with Docker: `docker build -t test .`
6. Submit a pull request

#### Development Setup

```bash
# Clone repository
git clone https://github.com/timveil/dynamic-certs.git
cd dynamic-certs

# Build with Maven
mvn clean package

# Build Docker image
docker build -t dynamic-certs:dev .

# Test locally
docker run --rm -e NODE_ALTERNATIVE_NAMES=localhost dynamic-certs:dev
```

### Dependency Management
- **Dependabot**: Monitors Maven (daily), Docker (weekly), and GitHub Actions (weekly)

## License

This project is open source. See the repository for license details.

## Support

For issues, questions, or contributions:
- 🐛 **Issues**: [GitHub Issues](https://github.com/timveil/dynamic-certs/issues)
- 🚀 **Feature Requests**: [GitHub Discussions](https://github.com/timveil/dynamic-certs/discussions)
- 📖 **Documentation**: This README and inline code comments

---

**Quick Start**: `docker run --rm -e NODE_ALTERNATIVE_NAMES=localhost timveil/cockroachdb-dynamic-certs:latest`
