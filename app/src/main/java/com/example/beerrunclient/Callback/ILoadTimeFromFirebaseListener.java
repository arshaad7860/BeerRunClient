package com.example.beerrunclient.Callback;

import com.example.beerrunclient.Model.OrderModel;

public interface ILoadTimeFromFirebaseListener {
    void onLoadTimeSuccess(OrderModel orderModel, long estimatedTimeInMs);
    void onLoadOnlyTimeSuccess(long estimatedTimeInMs);

    void onLoadTimeFailed(String message);

}
