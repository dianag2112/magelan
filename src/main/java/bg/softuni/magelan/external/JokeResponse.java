package bg.softuni.magelan.external;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class JokeResponse {

    private int id;
    private String type;
    private String setup;
    private String punchline;
}
