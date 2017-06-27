package org.ceph.study.upload;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.httpclient.util.DateUtil;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import sun.misc.BASE64Encoder;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class rgw_rest_put_file {
	private static final Logger logger = Logger
			.getLogger(rgw_rest_put_file.class);

	 static String accessKey = "U2A27HEBIS06RIF3JOBC";
	 static String secretKey = "C4UB88kkbR5QQhBEnKau2zUmkqxdttQOYSexIcDg";
	 static String bucket = "helloworld";
	 static String endPoint = "http://192.168.30.11/";

	public static void main(String[] args) throws Exception {

//		File localFile = new File("D:/1.jpg");
//		putObject(localFile);

        System.out.println(md5file(new File("D:/2.png")));
//
//        String md5 = "5fd6e6fdca4f97b13cc8d5b668ca25dc";
//        BASE64Encoder encoder = new BASE64Encoder();
//        System.out.println(encoder.encode(md5.getBytes()));
    }

	public static void putObject(File localFile) throws Exception {
		HttpURLConnection conn = null;
		BufferedInputStream in = null;
		BufferedOutputStream out = null;
		try {
			URL url = new URL(endPoint + bucket + "/" + localFile.getName());
			conn = (HttpURLConnection) url.openConnection();
			conn.setRequestMethod("PUT");
			conn.setDoOutput(true);
			conn.setDoInput(true);

			Map<String,String> meta = new HashMap<String,String>();
	        meta.put("x-amz-acl", "public-read");
	        meta.put("x-amz-meta-title", "my-title-str");

			String contentMD5 = md5file(localFile);                 //需要由前端计算后传给服务端
			System.out.println("ContentMD5: " + contentMD5);

			Path source = Paths.get(localFile.getAbsolutePath());
			String contentType = Files.probeContentType(source);    //需要由前端计算后传给服务端

			System.out.println(contentType);

			Date date = new Date();
			String dateString = DateUtil.formatDate(date,
					DateUtil.PATTERN_RFC1036);
			String sign = sign("PUT", contentMD5, contentType, dateString, "/"
					+ bucket + "/" + localFile.getName(), meta);
			conn.setRequestProperty("Date", dateString);
			conn.setRequestProperty("Authorization", sign);
			conn.setRequestProperty("Content-Type", contentType);
			conn.setRequestProperty("Content-MD5", contentMD5);
			conn.setRequestProperty("x-amz-acl", "public-read");
			conn.setRequestProperty("x-amz-meta-title", "my-title-str");

			out = new BufferedOutputStream(conn.getOutputStream());
			in = new BufferedInputStream(new FileInputStream(localFile));

			byte[] buffer = new byte[1024];
			int p = 0;
			while ((p = in.read(buffer)) != -1) {
				out.write(buffer, 0, p);
				out.flush();
			}

			int status = conn.getResponseCode();
			System.out.println("http status: " + status);
			System.out.println("after:\n" + conn.getHeaderFields());

		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		} finally {
			close(in);
			close(out);
		}
	}

	private static void close(Closeable c) {
		try {
			if (c != null) {
				c.close();
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public static String md5file(File file) throws Exception {
		MessageDigest messageDigest = MessageDigest.getInstance("MD5");
		BufferedInputStream in = new BufferedInputStream(new FileInputStream(
				file));
		byte[] buf = new byte[1024 * 100];
		int p = 0;
		while ((p = in.read(buf)) != -1) {
			messageDigest.update(buf, 0, p);
		}
		in.close();
		byte[] digest = messageDigest.digest();

        System.out.println(new String(digest));

		BASE64Encoder encoder = new BASE64Encoder();
		return encoder.encode(digest);
	}

	public static String sign(String httpVerb, String contentMD5,
			String contentType, String date, String resource,
			Map<String, String> metas) {

		String stringToSign = httpVerb + "\n"
				+ StringUtils.trimToEmpty(contentMD5) + "\n"
				+ StringUtils.trimToEmpty(contentType) + "\n" + date + "\n";
		if (metas != null) {
			for (Map.Entry<String, String> entity : metas.entrySet()) {
				stringToSign += StringUtils.trimToEmpty(entity.getKey()) + ":"
						+ StringUtils.trimToEmpty(entity.getValue()) + "\n";
			}
		}
		stringToSign += resource;
		try {
			Mac mac = Mac.getInstance("HmacSHA1");
			byte[] keyBytes = secretKey.getBytes("UTF8");
			SecretKeySpec signingKey = new SecretKeySpec(keyBytes, "HmacSHA1");
			mac.init(signingKey);
			byte[] signBytes = mac.doFinal(stringToSign.getBytes("UTF8"));
			String signature = encodeBase64(signBytes);
			return "AWS" + " " + accessKey + ":" + signature;
		} catch (Exception e) {
			throw new RuntimeException("MAC CALC FAILED.");
		}

	}

	private static String encodeBase64(byte[] data) {
		String base64 = new String(Base64.encodeBase64(data));
		if (base64.endsWith("\r\n"))
			base64 = base64.substring(0, base64.length() - 2);
		if (base64.endsWith("\n"))
			base64 = base64.substring(0, base64.length() - 1);

		return base64;
	}

}