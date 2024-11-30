package com.algo.finalproject.webcrawler.controller;

import com.algo.finalproject.webcrawler.service.CrawlerService;
import lombok.AllArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/crawl")
@AllArgsConstructor
public class CrawlerController {

    private static final Logger logger = LogManager.getLogger(CrawlerController.class);
    private final CrawlerService crawlerService;
    private final Neo4jClient neo4jClient;

    @PostMapping("/start")
    public void crawl(@RequestBody List<String> urls) {
        crawlerService.crawl(urls);
    }

    @GetMapping("/checkDatabase")
    public boolean checkDatabase() {
        try {
            neo4jClient.query("RETURN 1").run();
            logger.info("Successfully connected to Neo4j database.");
            return true;
        } catch (Exception e) {
            logger.error("Failed to connect to Neo4j database.", e);
            return false;
        }
    }
}
