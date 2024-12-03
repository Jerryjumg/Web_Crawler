package com.algo.finalproject.webcrawler.services;

import com.algo.finalproject.webcrawler.service.ResultService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.neo4j.core.KRecordFetchSpec;
import org.springframework.data.neo4j.core.Neo4jClient;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ResultServiceTest {

    @Mock
    private ResultService resultService;

    @Mock
    private Neo4jClient neo4jClient;

    @InjectMocks
    private ResultService resultServiceTest;


    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        resultService = new ResultService(neo4jClient);
    }

    @Test
    public void testGetUrlsByPageRank() {
        String cypherQuery = "MATCH (p:Page) RETURN p.url AS url ORDER BY p.pageRank ASC";
        List<String> expectedResults = List.of("url1", "url2", "url3");

        Neo4jClient.UnboundRunnableSpec runnableSpec = mock(Neo4jClient.UnboundRunnableSpec.class);
        Neo4jClient.MappingSpec<String> fetchSpec = mock(Neo4jClient.MappingSpec.class);

        when(neo4jClient.query(cypherQuery)).thenReturn(runnableSpec); // query() returns UnboundRunnableSpec
        when(runnableSpec.fetchAs(String.class)).thenReturn(fetchSpec); // fetchAs() returns MappingSpec<String>
        when(fetchSpec.all()).thenReturn(expectedResults); // all() returns a list

        List<String> actualResults = resultService.getUrlsByPageRank();

        assertEquals(expectedResults, actualResults);
    }

    @Test
    public void testGetUrlsByPageRankEmpty() {
        String cypherQuery = "MATCH (p:Page) RETURN p.url AS url ORDER BY p.pageRank ASC";
        List<String> expectedResults = List.of();

        Neo4jClient.UnboundRunnableSpec runnableSpec = mock(Neo4jClient.UnboundRunnableSpec.class);
        Neo4jClient.MappingSpec<String> fetchSpec = mock(Neo4jClient.MappingSpec.class);

        when(neo4jClient.query(cypherQuery)).thenReturn(runnableSpec); // query() returns UnboundRunnableSpec
        when(runnableSpec.fetchAs(String.class)).thenReturn(fetchSpec); // fetchAs() returns MappingSpec<String>
        when(fetchSpec.all()).thenReturn(expectedResults); // all() returns a list

        List<String> actualResults = resultService.getUrlsByPageRank();

        assertEquals(expectedResults, actualResults);
    }

}
