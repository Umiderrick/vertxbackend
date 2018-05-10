package io.vertx.blog.first;

import io.vertx.core.json.JsonObject;

public class Poem {

	private String id;

	private String header;

	private String content;

	private String useDay;
	// Image
	private String imgUrl;
	// music
	private String attach;

	public Poem(String header, String content) {
		this.header = header;
		this.content = content;
		this.id = "";
	}
	
	public Poem(String header, String content,String useDay,String imgUrl,String attach) {
		this.header = header;
		this.content = content;
		this.useDay = useDay;
		this.imgUrl = imgUrl;
		this.attach = attach;
		this.id = "";
	}

	public Poem(JsonObject json) {
		this.header = json.getString("header");
		this.content = json.getString("content");
		this.useDay = json.getString("useDay");
		this.imgUrl = json.getString("imgUrl");
		this.attach = json.getString("attach");
		this.id = json.getString("_id");
	}

	public Poem() {
		this.id = "";
	}

	public Poem(String id, String header, String content,String useDay,String imgUrl,String attach) {
		this.id = id;
		this.header = header;
		this.content = content;
		this.useDay = useDay;
		this.imgUrl = imgUrl;
		this.attach = attach;
	}

	public JsonObject toJson() {
		JsonObject json = new JsonObject()
				.put("header", header)
				.put("content", content)
				.put("useDay", useDay)
				.put("imgUrl", imgUrl)
				.put("attach", attach);
		if (id != null && !id.isEmpty()) {
			json.put("_id", id);
		}
		return json;
	}


	public String getId() {
		return id;
	}

	public String getHeader() {
		return header;
	}

	public Poem setHeader(String header) {
		this.header = header;
		return this;
	}

	public String getContent() {
		return content;
	}

	public Poem setContent(String content) {
		this.content = content;
		return this;
	}

	public String getUseDay() {
		return useDay;
	}

	public Poem setUseDay(String useDay) {
		this.useDay = useDay;
		return this;
	}

	public String getImgUrl() {
		return imgUrl;
	}

	public Poem setImgUrl(String imgUrl) {
		this.imgUrl = imgUrl;
		return this;
	}

	public String getAttach() {
		return attach;
	}

	public Poem setAttach(String attach) {
		this.attach = attach;
		return this;
	}

	public Poem setId(String id) {
		this.id = id;
		return this;
	}
	
	


}