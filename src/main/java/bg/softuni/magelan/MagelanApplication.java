package bg.softuni.magelan;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients(basePackages = "bg.softuni.magelan.payment")
public class MagelanApplication {

    public static void main(String[] args) {
        SpringApplication.run(MagelanApplication.class, args);
    }

}
