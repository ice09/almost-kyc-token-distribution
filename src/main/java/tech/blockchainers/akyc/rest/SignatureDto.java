package tech.blockchainers.akyc.rest;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class SignatureDto {

    private String privateKey;
    private String address;
    private String message;
    private String signature;

}
