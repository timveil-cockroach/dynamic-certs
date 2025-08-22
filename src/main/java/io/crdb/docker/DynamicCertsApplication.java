package io.crdb.docker;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.core.env.Environment;

import java.io.IOException;
import java.util.*;

/**
 * Spring Boot application that dynamically generates CockroachDB certificates.
 * <p>
 * This application combines a Java service with the CockroachDB binary to create:
 * <ul>
 *   <li>CA certificates and keys</li>
 *   <li>Client certificates for specified users</li>
 *   <li>Node certificates with configurable alternative names</li>
 * </ul>
 * <p>
 * The certificate generation process runs once at startup and creates all necessary
 * certificates for secure CockroachDB deployments.
 * 
 * @author Dynamic Certs
 * @version 1.0
 * @since 1.0
 */
@SpringBootApplication
public class DynamicCertsApplication implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DynamicCertsApplication.class);

    /** Environment variable name for specifying the client username */
    private static final String CLIENT_USERNAME = "CLIENT_USERNAME";
    /** Environment variable name for specifying node alternative names */
    private static final String NODE_ALTERNATIVE_NAMES = "NODE_ALTERNATIVE_NAMES";

    /** Directory where CockroachDB certificates will be stored */
    private static final String COCKROACH_CERTS_DIR = "/.cockroach-certs";
    /** Path to the CA private key file */
    private static final String COCKROACH_KEY = "/.cockroach-key/ca.key";
    /** Default username for client certificates when CLIENT_USERNAME is not specified */
    private static final String DEFAULT_USERNAME = "root";

    /**
     * Main entry point for the Spring Boot application.
     * 
     * @param args command line arguments passed to the application
     */
    public static void main(String[] args) {
        SpringApplication.run(DynamicCertsApplication.class, args);
    }

    /** Spring Environment for accessing configuration properties and environment variables */
    private final Environment env;

    /**
     * Constructor for dependency injection of Spring Environment.
     * 
     * @param env the Spring Environment instance for accessing configuration
     */
    public DynamicCertsApplication(Environment env) {
        this.env = env;
    }

    /**
     * Executes the certificate generation process after Spring Boot startup.
     * <p>
     * This method performs the following operations:
     * <ol>
     *   <li>Reads configuration from environment variables</li>
     *   <li>Creates CA certificate and key</li>
     *   <li>Generates client certificates for specified users</li>
     *   <li>Creates node certificates with alternative names</li>
     * </ol>
     * 
     * @param args application arguments (not used in this implementation)
     * @throws Exception if certificate generation fails or required environment variables are missing
     */
    @Override
    public void run(ApplicationArguments args) throws Exception {

        final List<String> nodeAlternativeNames = Arrays.asList(env.getRequiredProperty(NODE_ALTERNATIVE_NAMES).trim().split("\\s+"));
        final String clientUsername = env.getProperty(CLIENT_USERNAME, DEFAULT_USERNAME);

        log.info("{} is [{}]", NODE_ALTERNATIVE_NAMES, nodeAlternativeNames);
        log.info("{} is [{}]", CLIENT_USERNAME, clientUsername);

        Set<String> usernames = new HashSet<>();
        usernames.add(clientUsername);

        if (!clientUsername.equals(DEFAULT_USERNAME)){
            usernames.add(DEFAULT_USERNAME);
        }

        List<String> createCACommands = new ArrayList<>();
        createCACommands.add("/cockroach");
        createCACommands.add("cert");
        createCACommands.add("create-ca");
        createCACommands.add("--certs-dir");
        createCACommands.add(COCKROACH_CERTS_DIR);
        createCACommands.add("--ca-key");
        createCACommands.add(COCKROACH_KEY);

        ProcessBuilder createCA = new ProcessBuilder(createCACommands);
        handleProcess(createCA);

        for (String username : usernames) {
            List<String> createClientCommands = new ArrayList<>();
            createClientCommands.add("/cockroach");
            createClientCommands.add("cert");
            createClientCommands.add("create-client");
            createClientCommands.add(username);
            createClientCommands.add("--certs-dir");
            createClientCommands.add(COCKROACH_CERTS_DIR);
            createClientCommands.add("--ca-key");
            createClientCommands.add(COCKROACH_KEY);
            createClientCommands.add("--also-generate-pkcs8-key");

            ProcessBuilder createClient = new ProcessBuilder(createClientCommands);
            handleProcess(createClient);
        }


        List<String> createNodeCommands = new ArrayList<>();
        createNodeCommands.add("/cockroach");
        createNodeCommands.add("cert");
        createNodeCommands.add("create-node");
        createNodeCommands.addAll(nodeAlternativeNames);
        createNodeCommands.add("--certs-dir");
        createNodeCommands.add(COCKROACH_CERTS_DIR);
        createNodeCommands.add("--ca-key");
        createNodeCommands.add(COCKROACH_KEY);

        ProcessBuilder createNode = new ProcessBuilder(createNodeCommands);
        handleProcess(createNode);
    }

    /**
     * Executes a CockroachDB command and handles the process lifecycle.
     * <p>
     * This method starts the process, waits for completion, and validates the exit code.
     * If the process exits with a non-zero code, a RuntimeException is thrown.
     * 
     * @param builder the ProcessBuilder configured with the command to execute
     * @throws IOException if an I/O error occurs during process execution
     * @throws InterruptedException if the current thread is interrupted while waiting
     * @throws RuntimeException if the process exits with a non-zero exit code
     */
    private void handleProcess(ProcessBuilder builder) throws IOException, InterruptedException {

        builder.inheritIO();

        String command = builder.command().toString();

        log.debug("starting command... {}", command);

        Process process = builder.start();
        int exitCode = process.waitFor();

        if (exitCode != 0) {
            throw new RuntimeException(String.format("the following command exited ABNORMALLY with code [%d]: %s", exitCode, command));
        } else {
            log.debug("command exited SUCCESSFULLY with code [{}]", exitCode);
        }

    }
}
