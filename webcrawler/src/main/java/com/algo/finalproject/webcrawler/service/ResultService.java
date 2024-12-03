package com.algo.finalproject.webcrawler.service;

import lombok.AllArgsConstructor;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@AllArgsConstructor
public class ResultService {

    private final Neo4jClient neo4jClient;

    public List<String> getUrlsByPageRank() {
        String cypherQuery = "MATCH (p:Page) RETURN p.url AS url ORDER BY p.pageRank ASC";
        return (List<String>) neo4jClient.query(cypherQuery)
                .fetchAs(String.class)
                .all();
    }
}
