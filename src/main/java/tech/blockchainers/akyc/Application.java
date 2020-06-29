package tech.blockchainers.akyc;

import org.springframework.util.StringUtils;
import tech.blockchainers.akyc.rest.AKYCController;
import tech.blockchainers.akyc.scheduler.AuditTask;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.stereotype.Service;
import org.web3j.contracts.eip20.generated.ERC20;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.http.HttpService;
import org.web3j.tx.gas.DefaultGasProvider;
import org.web3j.tx.gas.StaticGasProvider;

import java.math.BigInteger;

@SpringBootApplication
@Slf4j
@EnableScheduling
@Service
public class Application implements CommandLineRunner {

	@Value("${address.registry}")
	private String registryAddress;

	@Value("${address.token}")
	private String tokenAddress;
	@Value("${rpc.url}")
	private String rpcUrl;

	@Value("${check.claims}")
	private boolean checkClaims;

	@Value("${private.key.deployer}")
	private String privateKeyDeployer;

	private final AuditTask auditTask;
	private Credentials credentials;
	private final AKYCController AKYCController;

	public Application(AuditTask auditTask, AKYCController AKYCController) {
		this.auditTask = auditTask;
		this.AKYCController = AKYCController;
	}

	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
	}

	@Override
	public void run(String... args) throws Exception {
		this.credentials = CredentialsUtil.createFromPrivateKey(privateKeyDeployer);
		log.info("Key-Derived Ethereum address: " + credentials.getAddress());
		connectToLocalBlockchain();
	}

	private void connectToLocalBlockchain() throws Exception {
		if (StringUtils.isEmpty(rpcUrl)) {
			log.error("No JSON RPC-URL set, only valid in I-Test.");
			return;
		}
		Web3j httpWeb3 = Web3j.build(new HttpService(rpcUrl));
		auditTask.setWeb3(httpWeb3);
		TokenProspectRegistry registry;
		if (StringUtils.isEmpty(registryAddress)) {
			registry = TokenProspectRegistry.deploy(httpWeb3, credentials, new StaticGasProvider(DefaultGasProvider.GAS_PRICE, DefaultGasProvider.GAS_LIMIT)).send();
			AKYCController.setRegistryAddress(registry.getContractAddress());
			log.info("Deployed TokenPropectRegistry Contract: " + registry.getContractAddress());
		} else {
			registry = TokenProspectRegistry.load(registryAddress, httpWeb3, credentials, new StaticGasProvider(DefaultGasProvider.GAS_PRICE, DefaultGasProvider.GAS_LIMIT));
		}
		if (StringUtils.isEmpty(tokenAddress)) {
			UnlimitedCurrencyToken uct = UnlimitedCurrencyToken.deploy(httpWeb3, credentials, new StaticGasProvider(DefaultGasProvider.GAS_PRICE, DefaultGasProvider.GAS_LIMIT), "YST", "2").send();
			uct.mintToken(credentials.getAddress(), BigInteger.valueOf(10000)).send();
			tokenAddress = uct.getContractAddress();
			log.info("Deployed ERC20-UnlimitedCurrencyToken at " + tokenAddress + " and minted balance of " + uct.balanceOf(tokenAddress).send());
		}
		ERC20 token = ERC20.load(tokenAddress, httpWeb3, credentials, new StaticGasProvider(DefaultGasProvider.GAS_PRICE, DefaultGasProvider.GAS_LIMIT));
		auditTask.initTokenProspectRegistry(registry, token, checkClaims);
	}

}
