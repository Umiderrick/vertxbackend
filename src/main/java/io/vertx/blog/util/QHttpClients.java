package io.vertx.blog.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.http.Header;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.CookieStore;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.AbstractHttpEntity;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;

import io.vertx.blog.exception.ServiceException;
import io.vertx.core.json.Json;


public final class QHttpClients implements Cloneable {
	public final static String CONTENT_TYPE = "Content-Type";
	public final static String CONTENT_XML = "application/xml";
	public final static String CONTENT_JSON = "application/json";
	public final static String CONTENT_FORM = "application/x-www-form-urlencoded";
	final static String UTF8 = "UTF-8";
	
	public final static String METHOD_POST = "POST";
	public final static String METHOD_GET = "GET";

	public final static Charset CHARSET_UTF8 = Charset.forName(UTF8);

	private CloseableHttpClient httpclient;
	private final CookieStore cookieStore;

	private Header[] headers;
	private final int timeout;

	public QHttpClients() {
		this(15000);
	}

	public QHttpClients(int timeOut) {
		timeout = timeOut;
		cookieStore = new BasicCookieStore();

		if (httpclient == null) {
			httpclient = HttpClients.createDefault();
		}
	}

	public Header[] getHeaders() {
		return headers;
	}

	public String getString(byte[] is, Charset cs) throws IOException {
		return new String(is, cs);
	}

	private byte[] doPost(HttpPost httpPost, AbstractHttpEntity entity)
			throws ClientProtocolException, IOException {
		httpPost.setEntity(entity);
		return exec(httpPost);
	}

	private byte[] exec(HttpRequestBase httpRequest) throws ClientProtocolException, IOException {
		RequestConfig requestConfig = RequestConfig.custom().setSocketTimeout(timeout)//
				.setConnectTimeout(timeout)//
				.setConnectionRequestTimeout(timeout)//
				.build();
		httpRequest.setConfig(requestConfig);

		// Execution context can be customized locally.
		HttpClientContext context = HttpClientContext.create();

		context.setCookieStore(cookieStore);
		httpRequest.setHeader("Connection", "close");

		context.setCookieStore(cookieStore);

		CloseableHttpResponse response = null;
		InputStream is = null;
		try {
			response = httpclient.execute(httpRequest, context);
			headers = response.getAllHeaders();
			is = response.getEntity().getContent();
			byte bb[] = getBytes(is);
			is.close();
			is = null;
			return bb;
		} finally {
			IOUtils.close(is);
			if (response != null) {
				close(response);
			}
		}
	}

	public byte[] get(String url, Map<String, String> param) {
		HttpGet httpget = null;
		try {
			URI uri = toURI(url, param);
			httpget = new HttpGet(uri);
			return exec(httpget);
		} catch (Exception e) {
			throw new ServiceException(e);
		} finally {
			if (httpget != null) {
				httpget.releaseConnection();
			}
		}
	}

	public static void close(CloseableHttpResponse response) {
		try {
			response.close();
		} catch (IOException e) {
			// NOP
		}
	}

	/**
	 * 使用GET方法取得一个JSON，并序列化为指字的对象。
	 * 
	 * @param url
	 * @param param
	 * @param type
	 * @return
	 */
	public <T> T getAsObject(String url, Map<String, String> param, Class<T> type) {
		try {
			byte[] bb = get(url, param);
			String json = new String(bb, CHARSET_UTF8);
			return Json.decodeValue(json, type);
		} catch (Exception e) {
			throw new ServiceException(e);
		}
	}


	public static URI toURI(String s, Map<String, String> param) {
		try {
			URIBuilder rulBuilder = new URIBuilder(s);
			if (param != null) {
				for (Entry<String, String> e : param.entrySet()) {
					rulBuilder.setParameter(e.getKey(), e.getValue());
				}
			}
			return rulBuilder.build();
		} catch (Exception e) {
			throw new ServiceException(e.getMessage() + "," + s, e);
		}
	}

	private byte[] getBytes(InputStream input) throws IOException {
		ByteArrayOutputStream bb = new ByteArrayOutputStream(1024 * 8);
		int b;
		while ((b = input.read()) != -1) {
			bb.write(b);
		}
		return bb.toByteArray();
	}

	public byte[] post(URI uri, String contentType, byte[] ctx) {
		HttpPost post = new HttpPost(uri);
		post.setHeader(CONTENT_TYPE, contentType);
		ByteArrayEntity entity = new ByteArrayEntity(ctx);
		try {
			return doPost(post, entity);
		} catch (Exception e) {
			throw new ServiceException(e);
		}
	}

	public byte[] post(URI uri, Map<String, String> params) throws IOException {
		HttpPost post = new HttpPost(uri);
		post.setHeader(CONTENT_TYPE, "application/x-www-form-urlencoded; charset=UTF-8");
		List<NameValuePair> pairs = new ArrayList<NameValuePair>();
		if (params != null)
			for (Map.Entry<String, String> e : params.entrySet()) {
				NameValuePair nvp;
				nvp = new BasicNameValuePair(e.getKey(), e.getValue());
				pairs.add(nvp);
			}

		try {
			UrlEncodedFormEntity formEntity = new UrlEncodedFormEntity(pairs,UTF8);
			formEntity.setContentEncoding(UTF8);
			post.setEntity(formEntity);
			return doPost(post, formEntity);
		} catch (Exception e) {
			throw new ServiceException(e);
		}
	}

	public byte[] post(URI uri, Object object) {
		String json = Json.encode(object);
		StringEntity entity = new StringEntity(json, ContentType.APPLICATION_JSON);
		HttpPost post = new HttpPost(uri);
		post.setEntity(entity);
		try {
			return doPost(post, entity);
		} catch (Exception e) {
			throw new ServiceException(e);
		}
	}

	public <T> T postAsObject(URI uri, Object object, Class<T> type){
		byte b[] = post(uri, object);
		try {
		return Json.decodeValue(new String(b,UTF8), type);
		} catch (Exception e) {
			throw new ServiceException(e);
		}
	}

}
