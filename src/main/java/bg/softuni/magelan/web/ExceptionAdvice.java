package bg.softuni.magelan.web;

import bg.softuni.magelan.exception.BookingNotFoundException;
import bg.softuni.magelan.exception.OrderNotFoundException;
import bg.softuni.magelan.exception.UserNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.ModelAndView;

@Slf4j
@ControllerAdvice
public class ExceptionAdvice {

        @ResponseStatus(HttpStatus.NOT_FOUND)
        @ExceptionHandler({
                UserNotFoundException.class,
                OrderNotFoundException.class,
                BookingNotFoundException.class
        })
        public ModelAndView handleNotFound(RuntimeException ex) {
            log.warn("Not found: {}", ex.getMessage());

            ModelAndView mav = new ModelAndView("error-not-found");
            mav.addObject("errorMessage", ex.getMessage());
            return mav;
        }

        @ResponseStatus(HttpStatus.BAD_REQUEST)
        @ExceptionHandler({IllegalStateException.class, IllegalArgumentException.class})
        public ModelAndView handleBadRequest(RuntimeException ex) {
            log.warn("Bad request: {}", ex.getMessage());

            ModelAndView mav = new ModelAndView("error-bad-request");
            mav.addObject("errorMessage", ex.getMessage());
            return mav;
        }

        @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
        @ExceptionHandler(Exception.class)
        public ModelAndView handleAnyException(Exception exception) {
            log.error("Unhandled exception:", exception);

            ModelAndView modelAndView = new ModelAndView("internal-server-error");
            modelAndView.addObject("errorMessage", exception.getClass().getSimpleName());
            return modelAndView;
        }
}