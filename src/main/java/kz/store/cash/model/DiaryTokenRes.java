package kz.store.cash.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record DiaryTokenRes(
    @JsonProperty("token") String token,
    @JsonProperty("id") Long id,
    @JsonProperty("fullName") String fullName,
    @JsonProperty("roles") List<String> roles
) {

}
