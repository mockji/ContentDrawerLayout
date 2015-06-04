package com.sohu.kuaizhan.sitebuilder.utils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.telephony.TelephonyManager;

/**
 * Created by zhulijun on 5/7/15.
 */
public class NetworkUtil {

    public static boolean isNetworkAvailable(Context context){
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = cm.getActiveNetworkInfo();
        return networkInfo != null && networkInfo.isConnectedOrConnecting();
    }

    public static String getCarrier(Context context){
        TelephonyManager manager = (TelephonyManager)context.getSystemService(context.TELEPHONY_SERVICE);
        return manager.getNetworkOperatorName();
    }

    public static String getNetworkType(Context context){
        String netType = new String("no wifi or cellular");

        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
        if (networkInfo != null && networkInfo.isConnectedOrConnecting()) {
            if(networkInfo.getType() == ConnectivityManager.TYPE_MOBILE){
                switch (networkInfo.getSubtype()){
                    case TelephonyManager.NETWORK_TYPE_GPRS:
                    case TelephonyManager.NETWORK_TYPE_EDGE:
                    case TelephonyManager.NETWORK_TYPE_CDMA:
                    case TelephonyManager.NETWORK_TYPE_1xRTT:
                    case TelephonyManager.NETWORK_TYPE_IDEN:
                        netType = "2G";
                        break;
                    case TelephonyManager.NETWORK_TYPE_UMTS:
                    case TelephonyManager.NETWORK_TYPE_EVDO_0:
                    case TelephonyManager.NETWORK_TYPE_EVDO_A:
                    case TelephonyManager.NETWORK_TYPE_HSDPA:
                    case TelephonyManager.NETWORK_TYPE_HSUPA:
                    case TelephonyManager.NETWORK_TYPE_HSPA:
                    case TelephonyManager.NETWORK_TYPE_EVDO_B:
                    case TelephonyManager.NETWORK_TYPE_EHRPD:
                    case TelephonyManager.NETWORK_TYPE_HSPAP:
                        netType = "3G";
                        break;
                    case TelephonyManager.NETWORK_TYPE_LTE:
                        netType = "4G";
                        break;
                    default:
                        netType = "unable to identify";
                }
            }

            if(networkInfo.getType() == ConnectivityManager.TYPE_WIFI){
                netType = "WiFi";
            }
        }
        return netType;
    }


}
