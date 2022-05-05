package com.give928.querydsl.repository;

import com.give928.querydsl.dto.MemberSearchCondition;
import com.give928.querydsl.dto.MemberTeamDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface MemberRepositoryQuerydslSupport {
    Page<MemberTeamDto> searchPageByQuerydslRepositorySupport(MemberSearchCondition condition, Pageable pageable);
}
