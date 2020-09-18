package com.example.beerrunclient.Callback;

import com.example.beerrunclient.Model.CommentModel;

import java.util.List;

public interface ICommentCallbackListener {
    void onCommentLoadSuccess(List<CommentModel> commentModels);

    void onCommentLoadFailed(String message);
}
