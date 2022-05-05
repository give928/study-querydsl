package com.give928.querydsl.repository;

import com.give928.querydsl.dto.MemberSearchCondition;
import com.give928.querydsl.dto.MemberTeamDto;
import com.give928.querydsl.dto.QMemberTeamDto;
import com.querydsl.core.QueryResults;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.util.StringUtils;

import java.util.List;

import static com.give928.querydsl.entity.QMember.member;
import static com.give928.querydsl.entity.QTeam.team;

@RequiredArgsConstructor
public class MemberRepositoryCustomImpl implements MemberRepositoryCustom {
    private final JPAQueryFactory queryFactory;

    @Override
    public List<MemberTeamDto> search(MemberSearchCondition condition) {
        return queryFactory
                .select(new QMemberTeamDto(
                        member.id,
                        member.username,
                        member.age,
                        team.id,
                        team.name))
                .from(member)
                .leftJoin(member.team, team)
                .where(usernameEq(condition.getUsername()),
                       teamNameEq(condition.getTeamName()),
                       ageGoe(condition.getAgeGoe()),
                       ageLoe(condition.getAgeLoe()))
                .orderBy(member.id.asc())
                .fetch();
    }

    /**
     * 단순한 페이징, fetchResults() 사용
     */
    @Override
    public Page<MemberTeamDto> searchPageSimple(MemberSearchCondition condition, Pageable pageable) {
        QueryResults<MemberTeamDto> results = queryFactory
                .select(new QMemberTeamDto(
                        member.id,
                        member.username,
                        member.age,
                        team.id,
                        team.name))
                .from(member)
                .leftJoin(member.team, team)
                .where(usernameEq(condition.getUsername()),
                       teamNameEq(condition.getTeamName()),
                       ageGoe(condition.getAgeGoe()),
                       ageLoe(condition.getAgeLoe()))
                .orderBy(member.id.asc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetchResults(); // deprecated

        List<MemberTeamDto> content = results.getResults();
        long total = results.getTotal();

        return new PageImpl<>(content, pageable, total);
    }

    /**
     * 복잡한 페이징
     * 데이터 조회 쿼리와, 전체 카운트 쿼리를 분리
     */
    @Override
    public Page<MemberTeamDto> searchPageComplex(MemberSearchCondition condition, Pageable pageable) {
        List<MemberTeamDto> content = queryFactory
                .select(new QMemberTeamDto(
                        member.id,
                        member.username,
                        member.age,
                        team.id,
                        team.name))
                .from(member)
                .leftJoin(member.team, team)
                .where(usernameEq(condition.getUsername()),
                       teamNameEq(condition.getTeamName()),
                       ageGoe(condition.getAgeGoe()),
                       ageLoe(condition.getAgeLoe()))
                .orderBy(member.id.asc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        JPAQuery<Long> countQuery = getMemberCountQuery(condition);

//        return new PageImpl<>(content, pageable, total);
        return PageableExecutionUtils.getPage(content, pageable, countQuery::fetchOne);
    }

    private JPAQuery<Long> getMemberCountQuery(MemberSearchCondition condition) {
        return queryFactory
                .select(member.count())
                .from(member)
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

    // 조합해서 사용 가능
    private BooleanExpression ageBetween(Integer ageGoe, Integer ageLoe) {
        BooleanExpression ageGoeExpression = ageGoe(ageGoe);
        BooleanExpression ageLoeExpression = ageLoe(ageLoe);
        if (ageGoeExpression == null) {
            return ageLoeExpression;
        }
        return ageGoeExpression.and(ageLoeExpression);
    }
}
