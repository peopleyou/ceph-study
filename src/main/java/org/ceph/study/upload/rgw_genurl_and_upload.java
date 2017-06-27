package org.ceph.study.upload;

import com.amazonaws.*;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.S3ClientOptions;
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;

public class rgw_genurl_and_upload {
	public static String access_key = "U2A27HEBIS06RIF3JOBC";
	public static String secret_key = "C4UB88kkbR5QQhBEnKau2zUmkqxdttQOYSexIcDg";
	public static String endpoint = "192.168.30.11";
	public static String bucketname = "helloworld";
	public static String objectname = "2.png";

	static AmazonS3 client;

	public static void main(String[] args) throws IOException {

		AWSCredentials credentials = new BasicAWSCredentials(
				access_key, secret_key);
		ClientConfiguration clientconfig = new ClientConfiguration();
		clientconfig.setProtocol(Protocol.HTTP);

		S3ClientOptions client_options = new S3ClientOptions();
		client_options.setPathStyleAccess(true);

		client = new AmazonS3Client(credentials, clientconfig);
		client.setEndpoint(endpoint);
		client.setS3ClientOptions(client_options);

		URL url_str = generate_url(objectname);
//		upload_by_url(url_str,"D:/" + objectname);
	}
	public static URL generate_url(String objname) {

		try {
			java.util.Date expiration = new java.util.Date();

			long milliSeconds = expiration.getTime();
			milliSeconds += 1000 * 60 * 5;
			expiration.setTime(milliSeconds);

			GeneratePresignedUrlRequest genurl_req = new GeneratePresignedUrlRequest(
					bucketname, objname);
			genurl_req.setMethod(HttpMethod.PUT);
			genurl_req.setExpiration(expiration);
//			genurl_req.setContentType("image/jpeg");
			genurl_req.setContentType("image/png");
			genurl_req.addRequestParameter("x-amz-acl", "public-read");

			URL url = client.generatePresignedUrl(genurl_req);
			System.out.println(url.toString());

//			try {
//				upload_by_url(url, "D:/" + objectname);
//			} catch (IOException e) {
//				e.printStackTrace();
//			}

//			System.out.println(url.toString());
			return url;
		} catch (AmazonServiceException ase) {
			System.out.println("svr_error_message:" + ase.getMessage());
			System.out.println("svr_status_code:  " + ase.getStatusCode());
			System.out.println("svr_error_code:   " + ase.getErrorCode());
			System.out.println("svr_error_type:   " + ase.getErrorType());
			System.out.println("svr_request_id:   " + ase.getRequestId());
		} catch (AmazonClientException ace) {
			System.out.println("clt_error_message:" + ace.getMessage());
		}

		return null;
	}

	public static void upload_by_url(URL url, String file_path)
			throws IOException {

		BufferedInputStream instream = null;
		BufferedOutputStream outstream = null;
		try {
			HttpURLConnection http_conn = (HttpURLConnection) url
					.openConnection();
			http_conn.setDoOutput(true);
			http_conn.setRequestMethod("PUT");

			http_conn.setRequestProperty("Content-Type", "image/jpeg");
			http_conn.setRequestProperty("x-amz-acl", "public-read");

			outstream = new BufferedOutputStream(
					http_conn.getOutputStream());
			instream = new BufferedInputStream(new FileInputStream(
					new File(file_path)));

			byte[] buffer = new byte[1024];
			int offset = 0;
			while ((offset = instream.read(buffer)) != -1) {
				outstream.write(buffer, 0, offset);
				outstream.flush();
			}

			System.out.println("http_status : "
					+ http_conn.getResponseCode());
			System.out.println("http_headers: "
					+ http_conn.getHeaderFields());
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (null != outstream)
				outstream.close();
			if (null != instream)
				instream.close();
		}
	}
}
