package com.woozoom.maplibrary;

import java.io.Serializable;

public class WayPoint implements Serializable{

	public Double latitude;
	public Double longitude;
	public Double altitude;
	private Double radius = 5.0;
	public short pstate;//航迹点时为state
	public int xLon;
	public int yAla;
	public short type;//0为GPS点，1为GCJ点
	public WayPoint(Double latitude, Double longitude) {
		this.latitude = latitude;
		this.longitude = longitude;
		this.altitude = Double.valueOf(0);
		this.type = 0;
	}
	public WayPoint(Double latitude, Double longitude, Double altitude, short type) {
		this.latitude = latitude;
		this.longitude = longitude;
		this.altitude = altitude;
		this.type = type;
	}

	public WayPoint(Double latitude, Double longitude, Double altitude, short type, Double radius, short pstate) {
		this.latitude = latitude;
		this.longitude = longitude;
		this.altitude = altitude;
		this.type = type;
		this.radius = radius;
		this.pstate = pstate;
	}

	public double getRadius() {
		if (this.radius !=null){
			return this.radius.doubleValue();
		}
		return 0;
	}

	public enum PointType{
		WGCS,
		GCJ02,
		BAIDU
	}
	public WayPoint(double latitude, double longitude, short type) {
		this(latitude,longitude, (double) 0,type);
	}

	public void setRadius(Double radius) {
		this.radius = radius;
	}

}
