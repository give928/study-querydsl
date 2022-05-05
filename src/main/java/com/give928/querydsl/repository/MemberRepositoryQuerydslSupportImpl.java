package com.give928.querydsl.repository;

import com.give928.querydsl.dto.MemberSearchCondition;
import com.give928.querydsl.dto.MemberTeamDto;
import com.give928.querydsl.dto.QMemberTeamDto;
import com.give928.querydsl.entity.Member;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.JPQLQuery;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.support.QuerydslRepositorySupport;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.util.StringUtils;

import java.util.List;

import static com.give928.querydsl.entity.QMember.member;
import static com.give928.querydsl.entity.QTeam.team;

public class MemberRepositoryQuerydslSupportImpl extends QuerydslRepositorySupport implements MemberRepositoryQuerydslSupport {
    public MemberRepositoryQuerydslSupportImpl() {
        super(Member.class);
    }

    public Page<MemberTeamDto> searchPageByQuerydslRepositorySupport(MemberSearchCondition condition, Pageable pageable) {
        JPQLQuery<MemberTeamDto> jpqlQuery = from(member)
                .select(new QMemberTeamDto(
                        member.id,
                        member.username,
                        member.age,
                        team.id,
                        team.name))
                .leftJoin(member.team, team)
                .where(usernameEq(condition.getUsername()),
                       teamNameEq(condition.getTeamName()),
                       ageGoe(condition.getAgeGoe()),
                       ageLoe(condition.getAgeLoe()))
                .orderBy(member.id.asc());

        JPQLQuery<MemberTeamDto> memberTeamDtoJPQLQuery = getQuerydsl().applyPagination(pageable, jpqlQuery);

        List<MemberTeamDto> content = memberTeamDtoJPQLQuery.fetch();

        JPQLQuery<Long> countQuery = getMemberCountQuery(condition);

        return PageableExecutionUtils.getPage(content, pageable, countQuery::fetchOne);
    }

    private JPQLQuery<Long> getMemberCountQuery(MemberSearchCondition condition) {
        return from(member)
                .select(member.count())
                .leftJoin(member.team, team)
                .where(usernameEq(condition.getUsername()),
                       teamNameEq(condition.getTeamName()),
                       ageGoe(condition.getAgeGoe()),
                       ageLoe(condition.getAgeLoe()));
    }

    private BooleanExpression usernameEq(String username) {
        return StringUtils.hasText(username) ? member.username.eq(username) : null;
    }

    private BooleanExpression teamNameEq(String teamName) {
        return StringUtils.hasText(teamName) ? team.name.eq(teamName) : null;
    }

    private BooleanExpression ageGoe(Integer ageGoe) {
        return ageGoe != null ? member.age.goe(ageGoe) : null;
    }

    private BooleanExpression ageLoe(Integer ageLoe) {
        return ageLoe != null ? member.age.loe(ageLoe) : null;
    }
}
