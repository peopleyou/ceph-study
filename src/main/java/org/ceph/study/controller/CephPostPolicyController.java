package org.ceph.study.controller;

import com.aliyun.oss.ClientException;
import com.aliyun.oss.common.auth.HmacSHA1Signature;
import com.aliyun.oss.common.utils.BinaryUtil;
import com.aliyun.oss.common.utils.DateUtil;
import com.aliyun.oss.model.PolicyConditions;
import net.sf.json.JSONObject;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Created by yuanyouz on 2017/6/23.
 */
@Controller
public class CephPostPolicyController {

    /**
     * 测试跨域
     */
    @RequestMapping("/testCORS")
    public void testCORS(HttpServletRequest request, HttpServletResponse response) throws IOException {
        JSONObject json = new JSONObject();
        String callbackFunName  = request.getParameter("callbackparam");
        json.put("test", true);
        response.setContentType("text/html;charset=UTF-8");
        PrintWriter out = response.getWriter();
        out.write(callbackFunName +"("+json.toString()+")");
    }

    /**
     * 测试跨域
     */
    @RequestMapping("/testCORSHeader")
    public void testCORSHeader(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("text/html;charset=UTF-8");
        response.setHeader("Access-Control-Allow-Origin", "*");
        response.setHeader("Access-Control-Allow-Methods", "GET,POST,OPTIONS,DELETE.PUT");
        response.setHeader("Access-Control-Allow-Headers", "*");

        PrintWriter out = response.getWriter();
        out.write("hello");
    }

    static String accessKey = "U2A27HEBIS06RIF3JOBC";
    static String secretKey = "C4UB88kkbR5QQhBEnKau2zUmkqxdttQOYSexIcDg";
    static String bucket = "helloworld";
    static String endpoint = "http://192.168.30.11/";

    @RequestMapping("/get")
    public void getPolicy(HttpServletRequest request, HttpServletResponse response) {
        String filename = "2.png";                                  //需要由前端计算后传给服务端
        String contentMD5 = "eGp+z6lDAu09eR9mc2np0w==";             //需要由前端计算后传给服务端
        String contentType = "image/jpeg";                          //需要由前端计算后传给服务端

        String host = endpoint + bucket + "/" + filename;
//        String host = endpoint + bucket;

        String dateString = org.apache.commons.httpclient.util.DateUtil.formatDate(new Date(),
                org.apache.commons.httpclient.util.DateUtil.PATTERN_RFC1036);

        Map<String,String> meta = new HashMap<String,String>();
        meta.put("x-amz-acl", "public-read");
        meta.put("x-amz-meta-title", "my-title-str");

        String sign = sign("PUT", contentMD5, contentType, dateString, "/" + bucket + "/" + filename, meta);
//        String sign = sign("PUT", contentMD5, contentType, dateString, "/" + bucket, meta);

        Map<String, String> respMap = new LinkedHashMap<String, String>();
        respMap.put("host", host);
        respMap.put("Date", dateString);
        respMap.put("Authorization", sign);
        respMap.put("Content-Type", contentType);
        respMap.put("Content-MD5", contentMD5);

        respMap.putAll(meta);

        JSONObject ja1 = JSONObject.fromObject(respMap);
        System.out.println(ja1.toString());

        try {
            response.setHeader("Access-Control-Allow-Origin", "*");
            response.setHeader("Access-Control-Allow-Methods", "GET, POST");
            response(request, response, ja1.toString());

        } catch (Exception e) {
            e.printStackTrace();
        }
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

    public String generatePostPolicy(java.util.Date expiration, PolicyConditions conds) {
        String formatedExpiration = DateUtil.formatIso8601Date(expiration);
        String jsonizedExpiration = String.format("\"expiration\":\"%s\"", new Object[]{formatedExpiration});
        String jsonizedConds = conds.jsonize();
        StringBuilder postPolicy = new StringBuilder();
        postPolicy.append("{");
        postPolicy.append(String.format("%s,%s", new Object[]{jsonizedExpiration, jsonizedConds}));
        postPolicy.append("}");
        return postPolicy.toString();
    }

    public String calculatePostSignature(String postPolicy, String secretAccessKey) {
        try {
            byte[] ex = postPolicy.getBytes("utf-8");
            String encPolicy = BinaryUtil.toBase64String(ex);
            return new HmacSHA1Signature().computeSignature(secretAccessKey, encPolicy);
        } catch (UnsupportedEncodingException var4) {
            throw new ClientException("Unsupported charset: " + var4.getMessage());
        }
    }

    private void response(HttpServletRequest request, HttpServletResponse response, String results) throws IOException {
        String callbackFunName = request.getParameter("callback");
        if (callbackFunName==null || callbackFunName.equalsIgnoreCase(""))
            response.getWriter().println(results);
        else
            response.getWriter().println(callbackFunName + "( "+results+" )");
        response.setStatus(HttpServletResponse.SC_OK);
        response.flushBuffer();
    }

}
