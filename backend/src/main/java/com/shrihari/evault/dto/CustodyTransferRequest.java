package com.shrihari.evault.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustodyTransferRequest {

    @NotNull(message = "Recipient party (officer/analyst/court ID) is required")
    private UUID toPartyId;

    @NotBlank(message = "Custody transfer reason is mandatory for chain of custody compliance")
    @Size(max = 512, message = "Reason cannot exceed 512 characters")
    private String reason;
}
