package org.honeycomb.tools.netty.bootstrap;
import io.netty.bootstrap.Bootstrap;
import org.springframework.boot.autoconfigure.AutoConfigureOrder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.condition.SearchStrategy;
import org.springframework.boot.web.servlet.server.ServletWebServerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;


/**
 * User: luluful
 * Date: 4/8/19
 */
@AutoConfigureOrder(Ordered.HIGHEST_PRECEDENCE)
@Configuration
@ConditionalOnWebApplication // 在Web环境下才会起作用
public class NettyAutoConfiguration {
    @Configuration
    @ConditionalOnClass({Bootstrap.class}) // Netty的Bootstrap类必须在classloader中存在，才能启动Netty容器
    @ConditionalOnMissingBean(value = ServletWebServerFactory.class, search = SearchStrategy.CURRENT) //当前Spring容器中不存在EmbeddedServletContainerFactory接口的实例
    public static class EmbeddedNetty {
        //上述条件注解成立的话就会构造EmbeddedNettyFactory这个EmbeddedServletContainerFactory
        @Bean
        public NettyFactory embeddedNettyFactory() {
            return new NettyFactory();
        }
    }
}
