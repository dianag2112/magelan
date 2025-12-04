package bg.softuni.magelan.external;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class JokeService {

    private final JokeClient jokeClient;

    public JokeResponse getRandomJoke() {
        return jokeClient.getRandomJoke();
    }

    public String getRandomJokeText() {
        JokeResponse joke = jokeClient.getRandomJoke();
        return joke.getSetup() + " " + joke.getPunchline();
    }
}
