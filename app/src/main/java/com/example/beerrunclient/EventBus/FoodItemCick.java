package com.example.beerrunclient.EventBus;

import com.example.beerrunclient.Model.FoodModel;

public class FoodItemCick {
    private boolean success;
    private FoodModel foodModel;

    public FoodItemCick(boolean success, FoodModel foodModel) {
        this.success = success;
        this.foodModel = foodModel;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public FoodModel getFoodModel() {
        return foodModel;
    }

    public void setFoodModel(FoodModel foodModel) {
        this.foodModel = foodModel;
    }
}
