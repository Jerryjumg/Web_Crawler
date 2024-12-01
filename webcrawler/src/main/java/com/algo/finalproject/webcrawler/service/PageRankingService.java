package com.algo.finalproject.webcrawler.service;

import lombok.AllArgsConstructor;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.stereotype.Service;
import java.util.Collection;

@Service
@AllArgsConstructor
public class PageRankingService {

    private Neo4jClient neo4jClient;

    public void updatePageRank(String url) {
        int inDegree = getInDegree(url);

        // 2. Calculate PageRank based on in-degree and other factors
        double pageRankScore = calculatePageRank(url, inDegree);

        // 3. Update PageRank property of the node in Neo4j
        updatePageRankInNeo4j(url, pageRankScore);
    }

    private int getInDegree(String url) {
        String cypherQuery = "MATCH (p:Url {address: $url}) " +
                "RETURN size((p)<--()) AS inDegree";
        return neo4jClient.query(cypherQuery)
                .bind(url).to("url")
                .fetchAs(Integer.class)
                .one()
                .orElse(0);
    }

    private double calculatePageRank(String url, int inDegree) {
        double dampingFactor = 0.85;
        double initialPageRank = 1.0;
        return (1 - dampingFactor) + dampingFactor * inDegree * initialPageRank;
    }

    private void updatePageRankInNeo4j(String url, double pageRankScore) {
        String cypherQuery = "MATCH (p:Url {address: $url}) SET p.pageRank = $pageRank";
        neo4jClient.query(cypherQuery)
                .bind(url).to("url")
                .bind(pageRankScore).to("pageRank")
                .run();
    }

    public Collection<String> getUrlsByInDegree() {
        String cypherQuery = "MATCH (p:Url) " +
                "RETURN p.address AS url, size((p)<--()) AS inDegree " +
                "ORDER BY inDegree DESC";
        return neo4jClient.query(cypherQuery)
                .fetchAs(String.class)
                .mappedBy((typeSystem, record) -> record.get("url").asString())
                .all();
    }
}