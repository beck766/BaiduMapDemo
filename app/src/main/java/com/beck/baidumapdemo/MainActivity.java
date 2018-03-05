package com.beck.baidumapdemo;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ZoomControls;

import com.baidu.location.BDLocation;
import com.baidu.location.BDLocationListener;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.baidu.mapapi.SDKInitializer;
import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.MapStatus;
import com.baidu.mapapi.map.MapStatusUpdate;
import com.baidu.mapapi.map.MapStatusUpdateFactory;
import com.baidu.mapapi.map.MapView;
import com.baidu.mapapi.map.MyLocationData;
import com.baidu.mapapi.model.LatLng;
import com.baidu.mapapi.search.core.SearchResult;
import com.baidu.mapapi.search.poi.OnGetPoiSearchResultListener;
import com.baidu.mapapi.search.poi.PoiDetailResult;
import com.baidu.mapapi.search.poi.PoiIndoorResult;
import com.baidu.mapapi.search.poi.PoiNearbySearchOption;
import com.baidu.mapapi.search.poi.PoiResult;
import com.baidu.mapapi.search.poi.PoiSearch;
import com.baidu.mapapi.search.poi.PoiSortType;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;


public class MainActivity extends AppCompatActivity {


    private static final String TAG = MainActivity.class.getSimpleName();

    @BindView(R.id.baidu_map_city)
    TextView baiduMapCity;

    @BindView(R.id.baidu_map_card_view)
    CardView baiduMapCardView;

    @BindView(R.id.bmapView)
    MapView bmapView;

    @BindView(R.id.baidu_map_location)
    ImageView baiduMapLocation;

    @BindView(R.id.baidu_map_recycler_view)
    RecyclerView baiduMapRecyclerView;

    private List mapAddrBeanList = new ArrayList<>();
    private LocationClient mLocationClient;
    private BaiduMap baiduMap;
    private LatLng mLatLng;
    private PoiSearch mPoiSearch;
    private boolean isFirstLocate = true;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mLocationClient = new LocationClient(getApplicationContext());
        mLocationClient.registerLocationListener(new MyLocationListener());
        SDKInitializer.initialize(getApplicationContext());
        statePermission();

        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        initMapView();
    }

    private void initMapView() {
        baiduMap = bmapView.getMap();
        //设置为普通模式的地图
        baiduMap.setMapType(BaiduMap.MAP_TYPE_NORMAL);
        // 开启定位图层
        baiduMap.setMyLocationEnabled(true);
        //隐藏LOGO、地图比例尺、缩放控件
        View child = bmapView.getChildAt(1);
        if (child != null && (child instanceof ImageView || child instanceof ZoomControls)) {
            child.setVisibility(View.GONE);
        }
        bmapView.showScaleControl(false);
        bmapView.showZoomControls(false);
    }

    /**
     * 声明权限
     */
    private void statePermission() {
        List<String> permissionList = new ArrayList<>();
        if (ContextCompat.checkSelfPermission(this, Manifest.
                permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            permissionList.add(Manifest.permission.ACCESS_FINE_LOCATION);
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.
                permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            permissionList.add(Manifest.permission.READ_PHONE_STATE);
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.
                permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            permissionList.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }
        if (!permissionList.isEmpty()) {
            String[] permissions = permissionList.toArray(new String[permissionList.size()]);
            ActivityCompat.requestPermissions(this, permissions, 1);
        } else {
            requestLocation();
        }
    }

    private void requestLocation() {
        initLocation();
        mLocationClient.start();

    }

    private void initLocation() {
        LocationClientOption option = new LocationClientOption();
        option.setOpenGps(true);//打开GPS
        option.setLocationMode(LocationClientOption.LocationMode.Hight_Accuracy);//设置定位模式
        option.setCoorType("bd0911");//返回的定位结果是百度经纬度，默认值是bd0911
        option.setScanSpan(5000);//设置发起定位请求的时间间隔为5000ms
        option.setIsNeedAddress(true);//返回的定位结果饱饭地址信息
        option.setNeedDeviceDirect(true);// 返回的定位信息包含手机的机头方向
        mLocationClient.setLocOption(option);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case 1:
                if (grantResults.length > 0) {
                    for (int result : grantResults) {
                        if (result != PackageManager.PERMISSION_GRANTED) {
                            Toast.makeText(MainActivity.this, "必须同意所有权限才能使用本程序",
                                    Toast.LENGTH_SHORT).show();
                            this.finish();
                            return;
                        }
                    }
                    requestLocation();
                } else {
                    Toast.makeText(MainActivity.this, "发生未知错误", Toast.LENGTH_SHORT).show();
                    this.finish();
                }
                break;
            default:
        }
    }

    /**
     * 传入位置生成地图
     */
    private void navigateTo() {
        if (isFirstLocate) {
            MapStatusUpdate update = MapStatusUpdateFactory.newLatLng(mLatLng);
            baiduMap.animateMapStatus(update);
            update = MapStatusUpdateFactory.zoomTo(20f);
            baiduMap.animateMapStatus(update);
            isFirstLocate = false;
        }

    }

    public class MyLocationListener implements BDLocationListener {

        @Override
        public void onReceiveLocation(BDLocation bdLocation) {
            baiduMapCity.setText(bdLocation.getCity());
            mLatLng = new LatLng(bdLocation.getLatitude(), bdLocation.getLongitude());
            // 构造定位数据
            MyLocationData locData = new MyLocationData.Builder()
                    .accuracy(bdLocation.getRadius())
                    // 此处设置开发者获取到的方向信息，顺时针0-360
                    .direction(100).latitude(bdLocation.getLatitude())
                    .longitude(bdLocation.getLongitude()).build();
            //初始化后自行定位
            MapStatus.Builder builder = new MapStatus.Builder();
            builder.overlook(0);
            builder.zoom(20);
            builder.target(mLatLng);
            baiduMap.animateMapStatus(MapStatusUpdateFactory.newMapStatus(builder.build()));
            // 设置定位数据
            baiduMap.setMyLocationData(locData);
            if (bdLocation.getLocType() == BDLocation.TypeGpsLocation
                    || bdLocation.getLocType() == BDLocation.TypeNetWorkLocation) {
                navigateTo();
                searchNeayBy();
            }
        }
    }

    private void searchNeayBy() {
        // POI初始化搜索模块，注册搜索事件监听
        mPoiSearch = PoiSearch.newInstance();

        //创建POI检索监听者
        OnGetPoiSearchResultListener poiListener = new OnGetPoiSearchResultListener() {

            //获取POI检索结果
            @Override
            public void onGetPoiResult(PoiResult poiResult) {
                Log.d(TAG, "onGetPoiResult: " + poiResult);
                if (poiResult == null || poiResult.error == SearchResult.ERRORNO.RESULT_NOT_FOUND) {
                    Toast.makeText(MainActivity.this, "未找到结果", Toast.LENGTH_SHORT).show();
                    Log.i(TAG, "onGetPoiResult: ======>null");
                    return;
                }
                if (poiResult.error == SearchResult.ERRORNO.NO_ERROR) {
                    if (poiResult != null) {
                        if (poiResult.getAllPoi() != null && poiResult.getAllPoi().size() > 0) {
                            if (mapAddrBeanList != null) {
                                mapAddrBeanList.clear();
                            }
                            mapAddrBeanList.addAll(poiResult.getAllPoi());
                            Log.i(TAG, "onGetPoiResult: success");
                            for (int i = 0; i < poiResult.getAllPoi().size(); i++) {
                                Log.i(TAG, "onGetPoiResult: " + poiResult.getAllPoi().get(i).address);
                            }
                            //mapAddrAdapter.notifyDataSetChanged();
                        }
                    }
                }
                Log.i(TAG, "onGetPoiResult: ===>Poi" + poiResult.getAllPoi().size());

            }

            //获取Place详情页检索结果
            @Override
            public void onGetPoiDetailResult(PoiDetailResult poiDetailResult) {

            }

            @Override
            public void onGetPoiIndoorResult(PoiIndoorResult poiIndoorResult) {

            }
        };
        //设置POI检索监听者；
        mPoiSearch.setOnGetPoiSearchResultListener(poiListener);
        //发起检索请求
        PoiNearbySearchOption poiNearbySearchOption = new PoiNearbySearchOption().keyword("餐厅").location(mLatLng).sortType(PoiSortType.distance_from_near_to_far)
                .radius(1000)  // 检索半径，单位是米
                .pageNum(10);

        Log.i(TAG, "searchNeayBy: ===>" + mLatLng);
        mPoiSearch.searchNearby(poiNearbySearchOption);  // 发起附近检索请求
    }


}
