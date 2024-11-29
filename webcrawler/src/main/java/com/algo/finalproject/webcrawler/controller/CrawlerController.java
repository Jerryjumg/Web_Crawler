package com.algo.finalproject.webcrawler.controller;

import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/crawl")
public class CrawlerController {

    @PostMapping("/start")
    public List<String> crawl(@RequestBody List<String> urls) {
        return urls;
    }
}
