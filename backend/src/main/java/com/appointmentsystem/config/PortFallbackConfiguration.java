package com.appointmentsystem.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.boot.web.servlet.server.ConfigurableServletWebServerFactory;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.ServerSocket;

@Component
public class PortFallbackConfiguration implements WebServerFactoryCustomizer<ConfigurableServletWebServerFactory> {

    private static final Logger log = LoggerFactory.getLogger(PortFallbackConfiguration.class);
    private static final int DEFAULT_PORT = 1010;
    private static final int MAX_FALLBACK_PORT = 9190;

    private final Environment environment;

    public PortFallbackConfiguration(Environment environment) {
        this.environment = environment;
    }

    @Override
    public void customize(ConfigurableServletWebServerFactory factory) {
        int preferredPort = environment.getProperty("server.port", Integer.class, DEFAULT_PORT);

        if (isExplicitPortOverridePresent()) {
            factory.setPort(preferredPort);
            return;
        }

        if (isPortAvailable(preferredPort)) {
            factory.setPort(preferredPort);
            return;
        }

        int fallbackPort = findAvailablePort(preferredPort + 1, MAX_FALLBACK_PORT);
        if (fallbackPort == -1) {
            fallbackPort = findEphemeralPort();
        }

        factory.setPort(fallbackPort);
        log.warn("Port {} is already in use. Falling back to port {}.", preferredPort, fallbackPort);
    }

    private boolean isExplicitPortOverridePresent() {
        return hasText(System.getenv("PORT"))
                || hasText(System.getProperty("server.port"));
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private int findAvailablePort(int start, int end) {
        for (int port = start; port <= end; port++) {
            if (isPortAvailable(port)) {
                return port;
            }
        }
        return -1;
    }

    private int findEphemeralPort() {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to find an available server port.", exception);
        }
    }

    private boolean isPortAvailable(int port) {
        try (ServerSocket ignored = new ServerSocket(port)) {
            return true;
        } catch (IOException exception) {
            return false;
        }
    }
}
