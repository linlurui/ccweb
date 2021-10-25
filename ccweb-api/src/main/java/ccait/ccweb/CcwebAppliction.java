/**
 *
 *  License: http://www.apache.org/licenses/LICENSE-2.0
 *  Home page: https://github.com/linlurui/ccweb
 *  Note: to build on java, include the jdk1.8+ compiler symbol (and yes,
 *  I know the difference between language and runtime versions; this is a compromise).
 * @author linlurui
 * @Date Date: 2019-02-10
 */

package ccait.ccweb;

import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;
import org.springframework.web.reactive.config.EnableWebFlux;
import java.io.FileNotFoundException;
import java.net.MalformedURLException;

//@EnableHystrixDashboard
//@EnableDiscoveryClient
//@EnableFeignClients
//@EnableHystrix
//@EnableZuulProxy
//@EnableEurekaClient
@EnableWebFlux
@SpringBootApplication( exclude = {DataSourceAutoConfiguration.class, DataSourceTransactionManagerAutoConfiguration.class} )
public class CcwebAppliction {

    private static final Logger log = LoggerFactory.getLogger( CcwebAppliction.class );

    public static void main(String[] args) throws FileNotFoundException, MalformedURLException {
        run(null, args);
    }

    public static void run(Class clazz, String[] args) throws FileNotFoundException, MalformedURLException {
        SpringApplication app = new SpringApplication(CcwebAppliction.class);
        if(clazz == null) {
            app.run(args);
        }

        else {
            app.run(clazz, args);
        }

        log.info( "---------------------------------------------------------------------------------------" );
        log.info( "ccweb started!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!" );
    }
}
