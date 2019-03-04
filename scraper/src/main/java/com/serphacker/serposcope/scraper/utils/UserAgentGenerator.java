package com.serphacker.serposcope.scraper.utils;

import java.io.IOException;
import java.security.SecureRandom;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;

public class UserAgentGenerator {

	private static final Logger LOG = LoggerFactory.getLogger(UserAgentGenerator.class);

	// IE 11 on Windows 10
	public final static String DEFAULT_DESKTOP_USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; WOW64; Trident/7.0; rv:11.0) like Gecko";

	// Safari 12 on iOS 12
	public final static String DEFAULT_MOBILE_USER_AGENT = "Mozilla/5.0 (iPhone; CPU iPhone OS 12_1 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/12.0 Mobile/15E148 Safari/604.1";

	private static ObjectMapper mapper = new ObjectMapper();

	private static ArrayNode desktopUserAgents;

	private static ArrayNode mobileUserAgents;

	private static final int TOTAL_WEIGHT = 100;

	static {
		try {
			desktopUserAgents = (ArrayNode) mapper
					.readTree(UserAgentGenerator.class.getResourceAsStream("/user-agents/desktop.json"));
			mobileUserAgents = (ArrayNode) mapper
					.readTree(UserAgentGenerator.class.getResourceAsStream("/user-agents/mobile.json"));
		} catch (IOException e) {
			LOG.error("Failed to parse user-agents.");
		}
	}

	public static String getUserAgent(boolean desktop) {
		String header = desktop ? DEFAULT_DESKTOP_USER_AGENT : DEFAULT_MOBILE_USER_AGENT;
		ArrayNode arrayNode = desktop ? desktopUserAgents : mobileUserAgents;
		SecureRandom rand = new SecureRandom();
		int r = rand.nextInt(TOTAL_WEIGHT);
		for (int i = 0; i < arrayNode.size(); i++) {
			JsonNode clientNode = arrayNode.get(i);
			int weight = clientNode.get("weight").asInt();
			if ((r = r - weight) < 0) {
				String os = clientNode.get("os").asText();
				ArrayNode browserNodes = (ArrayNode) clientNode.get("browsers");
				r = rand.nextInt(TOTAL_WEIGHT);
				for (int j = 0; j < browserNodes.size(); j++) {
					JsonNode browserNode = browserNodes.get(j);
					String browser = browserNode.get("browser").asText();
					weight = browserNode.get("weight").asInt();
					if ((r = r - weight) < 0) {
						ArrayNode versionNodes = (ArrayNode) browserNode.get("versions");
						r = rand.nextInt(TOTAL_WEIGHT);
						for (int k = 0; k < versionNodes.size(); k++) {
							JsonNode versionNode = versionNodes.get(k);
							String version = versionNode.get("version").asText();
							weight = versionNode.get("weight").asInt();
							if ((r = r - weight) < 0) {
								header = versionNode.get("header").asText();
								LOG.debug(
										"Generate User-Agent: device: {} ,OS: {} ,browser: {} ,version: {} ,header: [{}]",
										"mobile", os, browser, version, header);
								return header;
							}
						}
						break;
					}
				}
				break;
			}
		}
		LOG.debug("Use default User-Agent: device: {} ,header [{}]", desktop ? "desktop" : "mobile", header);
		return header;
	}

	public static void main(String[] args) {
		System.out.println(getUserAgent(true));
	}
}
