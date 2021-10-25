
package ccait;

import ccait.ccweb.repo.CCRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class CCServiceRunner implements ApplicationRunner {

    @Autowired
    CCRepository repo;

    @Override
    public void run(ApplicationArguments args) {
        try {

        }
        finally {

        }
    }
}
