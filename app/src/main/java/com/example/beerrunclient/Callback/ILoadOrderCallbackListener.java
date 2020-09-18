package com.example.beerrunclient.Callback;

import com.example.beerrunclient.Model.OrderModel;

import java.util.List;

public interface ILoadOrderCallbackListener {
    void onLoadOrderSuccess(List<OrderModel> orderModelList);
    void onLoadOrderFailed(String message);
}
