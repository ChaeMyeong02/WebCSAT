package com.CBTServer.WebCSAT.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class UserBaseStatsDTO {
    private long totalUsers;
    private long thisMonthNewUsers;
    private long lastMonthNewUsers;
}
