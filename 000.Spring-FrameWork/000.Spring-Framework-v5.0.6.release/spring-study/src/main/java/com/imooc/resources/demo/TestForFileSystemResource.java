package com.imooc.resources.demo;


import org.springframework.core.io.FileSystemResource;

import java.io.*;

/**
 * FileSystemResource学习
 */
public class TestForFileSystemResource {
	public static void main(String[] args) throws IOException {

		String path = "./spring-demo/src/main/java/com/com.imooc/resources/demo/FileSystemResources.txt";
		FileSystemResource resource = new FileSystemResource(path);
		System.out.println(resource.isFile());


		/**
		 * 通过Resource来对文件进行读取操作
		 */
		BufferedReader reader = new BufferedReader(new InputStreamReader(resource.getInputStream()));
		int num = 0;
		char[] arr = new char[1024];
		StringBuffer buffer = new StringBuffer();
		while ((num = reader.read(arr, 0, 1024)) > 0) {
			buffer.append(new String(arr, 0, num));
		}
		reader.close();
		System.out.println("读取到的内容: " + buffer.toString());

		/**
		 * 通过Resource来对文件进行写入操作
		 */
		BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(resource.getOutputStream()));
		String mess = "Resource 封装的写文件操作";
		writer.write(mess);
		writer.flush();
		writer.close();
	}
}
