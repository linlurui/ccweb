/**
 *
 *  License: http://www.apache.org/licenses/LICENSE-2.0
 *  Home page: https://github.com/linlurui/ccweb
 *  Note: to build on java, include the jdk1.8+ compiler symbol (and yes,
 *  I know the difference between language and runtime versions; this is a compromise).
 * @author linlurui
 * @Date Date: 2019-02-10
 */

package ccait.ccweb.websocket;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.servlet.ServletContextInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.*;
import org.springframework.web.socket.server.standard.ServerEndpointExporter;
import org.springframework.web.socket.server.standard.ServletServerContainerFactoryBean;
import org.springframework.web.util.WebAppRootListener;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import java.util.EventListener;

@EnableWebSocket
@Configuration
@EnableAutoConfiguration
//@EnableWebSocketMessageBroker
@ConditionalOnProperty(prefix = "websocket", name = "enable", havingValue = "true")
public class WebSocketConfig /*extends AbstractSessionWebSocketMessageBrokerConfigurer*/ implements ServletContextInitializer {

    @Bean
    public ServerEndpointExporter serverEndpointExporter() {
        return new ServerEndpointExporter();  //application.yml的websocket.enable设为true才会进来
    }

//    @Override
//    protected void configureStompEndpoints(StompEndpointRegistry stompEndpointRegistry) {
//        /**
//         * 注册 Stomp的端点
//         * addEndpoint：添加STOMP协议的端点。这个HTTP URL是供WebSocket或SockJS客户端访问的地址
//         * withSockJS：指定端点使用SockJS协议
//         */
//        stompEndpointRegistry.addEndpoint("/stomp")
//                .setAllowedOrigins("*") // 添加允许跨域访问
//                .withSockJS();
//    }

//    @Override
//    public void configureMessageBroker(MessageBrokerRegistry registry) {
//        /**
//         * 配置消息代理
//         * 启动简单Broker，消息的发送的地址符合配置的前缀来的消息才发送到这个broker
//         */
//        registry.enableSimpleBroker("/topic","/queue");
//    }

    //    @Override
//    public void configureClientInboundChannel(ChannelRegistration registration) {
//        super.configureClientInboundChannel(registration);
//    }
    @Bean
    public ServletServerContainerFactoryBean createWebSocketContainer() {
        ServletServerContainerFactoryBean container = new ServletServerContainerFactoryBean();
        // ws 传输数据的时候，数据过大有时候会接收不到，所以在此处设置bufferSize
        container.setMaxTextMessageBufferSize(1024000);
        container.setMaxBinaryMessageBufferSize(1024000);
        container.setMaxSessionIdleTimeout(15 * 60000L);
        return container;
    }

    @Override
    public void onStartup(ServletContext servletContext) throws ServletException {
        servletContext.addListener(WebAppRootListener.class);
        servletContext.setInitParameter("org.apache.tomcat.websocket.textBufferSize","1024000");
    }
}
