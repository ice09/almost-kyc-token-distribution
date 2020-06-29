package tech.blockchainers.akyc.scheduler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.java.Log;
import org.apache.commons.io.IOUtils;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.web3j.abi.EventEncoder;
import org.web3j.contracts.eip20.generated.ERC20;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.Request;
import org.web3j.protocol.core.methods.request.EthFilter;
import org.web3j.protocol.core.methods.response.EthLog;
import tech.blockchainers.akyc.TokenProspectRegistry;

import java.io.IOException;
import java.math.BigInteger;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Component
@Log
public class AuditTask {

    private Web3j httpWeb3;
    private EthFilter filterReg;

    private TokenProspectRegistry tokenProspectRegistry;
    private ERC20 token;

    private String jsonUrl = "https://ipfs.3box.io/profile?address=";
    private boolean checkClaims;
    private BigInteger lastBlockNum = BigInteger.ZERO;

    @Scheduled(fixedRate = 10000)
    public void auditEvents() throws Exception {
        if (tokenProspectRegistry == null) {
            return;
        }
        log.info("Auditing events.");
        Request<?, EthLog> resReg = httpWeb3.ethGetLogs(filterReg);
        List<EthLog.LogResult> regLogs = resReg.send().getLogs();
        if (regLogs.size()>0) {
            org.web3j.protocol.core.methods.response.Log lastLogEntry = ((EthLog.LogObject) regLogs.get(regLogs.size() - 1)).get();
            if (lastLogEntry.getBlockNumber().compareTo(lastBlockNum) > 0) {
                List<String> ethLogTopics = lastLogEntry.getTopics();
                String prospect = "0x" + ethLogTopics.get(1).substring(26);
                List<String> registrations = readJsonFromUrl(prospect);
                sendTokens(registrations);
                lastBlockNum = lastLogEntry.getBlockNumber();
            } else {
                log.warning("Old Log Entries detected, skipping.");
            }
        }
    }

    private void sendTokens(List<String> registrations) {
        for (String registration : registrations) {
            try {
                token.transfer(registration, BigInteger.valueOf(100)).send();
                BigInteger balance = token.balanceOf(registration).send();
                log.info("Balance of address " + registration + " is " + balance);
                if (balance.compareTo(BigInteger.ZERO) == 0) {
                    throw new IllegalStateException("Balance of address " + registration + " is 0.");
                }
            } catch (Exception e) {
                log.severe("Could not send Token: " + e.getMessage());
            }
        }
    }


    private List<String> readJsonFromUrl(String prospect) throws IOException, URISyntaxException {
        List<String> registrations = new ArrayList<>();
        if (!checkClaims) {
            registrations.add(prospect);
            return registrations;
        }
        try {
            String json = IOUtils.toString(new URI(jsonUrl + prospect), StandardCharsets.UTF_8);
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode parser = objectMapper.readTree(json);
            if (!StringUtils.isEmpty(parser.path("proof_twitter").asText())) {
                log.info("Found Twitter claim for address " + prospect);
                registrations.add(prospect);
            } else if (!StringUtils.isEmpty(parser.path("proof_github").asText())) {
                log.info("Found GitHub claim for address " + prospect);
                registrations.add(prospect);
            } else {
                log.warning("No Twitter or GitHub claim for address " + prospect);
            }
        } catch (Exception ex) {
            log.severe("Cannot derive Twitter nor GitHub claims for address " + prospect);
        }
        return registrations;
    }

    public void setWeb3(Web3j httpWeb3) throws IOException {
        this.httpWeb3 = httpWeb3;
    }

    public void initTokenProspectRegistry(TokenProspectRegistry tokenProspectRegistry, ERC20 token, boolean checkClaims) throws IOException {
        this.tokenProspectRegistry = tokenProspectRegistry;
        this.token = token;
        this.checkClaims = checkClaims;
        //For historic search, adjust start block number: EthBlockNumber currentBlockNum = httpWeb3.ethBlockNumber().send();
        this.filterReg = new EthFilter(DefaultBlockParameterName.LATEST, DefaultBlockParameterName.LATEST, tokenProspectRegistry.getContractAddress());
        String encodedEventSignatureReg = EventEncoder.encode(TokenProspectRegistry.REGISTERED_EVENT);
        filterReg.addSingleTopic(encodedEventSignatureReg);
    }
}
