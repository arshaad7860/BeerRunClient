package com.example.beerrunclient.Callback;

import com.example.beerrunclient.Model.BestDealModel;
import com.example.beerrunclient.Model.PopularCategoryModel;

import java.util.List;

public interface IBestDealCallbackListener {
    void onBestDealLoadSuccess(List<BestDealModel> bestDealModels);
    void onBestDealLoadFailed(String message);
}
