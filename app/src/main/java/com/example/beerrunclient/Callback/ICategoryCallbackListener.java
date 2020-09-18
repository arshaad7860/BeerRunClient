package com.example.beerrunclient.Callback;

import com.example.beerrunclient.Model.BestDealModel;
import com.example.beerrunclient.Model.CategoryModel;

import java.util.List;

public interface ICategoryCallbackListener {
    void onCategoryLoadSuccess(List<CategoryModel> categoryModels);
    void onCategoryLoadFailed(String message);
}
