package com.algo.finalproject.webcrawler.services;

import com.algo.finalproject.webcrawler.service.CrawlerService;
import com.algo.finalproject.webcrawler.service.PageRankingService;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import org.neo4j.driver.summary.ResultSummary;
import org.springframework.data.neo4j.core.Neo4jClient;

import java.io.IOException;
import java.util.ArrayList;

import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


public class CrawlerServiceTest {

    @Mock
    private CrawlerService crawlerService;

    @Mock
    private Neo4jClient neo4jClient;

    @Mock
    private PageRankingService pageRankingService;

    @Mock
    private Document mockDocument;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        crawlerService = new CrawlerService(neo4jClient, pageRankingService,2);
    }

    @Test
    public void testStartCrawling() {
        // Test the startCrawling method

        ArrayList<String> urls = new ArrayList<>();
        urls.add("https://i.sstatic.net/ZKh9e.png");
        urls.add("https://i.sstatic.net/ZKh9e.png");
        urls.add("https://i.sstatic.net/ZKh9e.png");

        String cypherQuery1 = """
                  MERGE (u1:Page {url: $currentUrl})
                  SET u1.pageRank = $initialPageRank
                """;

        String cypherQuery2 = """
                MATCH (n) DETACH DELETE n;
                """;

        Neo4jClient.UnboundRunnableSpec runnableSpec = mock(Neo4jClient.UnboundRunnableSpec.class);
        Neo4jClient.OngoingBindSpec ongoingBindSpec = mock(Neo4jClient.OngoingBindSpec.class);
        Neo4jClient.RunnableSpec runnableSpec1 = mock(Neo4jClient.RunnableSpec.class);
        ResultSummary resultSummary = mock(ResultSummary.class);

        when(neo4jClient.query(cypherQuery2)).thenReturn(runnableSpec);
        when(runnableSpec.run()).thenReturn(resultSummary);

        when(neo4jClient.query(cypherQuery1)).thenReturn(runnableSpec);
        when(runnableSpec.bind(anyString())).thenReturn(ongoingBindSpec);
        when(ongoingBindSpec.to("currentUrl")).thenReturn(runnableSpec1);
        when(runnableSpec1.bind(anyFloat())).thenReturn(ongoingBindSpec);
        when(ongoingBindSpec.to("initialPageRank")).thenReturn(runnableSpec1);
        when(runnableSpec1.run()).thenReturn(resultSummary);
        crawlerService.startCrawling(urls,1);

        assert(crawlerService.isCrawlInProgress());
    }

    @Test
    public void testStartCrawlingInProgress() throws IOException {

        ArrayList<String> urls = new ArrayList<>();
        urls.add("https://www.metmuseum.org/");

        String cypherQuery1 = """
                  MERGE (u1:Page {url: $currentUrl})
                  SET u1.pageRank = $initialPageRank
                """;

        String cypherQuery2 = """
                MATCH (n) DETACH DELETE n;
                """;
        Neo4jClient.UnboundRunnableSpec runnableSpec2 = mock(Neo4jClient.UnboundRunnableSpec.class);
        ResultSummary resultSummary1 = mock(ResultSummary.class);

        when(neo4jClient.query(cypherQuery2)).thenReturn(runnableSpec2);
        when(runnableSpec2.run()).thenReturn(resultSummary1);

        Neo4jClient.UnboundRunnableSpec runnableSpec = mock(Neo4jClient.UnboundRunnableSpec.class);
        Neo4jClient.OngoingBindSpec ongoingBindSpec = mock(Neo4jClient.OngoingBindSpec.class);
        Neo4jClient.RunnableSpec runnableSpec1 = mock(Neo4jClient.RunnableSpec.class);
        ResultSummary resultSummary = mock(ResultSummary.class);

        when(neo4jClient.query(cypherQuery1)).thenReturn(runnableSpec);
        when(runnableSpec.bind(anyString())).thenReturn(ongoingBindSpec);
        when(ongoingBindSpec.to("currentUrl")).thenReturn(runnableSpec1);
        when(runnableSpec1.bind(anyFloat())).thenReturn(ongoingBindSpec);
        when(ongoingBindSpec.to("initialPageRank")).thenReturn(runnableSpec1);
        when(runnableSpec1.run()).thenReturn(resultSummary);

        crawlerService.startCrawling(urls,2);
        assert(crawlerService.isCrawlInProgress());
    }

}
