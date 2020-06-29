package tech.blockchainers.akyc.rest;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.http.HttpService;
import org.web3j.tx.gas.DefaultGasProvider;
import org.web3j.tx.gas.StaticGasProvider;
import org.web3j.utils.Numeric;
import tech.blockchainers.akyc.CredentialsUtil;
import tech.blockchainers.akyc.TokenProspectRegistry;
import tech.blockchainers.akyc.service.SignatureService;

import javax.annotation.PostConstruct;

@RestController
public class AKYCController {

    private Web3j httpWeb3;
    @Value("${rpc.url}")
    private String rpcUrl;
    @Value("${address.registry}")
    private String registryAddress;

    @Value("${signature.message}")
    private String signatureMessage;

    @PostConstruct
    public void init() {
        this.httpWeb3 = Web3j.build(new HttpService(rpcUrl));
    }

    @PostMapping("/createAccount")
    public AccountDto createAccount() throws Exception {
        Credentials signer = CredentialsUtil.create();
        return new AccountDto(Numeric.toHexStringWithPrefix(signer.getEcKeyPair().getPrivateKey()), signer.getAddress());
    }

    @GetMapping("/signMessage")
    public SignatureDto signMessage(@RequestParam String privateKey) throws Exception {
        Credentials signer = CredentialsUtil.createFromPrivateKey(privateKey);
        SignatureService signatureService = new SignatureService(signer);
        String message = signatureMessage.replaceAll("#addr#", signer.getAddress().toLowerCase());
        String signature = signatureService.sign(message);
        return new SignatureDto(Numeric.toHexStringWithPrefix(signer.getEcKeyPair().getPrivateKey()), signer.getAddress(), message, signature);
    }

    @PostMapping("/signMessageAndRegister")
    public SignatureDto signMessageAndRegister(@RequestParam String privateKey) throws Exception {
        Credentials signer = CredentialsUtil.createFromPrivateKey(privateKey);
        SignatureService signatureService = new SignatureService(signer);
        String message = signatureMessage.replaceAll("#addr#", signer.getAddress().toLowerCase());
        String signature = signatureService.sign(message);
        SignatureDto signatureDto = new SignatureDto(Numeric.toHexStringWithPrefix(signer.getEcKeyPair().getPrivateKey()), signer.getAddress(), message, signature);
        TokenProspectRegistry registry = TokenProspectRegistry.load(registryAddress, httpWeb3, signer, new StaticGasProvider(DefaultGasProvider.GAS_PRICE, DefaultGasProvider.GAS_LIMIT));
        registry.registerProspect(Numeric.hexStringToByteArray(signature)).send();
        return signatureDto;
    }

    public void setRegistryAddress(String registryAddress) {
        this.registryAddress = registryAddress;
    }
}
