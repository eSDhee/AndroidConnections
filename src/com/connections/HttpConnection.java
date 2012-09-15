package com.connections;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.BufferedHttpEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.protocol.HTTP;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Message;
import android.util.Base64;
import android.util.Log;

/**
 * Asynchronous HTTP connections
 * 
 * @author Greg Zavitz & Joseph Roth
 * 
 * @modifier Stefanus Diptya; Twitter @eSDhee
 * 
 * @category Blog {@link http ://stefanusdiptya.wordpress.com}
 * 
 * @since August 2012; Adding an Authorization for Secured Connection
 */
public class HttpConnection implements Runnable {

	public static final int DID_START = 0;
	public static final int DID_ERROR = 1;
	public static final int DID_SUCCEED = 2;

	private static final int GET = 0;
	private static final int POST = 1;
	private static final int PUT = 2;
	private static final int DELETE = 3;
	private static final int BITMAP = 4;

	private String url;
	private int method;
	private Handler handler;
	private String data;
	private String username;
	private String password;
	private ArrayList<NameValuePair> valuePair;

	private HttpClient httpClient;

	public HttpConnection() {
		this(new Handler());
	}

	public HttpConnection(Handler _handler) {
		handler = _handler;
	}

	public void create(int method, String url, String data) {
		this.method = method;
		this.url = url;
		this.data = data;
		ConnectionManager.getInstance().push(this);
	}

	public void create(int method, String url, String data, String username,
			String password) {
		this.method = method;
		this.url = url;
		this.data = data;
		this.username = username;
		this.password = password;
		ConnectionManager.getInstance().push(this);
	}

	public void create(int method, String url,
			ArrayList<NameValuePair> valuePair) {
		this.method = method;
		this.url = url;
		this.valuePair = valuePair;
		ConnectionManager.getInstance().push(this);
	}

	public void create(int method, String url,
			ArrayList<NameValuePair> valuePair, String username, String password) {
		this.method = method;
		this.url = url;
		this.valuePair = valuePair;
		this.username = username;
		this.password = password;
		ConnectionManager.getInstance().push(this);
	}

	public void get(String url) {
		create(GET, url, new String());
	}

	public void get(String url, String username, String password) {
		create(GET, url, new String(), username, password);
	}

	public void post(String url, ArrayList<NameValuePair> valuePair) {
		create(POST, url, valuePair);
	}

	public void post(String url, ArrayList<NameValuePair> valuePair,
			String username, String password) {
		create(POST, url, valuePair, username, password);
	}

	public void put(String url, String data) {
		create(PUT, url, data);
	}

	public void delete(String url) {
		create(DELETE, url, new String());
	}

	public void bitmap(String url) {
		create(BITMAP, url, new String());
	}

	public void run() {
		handler.sendMessage(Message.obtain(handler, HttpConnection.DID_START));
		httpClient = new DefaultHttpClient();
		HttpConnectionParams.setSoTimeout(httpClient.getParams(), 25000);
		try {
			HttpResponse response = null;
			switch (method) {
			case GET:
				response = httpClient.execute(new HttpGet(url));
				break;
			case POST:
				HttpPost httpPost = new HttpPost(url);
				if (username != null) {
					httpPost.setHeader("Authorization",
							getB64Auth(username, password));
				}
				httpPost.setEntity(new UrlEncodedFormEntity(valuePair,
						HTTP.UTF_8));
				response = httpClient.execute(httpPost);
				break;
			case PUT:
				HttpPut httpPut = new HttpPut(url);
				if (username != null) {
					httpPut.setHeader("Authorization",
							getB64Auth(username, password));
				}
				httpPut.setEntity(new StringEntity(data));
				response = httpClient.execute(httpPut);
				break;
			case DELETE:
				response = httpClient.execute(new HttpDelete(url));
				break;
			case BITMAP:
				response = httpClient.execute(new HttpGet(url));
				processBitmapEntity(response.getEntity());
				break;
			}
			if (method < BITMAP)
				processEntity(response.getEntity());
		} catch (Exception e) {
			handler.sendMessage(Message.obtain(handler,
					HttpConnection.DID_ERROR, e));
		}
		ConnectionManager.getInstance().didComplete(this);
	}

	private String getB64Auth(String login, String pass) {
		String source = login + ":" + pass;
		String ret = "Basic "
				+ Base64.encodeToString(source.getBytes(), Base64.URL_SAFE
						| Base64.NO_WRAP);
		return ret;
	}

	private void processEntity(HttpEntity entity) throws IllegalStateException,
			IOException {
		BufferedReader br = new BufferedReader(new InputStreamReader(
				entity.getContent()));
		String line, result = "";
		while ((line = br.readLine()) != null) {
			result += line;
			Log.e("HttpResponse", line);
		}
		Message message = Message.obtain(handler, DID_SUCCEED, result);
		handler.sendMessage(message);
	}

	private void processBitmapEntity(HttpEntity entity) throws IOException {
		BufferedHttpEntity bufHttpEntity = new BufferedHttpEntity(entity);
		Bitmap bm = BitmapFactory.decodeStream(bufHttpEntity.getContent());
		handler.sendMessage(Message.obtain(handler, DID_SUCCEED, bm));
	}

}
