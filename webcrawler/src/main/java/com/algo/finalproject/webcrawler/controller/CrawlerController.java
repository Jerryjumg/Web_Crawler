package com.algo.finalproject.webcrawler.controller;

import com.algo.finalproject.webcrawler.service.CrawlerService;
import com.algo.finalproject.webcrawler.service.ResultService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.AllArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/crawl")
@AllArgsConstructor
public class CrawlerController {

    private static final Logger logger = LogManager.getLogger(CrawlerController.class);
    private final CrawlerService crawlerService;
    private final Neo4jClient neo4jClient;
    private final ResultService resultService;

    @PostMapping("/start/{depth}")
    @Operation(summary = "Start crawling the given URLs with the specified depth")
    public ResponseEntity<Void> crawl(@PathVariable int depth,@RequestBody List<String> urls) {
        crawlerService.startCrawling(urls, depth);
        return ResponseEntity.status(202).build();
    }

    @GetMapping("/checkDatabase")
    @Operation(summary = "Check if the application can connect to the Neo4j database")
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
    @GetMapping("/status")
    @Operation(summary = "Check if the crawling process is in progress")
    public boolean getStatus() {
        return crawlerService.isCrawlInProgress();
    }

    @GetMapping("/result")
    @Operation(summary = "Get the URLs sorted by page rank")
    public List<String> getResult() {
        return resultService.getUrlsByPageRank();
    }
}
