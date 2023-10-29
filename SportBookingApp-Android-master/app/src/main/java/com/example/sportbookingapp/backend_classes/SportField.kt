package com.example.sportbookingapp.backend_classes

class SportField(
    private var id: String,
    private var name: String,
    private var imageUrl: String,
    private var sportCategory: String,
    private var price: Int,
    private var description: String
) {
    // Getters
    public fun getId(): String {
        return id
    }
    public fun getName(): String {
        return name
    }
    public fun getImageUrl(): String {
        return imageUrl
    }
    public fun getSportCategory(): String {
        return sportCategory
    }
    public fun getPrice(): Int {
        return price
    }
    public fun getDescription(): String {
        return description
    }
    // Setters
    public fun setId(id: String) {
        this.id = id
    }
    public fun setName(name: String) {
        this.name = name
    }
    public fun setImageUrl(imageUrl: String) {
        this.imageUrl = imageUrl
    }
    public fun setSportCategory(sportCategory: String) {
        this.sportCategory = sportCategory
    }
    public fun setPrice(price: Int) {
        this.price = price
    }
    public fun setDescription(description: String) {
        this.description = description
    }
}
