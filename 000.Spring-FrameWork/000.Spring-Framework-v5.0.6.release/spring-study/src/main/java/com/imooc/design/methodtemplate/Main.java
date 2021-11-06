package com.imooc.design.methodtemplate;

public class Main {
	public static void main(String[] args) {
		RoomForAmericanSinger roomForAmericanSinger = new RoomForAmericanSinger();
		RoomForChinaSonger roomForChinaSonger = new RoomForChinaSonger();
		roomForAmericanSinger.procedure();
		roomForChinaSonger.procedure();
	}
}
