package com.sap.db.util;

import java.net.Socket;
import java.nio.channels.AsynchronousSocketChannel;

/**
 * No-op shim replacing ngdbc's com.sap.db.util.SocketUtils.
 *
 * ngdbc ships two variants of SocketUtils in a multi-release JAR:
 *  - Base (Java 8): throws AssertionError when loaded on Java > 10
 *  - Java 11:      sets TCP keepalive via jdk.net.ExtendedSocketOptions,
 *                  which is inaccessible from within Cytoscape's OSGi runtime
 *
 * Both variants are unusable in this environment. Since SocketUtils only
 * provides optional TCP keepalive tuning (not required for JDBC connectivity),
 * this shim replaces it with no-ops. The bundle's own class takes precedence
 * over the inlined ngdbc version because Multi-Release is not declared.
 *
 * FELIX-6396 workaround — https://issues.apache.org/jira/browse/FELIX-6396
 * Removable when Cytoscape ships an OSGi R7-capable Felix (see pom.xml for full cleanup instructions).
 */
public final class SocketUtils {

    private SocketUtils() {}

    public static void setKeepAliveOptions(Socket socket, int idle, int interval, int count) {
        // no-op: TCP keepalive tuning skipped (jdk.net.ExtendedSocketOptions not accessible in OSGi)
    }

    public static void setKeepAliveOptions(AsynchronousSocketChannel channel, int idle, int interval, int count) {
        // no-op: TCP keepalive tuning skipped (jdk.net.ExtendedSocketOptions not accessible in OSGi)
    }
}
