package com.woozoom.maplibrary;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Administrator on 2017/2/4.
 */

public class GPSTransformBD {
    private static GPSTransformBD instance;
    public static double pi = 3.14159265358979324;
    public static double a = 6378245.0;
    public static double ee = 0.00669342162296594323;
    public static double x_pi = 3.14159265358979324 * 3000.0 / 180.0;

    private GPSTransformBD(){

    }
    public static GPSTransformBD getInstance() {
        if (instance == null) {
            instance = new GPSTransformBD();
        }
        return instance;
    }
    public WayPoint db2wgs(double lat, double lon)
    {
        WayPoint db2gcj = bd2gcjFun(lat, lon);
        WayPoint gcj2wgs = gcj2wgsFun(db2gcj.latitude, db2gcj.longitude);
        return gcj2wgs;
    }

    //WGS坐标转换成百度坐标
    public WayPoint wgs2bd(double lat, double lon)
    {
        WayPoint wgs2gcj = wgs2gcjFun(lat, lon);
        WayPoint gcj2bd = gcj2bdFun(wgs2gcj.latitude, wgs2gcj.longitude);
        return gcj2bd;
    }

    //GCJ坐标转换百百度坐标
    public WayPoint gcj2bdFun(double lat, double lon)
    {
        double x = lon, y = lat;
        double z = Math.sqrt(x * x + y * y) + 0.00002 * Math.sin(y * x_pi);
        double theta = Math.atan2(y, x) + 0.000003 * Math.cos(x * x_pi);
        double bd_lon = z * Math.cos(theta) + 0.0065;
        double bd_lat = z * Math.sin(theta) + 0.006;

        WayPoint GPS = new WayPoint(bd_lat, bd_lon);

        return GPS;
    }

    //百度坐标转换成GCJ坐标
    public WayPoint bd2gcjFun(double lat, double lon)
    {
        double x = lon - 0.0065, y = lat - 0.006;
        double z = Math.sqrt(x * x + y * y) - 0.00002 * Math.sin(y * x_pi);
        double theta = Math.atan2(y, x) - 0.000003 * Math.cos(x * x_pi);
        double gg_lon = z * Math.cos(theta);
        double gg_lat = z * Math.sin(theta);

        WayPoint GPS = new WayPoint(gg_lat, gg_lon);

        return GPS;
    }

    //WGS坐标转换成GCJ坐标
    public WayPoint wgs2gcjFun(double lat, double lon)
    {
        if (outOfChina(lat, lon)) {
            // 出国就用GPS84
            return new WayPoint(lat,lon);
        }
        double dLat = transformLat(lon - 105.0, lat - 35.0);
        double dLon = transformLon(lon - 105.0, lat - 35.0);
        double radLat = lat / 180.0 * pi;
        double magic = Math.sin(radLat);
        magic = 1 - ee * magic * magic;
        double sqrtMagic = Math.sqrt(magic);
        dLat = (dLat * 180.0) / ((a * (1 - ee)) / (magic * sqrtMagic) * pi);
        dLon = (dLon * 180.0) / (a / sqrtMagic * Math.cos(radLat) * pi);
        double mgLat = lat + dLat;
        double mgLon = lon + dLon;
        WayPoint GPS = new WayPoint(mgLat, mgLon);
        return GPS;
    }

    //GCJ坐标转换成WGS坐标
    public WayPoint gcj2wgsFun(double lat, double lon)
    {
        WayPoint gps = transform(lat, lon);
        double latitude = lat * 2 - gps.latitude;
        double lontitude = lon * 2 - gps.longitude;
        WayPoint GPS = new WayPoint(latitude,lontitude);
        return GPS;
    }

    public boolean outOfChina(double lat, double lon)
    {
        if (lon < 72.004 || lon > 137.8347)
            return true;
        if (lat < 0.8293 || lat > 55.8271)
            return true;
        return false;
    }

    public WayPoint transform(double lat, double lon)
    {
        if (outOfChina(lat, lon))
        {
            return new WayPoint(lat, lon);
        }
        double dLat = transformLat(lon - 105.0, lat - 35.0);
        double dLon = transformLon(lon - 105.0, lat - 35.0);
        double radLat = lat / 180.0 * pi;
        double magic = Math.sin(radLat);
        magic = 1 - ee * magic * magic;
        double sqrtMagic = Math.sqrt(magic);
        dLat = (dLat * 180.0) / ((a * (1 - ee)) / (magic * sqrtMagic) * pi);
        dLon = (dLon * 180.0) / (a / sqrtMagic * Math.cos(radLat) * pi);
        double mgLat = lat + dLat;
        double mgLon = lon + dLon;
        return new WayPoint(mgLat,mgLon);
    }

    private double transformLat(double x, double y)
    {
        double ret = -100.0 + 2.0 * x + 3.0 * y + 0.2 * y * y + 0.1 * x * y + 0.2 * Math.sqrt(Math.abs(x));
        ret += (20.0 * Math.sin(6.0 * x * pi) + 20.0 * Math.sin(2.0 * x * pi)) * 2.0 / 3.0;
        ret += (20.0 * Math.sin(y * pi) + 40.0 * Math.sin(y / 3.0 * pi)) * 2.0 / 3.0;
        ret += (160.0 * Math.sin(y / 12.0 * pi) + 320 * Math.sin(y * pi / 30.0)) * 2.0 / 3.0;
        return ret;
    }

    private double transformLon(double x, double y)
    {
        double ret = 300.0 + x + 2.0 * y + 0.1 * x * x + 0.1 * x * y + 0.1 * Math.sqrt(Math.abs(x));
        ret += (20.0 * Math.sin(6.0 * x * pi) + 20.0 * Math.sin(2.0 * x * pi)) * 2.0 / 3.0;
        ret += (20.0 * Math.sin(x * pi) + 40.0 * Math.sin(x / 3.0 * pi)) * 2.0 / 3.0;
        ret += (150.0 * Math.sin(x / 12.0 * pi) + 300.0 * Math.sin(x / 30.0 * pi)) * 2.0 / 3.0;
        return ret;
    }

    public List<WayPoint> wgs2gcjFun(List<WayPoint> pointList) {
        if (pointList!=null){
            List<WayPoint> result = new ArrayList<>();
            for (WayPoint pt:pointList){
                if (pt.type == WayPoint.PointType.WGCS.ordinal()){
                    result.add(GPSTransformBD.getInstance().wgs2gcjFun(pt.latitude,pt.longitude));
                }else {
                    result.add(pt);
                }
            }
            return result;
        }
        return null;
    }
}
