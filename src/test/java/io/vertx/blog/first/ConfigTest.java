/**
 * 
 */
package io.vertx.blog.first;

import io.vertx.blog.util.AppConfig;

/**
 * @author pengbo
 *
 */
public class ConfigTest {

	public static void main(String[] args){
		AppConfig.load("config.properties");
		String string = AppConfig.getString("APPID");
		String APPSCRET = AppConfig.getString("APPSCRET");
		String access_token = AppConfig.getString("access_token");
		System.out.println(string+APPSCRET+access_token);
	}
}
