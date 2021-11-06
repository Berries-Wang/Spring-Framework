package com.imooc.design.methodtemplate;

public class RoomForChinaSonger extends KTVRoom {
	/**
	 * 中国人点歌
	 */
	@Override
	public void orderSong() {
		System.out.println("来一首经典的中文歌曲");
	}

	/**
	 * 中国人觉得东西很便宜，因此消费了一波
	 */
	@Override
	public void orderExtra() {
		System.out.println("东西很便宜，消费一下");
	}
}
