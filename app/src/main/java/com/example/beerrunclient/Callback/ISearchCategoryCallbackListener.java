package com.example.beerrunclient.Callback;

import com.example.beerrunclient.Database.CartItem;
import com.example.beerrunclient.Model.CategoryModel;
import com.example.beerrunclient.Model.FoodModel;

public interface ISearchCategoryCallbackListener {
    void onSearchCategoryFound(CategoryModel categoryModel, CartItem cartItem);
    void onSearchCategoryNotFound(String message);
}
