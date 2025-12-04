package bg.softuni.magelan.web;

import bg.softuni.magelan.external.JokeResponse;
import bg.softuni.magelan.external.JokeService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.ModelAndView;
import bg.softuni.magelan.security.UserData;

@Controller
@RequiredArgsConstructor
public class JokeController {

    private final JokeService jokeService;

    @GetMapping("/joke")
    public ModelAndView showJokePage(@AuthenticationPrincipal UserData userData) {
        String jokeText = jokeService.getRandomJokeText();

        ModelAndView modelAndView = new ModelAndView("joke");
        modelAndView.addObject("jokeText", jokeText);
        modelAndView.addObject("isAuthenticated", userData != null);
        return modelAndView;
    }
}
