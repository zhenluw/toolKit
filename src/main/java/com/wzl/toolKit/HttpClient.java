package com.wzl.toolKit;


import com.alibaba.fastjson.JSONObject;
import okhttp3.*;
import org.apache.commons.collections.MapUtils;
import org.apache.http.*;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustStrategy;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.util.EntityUtils;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class HttpClient {

    private static final String HTTP = "http";
    private static final String HTTPS = "https";
    private static SSLConnectionSocketFactory sslsf = null;
    private static PoolingHttpClientConnectionManager cm = null;
    private static SSLContextBuilder builder = null;

    static {
        try {
            builder = new SSLContextBuilder();
            // 全部信任 不做身份鉴定
            builder.loadTrustMaterial(null, new TrustStrategy() {
                @Override
                public boolean isTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {
                    return true;
                }
            });
            sslsf = new SSLConnectionSocketFactory(builder.build(), new String[]{"SSLv2Hello", "SSLv3", "TLSv1", "TLSv1.2"}, null, NoopHostnameVerifier.INSTANCE);
            Registry<ConnectionSocketFactory> registry = RegistryBuilder.<ConnectionSocketFactory>create()
                    .register(HTTP, new PlainConnectionSocketFactory())
                    .register(HTTPS, sslsf)
                    .build();
            cm = new PoolingHttpClientConnectionManager(registry);
            cm.setMaxTotal(200);//max connection
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    /**
     * 同步的Get请求, 返回Response对象
     */
    public static Response okhttp_getSync(String url) throws IOException {
        OkHttpClient okHttpClient=new OkHttpClient();
        Request request = new Request.Builder().url(url) .build();
        Response response =okHttpClient.newCall(request).execute();
        return response;
    }

    /**
     * 同步的Get请求, 返回string
     */
    public static String okhttp_getSyncString(String url) throws IOException {
        Response response = okhttp_getSync(url);
        return response.body().string();
    }

    /**
     * 异步的Get请求
     */
    public static void okhttp_getAsync(String url) {

        OkHttpClient okHttpClient = new OkHttpClient();
        Request request = new Request.Builder()
                .url(url)
                .build();
        Call call = okHttpClient.newCall(request);
        call.enqueue(new Callback() {
            public void onFailure(Call call, IOException e) {

            }
            public void onResponse(Call call, Response response) throws IOException {
//                html = response.body().string();
            }
        });

    }

    /**
     * 同步的Post请求, 返回Response对象  提交表单方式
     */
    public static String okhttp_postForm(String url, Map<String,String> map) {

        OkHttpClient okHttpClient  = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .writeTimeout(10,TimeUnit.SECONDS)
                .readTimeout(20, TimeUnit.SECONDS)
                .build();

        //post方式提交的数据
        FormBody.Builder formBodyB =  new FormBody.Builder();

        for (Map.Entry<String, String> entry : map.entrySet()) {
            formBodyB.add(entry.getKey(), entry.getValue());
        }

        FormBody formBody = formBodyB.build();

        final Request request = new Request.Builder()
                .url(url)//请求的url
                .post(formBody)
                .build();

        //创建/Call
        Call call = okHttpClient.newCall(request);

        // 同步操作
        String html = null;
        try {
            html = call.execute().body().toString();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return html;
    }

    /**
     * 同步的Post请求, 返回Response对象  提交json方式
     */
    public static String okhttp_postJson(String url,JSONObject json) {

        OkHttpClient okHttpClient  = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .writeTimeout(10,TimeUnit.SECONDS)
                .readTimeout(20, TimeUnit.SECONDS)
                .build();

        //MediaType  设置Content-Type 标头中包含的媒体类型值
        RequestBody requestBody = FormBody.create(MediaType.parse("application/json; charset=utf-8"),json.toJSONString());

        Request request = new Request.Builder()
                .url(url)//请求的url
                .post(requestBody)
                .build();

        //创建Call
        Call call = okHttpClient.newCall(request);

//        //加入队列 异步操作
//        call.enqueue(new Callback() {
//            //请求错误回调方法
//            public void onFailure(Call call, IOException e) {
//                System.out.println("连接失败");
//            }
//            public void onResponse(Call call, Response response) throws IOException {
//                System.out.println(response.body().string());
//            }
//        });

        // 同步操作
        String html = null;
        try {
            html = call.execute().body().toString();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return html;
    }


    /**
     * httpclient get请求
     * @param url
     * @param header
     * @return
     */
    public static String httpClient_get(String url, Map<String, String> header)  {

        String result = "";
        CloseableHttpClient httpClient = null;
        try {
            httpClient = getHttpClient();
            HttpGet httpGet = new HttpGet(url);
            // 设置头信息
            if (MapUtils.isNotEmpty(header)) {
                for (Map.Entry<String, String> entry : header.entrySet()) {
                    httpGet.addHeader(entry.getKey(), entry.getValue());
                }
            }

            HttpResponse httpResponse = httpClient.execute(httpGet);
            int statusCode = httpResponse.getStatusLine().getStatusCode();
            if (statusCode == HttpStatus.SC_OK) {
                HttpEntity resEntity = httpResponse.getEntity();
                result = EntityUtils.toString(resEntity,"utf-8");
            } else {
                readHttpResponse(httpResponse);
            }
        } catch (Exception e) {
            e.printStackTrace();

        } finally {
            if (httpClient != null) {
                try {
                    httpClient.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return result;
    }

    /**
     * httpClient post请求
     * @param url 请求url
     * @param header 头部信息
     * @param param 请求参数 form提交适用
     * @param entity 请求实体 json/xml提交适用
     * @return 可能为空 需要处理
     * @throws Exception
     *
     */
    public static String httpClient_post(String url, Map<String, String> header, Map<String, String> param, HttpEntity entity)  {

        String result = "";
        CloseableHttpClient httpClient = null;
        try {
            httpClient = getHttpClient();
            HttpPost httpPost = new HttpPost(url);
            // 设置头信息
            if (MapUtils.isNotEmpty(header)) {
                for (Map.Entry<String, String> entry : header.entrySet()) {
                    httpPost.addHeader(entry.getKey(), entry.getValue());
                }
            }
            // 设置请求参数
            if (MapUtils.isNotEmpty(param)) {
                List<NameValuePair> formparams = new ArrayList<NameValuePair>();
                for (Map.Entry<String, String> entry : param.entrySet()) {
                    //给参数赋值
                    formparams.add(new BasicNameValuePair(entry.getKey(), entry.getValue()));
                }
                UrlEncodedFormEntity urlEncodedFormEntity = new UrlEncodedFormEntity(formparams, Consts.UTF_8);
                httpPost.setEntity(urlEncodedFormEntity);
            }
            // 设置实体 优先级高
            if (entity != null) {
                httpPost.setEntity(entity);
            }
            HttpResponse httpResponse = httpClient.execute(httpPost);
            int statusCode = httpResponse.getStatusLine().getStatusCode();
            if (statusCode == HttpStatus.SC_OK) {
                HttpEntity resEntity = httpResponse.getEntity();
                result = EntityUtils.toString(resEntity);
            } else {
                readHttpResponse(httpResponse);
            }
        } catch (Exception e) {
            e.printStackTrace();

        } finally {
            if (httpClient != null) {
                try {
                    httpClient.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return result;
    }


    public static CloseableHttpClient getHttpClient() throws Exception {

        CloseableHttpClient httpClient = HttpClients.custom()
                .setSSLSocketFactory(sslsf)
                .setConnectionManager(cm)
                .setConnectionManagerShared(true)
                .build();

        return httpClient;
    }


    public static String readHttpResponse(HttpResponse httpResponse)
            throws ParseException, IOException {
        StringBuilder builder = new StringBuilder();
        // 获取响应消息实体
        HttpEntity entity = httpResponse.getEntity();
        // 响应状态
        builder.append("status:" + httpResponse.getStatusLine());
        builder.append("headers:");
        HeaderIterator iterator = httpResponse.headerIterator();
        while (iterator.hasNext()) {
            builder.append("\t" + iterator.next());
        }
        // 判断响应实体是否为空
        if (entity != null) {
            String responseString = EntityUtils.toString(entity);
            builder.append("response length:" + responseString.length());
            builder.append("response content:" + responseString.replace("\r\n", ""));
        }
        return builder.toString();
    }


    /**
     * URL请求接口
     * @param urlPath
     * @return
     */
    public String url_get(String urlPath) {

        HttpURLConnection connection = null;
        InputStream is = null;
        try {
            URL url = new URL(urlPath);
            //获得URL对象
            connection = (HttpURLConnection) url.openConnection();
            //获得HttpURLConnection对象
            connection.setRequestMethod("GET");
            // 默认为GET
            connection.setUseCaches(false);
            //不使用缓存
            connection.setConnectTimeout(10000);
            //设置超时时间
            connection.setReadTimeout(10000);
            //设置读取超时时间
            connection.setDoInput(true);
            //设置是否从httpUrlConnection读入，默认情况下是true;
            if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                //相应码是否为200
                is = connection.getInputStream();
                //获得输入流
                BufferedReader reader = new BufferedReader(new InputStreamReader(is));
                //包装字节流为字符流
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                return response.toString();
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (connection != null) {
                connection.disconnect();
                connection = null;
            }
            if (is != null) {
                try {
                    is.close();
                    is = null;
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return null;
    }

    private String url_post(String urlPath, Map<String, String> params) {

        if (params == null || params.size() == 0) {
            return url_get(urlPath);
        }
        OutputStream os = null;
        InputStream is = null;
        HttpURLConnection connection = null;
        StringBuffer body = getParamString(params);
        byte[] data = body.toString().getBytes();
        try {
            URL url = new URL(urlPath);
            //获得URL对象
            connection = (HttpURLConnection) url.openConnection();
            //获得HttpURLConnection对象
            connection.setRequestMethod("POST");
            // 设置请求方法为post
            connection.setUseCaches(false);
            //不使用缓存
            connection.setConnectTimeout(10000);
            //设置超时时间
            connection.setReadTimeout(10000);
            //设置读取超时时间
            connection.setDoInput(true);
            //设置是否从httpUrlConnection读入，默认情况下是true;
            connection.setDoOutput(true);
            //设置为true后才能写入参数
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            connection.setRequestProperty("Content-Length", String.valueOf(data.length));
            os = connection.getOutputStream();
            os.write(data);
            //写入参数
            if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                //相应码是否为200
                is = connection.getInputStream();
                //获得输入流
                BufferedReader reader = new BufferedReader(new InputStreamReader(is));
                //包装字节流为字符流
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                return response.toString();
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            //关闭
            if (os != null) {
                try {
                    os.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (connection != null) {
                connection.disconnect();
                connection = null;
            }
        }
        return null;
    }

    private StringBuffer getParamString(Map<String, String> params) {
        StringBuffer result = new StringBuffer();
        Iterator<Map.Entry<String, String>> iterator = params.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, String> param = iterator.next();
            String key = param.getKey();
            String value = param.getValue();
            result.append(key).append('=').append(value);
            if (iterator.hasNext()) {
                result.append('&');
            }
        }
        return result;
    }


}
