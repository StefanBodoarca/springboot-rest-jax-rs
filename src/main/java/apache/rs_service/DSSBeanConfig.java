package apache.rs_service;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.sql.DataSource;

//Import log4j classes.
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ClassPathResource;

import eu.europa.esig.dss.alert.ExceptionOnStatusAlert;
import eu.europa.esig.dss.model.DSSException;
import eu.europa.esig.dss.service.crl.JdbcCacheCRLSource;
import eu.europa.esig.dss.service.crl.OnlineCRLSource;
import eu.europa.esig.dss.service.http.commons.CommonsDataLoader;
import eu.europa.esig.dss.service.http.commons.FileCacheDataLoader;
import eu.europa.esig.dss.service.http.commons.OCSPDataLoader;
import eu.europa.esig.dss.service.ocsp.JdbcCacheOCSPSource;
import eu.europa.esig.dss.service.ocsp.OnlineOCSPSource;
import eu.europa.esig.dss.spi.client.http.DSSFileLoader;
import eu.europa.esig.dss.spi.client.http.IgnoreDataLoader;
import eu.europa.esig.dss.spi.tsl.TrustedListsCertificateSource;
import eu.europa.esig.dss.spi.x509.KeyStoreCertificateSource;
import eu.europa.esig.dss.tsl.function.OfficialJournalSchemeInformationURI;
import eu.europa.esig.dss.tsl.job.TLValidationJob;
import eu.europa.esig.dss.tsl.source.LOTLSource;
import eu.europa.esig.dss.validation.CertificateVerifier;
import eu.europa.esig.dss.validation.CommonCertificateVerifier;
import eu.europa.esig.dss.ws.validation.common.RemoteDocumentValidationService;


@Configuration
@Import({ CXFConfig.class, PersistenceConfig.class })
public class DSSBeanConfig {
	private static final Logger LOG = LogManager.getLogger(DSSBeanConfig.class);

	@Value("${current.lotl.url}")
	private String lotlUrl;

	@Value("${lotl.country.code}")
	private String lotlCountryCode;

	@Value("${current.oj.url}")
	private String currentOjUrl;

	@Value("${oj.content.keystore.type}")
	private String ksType;

	@Value("${oj.content.keystore.filename}")
	private String ksFilename;

	@Value("${oj.content.keystore.password}")
	private String ksPassword;

	@Autowired
	private DataSource dataSource;
	
	@PostConstruct
	public void cachedCRLSourceInitialization() throws SQLException {
		JdbcCacheCRLSource jdbcCacheCRLSource = cachedCRLSource();
		jdbcCacheCRLSource.initTable();
	}
	
	@PostConstruct
	public void cachedOCSPSourceInitialization() throws SQLException {
		JdbcCacheOCSPSource jdbcCacheOCSPSource = cachedOCSPSource();
		jdbcCacheOCSPSource.initTable();
	}
	
	@PreDestroy
	public void cachedCRLSourceClean() throws SQLException {
		JdbcCacheCRLSource jdbcCacheCRLSource = cachedCRLSource();
		jdbcCacheCRLSource.destroyTable();
	}
	
	@PreDestroy
	public void cachedOCSPSourceClean() throws SQLException {
		JdbcCacheOCSPSource jdbcCacheOCSPSource = cachedOCSPSource();
		jdbcCacheOCSPSource.destroyTable();
	}
	
	@Bean
	public OnlineCRLSource onlineCRLSource() {
		OnlineCRLSource onlineCRLSource = new OnlineCRLSource();
		onlineCRLSource.setDataLoader(dataLoader());
		return onlineCRLSource;
	}
	
	@Bean
	public JdbcCacheCRLSource cachedCRLSource() {
		JdbcCacheCRLSource jdbcCacheCRLSource = new JdbcCacheCRLSource();
		jdbcCacheCRLSource.setDataSource(dataSource);
		jdbcCacheCRLSource.setProxySource(onlineCRLSource());
		jdbcCacheCRLSource.setDefaultNextUpdateDelay((long) (60 * 3)); // 3 minutes
		return jdbcCacheCRLSource;
	}
	
	@Bean
	public OCSPDataLoader ocspDataLoader() {
		OCSPDataLoader ocspDataLoader = new OCSPDataLoader();
		//ocspDataLoader.setProxyConfig(proxyConfig);
		return ocspDataLoader;
	}
	
	@Bean
	public OnlineOCSPSource onlineOcspSource() {
		OnlineOCSPSource onlineOCSPSource = new OnlineOCSPSource();
		onlineOCSPSource.setDataLoader(ocspDataLoader());
		return onlineOCSPSource;
	}
	
	@Bean
	public JdbcCacheOCSPSource cachedOCSPSource() {
		JdbcCacheOCSPSource jdbcCacheOCSPSource = new JdbcCacheOCSPSource();
		jdbcCacheOCSPSource.setDataSource(dataSource);
		jdbcCacheOCSPSource.setProxySource(onlineOcspSource());
		jdbcCacheOCSPSource.setDefaultNextUpdateDelay((long) (1000 * 60 * 3)); // 3 minutes
		return jdbcCacheOCSPSource;
	}
	
	@Bean
	public CommonsDataLoader dataLoader() {
		CommonsDataLoader dataLoader = new CommonsDataLoader();
		//dataLoader.setProxyConfig(proxyConfig);
		return dataLoader;
	}
	
	@Bean(name = "european-trusted-list-certificate-source")
	public TrustedListsCertificateSource trustedListSource() {
		return new TrustedListsCertificateSource();
	}
	
	@Bean
	public RemoteDocumentValidationService remoteValidationService() throws Exception {
		RemoteDocumentValidationService service = new RemoteDocumentValidationService();
		service.setVerifier(certificateVerifier());
		return service;
	}
	
	@Bean
	public CertificateVerifier certificateVerifier() throws Exception {
		CommonCertificateVerifier certificateVerifier = new CommonCertificateVerifier();
		certificateVerifier.setCrlSource(cachedCRLSource());
		certificateVerifier.setOcspSource(cachedOCSPSource());
		certificateVerifier.setDataLoader(dataLoader());
		certificateVerifier.setTrustedCertSources(trustedListSource());

		// Default configs
		certificateVerifier.setAlertOnMissingRevocationData(new ExceptionOnStatusAlert());
		certificateVerifier.setCheckRevocationForUntrustedChains(false);

		return certificateVerifier;
	}
	
	@Bean 
	public TLValidationJob job() {
		TLValidationJob job = new TLValidationJob();
		job.setTrustedListCertificateSource(trustedListSource());
		job.setListOfTrustedListSources(europeanLOTL());
		job.setOfflineDataLoader(offlineLoader());
		job.setOnlineDataLoader(onlineLoader());
		return job;
	}
	
	@Bean
	public DSSFileLoader onlineLoader() {
		FileCacheDataLoader onlineFileLoader = new FileCacheDataLoader();
		onlineFileLoader.setCacheExpirationTime(0);
		onlineFileLoader.setDataLoader(dataLoader());
		onlineFileLoader.setFileCacheDirectory(tlCacheDirectory());
		return onlineFileLoader;
	}
	
	@Bean(name = "european-lotl-source")
	public LOTLSource europeanLOTL() {
		LOTLSource lotlSource = new LOTLSource();
		lotlSource.setUrl(lotlUrl);
		lotlSource.setCertificateSource(ojContentKeyStore());
		lotlSource.setSigningCertificatesAnnouncementPredicate(new OfficialJournalSchemeInformationURI(currentOjUrl));
		lotlSource.setPivotSupport(true);
		return lotlSource;
	}
	
	@Bean
	public KeyStoreCertificateSource ojContentKeyStore() {
		try {
			return new KeyStoreCertificateSource(new ClassPathResource(ksFilename).getFile(), ksType, ksPassword);
		} catch (IOException e) {
			throw new DSSException("Unable to load the file " + ksFilename, e);
		}
	}
	
	@Bean
	public DSSFileLoader offlineLoader() {
		FileCacheDataLoader offlineFileLoader = new FileCacheDataLoader();
		offlineFileLoader.setCacheExpirationTime(Long.MAX_VALUE);
		offlineFileLoader.setDataLoader(new IgnoreDataLoader());
		offlineFileLoader.setFileCacheDirectory(tlCacheDirectory());
		return offlineFileLoader;
	}
	
	@Bean
	public File tlCacheDirectory() {
		File rootFolder = new File(System.getProperty("java.io.tmpdir"));
		File tslCache = new File(rootFolder, "dss-tsl-loader");
		if (tslCache.mkdirs()) {
			LOG.info("TL Cache folder : {}" + tslCache.getAbsolutePath());
		}
		return tslCache;
	}
}
