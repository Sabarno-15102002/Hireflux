package com.sabarno.hireflux.service.matching;

import com.sabarno.hireflux.entity.Job;
import com.sabarno.hireflux.entity.Resume;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class MatchContext {

    private Resume resume;
    private Job job;
}