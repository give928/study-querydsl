package com.give928.querydsl.dto;

import com.querydsl.core.annotations.QueryProjection;
import lombok.*;

@NoArgsConstructor(access = AccessLevel.PUBLIC)
@Getter
@Setter
@ToString
public class MemberDto {
    private String username;
    private int age;

    @QueryProjection
    public MemberDto(String username, int age) {
        this.username = username;
        this.age = age;
    }
}
