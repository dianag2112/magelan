package bg.softuni.magelan.external;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

@FeignClient(
        name = "jokeClient",
        url = "https://official-joke-api.appspot.com"
)
public interface JokeClient {

    @GetMapping("/random_joke")
    JokeResponse getRandomJoke();
}
