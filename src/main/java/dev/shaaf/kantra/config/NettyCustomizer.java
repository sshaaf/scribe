package dev.shaaf.kantra.config;

import io.quarkus.runtime.Startup;
import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.logging.Logger;

/**
 * Netty configuration marker for handling TCP_NODELAY socket option failures gracefully.
 * This addresses macOS-specific issues where setting TCP_NODELAY can fail with "Invalid argument"
 * on sockets that are already in use or in certain states.
 *
 * The actual configuration is done via application.properties with:
 * - Netty log level suppression to avoid warnings
 * - Vert.x transport configuration to use JDK transport instead of native
 * - TCP options configured at server level rather than per-socket
 *
 * Root cause: On macOS, Netty sometimes fails to set TCP_NODELAY on already-established
 * sockets, particularly when there's IPv4/IPv6 mixing or the socket is in a transitional state.
 * The fix ensures TCP options are set at the server level and suppresses spurious warnings.
 */
@Startup
@ApplicationScoped
public class NettyCustomizer {

    private static final Logger LOG = Logger.getLogger(NettyCustomizer.class);

    public NettyCustomizer() {
        LOG.info("Netty customizer initialized - TCP socket options configured via application.properties");
        LOG.debug("TCP_NODELAY warnings suppressed, using JDK transport to avoid native socket option issues");
    }
}
