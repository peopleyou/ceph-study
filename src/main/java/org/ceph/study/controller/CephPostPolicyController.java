package org.ceph.study.controller;

import com.aliyun.oss.OSSClient;
import com.aliyun.oss.common.utils.BinaryUtil;
import com.aliyun.oss.model.MatchMode;
import com.aliyun.oss.model.PolicyConditions;
import net.sf.json.JSONObject;
import org.jets3t.service.security.AWSCredentials;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Date;
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

    @RequestMapping("/get")
    public void getPolicy(HttpServletRequest request, HttpServletResponse response) {
        String endpoint = "http://192.168.30.11:7480";
        String accessId = "U2A27HEBIS06RIF3JOBC";
        String accessKey = "C4UB88kkbR5QQhBEnKau2zUmkqxdttQOYSexIcDg";

        String bucket = "helloworld";
//        String dir = "user-dir";
        String dir = "";
//        String host = "http://" + bucket + "." + "192.168.30.11:7480";
        String host = endpoint + "/" + bucket;

        AWSCredentials credentials = new BasicAWSCredentials(accessId, accessKey);
        AmazonS3 conn = new AmazonS3Client(credentials);
        conn.setEndpoint(endpoint);

        OSSClient client = new OSSClient(endpoint, accessId, accessKey);
        try {
            long expireTime = 30;
            long expireEndTime = System.currentTimeMillis() + expireTime * 1000;
            Date expiration = new Date(expireEndTime);
            PolicyConditions policyConds = new PolicyConditions();
            policyConds.addConditionItem(PolicyConditions.COND_CONTENT_LENGTH_RANGE, 0, 1048576000);
            policyConds.addConditionItem(MatchMode.StartWith, PolicyConditions.COND_KEY, dir);

            String postPolicy = client.generatePostPolicy(expiration, policyConds);
            byte[] binaryData = postPolicy.getBytes("utf-8");
            String encodedPolicy = BinaryUtil.toBase64String(binaryData);
            String postSignature = client.calculatePostSignature(postPolicy);

            Map<String, String> respMap = new LinkedHashMap<String, String>();
            respMap.put("accessid", accessId);
            respMap.put("policy", encodedPolicy);
            respMap.put("signature", postSignature);
            //respMap.put("expire", formatISO8601Date(expiration));
            respMap.put("dir", dir);
            respMap.put("host", host);
            respMap.put("expire", String.valueOf(expireEndTime / 1000));
            JSONObject ja1 = JSONObject.fromObject(respMap);
            System.out.println(ja1.toString());
            response.setHeader("Access-Control-Allow-Origin", "*");
            response.setHeader("Access-Control-Allow-Methods", "GET, POST");
            response(request, response, ja1.toString());

        } catch (Exception e) {
            e.printStackTrace();
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
