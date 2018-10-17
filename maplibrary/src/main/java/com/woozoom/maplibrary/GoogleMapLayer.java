package com.woozoom.maplibrary;

import android.text.TextUtils;

import com.esri.arcgisruntime.arcgisservices.LevelOfDetail;
import com.esri.arcgisruntime.arcgisservices.TileInfo;
import com.esri.arcgisruntime.data.TileKey;
import com.esri.arcgisruntime.geometry.Envelope;
import com.esri.arcgisruntime.geometry.Point;
import com.esri.arcgisruntime.geometry.SpatialReference;
import com.esri.arcgisruntime.layers.ImageTiledLayer;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * googleMap 辅助类
 * <p>
 * Created by zhangjian on 2017/9/20.
 */

public class GoogleMapLayer extends ImageTiledLayer {

    private boolean useScale = true;//是否缓存
    private int zoomScale = 15;//缓存的最小级别

    // 枚举
    public enum MapType {
        VECTOR,        //矢量标注地图
        IMAGE,        //影像地图
        ROAD        //道路标注图层
    }

    private static GoogleMapLayer googleMapLayer;

    private static TileInfo mTileInfo;
    private static MapType mMapType;

    public static double[] iScale =
            {
                    591657527.591555,
                    295828763.795777,
                    147914381.897889,
                    73957190.948944,
                    36978595.474472,
                    18489297.737236,
                    9244648.868618,
                    4622324.434309,
                    2311162.217155,
                    1155581.108577,
                    577790.554289,
                    288895.277144,
                    144447.638572,
                    72223.819286,
                    36111.909643,
                    18055.954822,
                    9027.977411,
                    4513.988705,
                    2256.994353,
                    1128.497176,
            };
    public static double[] iRes =
            {
                    156543.033928,
                    78271.5169639999,
                    39135.7584820001,
                    19567.8792409999,
                    9783.93962049996,
                    4891.96981024998,
                    2445.98490512499,
                    1222.99245256249,
                    611.49622628138,
                    305.748113140558,
                    152.874056570411,
                    76.4370282850732,
                    38.2185141425366,
                    19.1092570712683,
                    9.55462853563415,
                    4.77731426794937,
                    2.38865713397468,
                    1.19432856685505,
                    0.597164283559817,
                    0.298582141647617,
            };


    private GoogleMapLayer(TileInfo tileInfo, Envelope fullExtent) {
        super(tileInfo, fullExtent);
    }

    public static GoogleMapLayer getInstance(MapType mapType) {
        mMapType = mapType;
        googleMapLayer = new GoogleMapLayer(buildTileInfo(), new Envelope(-22041257.773878,
                -32673939.6727517, 22041257.773878, 20851350.0432886, SpatialReference.create(102113)));
        return googleMapLayer;
    }

    @Override
    protected byte[] getTile(TileKey tileKey) {
        if (useScale) {
            byte[] oldByte = getOffLineCacheFile(tileKey);
            if (oldByte != null) {
                return oldByte;
            }
        }
        byte[] iResult;
        try {
            URL iURL;
            byte[] iBuffer = new byte[1024];
            HttpURLConnection iHttpURLConnection;
            BufferedInputStream iBufferedInputStream;
            ByteArrayOutputStream iByteArrayOutputStream;

            iURL = new URL(this.getMapUrl(tileKey));
            iHttpURLConnection = (HttpURLConnection) iURL.openConnection();
            iHttpURLConnection.connect();
            iBufferedInputStream = new BufferedInputStream(iHttpURLConnection.getInputStream());
            iByteArrayOutputStream = new ByteArrayOutputStream();
            while (true) {
                int iLength = iBufferedInputStream.read(iBuffer);
                if (iLength > 0) {
                    iByteArrayOutputStream.write(iBuffer, 0, iLength);
                } else {
                    break;
                }
            }
            iBufferedInputStream.close();
            iHttpURLConnection.disconnect();
            iResult = iByteArrayOutputStream.toByteArray();
            addOfflineCacheFile(tileKey, iResult);
        } catch (Exception ex) {
//            ex.printStackTrace();
            iResult = new byte[1024];
        }
        return iResult;
    }

    /*
    lyrs=s为地图类型，如下：

    m：路线图
    t：地形图
    p：带标签的地形图
    s：卫星图
    y：带标签的卫星图
    h：标签层（路名、地名等）
     */
    private String getMapUrl(TileKey tileKey) {
        String iResult;
        Random iRandom;
        int level = tileKey.getLevel();
        int col = tileKey.getColumn();
        int row = tileKey.getRow();
        iResult = "http://mt";
        iRandom = new Random();
        iResult = iResult + iRandom.nextInt(4);
        switch (this.mMapType) {
            case VECTOR:
                iResult = iResult + ".google.cn/vt/lyrs=m@212000000&hl=zh-CN&gl=CN&src=app&x=" + col + "&y=" + row + "&z=" + level + "&s==Galil";
                break;
            case IMAGE:
                iResult = iResult + ".google.cn/vt/lyrs=y@126&hl=zh-CN&gl=CN&src=app&x=" + col + "&y=" + row + "&z=" + level + "&s==Galil";
                break;
            case ROAD:
                iResult = iResult + ".google.cn/vt/imgtp=png32&lyrs=h@212000000&hl=zh-CN&gl=CN&src=app&x=" + col + "&y=" + row + "&z=" + level + "&s==Galil";
                break;
            default:
                return "";
        }
        return iResult;
    }

    @Override
    public TileInfo getTileInfo() {
        return mTileInfo;
    }

    public static TileInfo buildTileInfo() {
        Point iPoint = new Point(-20037508.342787, 20037508.342787, SpatialReference.create(102113));
        List<LevelOfDetail> levelOfDetails = new ArrayList<>();
        for (int i = 0; i < iRes.length; i++) {
            LevelOfDetail levelOfDetail = new LevelOfDetail(i, iRes[i], iScale[i]);
            levelOfDetails.add(levelOfDetail);
        }
        mTileInfo = new TileInfo(160, TileInfo.ImageFormat.PNG, levelOfDetails, iPoint, SpatialReference.create(102113), 256, 256);
        return mTileInfo;
    }

    /**
     * 缓存瓦片
     *
     * @param tileKey
     * @param bytes
     */
    private void addOfflineCacheFile(TileKey tileKey, byte[] bytes) {
        if (useScale && tileKey.getLevel() >= zoomScale) {
            String fileName = createName(tileKey);//把图片的url当做文件名,并进行MD5加密
            File file = new File(Util.ADVERTNAME, fileName);
            File parentFile = file.getParentFile();
            if (!parentFile.exists()) {
                parentFile.mkdirs();
            }
            if (!file.exists()) {
                try {
                    FileOutputStream outputStream = new FileOutputStream(file);
                    outputStream.write(bytes);
                    outputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * 获取缓存瓦片
     *
     * @param tileKey
     * @return
     */
    private byte[] getOffLineCacheFile(TileKey tileKey) {
        String fileName = createName(tileKey);//把图片的url当做文件名,并进行MD5加密
        File file = new File(Util.ADVERTNAME, fileName);
        try {
            if (file.exists()) {
                FileInputStream in = new FileInputStream(file);
                ByteArrayOutputStream out = new ByteArrayOutputStream(1024);
                byte[] temp = new byte[1024];
                int size = 0;
                while ((size = in.read(temp)) != -1) {
                    out.write(temp, 0, size);
                }
                in.close();
                byte[] bytes = out.toByteArray();
                return bytes;
            } else {
                return null;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 创建瓦片名字
     *
     * @param tileKey
     * @return
     */
    private String createName(TileKey tileKey) {
        String result = "";
        result = mMapType.name() + "_" + tileKey.getLevel() + "_" + tileKey.getColumn() + "_" + tileKey.getRow();
        return GetMD5Code(result);
    }

    /**
     * 使用MD5加密
     *
     * @param string
     * @return
     */
    private String GetMD5Code(String string) {
        if (TextUtils.isEmpty(string)) {
            return "";
        }
        MessageDigest md5;
        try {
            md5 = MessageDigest.getInstance("MD5");
            byte[] bytes = md5.digest(string.getBytes());
            String result = "";
            for (byte b : bytes) {
                String temp = Integer.toHexString(b & 0xff);
                if (temp.length() == 1) {
                    temp = "0" + temp;
                }
                result += temp;
            }
            return result;
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return "";
    }

    public void onDestroy() {
        googleMapLayer = null;
        mMapType = null;
        mTileInfo = null;
    }
}