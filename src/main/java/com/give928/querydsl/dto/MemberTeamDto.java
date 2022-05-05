package com.give928.querydsl.dto;

import com.querydsl.core.annotations.QueryProjection;
import lombok.Getter;

@Getter
public class MemberTeamDto {
    private final Long memberId;
    private final String username;
    private final int age;
    private final Long teamId;
    private final String teamName;

    @QueryProjection
    public MemberTeamDto(Long memberId, String username, int age, Long teamId, String teamName) {
        this.memberId = memberId;
        this.username = username;
        this.age = age;
        this.teamId = teamId;
        this.teamName = teamName;
    }
}
