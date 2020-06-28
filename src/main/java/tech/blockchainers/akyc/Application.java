package tech.blockchainers.akyc;

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

import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;

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

	@Value("${private.key.deployer}")
	private String privateKeyDeployer;

	private final AuditTask auditTask;
	private Credentials credentials;

	public Application(AuditTask auditTask) {
		this.auditTask = auditTask;
	}

	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
	}

	@Override
	public void run(String... args) throws Exception {
		//String privateKey1 = "4CF4863EDE071AE98D932B0F91695C92F0FB970C7E4587A5801E78E93E59128C" // has the YST tokens!
		//String privateKey1 = "D0FACAF2F1D51A1A994432B5A6080FC1449598D43255A05D20CC07C7C7857094";
		//String privateKey = "710404145a788a5f2b7b6678f894a8ba621bdf8f4c04b44a3f703159916d39df"; // Ganache 1.
		this.credentials = CredentialsUtil.createFromPrivateKey(privateKeyDeployer);
		log.info("Key-Derived Ethereum address: " + credentials.getAddress());
		connectToLocalBlockchain();
	}

	private void connectToLocalBlockchain() throws IOException, InvalidAlgorithmParameterException, NoSuchAlgorithmException, NoSuchProviderException {
		//httpWeb3 = Web3j.build(new HttpService("https://rinkeby.infura.io/v3/b9498984bbf8462e9bc192f7679b6a75", createOkHttpClient()));
		Web3j httpWeb3 = Web3j.build(new HttpService(rpcUrl));
		auditTask.setWeb3(httpWeb3);
		TokenProspectRegistry registry = TokenProspectRegistry.load(registryAddress, httpWeb3, credentials, new StaticGasProvider(DefaultGasProvider.GAS_PRICE, DefaultGasProvider.GAS_LIMIT));
		ERC20 token = ERC20.load(tokenAddress, httpWeb3, credentials, new StaticGasProvider(DefaultGasProvider.GAS_PRICE, DefaultGasProvider.GAS_LIMIT));
		auditTask.initTokenProspectRegistry(registry, token);
	}

}
