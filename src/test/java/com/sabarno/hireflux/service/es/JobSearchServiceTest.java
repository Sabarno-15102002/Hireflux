package com.sabarno.hireflux.service.es;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;

import com.sabarno.hireflux.dto.request.JobSearchRequest;
import com.sabarno.hireflux.entity.es.JobDocument;
import com.sabarno.hireflux.service.impl.es.JobSearchService;

import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;

@ExtendWith(MockitoExtension.class)
class JobSearchServiceTest {

    @Mock
    private ElasticsearchOperations elasticsearchOperations;
 
    @InjectMocks
    private JobSearchService jobSearchService;
 
    private Pageable pageable;
 
    @BeforeEach
    void setUp() {
        pageable = PageRequest.of(0, 10);
    }

    @SuppressWarnings("unchecked")
    private SearchHits<JobDocument> mockSearchHits(List<JobDocument> documents, long totalHits) {
        SearchHits<JobDocument> searchHits = mock(SearchHits.class);
        List<SearchHit<JobDocument>> hits = documents.stream()
                .map(doc -> {
                    SearchHit<JobDocument> hit = mock(SearchHit.class);
                    when(hit.getContent()).thenReturn(doc);
                    return hit;
                })
                .toList();
 
        when(searchHits.getSearchHits()).thenReturn(hits);
        when(searchHits.getTotalHits()).thenReturn(totalHits);
        return searchHits;
    }

    @Test
    void testSeach_shouldMapSearchHitsToPage_whenResultsExist() {
        JobDocument doc1 = new JobDocument();
        doc1.setTitle("Backend Engineer");
        JobDocument doc2 = new JobDocument();
        doc2.setTitle("Frontend Engineer");
 
        SearchHits<JobDocument> searchHits = mockSearchHits(List.of(doc1, doc2), 2);
        when(elasticsearchOperations.search(any(NativeQuery.class), eq(JobDocument.class)))
                .thenReturn(searchHits);
 
        JobSearchRequest request = new JobSearchRequest();
        Page<JobDocument> result = jobSearchService.search(request, pageable);
 
        assertEquals(2, result.getContent().size());
        assertEquals("Backend Engineer", result.getContent().get(0).getTitle());
        assertEquals("Frontend Engineer", result.getContent().get(1).getTitle());
        assertEquals(2, result.getTotalElements());
        assertEquals(pageable.getPageNumber(), result.getPageable().getPageNumber());
        assertEquals(pageable.getPageSize(), result.getPageable().getPageSize());
    }
 
    @Test
    void testSeach_shouldReturnEmptyPage_whenNoResultsFound() {
        SearchHits<JobDocument> searchHits = mockSearchHits(List.of(), 0);
        when(elasticsearchOperations.search(any(NativeQuery.class), eq(JobDocument.class)))
                .thenReturn(searchHits);
 
        JobSearchRequest request = new JobSearchRequest();
        Page<JobDocument> result = jobSearchService.search(request, pageable);
 
        assertTrue(result.getContent().isEmpty());
        assertEquals(0, result.getTotalElements());
    }

    @Test
    void testSeach_shouldAddMustMultiMatchClause_whenKeywordProvided() {
        stubEmptySearch();
 
        JobSearchRequest request = new JobSearchRequest();
        request.setKeyword("java developer");
 
        jobSearchService.search(request, pageable);
 
        BoolQuery boolQuery = captureBoolQuery();
        assertEquals(1, boolQuery.must().size());
        assertTrue(boolQuery.must().get(0).isMultiMatch());
        assertEquals("java developer", boolQuery.must().get(0).multiMatch().query());
        assertTrue(boolQuery.must().get(0).multiMatch().fields().contains("title"));
        assertTrue(boolQuery.must().get(0).multiMatch().fields().contains("description"));
        assertTrue(boolQuery.must().get(0).multiMatch().fields().contains("requiredSkills"));
    }
 
    @Test
    void testSeach_shouldNotAddMustClause_whenKeywordIsNull() {
        stubEmptySearch();
 
        JobSearchRequest request = new JobSearchRequest();
        request.setKeyword(null);
 
        jobSearchService.search(request, pageable);
 
        assertEquals(0, captureBoolQuery().must().size());
    }
 
    @Test
    void testSeach_shouldNotAddMustClause_whenKeywordIsBlank() {
        stubEmptySearch();
 
        JobSearchRequest request = new JobSearchRequest();
        request.setKeyword("   ");
 
        jobSearchService.search(request, pageable);
 
        assertEquals(0, captureBoolQuery().must().size());
    }

    @Test
    void testSeach_shouldAddLocationFilterClause_whenLocationProvided() {
        stubEmptySearch();
 
        JobSearchRequest request = new JobSearchRequest();
        request.setLocation("Remote");
 
        jobSearchService.search(request, pageable);
 
        BoolQuery boolQuery = captureBoolQuery();
        assertEquals(1, boolQuery.filter().size());
        assertTrue(boolQuery.filter().get(0).isMatch());
        assertEquals("location", boolQuery.filter().get(0).match().field());
    }
 
    @Test
    void testSeach_shouldNotAddFilterClause_whenLocationIsNull() {
        stubEmptySearch();
 
        JobSearchRequest request = new JobSearchRequest();
        request.setLocation(null);
 
        jobSearchService.search(request, pageable);
 
        assertEquals(0, captureBoolQuery().filter().size());
    }
 
    @Test
    void testSeach_shouldNotAddFilterClause_whenLocationIsBlank() {
        stubEmptySearch();
 
        JobSearchRequest request = new JobSearchRequest();
        request.setLocation("   ");
 
        jobSearchService.search(request, pageable);
 
        assertEquals(0, captureBoolQuery().filter().size());
    }
 
    @Test
    void testSeach_shouldAddBothClauses_whenKeywordAndLocationProvided() {
        stubEmptySearch();
 
        JobSearchRequest request = new JobSearchRequest();
        request.setKeyword("java developer");
        request.setLocation("Remote");
 
        jobSearchService.search(request, pageable);
 
        BoolQuery boolQuery = captureBoolQuery();
        assertEquals(1, boolQuery.must().size());
        assertEquals(1, boolQuery.filter().size());
    }
 
    @Test
    void testSeach_shouldProduceEmptyBoolQuery_whenNoFiltersProvided() {
        stubEmptySearch();
 
        JobSearchRequest request = new JobSearchRequest(); // no keyword, no location
 
        jobSearchService.search(request, pageable);
 
        BoolQuery boolQuery = captureBoolQuery();
        assertEquals(0, boolQuery.must().size());
        assertEquals(0, boolQuery.filter().size());
    }

    private void stubEmptySearch() {
        SearchHits<JobDocument> searchHits = mockSearchHits(List.of(), 0);
        when(elasticsearchOperations.search(any(NativeQuery.class), eq(JobDocument.class)))
                .thenReturn(searchHits);
    }
 
    private BoolQuery captureBoolQuery() {
        ArgumentCaptor<NativeQuery> queryCaptor = ArgumentCaptor.forClass(NativeQuery.class);
        verify(elasticsearchOperations).search(queryCaptor.capture(), eq(JobDocument.class));
 
        Query query = queryCaptor.getValue().getQuery();
        assertTrue(query.isBool(), "Expected a bool query to have been built");
        return query.bool();
    }
}
