package com.example.beerrunclient.EventBus;

public class MenuInflateEvent {
    private boolean isShowDetail;

    public MenuInflateEvent(boolean isShowDetail) {
        this.isShowDetail = isShowDetail;
    }

    public boolean isShowDetail() {
        return isShowDetail;
    }

    public void setShowDetail(boolean showDetail) {
        isShowDetail = showDetail;
    }
}
