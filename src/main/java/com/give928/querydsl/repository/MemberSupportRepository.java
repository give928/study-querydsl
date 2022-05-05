package com.give928.querydsl.repository;

import com.give928.querydsl.dto.MemberSearchCondition;
import com.give928.querydsl.entity.Member;
import com.give928.querydsl.repository.support.Querydsl5RepositorySupport;
import com.querydsl.core.types.dsl.BooleanExpression;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.util.List;

import static com.give928.querydsl.entity.QMember.member;
import static com.give928.querydsl.entity.QTeam.team;

@Repository
public class MemberSupportRepository extends Querydsl5RepositorySupport<Member> {
    public MemberSupportRepository() {
        super(Member.class);
    }

    public List<Member> basicSelect() {
        return select(member)
                .from(member)
                .fetch();
    }

    public List<Member> basicSelectFrom() {
        return selectFrom(member)
                .fetch();
    }

    public Page<Member> applyPagination(MemberSearchCondition condition, Pageable pageable) {
        return applyPagination(pageable,
                               contentQuery -> contentQuery
                                       .selectFrom(member)
                                       .leftJoin(member.team, team)
                                       .where(usernameEq(condition.getUsername()),
                                              teamNameEq(condition.getTeamName()),
                                              ageGoe(condition.getAgeGoe()),
                                              ageLoe(condition.getAgeLoe())),
                               countQuery -> countQuery
                                       .select(member.count())
                                       .from(member)
                                       .leftJoin(member.team, team)
                                       .where(usernameEq(condition.getUsername()),
                                              teamNameEq(condition.getTeamName()),
                                              ageGoe(condition.getAgeGoe()),
                                              ageLoe(condition.getAgeLoe()))
        );
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
