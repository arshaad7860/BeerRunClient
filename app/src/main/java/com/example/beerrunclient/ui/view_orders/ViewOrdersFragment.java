package com.example.beerrunclient.ui.view_orders;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.androidwidgets.formatedittext.widgets.FormatEditText;
import com.example.beerrunclient.Adapter.MyOrdersAdapter;
import com.example.beerrunclient.Callback.ILoadOrderCallbackListener;
import com.example.beerrunclient.Common.Common;
import com.example.beerrunclient.Common.MySwipeHelper;
import com.example.beerrunclient.Database.CartDataSource;
import com.example.beerrunclient.Database.CartDatabase;
import com.example.beerrunclient.Database.CartItem;
import com.example.beerrunclient.Database.LocalCartDataSource;
import com.example.beerrunclient.EventBus.CounterCartEvent;
import com.example.beerrunclient.EventBus.MenuItemBack;
import com.example.beerrunclient.GoogleMaps.TrackingOrderActivity;
import com.example.beerrunclient.Model.OrderModel;
import com.example.beerrunclient.Model.RefundRequestModel;
import com.example.beerrunclient.Model.ShippingOrderModel;
import com.example.beerrunclient.R;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.greenrobot.eventbus.EventBus;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import dmax.dialog.SpotsDialog;
import io.reactivex.SingleObserver;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class ViewOrdersFragment extends Fragment implements ILoadOrderCallbackListener {

    private ViewOrdersViewModel viewOrdersViewModel;

    CartDataSource cartDataSource;
    CompositeDisposable compositeDisposable = new CompositeDisposable();

    @BindView(R.id.recycler_orders)
    RecyclerView recycler_orders;
    AlertDialog dialog;
    MyOrdersAdapter adapter;

    private ILoadOrderCallbackListener listener;

    private Unbinder unbinder;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        viewOrdersViewModel =
                new ViewModelProvider(this).get(ViewOrdersViewModel.class);
        View root = inflater.inflate(R.layout.fragment_view_order, container, false);
        unbinder = ButterKnife.bind(this, root);
        initViews(root);
        loadOrdersFromFirebase();
        viewOrdersViewModel.getMutableLiveDataOrderList().observe(getViewLifecycleOwner(), orderList -> {
            Collections.reverse(orderList);
            adapter = new MyOrdersAdapter(getContext(), orderList);
            recycler_orders.setAdapter(adapter);
        });
        return root;
    }

    private void loadOrdersFromFirebase() {
        List<OrderModel> orderModelList = new ArrayList<>();
        FirebaseDatabase.getInstance()
                .getReference(Common.RESTAURANT_REF)
                .child(Common.currentRestaurant.getUid())
                .child(Common.ORDER_REF)
                .orderByChild("userId")
                .equalTo(Common.currentUser.getUid())
                .limitToLast(100)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        for (DataSnapshot orderSnapShot : dataSnapshot.getChildren()) {
                            OrderModel orderModel = orderSnapShot.getValue(OrderModel.class);
                            orderModel.setOrderNumber(orderSnapShot.getKey());
                            orderModelList.add(orderModel);
                        }
                        listener.onLoadOrderSuccess(orderModelList);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        listener.onLoadOrderFailed(databaseError.getMessage());
                    }
                });
    }

    private void initViews(View root) {
        cartDataSource = new LocalCartDataSource(CartDatabase.getInstance(getContext()).cartDAO());

        listener = this;
        dialog = new SpotsDialog.Builder().setContext(getContext()).setCancelable(false).build();
        recycler_orders.setHasFixedSize(true);
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        recycler_orders.setLayoutManager(layoutManager);
        recycler_orders.addItemDecoration(new DividerItemDecoration(getContext(), layoutManager.getOrientation()));

        MySwipeHelper mySwipeHelper = new MySwipeHelper(getContext(), recycler_orders, 250) {
            @Override
            public void instantiateMyButton(RecyclerView.ViewHolder viewHolder, List<MyButton> buf) {
                buf.add(new MyButton(getContext(), "Cancel Order", 30, 0, Color.parseColor("#FF3C30"),
                        pos -> {
                            OrderModel orderModel = ((MyOrdersAdapter) recycler_orders.getAdapter()).getItemAtPosition(pos);
                            if (orderModel.getOrderStatus() == 0) {
                                if (orderModel.isCod()) {
                                    androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(getContext());
                                    builder.setTitle("Cancel Order")
                                            .setMessage("Are you sure you want to cancel this order?")
                                            .setNegativeButton("Cancel", (dialogInterface, i) -> dialogInterface.dismiss())
                                            .setPositiveButton("Confirm", (dialogInterface, i) -> {
                                                Map<String, Object> update_data = new HashMap<>();
                                                update_data.put("orderStatus", -1);//cancel order
                                                FirebaseDatabase.getInstance()
                                                        .getReference(Common.RESTAURANT_REF)
                                                        .child(Common.currentRestaurant.getUid())
                                                        .child(Common.ORDER_REF)
                                                        .child(orderModel.getOrderNumber())
                                                        .updateChildren(update_data)
                                                        .addOnFailureListener(e -> Toast.makeText(getContext(), "" + e.getMessage(), Toast.LENGTH_SHORT).show())
                                                        .addOnSuccessListener(aVoid -> {
                                                            orderModel.setOrderStatus(-1); //local update
                                                            ((MyOrdersAdapter) recycler_orders.getAdapter()).setItemAtPosition(pos, orderModel);
                                                            recycler_orders.getAdapter().notifyItemChanged(pos);
                                                            Toast.makeText(getContext(), "Order Cancelled Successfully", Toast.LENGTH_SHORT).show();
                                                        });
                                            });
                                    androidx.appcompat.app.AlertDialog dialog = builder.create();
                                    dialog.show();
                                } else // Not COD card payment refund
                                {
                                    View layout_refund_request = LayoutInflater.from(getContext())
                                            .inflate(R.layout.layout_refund_request, null);

                                    EditText edt_name = (EditText) layout_refund_request.findViewById(R.id.edt_card_name);
                                    FormatEditText edt_card_number = layout_refund_request.findViewById(R.id.edt_card_number);
                                    FormatEditText edt_card_exp = layout_refund_request.findViewById(R.id.edt_exp);

                                    //format credit card
                                    edt_card_number.setFormat("---- ---- ---- ----");
                                    edt_card_exp.setFormat("--/--");


                                    androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(getContext());
                                    builder.setTitle("Cancel Order")
                                            .setMessage("Are you sure you want to cancel this order?")
                                            .setView(layout_refund_request)
                                            .setNegativeButton("Cancel", (dialogInterface, i) -> dialogInterface.dismiss())
                                            .setPositiveButton("Confirm", (dialogInterface, i) -> {
                                                RefundRequestModel refundRequestModel = new RefundRequestModel();
                                                refundRequestModel.setName(Common.currentUser.getName());
                                                refundRequestModel.setPhone(Common.currentUser.getPhone());
                                                refundRequestModel.setCardName(edt_name.getText().toString());
                                                refundRequestModel.setCardNumber(edt_card_number.getText().toString());
                                                refundRequestModel.setCardExp(edt_card_exp.getText().toString());
                                                refundRequestModel.setAmount(orderModel.getFinalPayment());


                                                FirebaseDatabase.getInstance()
                                                        .getReference(Common.RESTAURANT_REF)
                                                        .child(Common.currentRestaurant.getUid())
                                                        .child(Common.REQUEST_REFUND_REF)
                                                        .child(orderModel.getOrderNumber())
                                                        .setValue(refundRequestModel)
                                                        .addOnFailureListener(e -> Toast.makeText(getContext(), "" + e.getMessage(), Toast.LENGTH_SHORT).show())
                                                        .addOnSuccessListener(aVoid -> {
                                                            //update firebase
                                                            Map<String, Object> update_data = new HashMap<>();
                                                            update_data.put("orderStatus", -1);//cancel order
                                                            FirebaseDatabase.getInstance()
                                                                    .getReference(Common.RESTAURANT_REF)
                                                                    .child(Common.currentRestaurant.getUid())
                                                                    .child(Common.ORDER_REF)
                                                                    .child(orderModel.getOrderNumber())
                                                                    .updateChildren(update_data)
                                                                    .addOnFailureListener(e -> Toast.makeText(getContext(), "" + e.getMessage(), Toast.LENGTH_SHORT).show())
                                                                    .addOnSuccessListener(a -> {
                                                                        orderModel.setOrderStatus(-1); //local update
                                                                        ((MyOrdersAdapter) recycler_orders.getAdapter()).setItemAtPosition(pos, orderModel);
                                                                        recycler_orders.getAdapter().notifyItemChanged(pos);
                                                                        Toast.makeText(getContext(), "Order Cancelled Successfully", Toast.LENGTH_SHORT).show();
                                                                    });
                                                        });
                                            });
                                    androidx.appcompat.app.AlertDialog dialog = builder.create();
                                    dialog.show();
                                }
                            } else {
                                Toast.makeText(getContext(), new StringBuilder("Order changed to: ")
                                        .append(Common.convertStatusToText(orderModel.getOrderStatus()))
                                        .append(", order can't be canceled")
                                        .toString(), Toast.LENGTH_SHORT).show();
                            }
                        }));
                buf.add(new MyButton(getContext(), "Track Order", 30, 0, Color.parseColor("#001970"),
                        pos -> {
                            OrderModel orderModel = ((MyOrdersAdapter) recycler_orders.getAdapter()).getItemAtPosition(pos);

                            //fetch from firebase
                            FirebaseDatabase.getInstance()
                                    .getReference(Common.RESTAURANT_REF)
                                    .child(Common.currentRestaurant.getUid())
                                    .child(Common.SHIPPING_ORDER_REF)
                                    .child(orderModel.getOrderNumber())
                                    .addListenerForSingleValueEvent(new ValueEventListener() {
                                        @Override
                                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                            if (dataSnapshot.exists()) {
                                                Common.currentShippingOrder = dataSnapshot.getValue(ShippingOrderModel.class);
                                                Common.currentShippingOrder.setKey(dataSnapshot.getKey());
                                                if (Common.currentShippingOrder.getCurrentLat() != -1 &&
                                                        Common.currentShippingOrder.getCurrentLng() != -1) {
                                                    startActivity(new Intent(getContext(), TrackingOrderActivity.class));
                                                } else {
                                                    Toast.makeText(getContext(), "Your order is still being prepared", Toast.LENGTH_SHORT).show();
                                                }
                                            } else {
                                                Toast.makeText(getContext(), "Your order was just placed. Please wait till your status changes to Shipping", Toast.LENGTH_SHORT).show();
                                            }
                                        }

                                        @Override
                                        public void onCancelled(@NonNull DatabaseError databaseError) {
                                            Toast.makeText(getContext(), "" + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
                                        }
                                    });
                        }));

                buf.add(new MyButton(getContext(), "Repeat Last Order", 30, 0, Color.parseColor("#5d4037"),
                        pos -> {
                            OrderModel orderModel = ((MyOrdersAdapter) recycler_orders.getAdapter()).getItemAtPosition(pos);

                            dialog.show();
                            cartDataSource.cleanCart(Common.currentUser.getUid(),Common.currentRestaurant.getUid()) //clear all items in cart
                                    .subscribeOn(Schedulers.io())
                                    .observeOn(AndroidSchedulers.mainThread())
                                    .subscribe(
                                            new SingleObserver<Integer>() {
                                                @Override
                                                public void onSubscribe(Disposable d) {

                                                }

                                                @Override
                                                public void onSuccess(Integer integer) {
                                                    //After clean cart just add new
                                                    CartItem[] cartItems = orderModel
                                                            .getCartItemList().toArray(new CartItem[orderModel.getCartItemList().size()]);
                                                    //insert new
                                                    compositeDisposable.add(
                                                            cartDataSource.insertOrReplaceAll(cartItems)
                                                                    .subscribeOn(Schedulers.io())
                                                                    .observeOn(AndroidSchedulers.mainThread())
                                                                    .subscribe(
                                                                            () -> {
                                                                                dialog.dismiss();
                                                                                Toast.makeText(getContext(), "Add all items in order to cart successful", Toast.LENGTH_SHORT).show();
                                                                                EventBus.getDefault().postSticky(new CounterCartEvent(true));

                                                                            }, throwable -> {
                                                                                dialog.dismiss();
                                                                                Toast.makeText(getContext(), "" + throwable.getMessage(), Toast.LENGTH_SHORT).show();

                                                                            }
                                                                    )
                                                    );


                                                }

                                                @Override
                                                public void onError(Throwable e) {
                                                    dialog.dismiss();
                                                    Toast.makeText(getContext(), "[Error]" + e.getMessage(), Toast.LENGTH_SHORT).show();
                                                }
                                            }
                                    );
                        }));
            }
        };


    }

    @Override
    public void onLoadOrderSuccess(List<OrderModel> orderModelList) {
        dialog.dismiss();
        viewOrdersViewModel.setMutableLiveDataOrderList(orderModelList);

    }

    @Override
    public void onLoadOrderFailed(String message) {
        dialog.dismiss();
        Toast.makeText(getContext(), "" + message, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onStop() {
        compositeDisposable.clear();
        super.onStop();
    }

    @Override
    public void onDestroy() {
        EventBus.getDefault().postSticky(new MenuItemBack());
        super.onDestroy();
    }
}