package com.example.phoneshop;

public class ProductItem {
    private String name;
    private String storage;
    private String ram;
    private String price;
    private float rating;
    private int imageResource;


    public ProductItem() {}

    public ProductItem(String name, String storage, String ram, String price, float rating, int imageResource) {
        this.name = name;
        this.storage = storage;
        this.ram = ram;
        this.price = price;
        this.rating = rating;
        this.imageResource = imageResource;
    }

    public String getName() {
        return name;
    }

    public String getStorage() {
        return storage;
    }

    public String getRam() {
        return ram;
    }

    public String getPrice() {
        return price;
    }

    public float getRating() {
        return rating;
    }

    public int getImageResource() {
        return imageResource;
    }
}
