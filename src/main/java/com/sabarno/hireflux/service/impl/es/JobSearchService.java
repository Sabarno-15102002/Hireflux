package com.sabarno.hireflux.service.impl.es;

import java.util.List;

import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import lombok.RequiredArgsConstructor;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.stereotype.Service;

import com.sabarno.hireflux.dto.request.JobSearchRequest;
import com.sabarno.hireflux.entity.es.JobDocument;

@Service
@RequiredArgsConstructor
public class JobSearchService {

    private final ElasticsearchOperations elasticsearchOperations;

    public Page<JobDocument> search(JobSearchRequest request, Pageable pageable) {

        BoolQuery.Builder boolQuery = new BoolQuery.Builder();

        // Keyword search
        if (request.getKeyword() != null && !request.getKeyword().isBlank()) {

            boolQuery.must(m -> m.multiMatch(mm -> mm
                    .query(request.getKeyword())
                    .fields(
                            "title",
                            "description",
                            "requiredSkills"
                    )
            ));
        }

        // Location filter
        if (request.getLocation() != null && !request.getLocation().isBlank()) {

            boolQuery.filter(f -> f.match(m -> m
                    .field("location")
                    .query(request.getLocation())
            ));
        }

        Query query = Query.of(q -> q.bool(boolQuery.build()));

        NativeQuery nativeQuery = NativeQuery.builder()
                .withQuery(query)
                .withPageable(pageable)
                .build();

        SearchHits<JobDocument> searchHits =
                elasticsearchOperations.search(nativeQuery, JobDocument.class);

        List<JobDocument> jobs = searchHits.getSearchHits()
                .stream()
                .map(SearchHit::getContent)
                .toList();

        return new PageImpl<>(
                jobs,
                pageable,
                searchHits.getTotalHits()
        );
    }
}