package com.algo.finalproject.webcrawler.controller;

import com.algo.finalproject.webcrawler.service.CrawlerService;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/crawl")
@AllArgsConstructor
public class CrawlerController {

   private final CrawlerService crawlerService;
    @PostMapping("/start")
    public void crawl(@RequestBody List<String> urls) {
        crawlerService.crawl(urls);
    }
}
