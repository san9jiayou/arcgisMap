package com.woozoom.maplibrary;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import com.esri.arcgisruntime.concurrent.ListenableFuture;
import com.esri.arcgisruntime.geometry.AreaUnit;
import com.esri.arcgisruntime.geometry.AreaUnitId;
import com.esri.arcgisruntime.geometry.Envelope;
import com.esri.arcgisruntime.geometry.GeodeticCurveType;
import com.esri.arcgisruntime.geometry.Geometry;
import com.esri.arcgisruntime.geometry.GeometryEngine;
import com.esri.arcgisruntime.geometry.LinearUnit;
import com.esri.arcgisruntime.geometry.LinearUnitId;
import com.esri.arcgisruntime.geometry.Point;
import com.esri.arcgisruntime.geometry.PointCollection;
import com.esri.arcgisruntime.geometry.Polygon;
import com.esri.arcgisruntime.geometry.Polyline;
import com.esri.arcgisruntime.geometry.SpatialReference;
import com.esri.arcgisruntime.geometry.SpatialReferences;
import com.esri.arcgisruntime.location.LocationDataSource;
import com.esri.arcgisruntime.mapping.ArcGISMap;
import com.esri.arcgisruntime.mapping.Basemap;
import com.esri.arcgisruntime.mapping.Viewpoint;
import com.esri.arcgisruntime.mapping.view.BackgroundGrid;
import com.esri.arcgisruntime.mapping.view.DefaultMapViewOnTouchListener;
import com.esri.arcgisruntime.mapping.view.Graphic;
import com.esri.arcgisruntime.mapping.view.GraphicsOverlay;
import com.esri.arcgisruntime.mapping.view.IdentifyGraphicsOverlayResult;
import com.esri.arcgisruntime.mapping.view.LocationDisplay;
import com.esri.arcgisruntime.mapping.view.MapView;
import com.esri.arcgisruntime.symbology.PictureMarkerSymbol;
import com.esri.arcgisruntime.symbology.SimpleFillSymbol;
import com.esri.arcgisruntime.symbology.SimpleLineSymbol;
import com.esri.arcgisruntime.symbology.Symbol;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * arcgis基础图层
 * <p>
 * Created by zhangjian on 2018/9/20.
 */

public class ArcgisMap extends Fragment {
    /*
      Point (lon , lat)
      in gcj
      out wgs
     */
    private MapView mMapView;
    private int mapType = 0;
    private LocationDisplay mLocationDisplay;
    private boolean isNotFirst;
    private boolean mapCanTouchPoint;//点是否可以点击 默认不能
    private boolean mapCanAddPoint;//地图是否可以点击点
    private GraphicsOverlay mGraphicsOverlay;//画布图层
    private onRotate rotateListener;//监听旋转角度
    private onMapClick mapClickListener;//监听地图点击事件
    private final int gridAreaColor = Color.parseColor("#f5f3f0");//背景色
    private final int gridLineColor = Color.parseColor("#ddd7d4");//背景线的颜色
    private Point centerPoint = new Point(116.397430274476, 39.90874620323124, SpatialReferences.getWgs84());//中心点
    private GoogleMapLayer googleMapLayer;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_arcgis, null);
        mapType = Util.getIntShareData(getActivity(), Util.MAPTYPE, 0);
        mMapView = view.findViewById(R.id.mapView);
        //添加key
//        ArcGISRuntimeEnvironment.setLicense("pk.eyJ1IjoicmptNDQxMyIsImEiOiJjajE3NGtseXQwMzk2MndvdjUwc3k4cHRnIn0.9ZPto-HOElNQL0FK_cJRyQ");
        initMap();
        setupLocationDisplay();
        return view;
    }

    private void initMap() {
        GoogleMapLayer.MapType mapTypes;
        if (mapType == 1) {
            mapTypes = GoogleMapLayer.MapType.IMAGE;// 卫星地图模式
        } else {
            mapTypes = GoogleMapLayer.MapType.VECTOR;// 矢量地图模式
        }
        googleMapLayer =
                GoogleMapLayer.getInstance(mapTypes);
        googleMapLayer.setName(GoogleMapLayer.MapType.VECTOR.name());
        googleMapLayer.loadAsync();
        Basemap basemap = new Basemap(googleMapLayer);
        ArcGISMap map = new ArcGISMap(basemap);
        map.setMaxScale(Level2Scale.getValueByKey(18));
        map.setMinScale(Level2Scale.getValueByKey(2));
        //116.39138600206913,39.90732395518082
        map.setInitialViewpoint(new Viewpoint(centerPoint, Level2Scale.getValueByKey(4)));//设置初始点
        mMapView.setMap(map);
        mMapView.setAttributionTextVisible(false);
        mMapView.setZOrderMediaOverlay(true);
        mGraphicsOverlay = new GraphicsOverlay();
        mMapView.getGraphicsOverlays().add(mGraphicsOverlay);
        BackgroundGrid grid = new BackgroundGrid();
        grid.setColor(gridAreaColor);
        grid.setGridLineWidth(1);
        grid.setGridLineColor(gridLineColor);
        mMapView.setBackgroundGrid(grid);//设置网格背景
        mMapView.setViewpointRotationAsync(0);
        mMapView.setOnTouchListener(new DefaultMapViewOnTouchListener(getActivity(), mMapView) {

            @Override
            public boolean onRotate(MotionEvent event, double rotationAngle) {
                if (rotateListener != null) {//地图旋转角度
                    rotateListener.rotate(360 - mMapView.getMapRotation());
                }
                return super.onRotate(event, rotationAngle);
            }

            @Override
            public boolean onSingleTapConfirmed(MotionEvent e) {
                final android.graphics.Point screenPoint = new android.graphics.Point(
                        Math.round(e.getX()),
                        Math.round(e.getY()));
                if (mapClickListener != null) {
                    if (mapCanTouchPoint) {//点击地图
                        final ListenableFuture<IdentifyGraphicsOverlayResult> future = mMapView
                                .identifyGraphicsOverlayAsync(mGraphicsOverlay, screenPoint, 2, false);//数字是点击的精度范围
                        future.addDoneListener(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    IdentifyGraphicsOverlayResult result = future.get();
                                    List<Graphic> graphics = result.getGraphics();
                                    if (graphics.size() > 0) {
                                        Graphic graphic = graphics.get(0);
                                        Geometry geometry = graphic.getGeometry();
                                        if (geometry instanceof Point) {//点击某个点
                                            mapClickListener.mapClickPoint(graphic);
                                        } else if (geometry instanceof Polyline) {//点击某条线
                                            mapClickListener.mapClickPolyLine(graphic);
                                        } else if (geometry instanceof Polygon) {//点击多边形
                                            mapClickListener.mapClickPolygon(graphic);
                                        }
                                    } else if (mapCanAddPoint) {
                                        Point mapPoint = mMapView.screenToLocation(screenPoint);
                                        mapClickListener.mapAddPoint(exchangePointMap2Gps(mapPoint));
                                    }
                                } catch (Exception e) {
                                    Log.e("arcgisMap", "Identify error: " + e.getMessage());
                                }
                            }
                        });
                    } else if (mapCanAddPoint) {
                        Point mapPoint = mMapView.screenToLocation(screenPoint);
                        mapClickListener.mapAddPoint(exchangePointMap2Gps(mapPoint));
                    }
                }
                return super.onSingleTapConfirmed(e);
            }
        });
    }

    /**
     * 定位监听
     */
    private void setupLocationDisplay() {
        mLocationDisplay = mMapView.getLocationDisplay();
     /* ** ADD ** */
        mLocationDisplay.addLocationChangedListener(new LocationDisplay.LocationChangedListener() {
            @Override
            public void onLocationChanged(LocationDisplay.LocationChangedEvent locationChangedEvent) {
                if (!isNotFirst) {
                    isNotFirst = true;
                    LocationDataSource.Location location = locationChangedEvent.getLocation();
                    if (location != null) {
                        Point points = location.getPosition();
                        if (points != null) {
                            mMapView.setViewpointCenterAsync(exchangePointGps2Gcj(points));
                        }
                    }
                }
            }
        });
        mLocationDisplay.setAutoPanMode(LocationDisplay.AutoPanMode.OFF);//
        mLocationDisplay.setShowLocation(false);
        mLocationDisplay.setShowAccuracy(false);
        mLocationDisplay.setInitialZoomScale(mMapView.getMapScale());
        mLocationDisplay.startAsync();
    }

    @Override
    public void onPause() {
        super.onPause();
        mMapView.pause();
        if (mLocationDisplay.isStarted()) {
            mLocationDisplay.stop();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        mMapView.resume();
        if (!mLocationDisplay.isStarted()) {
            mLocationDisplay.startAsync();
        }
    }

    @Override
    public void onDestroy() {
        mMapView.dispose();
        super.onDestroy();
        if (googleMapLayer != null) {
            googleMapLayer.onDestroy();
        }
    }

    /**
     * 设置默认的显示级别
     *
     * @param scale
     */
    protected void setDefaultValues(int scale) {
        mMapView.setViewpointScaleAsync(Level2Scale.getValueByKey(scale));
    }

    /**
     * 获取位置
     */
    protected void getLocation() {
        LocationDataSource.Location location = mLocationDisplay.getLocation();
        if (location != null) {
            Point points = location.getPosition();
            if (points != null) {
                mMapView.setViewpointCenterAsync(exchangePointGps2Gcj(points));
                mMapView.setViewpointRotationAsync(0);
                rotateListener.rotate(0);
            }
        }
    }

    /**
     * 获取位置点
     *
     * @return gps点
     */
    protected Point getLocationPoint() {
        Point tempPoint;
        LocationDataSource.Location location = mLocationDisplay.getLocation();
        if (location != null) {
            tempPoint = location.getPosition();
        } else {
            tempPoint = exchangePointGcj2gps(centerPoint);
        }
        return tempPoint;
    }

    /**
     * 设置地图类型
     *
     * @param type 0 矢量  1卫星
     */
    protected void setMapType(int type) {
        if (type == Util.getIntShareData(getActivity(), Util.MAPTYPE, 0)) {
            return;
        }
        Util.setIntShareData(getActivity(), Util.MAPTYPE, type);
        GoogleMapLayer.MapType mapType = GoogleMapLayer.MapType.VECTOR;
        if (type == 0) {
            mapType = GoogleMapLayer.MapType.VECTOR;// 矢量地图模式
        } else if (type == 1) {
            mapType = GoogleMapLayer.MapType.IMAGE;// 卫星地图模式
        }
        GoogleMapLayer webTiledLayer =
                GoogleMapLayer.getInstance(mapType);
        webTiledLayer.loadAsync();
        Basemap basemap = new Basemap(webTiledLayer);
        ArcGISMap arcGISMap = mMapView.getMap();
        arcGISMap.setBasemap(basemap);
        mMapView.setMap(arcGISMap);
    }

    /**
     * 放大
     */
    protected void zoomIn() {
        double mScale = mMapView.getMapScale();
        mMapView.setViewpointScaleAsync(mScale * 0.5);
    }

    /**
     * 缩小
     */
    protected void zoomOut() {
        double mScale = mMapView.getMapScale();
        mMapView.setViewpointScaleAsync(mScale * 2);
    }

    /**
     * gps2火星
     *
     * @param point
     * @return
     */
    @NonNull
    protected Point exchangePointGps2Gcj(Point point) {
        WayPoint oldPoint = GPSTransformBD.getInstance().wgs2gcjFun(point.getY(), point.getX());
        return new Point(oldPoint.longitude, oldPoint.latitude, SpatialReferences.getWgs84());
    }

    /**
     * 火星2gps
     *
     * @param point
     * @return
     */
    @NonNull
    protected Point exchangePointGcj2gps(Point point) {
        WayPoint oldPoint = GPSTransformBD.getInstance().gcj2wgsFun(point.getY(), point.getX());
        return new Point(oldPoint.longitude, oldPoint.latitude, SpatialReferences.getWgs84());
    }

    /**
     * 地图里面的点转成gps点
     *
     * @param mapPoint
     * @return
     */
    protected Point exchangePointMap2Gps(Point mapPoint) {
        Point tempPoint = (Point) GeometryEngine.project(mapPoint, SpatialReferences.getWgs84());
        Point gpsPoint = exchangePointGcj2gps(tempPoint);
        return gpsPoint;
    }

    public interface onRotate {
        void rotate(double angle);//地图旋转角度
    }

    public interface onMapClick {
        void mapClickPoint(Graphic graphic);

        void mapClickPolyLine(Graphic graphic);

        void mapClickPolygon(Graphic graphic);

        void mapAddPoint(Point point);
    }

    public void setRotate(onRotate rotate) {
        this.rotateListener = rotate;
    }

    public void setMapClickListener(onMapClick mapClickListener) {
        this.mapClickListener = mapClickListener;
    }

    /**
     * 设置地图点是否可以点击
     *
     * @param mapCanTouchPoint
     */
    public void setMapCanTouchPoint(boolean mapCanTouchPoint) {
        this.mapCanTouchPoint = mapCanTouchPoint;
    }

    /**
     * 转换资源id 2 symbol
     *
     * @param resId
     * @return
     * @throws ExecutionException
     * @throws InterruptedException
     */
    protected Symbol getViewById(int resId) throws ExecutionException, InterruptedException {
        Bitmap bitmap = BitmapFactory.decodeResource(getResources(), resId);
        BitmapDrawable bitmapDrawable = new BitmapDrawable(getResources(), bitmap);
        ListenableFuture<PictureMarkerSymbol> pictureMarkerSymbol = PictureMarkerSymbol.createAsync(bitmapDrawable);
        return pictureMarkerSymbol.get();
    }

    /**
     * 移动到某个点
     *
     * @param point 点
     * @param scale zoom级别不设置传-1
     */
    protected void move2Point(Point point, int scale) {
        double mScale = mMapView.getMapScale();
        if (scale != -1) {
            mScale = Level2Scale.getValueByKey(scale);
        }
        mMapView.setViewpointCenterAsync(point, mScale);
        mMapView.setViewpointRotationAsync(0);
        rotateListener.rotate(0);
    }

    /**
     * 添加点
     *
     * @param point 点
     * @param resId 资源
     * @return
     */
    protected Graphic addPoint(Point point, int resId) {
        return addPoint(point, resId, 0, 0);
    }

    /**
     * 添加点
     *
     * @param point 点
     * @param resId 资源
     * @param angle 角度
     * @return
     */
    protected Graphic addPoint(Point point, int resId, float angle) {
        return addPoint(point, resId, angle, 0);
    }

    /**
     * 添加点
     *
     * @param point 点
     * @param resId 资源
     * @param z     z轴高度
     * @return 点
     */
    protected Graphic addPoint(Point point, int resId, int z) {
        return addPoint(point, resId, 0, z);
    }

    /**
     * 添加点
     *
     * @param point 点
     * @param resId 资源
     * @param angle 角度
     * @param z     z轴高度
     * @return
     */
    protected Graphic addPoint(Point point, int resId, float angle, int z) {
        Graphic pointGraphic = null;
        try {
            PictureMarkerSymbol symbol = (PictureMarkerSymbol) getViewById(resId);
            symbol.setAngle(angle);
            pointGraphic = new Graphic(point, symbol);
            pointGraphic.setZIndex(z);
        } catch (Exception e) {
            e.printStackTrace();
        }
        mGraphicsOverlay.getGraphics().add(pointGraphic);
        return pointGraphic;
    }

    /**
     * 画圆
     *
     * @param point     中心点
     * @param radius    半径
     * @param lineColor 线颜色
     * @param areaColor 区域颜色
     * @param lineWidth 线宽度
     * @param z         z轴
     * @return
     */
    protected Graphic drawCircle(Point point, double radius, int lineColor, int areaColor, float lineWidth, int z) {
        Point tempPoint = (Point) GeometryEngine.project(point, SpatialReference.create(3857));
        List<Point> points = getPoints(tempPoint, radius);
        PointCollection mPointCollection = new PointCollection(SpatialReferences.getWebMercator());
        mPointCollection.addAll(points);
        Polygon polygon = new Polygon(mPointCollection);
        SimpleLineSymbol lineSymbol = new SimpleLineSymbol(SimpleLineSymbol.Style.SOLID, lineColor, lineWidth);
        SimpleFillSymbol simpleFillSymbol = new SimpleFillSymbol(SimpleFillSymbol.Style.SOLID, areaColor, lineSymbol);
        Graphic graphic = new Graphic(polygon, simpleFillSymbol);
        graphic.setZIndex(z);
        mGraphicsOverlay.getGraphics().add(graphic);
        return graphic;
    }

    /**
     * 通过中心点和半径计算得出圆形的边线点集合
     *
     * @param center
     * @param radius
     * @return
     */
    private List<Point> getPoints(Point center, double radius) {
        int pointSize = 50;
        List<Point> pointList = new ArrayList<>();
        double sin;
        double cos;
        double x;
        double y;
        double tempRadius = radius * 1.3;
        for (double i = 0; i < pointSize; i++) {
            sin = Math.sin(Math.PI * 2 * i / pointSize);
            cos = Math.cos(Math.PI * 2 * i / pointSize);
            x = center.getX() + tempRadius * sin;
            y = center.getY() + tempRadius * cos;
            pointList.add(new Point(x, y));
        }
        return pointList;
    }

    /**
     * 画线
     *
     * @param list      点集合
     * @param lineColor 线颜色
     * @param lineWidth 线宽度
     * @param z         z轴高度
     * @return 线
     */
    protected Graphic drawLine(List<Point> list, int lineColor, float lineWidth, int z) {
        PointCollection TempPoints = new PointCollection(SpatialReferences.getWgs84());
        TempPoints.addAll(list);
        Polyline polyline = new Polyline(TempPoints);
        SimpleLineSymbol polylineSymbol = new SimpleLineSymbol(SimpleLineSymbol.Style.SOLID, lineColor, lineWidth);
        Graphic graphic = new Graphic(polyline, polylineSymbol);
        graphic.setZIndex(z);
        mGraphicsOverlay.getGraphics().add(graphic);
        return graphic;
    }

    /**
     * 画多边形
     *
     * @param points    点集合
     * @param lineColor 线颜色
     * @param areaColor 面颜色
     * @param lineWidth 线宽度
     * @param z         z轴高度
     * @return 面
     */
    protected Graphic addPolygon(List<Point> points, int lineColor, int areaColor, float lineWidth, int z) {
        PointCollection pointCollection = new PointCollection(SpatialReferences.getWgs84());
        pointCollection.addAll(points);
        Polygon polygon = new Polygon(pointCollection);
        SimpleLineSymbol polylineSymbol = new SimpleLineSymbol(SimpleLineSymbol.Style.SOLID, lineColor, lineWidth);
        SimpleFillSymbol polygonSymbol = new SimpleFillSymbol(SimpleFillSymbol.Style.SOLID, areaColor, polylineSymbol);
        Graphic polygonGraphic = new Graphic(polygon, polygonSymbol);
        polygonGraphic.setZIndex(z);
        mGraphicsOverlay.getGraphics().add(polygonGraphic);
        return polygonGraphic;
    }

    /**
     * 计算两点间距离长度
     *
     * @param point0 点
     * @param point1 点
     * @return 米
     */
    protected double calcPolylineLength(Point point0, Point point1) {
        PointCollection borderCAtoNV = new PointCollection(SpatialReferences.getWgs84());
        borderCAtoNV.add(point0);
        borderCAtoNV.add(point1);
        Polyline polyline = new Polyline(borderCAtoNV);
        double lengthPolyline = GeometryEngine.lengthGeodetic(polyline, new LinearUnit(LinearUnitId.METERS),
                GeodeticCurveType.GEODESIC);
        return lengthPolyline;
    }

    /**
     * 计算面积
     *
     * @param pointList 点集合
     * @return 亩
     */
    protected double calcPolygonArea(List<Point> pointList) {
        PointCollection borderCAtoNV = new PointCollection(SpatialReferences.getWgs84());
        borderCAtoNV.addAll(pointList);
        Polygon polygon = new Polygon(borderCAtoNV);
        double areaPolygon = GeometryEngine.areaGeodetic(polygon, new AreaUnit(AreaUnitId.HECTARES),
                GeodeticCurveType.GEODESIC) * 15;//公顷 * 15 = 亩
        return areaPolygon;
    }

    /**
     * 获取地图中心点坐标
     *
     * @return gps点坐标
     */
    protected Point getCenterPoint() {
        Polygon polygon = mMapView.getVisibleArea();
        Envelope envelope = polygon.getExtent();
        Point point = envelope.getCenter();
        return exchangePointMap2Gps(point);
    }

    /**
     * 移除Graphic
     *
     * @param graphic 点对象 线对象 多边形对象
     */
    protected void removeGraphic(Graphic graphic) {
        if (graphic != null) {
            mGraphicsOverlay.getGraphics().remove(graphic);
        }
    }
}
