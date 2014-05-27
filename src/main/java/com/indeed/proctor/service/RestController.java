package com.indeed.proctor.service;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Map;

@Controller
public class RestController {
    @RequestMapping(value="/groups/identify", method= RequestMethod.GET)
    public @ResponseBody String groupsIdentify() {
        return "Hello, world!";
    }
}
