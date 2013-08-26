package com.macaroon.piztor;

//--------------------------------------//
//			Ask user info				//
//--------------------------------------//

public class ReqUserinfo extends Req {
	int uid; // user id

	ReqUserinfo(String token, String name, int id, long time, long alive) {
		super(3, token, name, time, alive); // for type 3
		uid = id;
	}
}