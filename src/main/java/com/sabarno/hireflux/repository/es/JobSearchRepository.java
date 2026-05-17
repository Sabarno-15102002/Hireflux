package com.sabarno.hireflux.repository.es;

import java.util.UUID;

import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import com.sabarno.hireflux.entity.es.JobDocument;

@Repository
public interface JobSearchRepository extends ElasticsearchRepository<JobDocument, UUID> {
}