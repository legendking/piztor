package com.macaroon.piztor;

import java.util.HashMap;

import android.annotation.SuppressLint;
import android.content.Intent;

@SuppressLint("UseSparseArrays")
public class AppMgr {
	// Status
	public enum  ActivityStatus{
		create, start, resume, restart, stop, pause, destroy
	} 
	static ActivityStatus status;
	static PiztorAct nowAct;

	static HashMap<Class<?>, HashMap<Integer, Class<?>>> mp;

	static void setStatus(ActivityStatus st) {
		status = st;
	}

	static void trigger(int event) {
		
		Intent i = new Intent();
		i.setClass(nowAct, mp.get(nowAct.getClass()).get(event));
		nowAct.startActivity(i);
	}

	static void add(Class<?> a, Integer event, Class<?> b) {
		if (mp.containsKey(a))
			mp.get(a).put(event, b);
		else {
			HashMap<Integer, Class<?>> h = new HashMap<Integer, Class<?>>();
			h.put(event, b);
			mp.put(a, h);
		}
	}

	static void addTransition(Class<?> a, int i, Class<?> b) {
		if (mp.containsKey(a)) {
			HashMap<Integer, Class<?>> h = mp.get(a);
			h.put(i, b);
			mp.put(a, h);
		} else {
			HashMap<Integer, Class<?>> h = new HashMap<Integer, Class<?>>();
			h.put(i, b);
			mp.put(a, h);
		}
	}

	static void addStatus(Class<?> a) {
		mp.put(a, new HashMap<Integer, Class<?>>());
	}

	static void init() {
		mp = new HashMap<Class<?>, HashMap<Integer, Class<?>>>();
		addStatus(InitAct.class);
		
	}

}