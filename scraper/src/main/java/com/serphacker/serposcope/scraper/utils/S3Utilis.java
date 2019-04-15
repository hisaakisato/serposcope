package com.serphacker.serposcope.scraper.utils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.SocketException;
import java.net.URLEncoder;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.AbortedException;
import com.amazonaws.ClientConfiguration;
import com.amazonaws.SdkClientException;
import com.amazonaws.auth.AWSCredentialsProviderChain;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.serphacker.serposcope.scraper.google.GoogleDevice;
import com.serphacker.serposcope.scraper.google.GoogleScrapSearch;

public class S3Utilis {

	private static final Logger LOG = LoggerFactory.getLogger(S3Utilis.class);

	static volatile AmazonS3 s3;

	static String bucket;

	static DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy/MM/dd");

	static AWSCredentialsProviderChain chain = new DefaultAWSCredentialsProviderChain();

	static {
		bucket = System.getenv("SERPOSCOPE_UPLOAD_BUCKET");
		if (bucket == null) {
			bucket = System.getProperty("serposcope.upload.bucket", "jp.markeship.serposcope.serps");
		}
	}

	public static synchronized AmazonS3 getClient() {
		if (s3 == null) {
			ClientConfiguration config = new ClientConfiguration().withGzip(false);
			s3 = AmazonS3Client.builder().withRegion(Regions.AP_NORTHEAST_1).withCredentials(chain)
					.withClientConfiguration(config).build();
		}
		return s3;
	}

	public static void upload(LocalDate date, GoogleScrapSearch search, int page, String content) {
		try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
				GZIPOutputStream out = new GZIPOutputStream(baos)) {
			byte[] bytes = content.getBytes("UTF-8");
			out.write(bytes, 0, bytes.length);
			out.finish();

			if (date == null) {
				date = LocalDate.now();
			}
			String key = generateKey(date, search.getSearchId(), page);

			ObjectMetadata metadata = new ObjectMetadata();
			metadata.setContentEncoding("gzip");
			metadata.addUserMetadata("keyword", encode(search.getKeyword()));
			metadata.addUserMetadata("device", search.getDevice() == GoogleDevice.DESKTOP ? "PC" : "SP");
			metadata.addUserMetadata("local", encodeLocal(search.getLocal()));
			metadata.addUserMetadata("country", search.getCountry().name());
			metadata.addUserMetadata("custom-params", encodeParams(search.getCustomParameters()));

			PutObjectRequest request = new PutObjectRequest(bucket, key, new ByteArrayInputStream(baos.toByteArray()),
					metadata);
			// request.setStorageClass(StorageClass.OneZoneInfrequentAccess);
			getClient().putObject(request);
			LOG.info("[Serps archive] Uploaded. id: {} page: {} key: {}", search.getSearchId(), page, key);
		} catch (IOException e) {
			LOG.error(e.getMessage(), e);
		} catch (SdkClientException e) {
			if (e instanceof AbortedException) {
				LOG.warn("[Serps archive] Upload aborted.");
				return;
			}
			Throwable cause = e.getCause();
			if (cause != null && cause instanceof SocketException) {
				LOG.warn("[Serps archive] Retry upload.");
				s3 = null;
				upload(date, search, page, content);
				return;
			}
			LOG.error(e.getMessage(), e);
		}
	}

	public static void download(OutputStream out, LocalDate date, int searchId, int page) {
		String key = generateKey(date, searchId, page);
		S3Object s3obj = getClient().getObject(bucket, key);
		try (InputStream in = new GZIPInputStream(s3obj.getObjectContent())) {
			int len = 0;
			byte bytes[] = new byte[4096];
			while ((len = in.read(bytes)) > 0) {
				out.write(bytes, 0, len);
				out.flush();
			}
			LOG.info("[Serps archive] Downloaded. id: {} page: {} key: {}", searchId, page + 1, key);
		} catch (IOException e) {
			LOG.error(e.getMessage(), e);
		} catch (SdkClientException e) {
			if (e instanceof AbortedException) {
				LOG.warn("[Serps archive] Download aborted.");
				return;
			}
			Throwable cause = e.getCause();
			if (cause != null && cause instanceof SocketException) {
				LOG.warn("[Serps archive] Retry download.");
				s3 = null;
				download(out, date, searchId, page);
				return;
			}
			LOG.error(e.getMessage(), e);
		}
	}

	private static String generateKey(LocalDate date, int searchId, int page) {
		return String.format("%s/%s/serps_%d-%d.html", date.format(formatter), searchId, searchId, page);
	}

	private static String encode(String s) {
		if (s == null || s.isEmpty()) {
			return s;
		}
		try {
			return URLEncoder.encode(s, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			return s;
		}
	}

	private static String encodeLocal(String s) {
		if (s == null || s.isEmpty()) {
			return s;
		}
		String[] params = s.split(",");
		StringBuilder sb = new StringBuilder();
		for (String param : params) {
			if (param.isEmpty()) {
				continue;
			}
			sb.append(encode(EncodeUtils.forceASCII(param)).replace("+", " ")).append(",");
		}
		if (sb.length() > 0) {
			sb.deleteCharAt(sb.length() - 1);
		}
		return sb.toString();
	}

	private static String encodeParams(String s) {
		if (s == null || s.isEmpty()) {
			return s;
		}
		String[] params = s.split("&");
		StringBuilder sb = new StringBuilder();
		for (String param : params) {
			if (param.isEmpty()) {
				continue;
			}
			String[] tmp = param.split("=", 2);
			if (tmp.length >= 1) {
				sb.append(encode(tmp[0]));
			}
			if (tmp.length > 1) {
				sb.append("=").append(encode(tmp[1]));
			}
			sb.append("&");
		}
		if (sb.length() > 0) {
			sb.deleteCharAt(sb.length() - 1);
		}
		return sb.toString();
	}

}
