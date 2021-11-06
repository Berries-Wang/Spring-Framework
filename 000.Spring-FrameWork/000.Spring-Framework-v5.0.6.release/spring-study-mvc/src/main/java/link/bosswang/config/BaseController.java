package link.bosswang.config;

import org.springframework.web.bind.annotation.ModelAttribute;

public class BaseController {

    private final ThreadLocal<String> threadLocal = new ThreadLocal<>();

    @ModelAttribute
    public void doPreHandle() {
        System.out.println("Hello World: " + this.threadLocal.toString());
    }

}
