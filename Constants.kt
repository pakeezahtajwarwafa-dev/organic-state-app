package com.example.organicstate.utils

object Constants {
    // Firebase Collections
    const val COLLECTION_USERS = "users"
    const val COLLECTION_PRODUCTS = "products"
    const val COLLECTION_ORDERS = "orders"
    const val COLLECTION_NOTIFICATIONS = "notifications"
    const val COLLECTION_COMPLAINTS = "complaints"
    const val COLLECTION_REVIEWS = "reviews"

    // Order Status
    const val ORDER_PENDING = "pending"
    const val ORDER_CONFIRMED = "confirmed"
    const val ORDER_PROCESSING = "processing"
    const val ORDER_SHIPPED = "shipped"
    const val ORDER_DELIVERED = "delivered"
    const val ORDER_CANCELLED = "cancelled"

    // User Roles
    const val ROLE_CUSTOMER = "customer"
    const val ROLE_FARMER = "farmer"
    const val ROLE_ADMIN = "admin"

    // Notification Types
    const val NOTIFICATION_ORDER = "order"
    const val NOTIFICATION_PROMOTION = "promotion"
    const val NOTIFICATION_SYSTEM = "system"
    const val NOTIFICATION_GENERAL = "general"

    // Product Categories
    const val CATEGORY_VEGETABLES = "Vegetables"
    const val CATEGORY_FRUITS = "Fruits"
    const val CATEGORY_GRAINS = "Grains"
    const val CATEGORY_DAIRY = "Dairy"
    const val CATEGORY_SEEDS = "Seeds"
    const val CATEGORY_FERTILIZERS = "Fertilizers"

    // Request Codes
    const val REQUEST_IMAGE_PICK = 100
    const val REQUEST_CAMERA = 101
    const val REQUEST_PERMISSION = 200
}