package com.algo.finalproject.webcrawler.service;

import lombok.AllArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.stereotype.Service;
import java.util.Collection;

@Service
@AllArgsConstructor
public class PageRankingService {

    private Neo4jClient neo4jClient;
    private static final Logger logger = LogManager.getLogger(PageRankingService.class);

    public void updatePageRank(String url) {
        int inDegree = getInDegree(url);
        logger.info("In-degree for URL: " + url + " is " + inDegree);

        // Calculate PageRank based on in-degree and other factors
        double pageRankScore = calculatePageRank(url, inDegree);
        logger.info("PageRank for URL: " + url + " is " + pageRankScore);
        // Update PageRank property of the node in Neo4j
        updatePageRankInNeo4j(url, pageRankScore);
    }

    private int getInDegree(String url) {
        String cypherQuery = "MATCH (p:Page) WHERE p.url = $url " +
                "RETURN size([(p)<--() | 1]) AS inDegree LIMIT 1";
        try {
            return neo4jClient.query(cypherQuery)
                    .bind(url).to("url")
                    .fetchAs(Integer.class)
                    .one()
                    .orElse(0);

        } catch (Exception e) {
            logger.error("Failed to get in-degree for URL: " + url, e);
            return 0;
        }
    }

    private double calculatePageRank(String url, int inDegree) {
        double dampingFactor = 0.85;
        double baseRank = (1 - dampingFactor);
        double sumPageRank = getSumPageRank(url);
        return baseRank + dampingFactor * sumPageRank;
    }

    private double getSumPageRank(String url) {
        String cypherQuery = """
        MATCH (p:Page {url: $url})<-[:LINKS_TO]-(q:Page)
        RETURN COALESCE(SUM(q.pageRank), 0) AS sumPageRank
    """;
        try {
            return neo4jClient.query(cypherQuery)
                    .bind(url).to("url")
                    .fetchAs(Double.class)
                    .one()
                    .orElse(0.0);
        } catch (Exception e) {
            logger.error("Failed to get sum of PageRank for URL: " + url, e);
            return 0.0;
        }
    }

    private void updatePageRankInNeo4j(String url, double pageRankScore) {
        String cypherQuery = "MATCH (p:Page) WHERE p.url = $url SET p.pageRank = $pageRank";
        try {
            neo4jClient.query(cypherQuery)
                    .bind(url).to("url")
                    .bind(pageRankScore).to("pageRank")
                    .run();
            logger.info("Updated PageRank for URL: " + url + " to " + pageRankScore);
        } catch (Exception e) {
            logger.error("Failed to update PageRank for URL: " + url, e);
        }
    }
}