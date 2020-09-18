package com.example.beerrunclient.ui.cart;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.os.Looper;
import android.os.Parcelable;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.view.menu.MenuView;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.braintreepayments.api.dropin.DropInRequest;
import com.braintreepayments.api.dropin.DropInResult;
import com.braintreepayments.api.models.PaymentMethodNonce;
import com.example.beerrunclient.Adapter.MyCartAdapter;
import com.example.beerrunclient.Callback.ILoadTimeFromFirebaseListener;
import com.example.beerrunclient.Callback.ISearchCategoryCallbackListener;
import com.example.beerrunclient.Common.Common;
import com.example.beerrunclient.Common.MySwipeHelper;
import com.example.beerrunclient.Database.CartDataSource;
import com.example.beerrunclient.Database.CartDatabase;
import com.example.beerrunclient.Database.CartItem;
import com.example.beerrunclient.Database.LocalCartDataSource;
import com.example.beerrunclient.EventBus.CounterCartEvent;
import com.example.beerrunclient.EventBus.HideFABCart;
import com.example.beerrunclient.EventBus.MenuItemBack;
import com.example.beerrunclient.EventBus.UpdateItemInCart;
import com.example.beerrunclient.Model.AddonModel;
import com.example.beerrunclient.Model.BraintreeTransaction;
import com.example.beerrunclient.Model.CategoryModel;
import com.example.beerrunclient.Model.FCMResponse;
import com.example.beerrunclient.Model.FCMSendData;
import com.example.beerrunclient.Model.FoodModel;
import com.example.beerrunclient.Model.OrderModel;
import com.example.beerrunclient.Model.SizeModel;
import com.example.beerrunclient.R;
import com.example.beerrunclient.Remote.ICloudFunctions;
import com.example.beerrunclient.Remote.RetrofitCloudClient;
import com.example.beerrunclient.Remote.RetrofitFCMClient;
import com.example.beerrunclient.Services.IFCMService;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.android.libraries.places.widget.AutocompleteSupportFragment;
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.karumi.dexter.Dexter;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.IOException;
import java.sql.Time;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.jar.Manifest;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;
import io.reactivex.Single;
import io.reactivex.SingleObserver;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.observers.DisposableSingleObserver;
import io.reactivex.schedulers.Schedulers;

public class CartFragment extends Fragment implements ILoadTimeFromFirebaseListener, ISearchCategoryCallbackListener, TextWatcher {

    private static final int REQUEST_BRAINTREE_CODE = 7777;
    private CartViewModel cartViewModel;
    private CompositeDisposable compositeDisposable = new CompositeDisposable();

    private ISearchCategoryCallbackListener searchCategoryCallbackListener;

    private BottomSheetDialog addonBottomSheetDialog;
    private ChipGroup chip_group_addon, chip_group_user_selected_addon;
    private EditText edt_search;

    private Place placeSelected;
    private AutocompleteSupportFragment places_fragment;
    private PlacesClient placesClient;
    private List<Place.Field> placeFields = Arrays.asList(Place.Field.ID,
            Place.Field.NAME,
            Place.Field.ADDRESS,
            Place.Field.LAT_LNG);

    LocationRequest locationRequest;
    LocationCallback locationCallback;
    FusedLocationProviderClient fusedLocationProviderClient;
    Location currentLocation;

    String address, comment;
    ICloudFunctions cloudFunctions;
    IFCMService ifcmService;
    ILoadTimeFromFirebaseListener listener;

    private Parcelable recyclerViewState;
    private CartDataSource cartDataSource;
    private MyCartAdapter adapter;
    @BindView(R.id.recycler_cart)
    RecyclerView recycler_cart;
    @BindView(R.id.txt_total_price)
    TextView txt_total_price;
    @BindView(R.id.txt_empty_cart)
    TextView txt_empty_cart;
    @BindView(R.id.group_place_holder)
    CardView group_place_holder;

    @OnClick(R.id.btn_place_order)
    void onPlaceOrderClick() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Shipping & Payment");
        View view = LayoutInflater.from(getContext()).inflate(R.layout.layout_place_order, null);
        RadioButton rdi_home_address = view.findViewById(R.id.rdi_home_address);
        RadioButton rdi_other_address = view.findViewById(R.id.rdi_other_address);
        RadioButton rdi_ship_this_address = view.findViewById(R.id.rdi_ship_this_address);
        RadioButton rdi_cod = view.findViewById(R.id.rdi_cod);
        RadioButton rdi_braintree = view.findViewById(R.id.rdi_braintree);
        EditText edt_comment = view.findViewById(R.id.edt_comment);
        TextView txt_address = view.findViewById(R.id.txt_address_detail);
        places_fragment = (AutocompleteSupportFragment) (getActivity()).getSupportFragmentManager()
                .findFragmentById(R.id.places_autocomplete_fragment);
        assert places_fragment != null;
        places_fragment.setPlaceFields(placeFields);
        places_fragment.setCountries("ZA"); //if crash comment out
        places_fragment.setOnPlaceSelectedListener(new PlaceSelectionListener() {
            @Override
            public void onPlaceSelected(@NonNull Place place) {
                placeSelected = place;
                txt_address.setText(place.getAddress());
            }

            @Override
            public void onError(@NonNull Status status) {
                Toast.makeText(getContext(), "" + status.getStatusMessage(), Toast.LENGTH_SHORT).show();
            }
        });

        //data
        txt_address.setText(Common.currentUser.getAddress()); //default Home Address

        //event
        rdi_home_address.setOnCheckedChangeListener((compoundButton, b) -> {
            if (b) {
                txt_address.setText(Common.currentUser.getAddress());
                txt_address.setVisibility(View.VISIBLE);
                places_fragment.setHint(Common.currentUser.getAddress());
            }
        });
        rdi_other_address.setOnCheckedChangeListener((compoundButton, b) -> {
            if (b) {
                txt_address.setVisibility(View.VISIBLE);
            }
        });
        rdi_ship_this_address.setOnCheckedChangeListener((compoundButton, b) -> {
            if (b) {
                fusedLocationProviderClient.getLastLocation()
                        .addOnFailureListener(e -> {
                            Toast.makeText(getContext(), "" + e.getMessage(), Toast.LENGTH_SHORT).show();
                            txt_address.setVisibility(View.GONE);
                        })
                        .addOnCompleteListener(task -> {
                            String coordinates = new StringBuilder().append(task.getResult().getLatitude())
                                    .append("/")
                                    .append(task.getResult().getLongitude()).toString();

                            Single<String> singleAddress = Single.just(getAddressFromLatLng(task.getResult().getLatitude(),
                                    task.getResult().getLongitude()));

                            Disposable disposable = singleAddress.subscribeWith(new DisposableSingleObserver<String>() {

                                @Override
                                public void onSuccess(String s) {
                                    txt_address.setText(s);
                                    txt_address.setVisibility(View.VISIBLE);
                                    places_fragment.setHint(s);
                                }

                                @Override
                                public void onError(Throwable e) {
                                    txt_address.setText(e.getMessage());
                                    txt_address.setVisibility(View.VISIBLE);
                                }
                            });

                        });
            }
        });


        builder.setView(view);
        builder.setNegativeButton("CANCEL", (dialog, which) -> {
            dialog.dismiss();
        }).setPositiveButton("CONFIRM", (dialog, which) -> {
            if (rdi_cod.isChecked()) {
                paymentCOD(txt_address.getText().toString(), edt_comment.getText().toString());
            }
            if (rdi_braintree.isChecked()) {
                address = txt_address.getText().toString();
                comment = edt_comment.getText().toString();
                if (!TextUtils.isEmpty(Common.currentToken)) {
                    DropInRequest dropInRequest = new DropInRequest().clientToken(Common.currentToken);
                    startActivityForResult(dropInRequest.getIntent(getContext()), REQUEST_BRAINTREE_CODE);
                }
            }
        });
        AlertDialog dialog = builder.create();
        dialog.setOnDismissListener(dialogInterface -> {
            //fix crash duplicate order
            if (places_fragment != null) {
                getActivity().getSupportFragmentManager()
                        .beginTransaction().remove(places_fragment)
                        .commit();
            }
        });
        dialog.show();

    }

    private void paymentCOD(String address, String comment) {
        compositeDisposable.add(cartDataSource.getAllCart(Common.currentUser.getUid(), Common.currentRestaurant.getUid())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(cartItems -> {

                    cartDataSource.sumPriceInCart(Common.currentUser.getUid(), Common.currentRestaurant.getUid())
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(new SingleObserver<Double>() {
                                @Override
                                public void onSubscribe(Disposable d) {

                                }

                                @Override
                                public void onSuccess(Double totalPrice) {
                                    double finalPrice = totalPrice; // we will modify this for discount later
                                    OrderModel orderModel = new OrderModel();
                                    orderModel.setUserId(Common.currentUser.getUid());
                                    orderModel.setUserName(Common.currentUser.getName());
                                    orderModel.setUserPhone(Common.currentUser.getPhone());
                                    orderModel.setShippingAddress(address);
                                    orderModel.setComment(comment);
                                    if (currentLocation != null) {
                                        orderModel.setLat(currentLocation.getLatitude());
                                        orderModel.setLng(currentLocation.getLongitude());
                                    } else {
                                        orderModel.setLat(-0.1f);
                                        orderModel.setLng(-0.1f);
                                    }

                                    orderModel.setCartItemList(cartItems);
                                    orderModel.setTotalPayment(totalPrice);
                                    orderModel.setDiscount(0);// modify later
                                    orderModel.setFinalPayment(finalPrice);
                                    orderModel.setCod(true);
                                    orderModel.setTransactionId("Cash On Delivery");

                                    //
                                    synchLocalTimeWithGlobalTime(orderModel);
                                    //writeOrderToFirebase(orderModel);// synchlocaltime already writes the model to firebase. no need to do it twice
                                }

                                @Override
                                public void onError(Throwable e) {
                                    if (!e.getMessage().contains("Query returned empty result set"))
                                        Toast.makeText(getContext(), "paymentCod line333" + e.getMessage(), Toast.LENGTH_SHORT).show();
                                }
                            });


                }, throwable -> {
                    Toast.makeText(getContext(), "paymentCod line339" + throwable.getMessage(), Toast.LENGTH_SHORT).show();
                }));

    }

    private void synchLocalTimeWithGlobalTime(OrderModel orderModel) {
        final DatabaseReference offsetRef = FirebaseDatabase.getInstance().getReference(".info/serverTimeOffset");
        offsetRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                long offset = dataSnapshot.getValue(Long.class);
                long estimatedServerTimeMs = System.currentTimeMillis() + offset;
                SimpleDateFormat sdf = new SimpleDateFormat("MMM dd,yyyy HH:mm");
                Date resultDate = new Date(estimatedServerTimeMs);
                //Date resultDate = new Date(estimatedServerTimeMs);
                Log.d("TEST_DATE", "" + sdf.format(resultDate));

                listener.onLoadTimeSuccess(orderModel, estimatedServerTimeMs);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                listener.onLoadTimeFailed(databaseError.getMessage());
            }
        });
    }

    private void writeOrderToFirebase(OrderModel order) {

        FirebaseDatabase.getInstance()
                .getReference(Common.RESTAURANT_REF)
                .child(Common.currentRestaurant.getUid())
                .child(Common.ORDER_REF)
                .child(Common.createOrderNumber())//create order number with only digits
                .setValue(order)
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "write order to firebase lin 458" + e.getMessage(), Toast.LENGTH_SHORT).show();
                })
                .addOnCompleteListener(task -> {
                    //write successful
                    cartDataSource.cleanCart(Common.currentUser.getUid(), Common.currentRestaurant.getUid())
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(
                                    new SingleObserver<Integer>() {
                                        @Override
                                        public void onSubscribe(Disposable d) {

                                        }

                                        @Override
                                        public void onSuccess(Integer integer) {

                                            Map<String, String> notiData = new HashMap<>();
                                            notiData.put(Common.NOTI_TITLE, "New Order");
                                            notiData.put(Common.NOTI_CONTENT, "You have a new Order to fulfill from " + Common.currentUser.getPhone());
                                            FCMSendData sendData = new FCMSendData(Common.createTopicOrder(), notiData);
                                            compositeDisposable.add(ifcmService.sendNotification(sendData)
                                                    .subscribeOn(Schedulers.io())
                                                    .observeOn(AndroidSchedulers.mainThread())
                                                    .subscribe(
                                                            fcmResponse -> {
                                                                Toast.makeText(getContext(), "Order Placed Successfully", Toast.LENGTH_SHORT).show();
                                                                EventBus.getDefault().postSticky(new CounterCartEvent(true));

                                                            }, throwable ->
                                                            {
                                                                Toast.makeText(getContext(), "Order Placed but notification did not send", Toast.LENGTH_SHORT).show();
                                                                EventBus.getDefault().postSticky(new CounterCartEvent(true));
                                                            }
                                                    ));
                                        }

                                        @Override
                                        public void onError(Throwable e) {
                                            Toast.makeText(getContext(), "write order to firebase [clean cart] line 396" + e.getMessage(), Toast.LENGTH_SHORT).show();

                                        }
                                    }
                            );
                });
    }

    private String getAddressFromLatLng(double latitude, double longitude) {
        Geocoder geocoder = new Geocoder(getContext(), Locale.getDefault());
        String result = "";
        try {
            List<Address> addressList = geocoder.getFromLocation(latitude, longitude, 1);
            if (addressList != null && addressList.size() > 0) {
                Address address = addressList.get(0);// always get first item
                StringBuilder sb = new StringBuilder(address.getAddressLine(0));
                result = sb.toString();
            } else {
                result = "Address not found";

            }
        } catch (IOException e) {
            e.printStackTrace();
            result = e.getMessage();
        }
        return result;
    }

    private Unbinder unbinder;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {


        cartViewModel =
                new ViewModelProvider(this).get(CartViewModel.class);
        View root = inflater.inflate(R.layout.fragment_cart, container, false);

        ifcmService = RetrofitFCMClient.getInstance().create(IFCMService.class);

        cloudFunctions = RetrofitCloudClient.getInstance(Common.currentRestaurant.getPaymentUrl()).create(ICloudFunctions.class);
        listener = this;
        cartViewModel.initCartDataSource(getContext());
        cartViewModel.getMutableLiveDataCartItems().observe(getViewLifecycleOwner(), new Observer<List<CartItem>>() {
            @Override
            public void onChanged(List<CartItem> cartItems) {
                if (cartItems == null || cartItems.isEmpty()) {
                    recycler_cart.setVisibility(View.GONE);
                    group_place_holder.setVisibility(View.GONE);
                    txt_empty_cart.setVisibility(View.VISIBLE);

                } else {
                    recycler_cart.setVisibility(View.VISIBLE);
                    group_place_holder.setVisibility(View.VISIBLE);
                    txt_empty_cart.setVisibility(View.GONE);
                    adapter = new MyCartAdapter(getContext(), cartItems);
                    recycler_cart.setAdapter(adapter);
                }
            }
        });
        unbinder = ButterKnife.bind(this, root);
        initViews();
        initLocation();
        return root;
    }

    private void initLocation() {
        buildLocationRequest();
        buildLocationCallback();
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(getContext());
        fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
    }

    private void buildLocationCallback() {
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                super.onLocationResult(locationResult);
                currentLocation = locationResult.getLastLocation();
            }
        };

    }

    private void buildLocationRequest() {
        locationRequest = new LocationRequest();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(5000);
        locationRequest.setFastestInterval(3000);
        locationRequest.setSmallestDisplacement(10f);

    }

    private void initViews() {

        searchCategoryCallbackListener = this;

        initPlaceClient();

        setHasOptionsMenu(true);
        cartDataSource = new LocalCartDataSource(CartDatabase.getInstance(getContext()).cartDAO());
        EventBus.getDefault().postSticky(new HideFABCart(true));
        recycler_cart.setHasFixedSize(true);
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        recycler_cart.setLayoutManager(layoutManager);
        recycler_cart.addItemDecoration(new DividerItemDecoration(getContext(), layoutManager.getOrientation()));

        MySwipeHelper mySwipeHelper = new MySwipeHelper(getContext(), recycler_cart, 200) {
            @Override
            public void instantiateMyButton(RecyclerView.ViewHolder viewHolder, List<MyButton> buf) {
                buf.add(new MyButton(getContext(), "Delete", 30, 0, Color.parseColor("#FF3C30"),
                        pos -> {
                            CartItem cartItem = adapter.getItemAtPosition(pos);
                            cartDataSource.deleteCartItem(cartItem)
                                    .subscribeOn(Schedulers.io())
                                    .observeOn(AndroidSchedulers.mainThread())
                                    .subscribe(
                                            new SingleObserver<Integer>() {
                                                @Override
                                                public void onSubscribe(Disposable d) {

                                                }

                                                @Override
                                                public void onSuccess(Integer integer) {
                                                    adapter.notifyItemRemoved(pos);
                                                    sumAllItemInCart();// update total price;
                                                    EventBus.getDefault().postSticky(new CounterCartEvent(true)); //update fab
                                                    Toast.makeText(getContext(), "Deleted item successfully", Toast.LENGTH_SHORT).show();
                                                }

                                                @Override
                                                public void onError(Throwable e) {
                                                    Toast.makeText(getContext(), "" + e.getMessage(), Toast.LENGTH_SHORT).show();
                                                }
                                            }
                                    );
                        }));
                buf.add(new MyButton(getContext(), "Update", 30, 0, Color.parseColor("#5D4037"),
                        pos -> {
                            CartItem cartItem = adapter.getItemAtPosition(pos);
                            FirebaseDatabase.getInstance()
                                    .getReference(Common.RESTAURANT_REF)
                                    .child(Common.currentRestaurant.getUid())
                                    .child(Common.CATEGORY_REF)
                                    .child(cartItem.getCategoryId())
                                    .addListenerForSingleValueEvent(new ValueEventListener() {
                                        @Override
                                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                            if (dataSnapshot.exists()) {
                                                CategoryModel categoryModel = dataSnapshot.getValue(CategoryModel.class);
                                                searchCategoryCallbackListener.onSearchCategoryFound(categoryModel, cartItem);
                                            } else {
                                                searchCategoryCallbackListener.onSearchCategoryNotFound("Category not found");
                                            }
                                        }

                                        @Override
                                        public void onCancelled(@NonNull DatabaseError databaseError) {
                                            searchCategoryCallbackListener.onSearchCategoryNotFound(databaseError.getMessage());

                                        }
                                    });
                        }));
            }
        };
        sumAllItemInCart();

        //Addon
        addonBottomSheetDialog = new BottomSheetDialog(getContext(), R.style.DialogStyle);
        View layout_addon_display = getLayoutInflater().inflate(R.layout.layout_addon_display, null);
        chip_group_addon = (ChipGroup) layout_addon_display.findViewById(R.id.chip_group_addon);
        edt_search = (EditText) layout_addon_display.findViewById(R.id.edt_search);
        addonBottomSheetDialog.setContentView(layout_addon_display);

        addonBottomSheetDialog.setOnDismissListener(dialogInterface -> {
            displayUserSelectedAddon(chip_group_user_selected_addon);
            calculateTotalPrice();

        });
    }

    private void displayUserSelectedAddon(ChipGroup chip_group_user_selected_addon) {
        if (Common.selectedFood.getUserSelectedAddon() != null && Common.selectedFood.getUserSelectedAddon().size() > 0) {
            chip_group_user_selected_addon.removeAllViews();
            for (AddonModel addonModel : Common.selectedFood.getUserSelectedAddon()) {
                Chip chip = (Chip) getLayoutInflater().inflate(R.layout.layout_chip_with_delete_icon, null);
                chip.setText(new StringBuilder(addonModel.getName()).append("(+R")
                        .append(addonModel.getPrice()).append(")"));
                chip.setOnCheckedChangeListener((compoundButton, b) -> {
                    if (b) {
                        if (Common.selectedFood.getUserSelectedAddon() == null)
                            Common.selectedFood.setUserSelectedAddon(new ArrayList<>());
                        Common.selectedFood.getUserSelectedAddon().add(addonModel);

                    }
                });
                chip_group_user_selected_addon.addView(chip);
            }
        } else
            chip_group_user_selected_addon.removeAllViews();
    }

    private void initPlaceClient() {
        Places.initialize(getContext(), getString(R.string.google_maps_key));
        placesClient = Places.createClient(getContext());

    }

    private void sumAllItemInCart() {
        cartDataSource.sumPriceInCart(Common.currentUser.getUid(),Common.currentRestaurant.getUid())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new SingleObserver<Double>() {
                    @Override
                    public void onSubscribe(Disposable d) {

                    }

                    @Override
                    public void onSuccess(Double aDouble) {
                        txt_total_price.setText(new StringBuilder().append("Total: R").append(aDouble));
                    }

                    @Override
                    public void onError(Throwable e) {
                        if (!e.getMessage().contains("Query returned empty result set"))
                            Toast.makeText(getContext(), "" + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    @Override
    public void onPrepareOptionsMenu(@NonNull Menu menu) {
        menu.findItem(R.id.action_settings).setVisible(false);//hide home menu already inflated
        super.onPrepareOptionsMenu(menu);
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        inflater.inflate(R.menu.cart_menu, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_clear_cart) {
            cartDataSource.cleanCart(Common.currentUser.getUid(),Common.currentRestaurant.getUid())
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(
                            new SingleObserver<Integer>() {
                                @Override
                                public void onSubscribe(Disposable d) {

                                }

                                @Override
                                public void onSuccess(Integer integer) {
                                    Toast.makeText(getContext(), "Clear Cart Successful", Toast.LENGTH_SHORT).show();
                                    EventBus.getDefault().postSticky(new CounterCartEvent(true));

                                }

                                @Override
                                public void onError(Throwable e) {
                                    Toast.makeText(getContext(), "" + e.getMessage(), Toast.LENGTH_SHORT).show();
                                }
                            }
                    );
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onStart() {
        super.onStart();
        if (!EventBus.getDefault().isRegistered(this))
            EventBus.getDefault().register(this);
    }

    @Override
    public void onStop() {
        EventBus.getDefault().removeAllStickyEvents();
        EventBus.getDefault().postSticky(new HideFABCart(false));
        EventBus.getDefault().postSticky(new CounterCartEvent(false));
        cartViewModel.onStop();
        if (EventBus.getDefault().isRegistered(this))
            EventBus.getDefault().unregister(this);
        if (fusedLocationProviderClient != null)
            fusedLocationProviderClient.removeLocationUpdates(locationCallback);
        compositeDisposable.clear();
        super.onStop();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (fusedLocationProviderClient != null)
            fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    public void onUpdateItemInCartEvent(UpdateItemInCart event) {
        if (event.getCartItem() != null) {
            //save state first
            recyclerViewState = recycler_cart.getLayoutManager().onSaveInstanceState();
            cartDataSource.updateCartItems(event.getCartItem())
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new SingleObserver<Integer>() {
                        @Override
                        public void onSubscribe(Disposable d) {

                        }

                        @Override
                        public void onSuccess(Integer integer) {
                            calculateTotalPrice();
                            recycler_cart.getLayoutManager().onRestoreInstanceState(recyclerViewState); //fix error refresh recycler view after update
                        }

                        @Override
                        public void onError(Throwable e) {
                            Toast.makeText(getContext(), "[UPDATE CART]" + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    }

    private void calculateTotalPrice() {
        cartDataSource.sumPriceInCart(Common.currentUser.getUid(),Common.currentRestaurant.getUid())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new SingleObserver<Double>() {
                    @Override
                    public void onSubscribe(Disposable d) {

                    }

                    @Override
                    public void onSuccess(Double price) {
                        txt_total_price.setText(new StringBuilder("Total: R").append(Common.formatPrice(price)));
                    }

                    @Override
                    public void onError(Throwable e) {
                        if (!e.getMessage().contains("Query returned empty result set"))
                            Toast.makeText(getContext(), "" + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });

    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_BRAINTREE_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                DropInResult result = data.getParcelableExtra(DropInResult.EXTRA_DROP_IN_RESULT);
                PaymentMethodNonce nonce = result.getPaymentMethodNonce();

                //calculate sum cart
                cartDataSource.sumPriceInCart(Common.currentUser.getUid(),Common.currentRestaurant.getUid())
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(new SingleObserver<Double>() {
                            @Override
                            public void onSubscribe(Disposable d) {

                            }

                            @Override
                            public void onSuccess(Double totalPrice) {

                                //get all item in cart
                                compositeDisposable.add(cartDataSource.getAllCart(Common.currentUser.getUid(),Common.currentRestaurant.getUid())
                                        .subscribeOn(Schedulers.io())
                                        .observeOn(AndroidSchedulers.mainThread())
                                        .subscribe(cartItems -> {

                                            //submit payment
                                            Map<String, String> headers = new HashMap<>();
                                            headers.put("Authorization", Common.buildToken(Common.authorizeKey));
                                            compositeDisposable.add(cloudFunctions.submitPayment(headers,totalPrice, nonce.getNonce())
                                                    .subscribeOn(Schedulers.io())
                                                    .observeOn(AndroidSchedulers.mainThread())
                                                    .subscribe(braintreeTransaction -> {

                                                        if (braintreeTransaction.isSuccess())
                                                        {
                                                            double finalPrice = totalPrice; // we will modify this for discount later
                                                            OrderModel orderModel = new OrderModel();
                                                            orderModel.setUserId(Common.currentUser.getUid());
                                                            orderModel.setUserName(Common.currentUser.getName());
                                                            orderModel.setUserPhone(Common.currentUser.getPhone());
                                                            orderModel.setShippingAddress(address);
                                                            orderModel.setComment(comment);
                                                            if (currentLocation != null) {
                                                                orderModel.setLat(currentLocation.getLatitude());
                                                                orderModel.setLng(currentLocation.getLongitude());
                                                            } else {
                                                                orderModel.setLat(-0.1f);
                                                                orderModel.setLng(-0.1f);
                                                            }
                                                            orderModel.setCartItemList(cartItems);
                                                            orderModel.setTotalPayment(totalPrice);
                                                            orderModel.setDiscount(0);// modify later
                                                            orderModel.setFinalPayment(finalPrice);
                                                            orderModel.setCod(false);
                                                            orderModel.setTransactionId(braintreeTransaction.getTransaction().getId());

                                                            synchLocalTimeWithGlobalTime(orderModel);
                                                            //writeOrderToFirebase(orderModel); // synchlocaltime already writes the model to firebase. no need to do it twice

                                                           }
                                                    }, throwable -> {
                                                        if (!throwable.getMessage().contains("Query returned empty result set"))
                                                            Toast.makeText(getContext(), "onActivityResult submitPayment line 810"+throwable.getMessage(), Toast.LENGTH_SHORT).show();
                                                    })
                                            );
                                        }, throwable -> Toast.makeText(getContext(), "onActivityResult getall line 813" + throwable.getMessage(), Toast.LENGTH_SHORT).show()));
                            }

                            @Override
                            public void onError(Throwable e) {
                                if (!e.getMessage().contains("Query returned empty result set"))
                                    Toast.makeText(getContext(), "onActivityResult sumcart line 818" + e.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        });

            }
        }
    }

    @Override
    public void onLoadTimeSuccess(OrderModel orderModel, long estimatedTimeInMs) {
        orderModel.setCreateDate(estimatedTimeInMs);
        orderModel.setOrderStatus(0);//orderModel placed status
        writeOrderToFirebase(orderModel);
    }

    @Override
    public void onLoadOnlyTimeSuccess(long estimatedTimeInMs) {
        // do nothing
    }

    @Override
    public void onLoadTimeFailed(String message) {
        Toast.makeText(getContext(), "" + message, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onDestroy() {
        EventBus.getDefault().postSticky(new MenuItemBack());
        super.onDestroy();

    }


    @Override
    public void onSearchCategoryFound(CategoryModel categoryModel, CartItem cartItem) {
        FoodModel foodModel = Common.findFoodInListById(categoryModel, cartItem.getFoodId());
        if (foodModel != null) {
            showUpdateDialog(cartItem, foodModel);

        } else {
            Toast.makeText(getContext(), "Food Id not Found", Toast.LENGTH_SHORT).show();
        }
    }

    private void showUpdateDialog(CartItem cartItem, FoodModel foodModel) {
        Common.selectedFood = foodModel;
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        View itemView = LayoutInflater.from(getContext()).inflate(R.layout.layout_dialog_update_cart, null);
        builder.setView(itemView);
        //View
        Button btn_ok = (Button) itemView.findViewById(R.id.btn_ok);
        Button btn_cancel = (Button) itemView.findViewById(R.id.btn_cancel);

        RadioGroup rdi_group_size = (RadioGroup) itemView.findViewById(R.id.rdi_group_size);

        chip_group_user_selected_addon = (ChipGroup) itemView.findViewById(R.id.chip_group_user_selected_addon);
        ImageView img_add_on = (ImageView) itemView.findViewById(R.id.img_add_addon);
        img_add_on.setOnClickListener(view -> {
            if (foodModel.getAddon() != null) {
                displayAddonList();
                addonBottomSheetDialog.show();
            }
        });

        //size
        if (foodModel.getSize() != null) {
            for (SizeModel sizeModel : foodModel.getSize()) {
                RadioButton radioButton = new RadioButton(getContext());
                radioButton.setOnCheckedChangeListener((compoundButton, b) -> {
                    if (b)
                        Common.selectedFood.setUserSelectedSize(sizeModel);
                    calculateTotalPrice();

                });

                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1.0f);
                radioButton.setLayoutParams(params);
                radioButton.setText(sizeModel.getName());
                radioButton.setTag(sizeModel.getPrice());
                rdi_group_size.addView(radioButton);
            }

            if (rdi_group_size.getChildCount() > 0) {
                RadioButton radioButton = (RadioButton) rdi_group_size.getChildAt(0); //get first radio button
                radioButton.setChecked(true); //Set default at first radio button

            }
        }

        //addon
        displayAlreadySelectedAddon(chip_group_user_selected_addon, cartItem);

        //show dialog
        AlertDialog dialog = builder.create();
        dialog.show();
        //custom dialog
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialog.getWindow().setGravity(Gravity.CENTER);
        //event
        btn_ok.setOnClickListener(view -> {
            //first delete item in cart
            cartDataSource.deleteCartItem(cartItem)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(
                            new SingleObserver<Integer>() {
                                @Override
                                public void onSubscribe(Disposable d) {

                                }

                                @Override
                                public void onSuccess(Integer integer) {
                                    //after that, update information and add new
                                    //Update price and info
                                    if (Common.selectedFood.getUserSelectedAddon() != null)
                                        cartItem.setFoodAddon(new Gson().toJson(Common.selectedFood.getUserSelectedAddon()));
                                    else
                                        cartItem.setFoodAddon("Default");
                                    if (Common.selectedFood.getUserSelectedSize() != null)
                                        cartItem.setFoodSize(new Gson().toJson(Common.selectedFood.getUserSelectedSize()));
                                    else
                                        cartItem.setFoodSize("Default");

                                    cartItem.setFoodExtraPrice(Common.calculateExtraPrice(Common.selectedFood.getUserSelectedSize(),
                                            Common.selectedFood.getUserSelectedAddon()));

                                    //insert new
                                    compositeDisposable.add(cartDataSource.insertOrReplaceAll(cartItem)
                                            .subscribeOn(Schedulers.io())
                                            .observeOn(AndroidSchedulers.mainThread())
                                            .subscribe(() -> {
                                                EventBus.getDefault().postSticky(new CounterCartEvent(true)); //count cart again
                                                calculateTotalPrice();
                                                dialog.dismiss();
                                                Toast.makeText(getContext(), "Update Cart Successful", Toast.LENGTH_SHORT).show();
                                            }, throwable -> {
                                                Toast.makeText(getContext(), "" + throwable.getMessage(), Toast.LENGTH_SHORT).show();
                                            })
                                    );
                                }

                                @Override
                                public void onError(Throwable e) {
                                    Toast.makeText(getContext(), "" + e.getMessage(), Toast.LENGTH_SHORT).show();
                                }
                            }
                    );
        });
        btn_cancel.setOnClickListener(view -> {
            dialog.dismiss();
        });


    }

    private void displayAlreadySelectedAddon(ChipGroup chip_group_user_selected_addon, CartItem cartItem) {
        //this method will display all addon we already select before add to cart and display on layout
        if (cartItem.getFoodAddon() != null && !cartItem.getFoodAddon().equals("Default")) {
            List<AddonModel> addonModels = new Gson().fromJson(
                    cartItem.getFoodAddon(), new TypeToken<List<AddonModel>>() {
                    }.getType());
            Common.selectedFood.setUserSelectedAddon(addonModels);
            chip_group_user_selected_addon.removeAllViews();
            //Add all view
            for (AddonModel addonModel :
                    addonModels) {
                Chip chip = (Chip) getLayoutInflater().inflate(R.layout.layout_chip_with_delete_icon, null);
                chip.setText(new StringBuilder(addonModel.getName()).append("(+R")
                        .append(addonModel.getPrice()).append(")"));
                chip.setClickable(false);
                chip.setOnCloseIconClickListener(view -> {
                    //remove when user selected delete
                    chip_group_user_selected_addon.removeView(view);
                    Common.selectedFood.getUserSelectedAddon().remove(addonModel);
                    calculateTotalPrice();
                });
                chip_group_user_selected_addon.addView(chip);
            }
        }
    }

    private void displayAddonList() {
        if (Common.selectedFood.getAddon() != null && Common.selectedFood.getAddon().size() > 0) {
            chip_group_addon.clearCheck();
            chip_group_addon.removeAllViews();

            edt_search.addTextChangedListener(this);

            //Add all view
            for (AddonModel addonModel : Common.selectedFood.getAddon()) {
                Chip chip = (Chip) getLayoutInflater().inflate(R.layout.layout_addon_item, null);
                chip.setText(new StringBuilder(addonModel.getName()).append("(+R")
                        .append(addonModel.getPrice()).append(")"));
                chip.setOnCheckedChangeListener((compoundButton, b) -> {
                    if (b) {
                        if (Common.selectedFood.getUserSelectedAddon() == null)
                            Common.selectedFood.setUserSelectedAddon(new ArrayList<>());
                        Common.selectedFood.getUserSelectedAddon().add(addonModel);

                    }
                });
                chip_group_addon.addView(chip);
            }
        }
    }

    @Override
    public void onSearchCategoryNotFound(String message) {
        Toast.makeText(getContext(), "" + message, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

    }

    @Override
    public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
        chip_group_addon.clearCheck();
        chip_group_addon.removeAllViews();
        for (AddonModel addonModel : Common.selectedFood.getAddon()) {
            if (addonModel.getName().toLowerCase().contains(charSequence.toString().toLowerCase())) {
                Chip chip = (Chip) getLayoutInflater().inflate(R.layout.layout_addon_item, null);
                chip.setText(new StringBuilder(addonModel.getName()).append("(+R")
                        .append(addonModel.getPrice()).append(")"));
                chip.setOnCheckedChangeListener((compoundButton, b) -> {
                    if (b) {
                        if (Common.selectedFood.getUserSelectedAddon() == null)
                            Common.selectedFood.setUserSelectedAddon(new ArrayList<>());
                        Common.selectedFood.getUserSelectedAddon().add(addonModel);

                    }
                });
                chip_group_addon.addView(chip);
            }
        }
    }

    @Override
    public void afterTextChanged(Editable s) {

    }
}