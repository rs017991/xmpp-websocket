package com.example.demo;

import org.jivesoftware.smackx.bytestreams.socks5.Socks5Proxy;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class XmppWebSocketApplication {
	public static void main(String[] args) {
		/*
		 * disable the local SOCKS5 proxy in favor of a server proxy
		 * local proxies are problematic when going across networks let alone organizational boundaries
		 * Smack uses the "ibb" property to indicate that only IBB should be use.  The use of SOCKS5 server proxies
		 * still imposes a security risk as it is not encrypted by default.  
		 * NOTE: The use of IBB is suppose to be used only as a fallback, however it is common denominator that meets the 
		 * security requirements out of the box.  It is also VERY slow.
		 * TODO: Looking for an alternative XEP that performs better and meets security requirements.
		 */
		Socks5Proxy.setLocalSocks5ProxyEnabled(false);
		System.setProperty("ibb", "true");

		SpringApplication.run(XmppWebSocketApplication.class, args);
	}
}