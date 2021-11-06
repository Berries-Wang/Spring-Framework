package link.bosswang.controller;


import link.bosswang.config.BaseController;
import link.bosswang.domain.NameReq;
import link.bosswang.domain.Response;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.validation.Valid;


@Controller
@RequestMapping(value = "/demo")
@Validated
public class ValidatedController extends BaseController {


    @RequestMapping(value = "/getObject", method = RequestMethod.POST)
    @ResponseBody
    public Response demoResp(@RequestBody @Valid NameReq req) {
        Response response = new Response();

        response.setSuccess(true);
        response.setMsg(req.getName());

        return response;
    }
}
