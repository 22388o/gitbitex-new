package com.gitbitex.restserver.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TotpUriDto {
    private String uri;
    private String secretKey;
}
