package com.imooc.design.methodtemplate;

public class RoomForAmericanSinger extends KTVRoom {
	/**
	 * 外国人点歌
	 */
	@Override
	public void orderSong() {
		System.out.println("来一首中文歌曲的英文版本");
	}

	// 因为老外觉得东西很贵，因此就没有进行额外的消费
}
