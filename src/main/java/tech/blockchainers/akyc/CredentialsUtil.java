package tech.blockchainers.akyc;

import lombok.extern.slf4j.Slf4j;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.ECKeyPair;
import org.web3j.crypto.Keys;
import org.web3j.utils.Numeric;

import java.math.BigInteger;
import java.security.InvalidAlgorithmParameterException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;

@Slf4j
public class CredentialsUtil {

    public static Credentials create() throws InvalidAlgorithmParameterException, NoSuchAlgorithmException, NoSuchProviderException {
        // create new private/public key pair
        ECKeyPair keyPair = Keys.createEcKeyPair();

        BigInteger publicKey = keyPair.getPublicKey();
        String publicKeyHex = Numeric.toHexStringWithPrefix(publicKey);

        BigInteger privateKey = keyPair.getPrivateKey();
        String privateKeyHex = Numeric.toHexStringWithPrefix(privateKey);

        // create credentials + address from private/public key pair
        Credentials credentials = Credentials.create(new ECKeyPair(privateKey, publicKey));
        String address = credentials.getAddress();

        // print resulting data of new account
        log.debug("private key: '" + privateKeyHex + "'");
        log.debug("public key: '" + publicKeyHex + "'");
        log.debug("address: '" + address + "'\n");
        return credentials;
    }

    public static Credentials createFromPrivateKey(String privateKeyHex) {
        return 	Credentials.create(privateKeyHex);
    }

}
