# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is a Spring Boot application that dynamically generates CockroachDB certificates. It combines a Java service with the CockroachDB binary to create CA certificates, client certificates, and node certificates for secure CockroachDB deployments.

## Development Commands

### Building and Running
- Build the application: `mvn clean package`
- Run locally: `mvn spring-boot:run`
- Run tests: `mvn test` (no tests currently exist)

### Docker Commands
- Build Docker image: `docker build --no-cache -t timveil/cockroachdb-dynamic-certs:latest .`
- Build for specific platform: `docker build --no-cache --platform linux/arm64 -t timveil/cockroachdb-dynamic-certs:latest .`
- Run container: `docker run -p 9999:9999 --env NODE_ALTERNATIVE_NAMES=localhost -it timveil/cockroachdb-dynamic-certs:latest`

## Architecture

### Core Application
- **Main class**: `DynamicCertsApplication` (src/main/java/io/crdb/docker/DynamicCertsApplication.java:15)
- **Framework**: Spring Boot 3.5.5 with Java 21 (Docker uses JDK 24)
- **Port**: 9999 (configured in application.properties:5)
- **Health endpoint**: `/actuator/health`

### Certificate Generation Flow
The application runs once at startup and executes these CockroachDB certificate generation commands:
1. Creates CA certificate and key
2. Generates client certificates for specified users (defaults to 'root')
3. Creates node certificates with configurable alternative names

### Environment Variables
- `NODE_ALTERNATIVE_NAMES` (required): Space-separated list of alternative names for node certificates
- `CLIENT_USERNAME` (optional): Username for client certificate, defaults to 'root'

### File Structure
- Certificate output: `/.cockroach-certs/`
- CA key location: `/.cockroach-key/ca.key`
- Maven wrapper scripts: `mvnw` and `mvnw.cmd` for platform-independent builds

## Docker Architecture

The Dockerfile uses multi-stage builds:
1. **Builder stage**: Uses Maven with Eclipse Temurin JDK 24 to compile and package
2. **CockroachDB stage**: Extracts the CockroachDB binary from latest image
3. **Runtime stage**: Combines Spring Boot layers with CockroachDB binary on JDK 24

## Configuration

Key application properties (src/main/resources/application.properties):
- Logging levels configured for debugging certificate generation
- Health endpoint exposure for container monitoring
- Server runs on port 9999

## CI/CD Pipeline

### GitHub Actions Workflows

**CI Build and Test** (.github/workflows/ci.yml):
- Triggers on push/PR to master branch
- Maven clean package build
- Docker image build and health endpoint testing
- Trivy security scanning (fails on CRITICAL/HIGH vulnerabilities)
- Simplified single-platform build for faster CI
- Uses GitHub Actions v5 and JDK 21

**Automated Release** (.github/workflows/release.yml):
- Triggers automatically on pushes to master that change source code, pom.xml, or Dockerfile
- No manual tagging required - versions are auto-generated
- Version format: YYYY.MM.DD and YYYY.MM.DD-sha (e.g., 2024.01.15-a1b2c3d)
- Multi-platform builds (linux/amd64, linux/arm64) using QEMU
- Publishes to Docker Hub with tags:
  - `latest` - Always the newest build
  - Date tag (e.g., `2024.01.15`)
  - Date-SHA tag (e.g., `2024.01.15-a1b2c3d`)
- Updates Docker Hub description from README
- Uses docker/build-push-action for efficient multi-arch builds
- Required secrets:
  - `DOCKER_USERNAME`, `DOCKER_PASSWORD` - Docker Hub login
  - `DOCKERHUB_USERNAME`, `DOCKERHUB_TOKEN` - For description updates

### Dependency Management

**Dependabot** (.github/dependabot.yml) monitors:
- Maven dependencies (daily)
- Docker base images (weekly)
- GitHub Actions (weekly)