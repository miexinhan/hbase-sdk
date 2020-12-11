package com.github.CCweixiao;

import com.github.CCweixiao.exception.HBaseOperationsException;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.HConstants;
import org.apache.hadoop.hbase.client.Connection;
import org.apache.hadoop.hbase.client.ConnectionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

/**
 * @author leojie 2020/11/13 11:52 下午
 */
public abstract class AbstractHBaseConfig implements HBaseOperations {
    private static final Logger LOGGER = LoggerFactory.getLogger(HBaseOperations.class);
    private Configuration configuration;

    private volatile Connection connection;

    public AbstractHBaseConfig(String zkHost, String zkPort) {
        Configuration configuration = getConfiguration(zkHost, zkPort);
        if (configuration == null) {
            throw new HBaseOperationsException("a valid configuration is provided.");
        }
        this.configuration = configuration;
    }

    public AbstractHBaseConfig(Configuration configuration) {
        String auth = configuration.get("hadoop.security.authentication", null);

        if (auth == null) {
            this.configuration = configuration;
        }else{
            if (auth.toLowerCase().equals("kerberos")) {
                this.configuration = KerberosAuthorization.INSTANCE.getInstance(configuration);
            } else {
                throw new HBaseOperationsException("This type of authentication " + auth + " is not supported for the time being.");
            }
        }
        if (this.configuration == null) {
            throw new HBaseOperationsException("a valid configuration is provided.");
        }
    }

    public AbstractHBaseConfig(Properties properties) {
        String auth = properties.getProperty("hadoop.security.authentication", null);
        Configuration configuration;
        if (auth == null) {
            configuration = getConfiguration(properties);
        } else {
            if (auth.toLowerCase().equals("kerberos")) {
                configuration = KerberosAuthorization.INSTANCE.getInstance(properties);
            } else {
                throw new HBaseOperationsException("This type of authentication " + auth + " is not supported for the time being.");
            }
        }
        if (configuration == null) {
            throw new HBaseOperationsException("a valid configuration is provided.");
        }
        this.configuration = configuration;
    }


    @Override
    public Connection getConnection() {
        if (null == this.connection) {
            synchronized (this) {
                if (null == this.connection) {
                    try {
                        this.connection = ConnectionFactory.createConnection(configuration);
                        LOGGER.info("the connection pool of HBase is created successfully.>>>>>>>>>>>>>>>>>>");
                    } catch (IOException e) {
                        LOGGER.error("the connection pool of HBase is created failed.>>>>>>>>>>>>>>>>>");
                        LOGGER.error(e.getMessage());
                    }
                }
            }
        }
        return this.connection;
    }

    public void setConnection(Connection connection) {
        this.connection = connection;
    }

    public Configuration getConfiguration() {
        return configuration;
    }

    public void setConfiguration(Configuration configuration) {
        this.configuration = configuration;
    }

    public Configuration getConfiguration(String zkHost, String zkPort) {
        this.configuration = HBaseConfiguration.create();
        configuration.set(HConstants.ZOOKEEPER_QUORUM, zkHost);
        configuration.set(HConstants.ZOOKEEPER_CLIENT_PORT, zkPort);
        return configuration;
    }

    public Configuration getConfiguration(Properties properties) {
        this.configuration = HBaseConfiguration.create();
        final List<String> keys = properties.keySet().stream().map(Object::toString).collect(Collectors.toList());
        keys.forEach(key -> configuration.set(key, properties.getProperty(key)));
        return configuration;
    }
}
