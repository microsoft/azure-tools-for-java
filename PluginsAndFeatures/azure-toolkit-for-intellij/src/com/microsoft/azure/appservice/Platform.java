package com.microsoft.azure.appservice;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class Platform {
    private String os;
    private String language;
    private String server;
}
