package tech.blockchainers.akyc.rest;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AccountDto {

    private String privateKey;
    private String address;

}
