package com.googlecode.kanbanik.server;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import com.googlecode.kanbanik.Configuration;
import com.googlecode.kanbanik.model.KanbanikConnectionManager;
import com.googlecode.kanbanik.security.KanbanikRealm;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.mgt.DefaultSecurityManager;

/**
 * KanbanikContextListener manages the initialization and destruction of
 * application-specific resources, such as database connections and security settings.
 */
public class KanbanikContextListener implements ServletContextListener {

    /**
     * Cleans up resources when the servlet context is destroyed.
     *
     * @param event the event that notifies about the context destruction
     */
    @Override
    public void contextDestroyed(ServletContextEvent event) {
        // Close the connection pool
        new KanbanikConnectionManager().destroyConnectionPool();
    }

    /**
     * Initializes resources when the servlet context is initialized.
     *
     * @param event the event that notifies about the context initialization
     */
    @Override
    public void contextInitialized(ServletContextEvent event) {
        // Set up security manager
        SecurityUtils.setSecurityManager(new DefaultSecurityManager(new KanbanikRealm()));

        ServletContext context = event.getServletContext();
        String server = getParam(context, "MONGODB_HOST");
        String port = getParam(context, "MONGODB_PORT");
        String user = getParam(context, "MONGODB_USER");
        String password = getParam(context, "MONGODB_PASSWORD");
        String dbName = getParam(context, "MONGODB_DATABASE");
        String authenticationRequired = getParam(context, "MONGODB_AUTHENTICATION_REQUIRED");

        // Validate mandatory parameters
        if (server == null || port == null || dbName == null) {
            throw new IllegalArgumentException("Required MongoDB parameters are missing. Please check the environment variables or context parameters.");
        }

        boolean enableGzipCommunication = Boolean.parseBoolean(getParam(context, "ENABLE_GZIP_COMMUNICATION"));
        boolean enableAccessControlHeaders = Boolean.parseBoolean(getParam(context, "ENABLE_ACCESS_CONTROL_HEADERS"));

        // Initialize configuration
        Configuration.init(enableGzipCommunication, enableAccessControlHeaders);

        // Initialize the connection pool
        new KanbanikConnectionManager().initConnectionPool(
                server,
                port,
                user,
                password,
                dbName,
                authenticationRequired
        );
    }

    /**
     * Retrieves a parameter value from either environment variables or servlet context initialization parameters.
     *
     * @param context   the servlet context
     * @param paramName the name of the parameter
     * @return the parameter value, or {@code null} if not found
     */
    private String getParam(ServletContext context, String paramName) {
        try {
            String fromEnv = System.getenv(paramName);
            if (fromEnv != null) {
                return fromEnv;
            }
        } catch (SecurityException e) {
            // Log the exception for debugging purposes
            System.err.println("Security exception while accessing environment variable: " + paramName);
        }
        return context.getInitParameter(paramName);
    }
}
