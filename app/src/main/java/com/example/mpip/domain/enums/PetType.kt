package com.example.mpip.domain.enums

enum class PetType(val displayName: String, val imageResource: String) {
    CAT("Cat", "pet_cat"),
    DOG("Dog", "pet_dog");

    companion object {
        fun fromString(type: String): PetType {
            return when (type.lowercase()) {
                "cat" -> CAT
                "dog" -> DOG
                else -> CAT
            }
        }
    }
}