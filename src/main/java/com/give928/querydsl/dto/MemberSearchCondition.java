package com.give928.querydsl.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@AllArgsConstructor
@Builder
@Getter
public class MemberSearchCondition {
    private String username;
    private String teamName;
    private Integer ageGoe;
    private Integer ageLoe;
}
