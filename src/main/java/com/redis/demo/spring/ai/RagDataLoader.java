package com.redis.demo.spring.ai;

import java.util.Map;
import java.util.zip.GZIPInputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.autoconfigure.vectorstore.redis.RedisVectorStoreProperties;
import org.springframework.ai.reader.JsonReader;
import org.springframework.ai.vectorstore.RedisVectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

@Component
public class RagDataLoader implements ApplicationRunner {

	private static final Logger logger = LoggerFactory.getLogger(RagDataLoader.class);

	private static final String[] KEYS = { "name", "abv", "ibu", "description" };

	@Value("classpath:/data/beers.json.gz")
	private Resource data;

	private final RedisVectorStore vectorStore;

	private final RedisVectorStoreProperties properties;

	public RagDataLoader(RedisVectorStore vectorStore, RedisVectorStoreProperties properties) {
		this.vectorStore = vectorStore;
		this.properties = properties;
	}

	@Override
	public void run(ApplicationArguments args) throws Exception {
		Map<String, Object> indexInfo = vectorStore.getJedis().ftInfo(properties.getIndex());
		long numDocs = (long)indexInfo.getOrDefault("num_docs", "0");
		if (numDocs > 20000) {
			logger.info("Embeddings already loaded. Skipping");
			return;
		}
		Resource file = data;
		if (data.getFilename().endsWith(".gz")) {
			GZIPInputStream inputStream = new GZIPInputStream(data.getInputStream());
			file = new InputStreamResource(inputStream, "beers.json.gz");
		}
		logger.info("Creating Embeddings...");
		// tag::loader[]
		// Create a JSON reader with fields relevant to our use case
		JsonReader loader = new JsonReader(file, KEYS);
		// Use the autowired VectorStore to insert the documents into Redis
		vectorStore.add(loader.get());
		// end::loader[]
		logger.info("Embeddings created.");
	}

}
