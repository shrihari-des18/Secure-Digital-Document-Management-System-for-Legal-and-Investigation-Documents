package com.shrihari.evault.common;

import com.shrihari.evault.model.RoleEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActorContext {

    private UUID actorId;
    private String username;
    private RoleEnum role;

    public static ActorContext system() {
        return ActorContext.builder()
                .actorId(UUID.fromString("00000000-0000-0000-0000-000000000001"))
                .username("SYSTEM")
                .role(RoleEnum.SYSTEM_ADMIN)
                .build();
    }
}
