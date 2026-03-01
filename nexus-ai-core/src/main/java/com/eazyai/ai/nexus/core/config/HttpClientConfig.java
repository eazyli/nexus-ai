package com.eazyai.ai.nexus.core.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.ConnectionPool;
import okhttp3.OkHttpClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.OkHttp3ClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.util.concurrent.TimeUnit;

/**
 * HTTP客户端配置
 * 为HttpToolExecutor提供连接池和超时配置
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties(NexusProperties.class)
public class HttpClientConfig {

    private final NexusProperties nexusProperties;

    /**
     * 配置OkHttpClient连接池
     */
    @Bean
    @ConditionalOnMissingBean(OkHttpClient.class)
    public OkHttpClient okHttpClient() {
        NexusProperties.HttpProperties httpProps = nexusProperties.getHttp();

        ConnectionPool connectionPool = new ConnectionPool(
            httpProps.getMaxConnectionsPerRoute(),
            httpProps.getKeepAliveDuration(),
            TimeUnit.MILLISECONDS
        );

        OkHttpClient.Builder builder = new OkHttpClient.Builder()
            .connectTimeout(httpProps.getConnectTimeout(), TimeUnit.MILLISECONDS)
            .readTimeout(httpProps.getReadTimeout(), TimeUnit.MILLISECONDS)
            .writeTimeout(httpProps.getWriteTimeout(), TimeUnit.MILLISECONDS)
            .connectionPool(connectionPool)
            .retryOnConnectionFailure(true);

        log.info("[HttpClientConfig] OkHttpClient初始化完成: connectTimeout={}ms, readTimeout={}ms, " +
                "maxConnections={}, keepAlive={}ms",
            httpProps.getConnectTimeout(), httpProps.getReadTimeout(),
            httpProps.getMaxConnections(), httpProps.getKeepAliveDuration());

        return builder.build();
    }

    /**
     * 配置RestTemplate使用OkHttp连接池
     */
    @Bean("toolRestTemplate")
    @ConditionalOnMissingBean(name = "toolRestTemplate")
    public RestTemplate toolRestTemplate(OkHttpClient okHttpClient) {
        OkHttp3ClientHttpRequestFactory factory = new OkHttp3ClientHttpRequestFactory(okHttpClient);
        return new RestTemplate(factory);
    }
}
