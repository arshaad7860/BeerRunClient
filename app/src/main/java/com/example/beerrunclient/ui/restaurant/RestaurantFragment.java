package com.example.beerrunclient.ui.restaurant;

import androidx.appcompat.app.AlertDialog;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelProviders;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.view.animation.LayoutAnimationController;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.example.beerrunclient.Adapter.MyRestaurantAdapter;
import com.example.beerrunclient.EventBus.CounterCartEvent;
import com.example.beerrunclient.EventBus.HideFABCart;
import com.example.beerrunclient.EventBus.MenuInflateEvent;
import com.example.beerrunclient.R;

import org.greenrobot.eventbus.EventBus;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;

public class RestaurantFragment extends Fragment {

    private RestaurantViewModel mViewModel;

    Unbinder unbinder;
    @BindView(R.id.recycler_restaurant)
    RecyclerView recycler_restaurant;
    AlertDialog dialog;
    LayoutAnimationController layoutAnimationController;
    MyRestaurantAdapter adapter;

    public static RestaurantFragment newInstance() {
        return new RestaurantFragment();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        mViewModel = new ViewModelProvider(this).get(RestaurantViewModel.class);
        View root = inflater.inflate(R.layout.fragment_restaurant, container, false);
        unbinder = ButterKnife.bind(this,root);
        initViews();

        mViewModel.getMessageError().observe(getViewLifecycleOwner(),message->{
            Toast.makeText(getContext(), ""+message, Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });
        mViewModel.getRestaurantListMutable().observe(getViewLifecycleOwner(), restaurantModels -> {
            dialog.dismiss();
            adapter= new MyRestaurantAdapter(getContext(),restaurantModels);
            recycler_restaurant.setAdapter(adapter);
            recycler_restaurant.setLayoutAnimation(layoutAnimationController);
        });
        return root;
    }

    private void initViews() {
        EventBus.getDefault().postSticky(new HideFABCart(true));
        setHasOptionsMenu(true);
        dialog = new AlertDialog.Builder(getContext()).setCancelable(false)
                .setMessage("Please wait...").create();
        dialog.show();
        layoutAnimationController = AnimationUtils.loadLayoutAnimation(getContext(),R.anim.layout_item_from_left);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getContext());
        linearLayoutManager.setOrientation(RecyclerView.VERTICAL);
        recycler_restaurant.setLayoutManager(linearLayoutManager);
        recycler_restaurant.addItemDecoration(new DividerItemDecoration(getContext(),linearLayoutManager.getOrientation()));

    }

    @Override
    public void onResume() {
        super.onResume();
        EventBus.getDefault().postSticky(new CounterCartEvent(true));
        EventBus.getDefault().postSticky(new MenuInflateEvent(false));
    }
}
