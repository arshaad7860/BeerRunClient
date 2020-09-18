package com.example.beerrunclient.Adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.LayoutAnimationController;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.andremion.counterfab.CounterFab;
import com.bumptech.glide.Glide;
import com.example.beerrunclient.Callback.IRecyclerClickListener;
import com.example.beerrunclient.Common.Common;
import com.example.beerrunclient.Database.CartDataSource;
import com.example.beerrunclient.Database.CartDatabase;
import com.example.beerrunclient.Database.CartItem;
import com.example.beerrunclient.Database.LocalCartDataSource;
import com.example.beerrunclient.EventBus.CounterCartEvent;
import com.example.beerrunclient.EventBus.FoodItemCick;
import com.example.beerrunclient.Model.FoodModel;
import com.example.beerrunclient.R;

import org.greenrobot.eventbus.EventBus;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import io.reactivex.SingleObserver;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class MyFoodListAdapter extends RecyclerView.Adapter<MyFoodListAdapter.MyViewHolder> {

    private Context context;
    private List<FoodModel> foodModelList;
    private CompositeDisposable compositeDisposable;
    private CartDataSource cartDataSource;

    public MyFoodListAdapter(Context context, List<FoodModel> foodModelList) {
        this.context = context;
        this.foodModelList = foodModelList;
        this.compositeDisposable = new CompositeDisposable();
        this.cartDataSource= new LocalCartDataSource(CartDatabase.getInstance(context).cartDAO());

    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new MyViewHolder(LayoutInflater.from(context)
        .inflate(R.layout.layout_food_item,parent,false));

    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
        Glide.with(context).load(foodModelList.get(position).getImage()).into(holder.img_food_image);
        holder.txt_food_price.setText(new StringBuilder("R")
        .append(foodModelList.get(position).getPrice())
        );
        holder.txt_food_name.setText(new StringBuilder(foodModelList.get(position)
                .getName()));

        //event when clicked
        holder.setListener((view, pos) -> {
            Common.selectedFood = foodModelList.get(pos);
            Common.selectedFood.setKey(String.valueOf(pos));
            EventBus.getDefault().postSticky(new FoodItemCick(true,foodModelList.get(pos)));
        });

        holder.img_cart.setOnClickListener(view -> {
            CartItem cartItem=new CartItem();
            cartItem.setRestaurantId(Common.currentRestaurant.getUid());
            cartItem.setUid(Common.currentUser.getUid());
            cartItem.setUserPhone(Common.currentUser.getPhone());

            cartItem.setCategoryId(Common.categorySelected.getMenu_id());
            cartItem.setFoodId(foodModelList.get(position).getId());
            cartItem.setFoodName(foodModelList.get(position).getName());
            cartItem.setFoodImage(foodModelList.get(position).getImage());
            cartItem.setFoodPrice(Double.valueOf(foodModelList.get(position).getPrice()));
            cartItem.setFoodQuantity(1);
            cartItem.setFoodExtraPrice(0.0); //coz no addon and default size selected
            cartItem.setFoodAddon("Default");
            cartItem.setFoodSize("Default");

            cartDataSource.getItemWithAllOpionsInCart(Common.currentUser.getUid(),
                    Common.categorySelected.getMenu_id(),
                    cartItem.getFoodId(),
                    cartItem.getFoodSize(),
                    cartItem.getFoodAddon(),Common.currentRestaurant.getUid())
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(
                            new SingleObserver<CartItem>() {
                                @Override
                                public void onSubscribe(Disposable d) {

                                }

                                @Override
                                public void onSuccess(CartItem cartItemFromDB) {
                                    if (cartItemFromDB.equals(cartItem))
                                    {cartItemFromDB.setFoodExtraPrice(cartItem.getFoodExtraPrice());
                                    cartItemFromDB.setFoodAddon(cartItem.getFoodAddon());
                                    cartItemFromDB.setFoodSize(cartItem.getFoodSize());
                                    cartItemFromDB.setFoodQuantity(cartItemFromDB.getFoodQuantity()+cartItem.getFoodQuantity());

                                    cartDataSource.updateCartItems(cartItemFromDB)
                                            .subscribeOn(Schedulers.io())
                                            .observeOn(AndroidSchedulers.mainThread())
                                            .subscribe(
                                                    new SingleObserver<Integer>() {
                                                        @Override
                                                        public void onSubscribe(Disposable d) {
                                                            
                                                        }

                                                        @Override
                                                        public void onSuccess(Integer integer) {
                                                            Toast.makeText(context, "Cart Updated Successfully", Toast.LENGTH_SHORT).show();
                                                            EventBus.getDefault().postSticky(new CounterCartEvent(true));
                                                        }

                                                        @Override
                                                        public void onError(Throwable e) {
                                                            Toast.makeText(context, "[UPDATE CART]"+e.getMessage(), Toast.LENGTH_SHORT).show();
                                                        }
                                                    }
                                            );
                                    }
                                    else
                                    {
                                        //Item not available in cart before ,insert new
                                        compositeDisposable.add(
                                                cartDataSource.insertOrReplaceAll(cartItem).subscribeOn(Schedulers.io())
                                                        .observeOn(AndroidSchedulers.mainThread())
                                                        .subscribe(()->{
                                                            Toast.makeText(context, "Added to cart Successfully", Toast.LENGTH_SHORT).show();
                                                            EventBus.getDefault().postSticky(new CounterCartEvent(true));
                                                            
                                                        },throwable -> Toast.makeText(context, "[CART ERROR]"+throwable.getMessage(), Toast.LENGTH_SHORT).show())
                                        );

                                    }
                                }

                                @Override
                                public void onError(Throwable e) {
                                    if (e.getMessage().contains("empty"))
                                    {
                                        //default if cart is empty
                                        compositeDisposable.add(
                                                cartDataSource.insertOrReplaceAll(cartItem).subscribeOn(Schedulers.io())
                                                        .observeOn(AndroidSchedulers.mainThread())
                                                        .subscribe(()->{
                                                            Toast.makeText(context, "Added to cart Successfully", Toast.LENGTH_SHORT).show();
                                                            EventBus.getDefault().postSticky(new CounterCartEvent(true));

                                                        },throwable -> Toast.makeText(context, "[CART ERROR]"+throwable.getMessage(), Toast.LENGTH_SHORT).show())
                                        );
                                    }
                                    else
                                        Toast.makeText(context, "[GET CART]"+e.getMessage(), Toast.LENGTH_SHORT).show();
                                }
                            }
                    );

        });

    }

    @Override
    public int getItemCount() {
        return foodModelList.size();
    }

    public class MyViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        private Unbinder unbinder;
        @BindView(R.id.txt_food_name)
        TextView txt_food_name;
        @BindView(R.id.txt_food_price)
        TextView txt_food_price;
        @BindView(R.id.img_food_image)
        ImageView img_food_image;
        @BindView(R.id.img_fav)
        ImageView img_fav;
        @BindView(R.id.img_quick_cart)
        ImageView img_cart;

        IRecyclerClickListener listener;

        public void setListener(IRecyclerClickListener listener) {
            this.listener = listener;
        }

        public MyViewHolder(@NonNull View itemView) {
            super(itemView);
            unbinder = ButterKnife.bind(this,itemView);
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
            listener.onItemClickListener(view,getAdapterPosition());
        }
    }
}
