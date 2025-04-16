package com.cosw.councilOfSocialWork.domain;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("")
public class CSWController {


    @GetMapping(path = "")
    public String home(){
        return "UP";
    }
}
