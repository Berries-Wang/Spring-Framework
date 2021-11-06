package link.bosswang.config;

import link.bosswang.domain.Response;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Import;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.view.InternalResourceViewResolver;

@Configurable
@EnableWebMvc
@EnableAspectJAutoProxy
@ComponentScan(basePackages = "link.bosswang")
@Import(DefaultMvcConfig.class)
@ControllerAdvice
public class MvcConfig {

    @Bean
    public InternalResourceViewResolver viewResolver() {
        InternalResourceViewResolver viewResolver = new InternalResourceViewResolver();
        viewResolver.setPrefix("/");
        viewResolver.setSuffix(".jsp");
        return viewResolver;
    }

    @ExceptionHandler(value = Exception.class)
    @ResponseBody
    public Response createGlobalExceptionHandler(Exception e) {
        Response resp = new Response();
        resp.setSuccess(false);
        if (e instanceof MethodArgumentNotValidException) {
            Response response = resp;
            MethodArgumentNotValidException ex = (MethodArgumentNotValidException) e;
            response.setMsg(ex.getBindingResult().getFieldError().getDefaultMessage());
        } else {
            resp.setSuccess(false);
            resp.setMsg(e.getMessage());
        }

        return resp;
    }

}
