package tech.blockchainers.akyc.service;

import lombok.extern.slf4j.Slf4j;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.Hash;
import org.web3j.crypto.Sign;
import org.web3j.utils.Numeric;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

@Slf4j
public class SignatureService {

    private final Credentials credentials;

    public SignatureService(Credentials credentials) {
        this.credentials = credentials;
    }

    public String sign(String message) {
        byte[] proof = createProof(message);

        // sign
        Sign.SignatureData signature = Sign.signMessage(proof, credentials.getEcKeyPair(), true);
        ByteBuffer sigBuffer = ByteBuffer.allocate(signature.getR().length + signature.getS().length + 1);
        sigBuffer.put(signature.getR());
        sigBuffer.put(signature.getS());
        sigBuffer.put(signature.getV());

        log.info(String.format("hashed message: %s", Numeric.toHexString(Hash.sha3(proof))));
        log.info(String.format("signed proof: %s", Numeric.toHexString(sigBuffer.array())));
        return Numeric.toHexString(sigBuffer.array());
    }

    public byte[] createProof(String message) {
        byte[] messageAsByteArray = message.getBytes(StandardCharsets.UTF_8);
        log.info(String.format("hashed plain: %s", Numeric.toHexString(Hash.sha3(messageAsByteArray))));
        byte[] ethPrefixMessage = "\u0019Ethereum Signed Message:\n".concat(String.valueOf(messageAsByteArray.length)).getBytes(StandardCharsets.UTF_8);
        ByteBuffer sigBuffer = ByteBuffer.allocate(ethPrefixMessage.length + messageAsByteArray.length);
        sigBuffer.put(ethPrefixMessage);
        sigBuffer.put(messageAsByteArray);
        return sigBuffer.array();
    }

}